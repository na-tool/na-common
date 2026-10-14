package com.na.common.autoconfigure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * na-common 的 Spring Boot 自动装配入口。
 *
 * <p>改造前，本模块的全部 Bean 只靠 {@code @EnableNA} 上的
 * {@code @ComponentScan(basePackages = "com.na")} 注册：宿主主类不在 {@code com.na} 包下、
 * 或忘了加 {@code @EnableNA} 时，整模块静默失效（不报错、不加载）。该扫描同时覆盖整个
 * {@code com.na} 命名空间，会把宿主与其它 na-* 模块的组件一并扫入，边界不可控。
 * 本类改为自动装配后，宿主不再依赖包名与该注解。
 *
 * <p><b>为什么注册走 BeanDefinitionRegistryPostProcessor 而不是 @Bean 方法</b>：
 * {@code @ConditionalOnMissingBean} 的让位依赖一个前提 —— 自动装配通过
 * {@code DeferredImportSelector} 在所有用户配置之后处理。但本类带 {@code @Configuration}
 * 元注解，宿主的组件扫描会把它当成普通配置类拾取；一旦被拾取，它就与宿主的 {@code @Bean}
 * 进入同一处理批次，顺序不再确定，于是：
 * <ul>
 *   <li>宿主声明同名 Bean → 启动直接抛 {@code BeanDefinitionOverrideException}
 *       （Spring Boot 2.1+ 默认禁止 Bean 覆盖）</li>
 *   <li>宿主用异名 Bean 提供同类型实现 → 不报错，两份实现静默共存</li>
 * </ul>
 * 注意不能用 {@code ImportBeanDefinitionRegistrar} 代替：它在
 * {@code ConfigurationClassBeanDefinitionReader} 内部逐个配置类调用，
 * 若本类排在宿主配置类之前，注册仍会先发生。只有
 * {@code BeanDefinitionRegistryPostProcessor} 是在 {@code ConfigurationClassPostProcessor}
 * 处理完全部配置类之后才被触发，此时宿主提供的 Bean 定义一定已经就位。
 *
 * <p><b>为什么被注册的类不再带 {@code @Component}/{@code @Configuration}</b>：它们是"注册手段"，
 * 注册已由本类接管；留着会被宿主的 {@code @ComponentScan("com.na")} 重复拾取。
 * 而 {@code @Aspect}、{@code @RestControllerAdvice}、{@code @ConfigurationProperties}、
 * {@code @PostConstruct}、{@code @Value}、{@code @ConditionalOnXxx} 是行为或语义注解，全部保留。
 */
@AutoConfiguration
@EnableTransactionManagement
public class NaCommonAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(NaCommonAutoConfiguration.class);

    /** Spring Boot 自动装配类的包前缀，用于识别"Boot 的默认实现" */
    private static final String BOOT_AUTO_CONFIGURATION_PACKAGE = "org.springframework.boot.autoconfigure";

    /** 容器提供的 Environment Bean 名 */
    private static final String MESSAGE_SOURCE_BEAN_NAME = "messageSource";

    /** Redis 相关类的类路径门禁 */
    private static final String REDIS_REQUIRED = "org.springframework.data.redis.core.RedisTemplate";

    /**
     * 注册时机控制器。用 static @Bean 声明，避免配置类因此被提前实例化。
     *
     * @return Bean 注册器
     */
    @Bean
    static BeanDefinitionRegistryPostProcessor naCommonBeanRegistrar() {
        return new NaCommonBeanRegistrar();
    }

    /**
     * 产品 Bean 的让位策略
     */
    enum Absent {
        /** 容器中已有该类型时不注册（宿主用异名 Bean 提供同类型实现也能让位） */
        BY_TYPE,
        /**
         * 仅当同名 Bean 已存在时不注册。
         * <p>用于类型会被"无关 Bean"占用的场景：例如 {@code WebMvcConfigurer} 上同时挂着
         * Spring Boot 自己的 WebMvcAutoConfigurationAdapter，按类型让位会让本模块整个不注册
         */
        BY_NAME,
        /**
         * 同名定义来自 Spring Boot 自动装配时移除它并由本模块接管；宿主自己声明的一律保留。
         * <p>只用于 {@code messageSource}：组件扫描时代本模块的配置属于"用户配置"、
         * 先于自动装配处理，因此总能注册成功并让 Boot 退让；注册器在**之后**运行，
         * 若按类型让位就会反过来被 Boot 的默认 messageSource 挡掉，静态 i18n 资源全部失效
         */
        REPLACE_BOOT_DEFAULT
    }

    /**
     * 一个配置类产出的 Bean
     *
     * @param beanName      注册用的 Bean 名
     * @param factoryMethod 工厂方法名（对应原 {@code @Bean} 方法）
     * @param beanType      产品类型 FQCN
     * @param absent        让位策略
     * @param qualifier     需要额外补回的 {@code @Qualifier} 元注解类型 FQCN，可为 null
     */
    record Product(String beanName, String factoryMethod, String beanType, Absent absent, String qualifier) {

        static Product of(String beanName, String factoryMethod, String beanType, Absent absent) {
            return new Product(beanName, factoryMethod, beanType, absent, null);
        }

        static Product qualified(String beanName, String factoryMethod, String beanType, String qualifier) {
            return new Product(beanName, factoryMethod, beanType, Absent.BY_TYPE, qualifier);
        }

        static Product byName(String beanName, String factoryMethod, String beanType) {
            return new Product(beanName, factoryMethod, beanType, Absent.BY_NAME, null);
        }
    }

    /**
     * 一个受管类的注册描述
     *
     * @param configClass  配置类 FQCN。全程用字符串：本模块依赖几乎全是 provided，
     *                     宿主可能完全没有 mybatis-plus / redisson / feign，
     *                     直接写 {@code XxxConfig.class} 会在类加载阶段就 NoClassDefFoundError
     * @param selfBeanName      配置类自身作为 Bean 注册时的名字；为 null 表示它只作为工厂
     * @param selfAbsent        自身作为 Bean 时的让位策略
     * @param requires          类路径门禁 FQCN，为 null 表示无
     * @param requiresBeanType  前置 Bean 门禁：容器中须存在该类型的 Bean，否则跳过注册；为 null 表示无
     * @param products          该配置类产出的 Bean（自身即产品时为空）
     */
    record Spec(String configClass, String selfBeanName, Absent selfAbsent, String requires,
                String requiresBeanType, List<Product> products) {

        /** 配置类自身就是 Bean，按 Bean 名让位 */
        static Spec self(String configClass, String selfBeanName) {
            return new Spec(configClass, selfBeanName, Absent.BY_NAME, null, null, List.of());
        }

        /** 配置类自身就是 Bean，按 Bean 名让位，且依赖某个可能缺失的类 */
        static Spec selfRequires(String configClass, String selfBeanName, String requires) {
            return new Spec(configClass, selfBeanName, Absent.BY_NAME, requires, null, List.of());
        }

        /**
         * 配置类自身就是 Bean，按类型让位。
         * <p>仅用于类型专属于本模块、宿主同类型 Bean 应当完全接管的场合（如 MetaObjectHandler）
         */
        static Spec selfByType(String configClass, String selfBeanName, String requires) {
            return new Spec(configClass, selfBeanName, Absent.BY_TYPE, requires, null, List.of());
        }

        /**
         * 配置类自身就是 Bean，但需要容器里先存在某个类型的 Bean，否则跳过注册。
         * <p>用于"依赖宿主提供 SPI 实现"的场景：宿主没提供时不应让它启动失败，
         * 而是跳过并给出明确提示
         *
         * @param requiresBeanType 前置 Bean 类型 FQCN
         */
        static Spec selfIfBeanPresent(String configClass, String selfBeanName, String requiresBeanType) {
            return new Spec(configClass, selfBeanName, Absent.BY_NAME, null, requiresBeanType, List.of());
        }

        /** 配置类自身是 Bean，同时又产出其它 Bean */
        static Spec selfAndFactory(String configClass, String selfBeanName, Product... products) {
            return new Spec(configClass, selfBeanName, Absent.BY_NAME, null, null, List.of(products));
        }

        /** 配置类只作为工厂 */
        static Spec factory(String configClass, String requires, Product... products) {
            return new Spec(configClass, null, Absent.BY_NAME, requires, null, List.of(products));
        }
    }

    /**
     * 在全部配置类处理完后注册 na-common 的 Bean，并让宿主提供的实现优先。
     */
    static class NaCommonBeanRegistrar implements BeanDefinitionRegistryPostProcessor {

        @Override
        public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
            // DefaultListableBeanFactory 同时实现 BeanDefinitionRegistry
            ConfigurableListableBeanFactory beanFactory = (ConfigurableListableBeanFactory) registry;
            ConditionContext context = NaConditions.newContext(beanFactory);

            for (Spec spec : specs()) {
                // 类路径门禁：缺失的 provided 依赖直接跳过，不尝试加载其类
                if (spec.requires() != null && !NaConditions.present(spec.requires())) {
                    log.debug("跳过 {}: 类路径缺少 {}", spec.configClass(), spec.requires());
                    continue;
                }
                // 前置 Bean 门禁：宿主没提供所需 SPI 时跳过并提示，不抛启动异常
                if (spec.requiresBeanType() != null
                        && !hasBeanOfType(beanFactory, NaConditions.resolve(spec.requiresBeanType()))) {
                    log.warn("跳过 {}: 容器中没有 {} 类型的 Bean, 该功能不会生效; "
                                    + "如需启用请提供其实现并注册为 Bean",
                            spec.configClass(), spec.requiresBeanType());
                    continue;
                }
                Class<?> configType = NaConditions.resolve(spec.configClass());
                // 条件求值：读取配置类上保留的 @ConditionalOnXxx，语义与处理 @Bean 方法时一致
                if (!NaConditions.matches(configType, context)) {
                    log.debug("跳过 {}: 条件不满足", spec.configClass());
                    continue;
                }
                registerSpec(registry, beanFactory, spec, configType);
            }
        }

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
            // 无需在 BeanFactory 层做额外处理
        }

        /**
         * 注册一个配置类及其全部产品
         *
         * @param registry    注册表
         * @param beanFactory Bean 工厂（用于按类型判断让位）
         * @param spec        注册描述
         * @param configType  配置类
         */
        private static void registerSpec(BeanDefinitionRegistry registry,
                                         ListableBeanFactory beanFactory,
                                         Spec spec,
                                         Class<?> configType) {
            // 1) 配置类自身也是 Bean
            if (spec.selfBeanName() != null && !shouldSkipSelf(registry, beanFactory, spec, configType)) {
                registry.registerBeanDefinition(spec.selfBeanName(), beanDefinition(configType));
            }

            // 2) 配置类作为工厂产出其它 Bean
            List<Product> pending = new ArrayList<>();
            for (Product product : spec.products()) {
                if (!shouldSkip(registry, beanFactory, product)) {
                    pending.add(product);
                }
            }
            if (pending.isEmpty()) {
                return;
            }
            // 工厂 Bean 用 FQCN 作 Bean 名：与 Spring 对 imported 配置类的命名惯例一致，
            // 且与宿主 Bean 名零碰撞概率（registerBeanDefinition 对同名是硬冲突）
            if (!registry.containsBeanDefinition(spec.configClass())) {
                registry.registerBeanDefinition(spec.configClass(), beanDefinition(configType));
            }
            for (Product product : pending) {
                RootBeanDefinition definition = new RootBeanDefinition(NaConditions.resolve(product.beanType()));
                definition.setFactoryBeanName(spec.configClass());
                definition.setFactoryMethodName(product.factoryMethod());
                // 与 Spring 处理 @Bean 方法的方式一致：这样工厂方法的参数会像真 @Bean 一样按类型注入
                definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
                // @LoadBalanced 这类"元注解形式的 @Qualifier"不会随工厂方法一起搬迁，
                // 必须显式补回，否则运行期表现为"服务名无法解析"且不报错
                if (StringUtils.hasText(product.qualifier())) {
                    definition.addQualifier(new AutowireCandidateQualifier(product.qualifier()));
                }
                registry.registerBeanDefinition(product.beanName(), definition);
            }
        }

        /**
         * 判断配置类自身是否让位
         *
         * @param registry    注册表
         * @param beanFactory Bean 工厂
         * @param spec        注册描述
         * @param configType  配置类
         * @return true=跳过注册
         */
        private static boolean shouldSkipSelf(BeanDefinitionRegistry registry,
                                              ListableBeanFactory beanFactory,
                                              Spec spec,
                                              Class<?> configType) {
            return switch (spec.selfAbsent()) {
                case BY_TYPE -> hasBeanOfType(beanFactory, configType);
                case BY_NAME -> registry.containsBeanDefinition(spec.selfBeanName());
                default -> false;
            };
        }

        /**
         * 构造一个按构造器自动装配的 BeanDefinition
         *
         * @param type Bean 类型
         * @return BeanDefinition
         */
        private static AbstractBeanDefinition beanDefinition(Class<?> type) {
            return BeanDefinitionBuilder.genericBeanDefinition(type)
                    .setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR)
                    .getBeanDefinition();
        }

        /**
         * 容器中是否已有该类型的 Bean
         *
         * @param beanFactory Bean 工厂
         * @param type        类型
         * @return true=已存在
         */
        private static boolean hasBeanOfType(ListableBeanFactory beanFactory, Class<?> type) {
            return beanFactory.getBeanNamesForType(type, false, false).length > 0;
        }

        /**
         * 按让位策略判断是否跳过
         *
         * @param registry    注册表
         * @param beanFactory Bean 工厂
         * @param product     产品
         * @return true=跳过注册
         */
        private static boolean shouldSkip(BeanDefinitionRegistry registry,
                                          ListableBeanFactory beanFactory,
                                          Product product) {
            return switch (product.absent()) {
                case BY_NAME -> registry.containsBeanDefinition(product.beanName());
                case BY_TYPE -> hasBeanOfType(beanFactory, NaConditions.resolve(product.beanType()));
                case REPLACE_BOOT_DEFAULT -> shouldSkipBootDefault(registry, product);
            };
        }

        /**
         * 处理"接管 Spring Boot 默认实现"的让位：仅当同名定义来自 Boot 自动装配时移除它
         *
         * @param registry 注册表
         * @param product  产品
         * @return true=宿主自带同名定义，本模块让位
         */
        private static boolean shouldSkipBootDefault(BeanDefinitionRegistry registry, Product product) {
            String beanName = product.beanName();
            if (!registry.containsBeanDefinition(beanName)) {
                return false;
            }
            if (isFromSpringBootAutoConfiguration(registry.getBeanDefinition(beanName))) {
                registry.removeBeanDefinition(beanName);
                log.debug("接管 Spring Boot 自动装配提供的 Bean: {}", beanName);
                return false;
            }
            return true;
        }

        /**
         * 判断 Bean 定义是否来自 Spring Boot 自动装配
         *
         * @param definition Bean 定义
         * @return true=来自 Boot 自动装配
         */
        private static boolean isFromSpringBootAutoConfiguration(BeanDefinition definition) {
            String factoryBeanName = definition.getFactoryBeanName();
            if (factoryBeanName != null && factoryBeanName.startsWith(BOOT_AUTO_CONFIGURATION_PACKAGE)) {
                return true;
            }
            String beanClassName = definition.getBeanClassName();
            return beanClassName != null && beanClassName.startsWith(BOOT_AUTO_CONFIGURATION_PACKAGE);
        }

        /**
         * 全部受管类的注册清单
         *
         * @return 注册描述列表
         */
        private static List<Spec> specs() {
            List<Spec> specs = new ArrayList<>();

            // ==== 无条件注册的工具与配置属性 ====
            // 这些类原本靠组件扫描注册；改为显式注册后不再依赖宿主包名，
            // Bean 名也一并收敛（原 SpringContextUtil 的名字 applicationUtil 过于通用）
            specs.add(Spec.self("com.na.common.utils.AddressUtil", "addressUtil"));
            specs.add(Spec.self("com.na.common.utils.SpringContextUtil", "springContextUtil"));
            specs.add(Spec.self("com.na.common.utils.LocalLangUtil", "localLangUtil"));
            // 两个属性类无条件注册：功能关闭时它们仍被 JacksonConfig / 各 Filter 工厂方法依赖
            specs.add(Spec.self("com.na.common.xss.AutoXssConfig", "autoXssConfig"));
            specs.add(Spec.self("com.na.common.csrf.AutoCsrfConfig", "autoCsrfConfig"));

            // ==== 默认开启（条件来自类上保留的 @ConditionalOnProperty） ====
            specs.add(Spec.factory("com.na.common.config.CorsConfig",
                    "org.springframework.web.filter.CorsFilter",
                    Product.of("corsFilter", "corsFilter",
                            "org.springframework.web.filter.CorsFilter", Absent.BY_TYPE)));

            // WebServerFactoryCustomizer 上挂着 Boot 自己的多个实现，按类型让位会整个跳过
            specs.add(Spec.factory("com.na.common.config.ErrorPageConfig", null,
                    Product.byName("webServerFactoryCustomizer", "webServerFactoryCustomizer",
                            "org.springframework.boot.web.server.WebServerFactoryCustomizer")));

            specs.add(Spec.factory("com.na.common.config.MybatisPlusConfig",
                    "com.baomidou.mybatisplus.core.handlers.MetaObjectHandler",
                    Product.of("mybatisPlusInterceptor", "mybatisPlusInterceptor",
                            "com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor", Absent.BY_TYPE)));

            // SqlTimeConfig 自身即产品，必须按 MetaObjectHandler 类型让位：
            // MyBatis-Plus 的自动配置是 getBean(MetaObjectHandler.class)，
            // 容器里存在两个该类型 Bean 会直接抛 NoUniqueBeanDefinitionException 导致启动失败
            specs.add(Spec.selfByType("com.na.common.config.SqlTimeConfig", "sqlTimeConfig",
                    "com.baomidou.mybatisplus.core.handlers.MetaObjectHandler"));

            // WebI18nConfig 自身是 WebMvcConfigurer，只能按 Bean 名让位
            specs.add(Spec.selfAndFactory("com.na.common.config.WebI18nConfig", "webI18nConfig",
                    // messageSource 接管 Boot 的默认实现；宿主自带的保留
                    Product.of(MESSAGE_SOURCE_BEAN_NAME, "messageSource",
                            "org.springframework.context.MessageSource", Absent.REPLACE_BOOT_DEFAULT),
                    Product.byName("localeResolver", "localeResolver",
                            "org.springframework.web.servlet.LocaleResolver"),
                    Product.byName("localeInterceptor", "localeInterceptor",
                            "org.springframework.web.servlet.config.annotation.WebMvcConfigurer")));

            specs.add(Spec.factory("com.na.common.config.OpenApiConfig",
                    "org.springdoc.core.customizers.OpenApiCustomizer",
                    Product.byName("customOpenAPI", "customOpenAPI", "io.swagger.v3.oas.models.OpenAPI"),
                    Product.byName("globalHeaderOpenApiCustomizer", "globalHeaderOpenApiCustomizer",
                            "org.springdoc.core.customizers.OpenApiCustomizer")));

            // restTemplate 必须补回 @LoadBalanced，否则服务名调用在运行期静默失效
            specs.add(Spec.factory("com.na.common.config.RestConfig",
                    "org.springframework.web.client.RestTemplate",
                    Product.qualified("restTemplate", "restTemplate",
                            "org.springframework.web.client.RestTemplate",
                            "org.springframework.cloud.client.loadbalancer.LoadBalanced")));

            specs.add(Spec.self("com.na.common.config.FfmpegConfig", "ffmpegConfig"));

            // ==== 默认关闭（matchIfMissing=false，必须显式开启） ====
            specs.add(Spec.factory("com.na.common.config.JacksonConfig", null,
                    Product.of("naJacksonModule", "naJacksonModule",
                            "com.na.common.config.model.JacksonModule", Absent.BY_TYPE),
                    // 原方法名 customizer 过于通用, 已改为 naJacksonCustomizer
                    Product.byName("naJacksonCustomizer", "naJacksonCustomizer",
                            "org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer")));

            specs.add(Spec.factory("com.na.common.xss.XssFilterConfig", null,
                    Product.byName("xssFilterRegistrationBean", "xssFilterRegistrationBean",
                            "org.springframework.boot.web.servlet.FilterRegistrationBean")));

            specs.add(Spec.factory("com.na.common.csrf.CsrfFilterConfig", null,
                    Product.byName("csrfFilterRegistration", "csrfFilterRegistration",
                            "org.springframework.boot.web.servlet.FilterRegistrationBean")));

            // FeignAop 依赖 AspectJ 织入, 宿主未引 aspectjweaver 时不应注册
            specs.add(Spec.selfRequires("com.na.common.feign.FeignAop", "feignAop",
                    "org.aspectj.lang.annotation.Aspect"));

            // ==== Redis 家族（AND 双条件: spring.data.redis.host 非空 且 na.X.enable=true） ====
            // redisTemplate 同样是"接管 Boot 默认实现"：RedisAutoConfiguration 无条件注册
            // 一个 RedisTemplate<Object,Object>(条件是按 Bean 名)，注册器后运行会让它赢，
            // 结果是本模块的 RedisTemplate<String,Object> 被跳过、CacheTemplateUtil 装配不上
            specs.add(Spec.factory("com.na.common.config.RedisConfig", REDIS_REQUIRED,
                    Product.of("redisTemplate", "redisTemplate",
                            "org.springframework.data.redis.core.RedisTemplate", Absent.REPLACE_BOOT_DEFAULT)));

            // 构造器需要 RedisTemplate, 由 AUTOWIRE_CONSTRUCTOR 解析
            specs.add(Spec.selfRequires("com.na.common.utils.CacheTemplateUtil", "cacheTemplateUtil",
                    REDIS_REQUIRED));

            specs.add(Spec.factory("com.na.common.config.RedissonConfig", "org.redisson.api.RedissonClient",
                    Product.of("redissonClient", "redissonClient",
                            "org.redisson.api.RedissonClient", Absent.BY_TYPE)));

            // 容器 Bean 名由 container 改为 redisMessageListenerContainer:
            // 原名过于通用, 宿主任何名为 container 的 Bean 都会与之硬冲突
            specs.add(Spec.factory("com.na.common.config.RedisListenerConfig", REDIS_REQUIRED,
                    Product.of("redisMessageListenerContainer", "redisMessageListenerContainer",
                            "org.springframework.data.redis.listener.RedisMessageListenerContainer",
                            Absent.BY_TYPE)));

            // 构造器用 ObjectProvider 取容器: 容器缺失时 init() 跳过而不是启动失败
            specs.add(Spec.selfRequires("com.na.common.config.RedisKeyExpirationListenerConfig",
                    "redisKeyExpirationListenerConfig", REDIS_REQUIRED));

            // 两个防重复提交切面, 依赖不同, 门禁也必须不同:
            //  - NoRepeatSubmitIPAOP 只认 IP + 请求路径, 与用户体系无关, 配置好 Redis 即可用
            //  - NoRepeatSubmitAOP 必须拿到当前用户标识, 而用户体系只能由宿主提供;
            //    宿主没实现该 SPI 时跳过注册并告警, 而不是让它启动失败 ——
            //    否则"只想用 IP 维度防重"的宿主会因为共用同一个开关而无法启动
            specs.add(Spec.selfIfBeanPresent("com.na.common.aop.NoRepeatSubmitAOP", "noRepeatSubmitAOP",
                    "com.na.common.aop.service.INoRepeatSubmitUserService"));
            specs.add(Spec.selfRequires("com.na.common.aop.NoRepeatSubmitIPAOP", "noRepeatSubmitIPAOP", REDIS_REQUIRED));

            // ==== 异常处理与 Feign 内部调用 ====
            // @RestControllerAdvice 是行为注解, 必须保留: ControllerAdviceBean 靠它发现 advice
            specs.add(Spec.selfAndFactory("com.na.common.exceptions.ApiExceptionHandler", "apiExceptionHandler",
                    Product.byName("asyncExceptionHandler", "asyncExceptionHandler",
                            "org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler")));

            // 宿主未引 feign-core 时 implements RequestInterceptor 无法解析:
            // 类路径门禁必须在此拦住, 否则注册器一碰这个类就 NoClassDefFoundError
            specs.add(Spec.selfRequires("com.na.common.feign.FeignInnerRequestInterceptor",
                    "feignInnerRequestInterceptor", "feign.RequestInterceptor"));

            return specs;
        }
    }
}
