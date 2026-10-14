package smoke;

import com.na.common.aop.service.INoRepeatSubmitUserService;
import smoke.enablena.EnableNaHost;
import smoke.nouser.NoUserServiceHost;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * na-common 自动装配冒烟夹具。
 *
 * <p><b>为什么启动类刻意放在 {@code com.na} 之外、且不加 {@code @EnableNA}</b>：
 * 改造前本模块的全部 Bean 只靠 {@code @ComponentScan(basePackages = "com.na")} 注册，
 * 宿主主类不在该包下时整个模块静默失效（不报错、不加载）。本夹具用"包名在 com.na 之外 +
 * 不写 @EnableNA"来正面验证自动装配已经接管 —— 这正是本次改造要解决的问题本身。
 *
 * <p>本类刻意不依赖 JUnit：本地 Maven 仓库缺少 surefire 的 JUnit Platform 提供者，
 * 离线环境跑不了 {@code mvn test}。用 main() + 手写断言即可在无网环境验证。
 *
 * <p>运行方式（工作目录为模块根）：
 * <pre>
 * mvn -o -q test-compile
 * mvn -o -q dependency:build-classpath -Dmdep.outputFile=target/cp.txt
 * java -cp "target/test-classes;target/classes;$(cat target/cp.txt)" smoke.NaCommonSmoke
 * </pre>
 */
public final class NaCommonSmoke {

    /**
     * 宿主启动类：位于 com.na 之外，不使用 @EnableNA。
     * <p>必须用 {@code @SpringBootApplication}（它含 {@code @EnableAutoConfiguration}）——
     * 只用 {@code @SpringBootConfiguration} 不会启用自动装配。
     */
    @SpringBootApplication
    static class HostApplication {

        /**
         * 宿主侧实现: 防重复提交依赖的用户标识 SPI。
         * <p>NoRepeatSubmitAOP 对它是对必填依赖（构造器注入），宿主不提供会导致上下文启动失败 ——
         * 这是既有行为，本夹具提供桩实现以便验证 AOP 能装配起来。
         *
         * @return 用户标识提供者
         */
        @Bean
        INoRepeatSubmitUserService noRepeatSubmitUserService() {
            return () -> "smoke-user";
        }
    }

    private static final List<String> FAILURES = new ArrayList<>();

    /** 夹具不连数据库: mybatis-plus 传递引入的 DataSource 自动配置没有 url 会直接启动失败 */
    private static final String EXCLUDE_DATASOURCE =
            "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration";

    private NaCommonSmoke() {
    }

    public static void main(String[] args) {
        System.out.println("=== na-common 自动装配冒烟 ===");
        checkAutoConfigurationLoaded();
        checkDefaultOffGroupWhenEnabled();
        checkRedisGroupWhenEnabled();
        checkCoexistenceWithEnableNA();
        checkIpOnlySubmitWithoutUserService();
        if (FAILURES.isEmpty()) {
            System.out.println("SMOKE OK");
        } else {
            System.out.println("SMOKE FAILED:");
            FAILURES.forEach(f -> System.out.println("  - " + f));
            System.exit(1);
        }
    }

    /** 自动装配入口本身必须被 Spring Boot 发现并注册 */
    private static void checkAutoConfigurationLoaded() {
        withContext(ctx -> {
            expectBean(ctx, "naCommonBeanRegistrar");
            // 自动装配类本身也是 Bean, 注册名是 FQCN(Spring 对 imported 配置类的命名惯例)
            expectBean(ctx, "com.na.common.autoconfigure.NaCommonAutoConfiguration");
            // 无条件注册的工具类
            expectBean(ctx, "addressUtil");
            expectBean(ctx, "springContextUtil");
            expectBean(ctx, "localLangUtil");
            expectBean(ctx, "autoXssConfig");
            expectBean(ctx, "autoCsrfConfig");

            // 默认开启组（条件来自类上保留的 @ConditionalOnProperty）
            expectBean(ctx, "corsFilter");
            expectBean(ctx, "webServerFactoryCustomizer");
            expectBean(ctx, "mybatisPlusInterceptor");
            expectBean(ctx, "sqlTimeConfig");
            expectBean(ctx, "webI18nConfig");
            expectBean(ctx, "localeResolver");
            expectBean(ctx, "localeInterceptor");
            expectBean(ctx, "customOpenAPI");
            expectBean(ctx, "globalHeaderOpenApiCustomizer");
            expectBean(ctx, "restTemplate");
            expectBean(ctx, "ffmpegConfig");

            // 关键回归：本模块的 Bean 必须真的由本模块提供，不能被 Spring Boot 的默认实现顶掉，
            // 也不能被"同名但同类型"的定义悄悄替换
            expectOwnedByNaCommon(ctx, "messageSource", "com.na.common.config.WebI18nConfig");
            expectOwnedByNaCommon(ctx, "webServerFactoryCustomizer", "com.na.common.config.ErrorPageConfig");
            expectOwnedByNaCommon(ctx, "mybatisPlusInterceptor", "com.na.common.config.MybatisPlusConfig");
            expectOwnedByNaCommon(ctx, "localeResolver", "com.na.common.config.WebI18nConfig");
            expectOwnedByNaCommon(ctx, "restTemplate", "com.na.common.config.RestConfig");
            expectOwnedByNaCommon(ctx, "customOpenAPI", "com.na.common.config.OpenApiConfig");
            expectOwnedByNaCommon(ctx, "sqlTimeConfig", null);
            expectOwnedByNaCommon(ctx, "webI18nConfig", null);
            expectOwnedByNaCommon(ctx, "corsFilter", "com.na.common.config.CorsConfig");
            // @LoadBalanced 是 @Qualifier 元注解, 工厂方法注册时极易丢失
            expectLoadBalancedQualifier(ctx, "restTemplate");

            // 默认关闭组不应存在
            expectNoBean(ctx, "xssFilterRegistrationBean");
            expectNoBean(ctx, "csrfFilterRegistration");
            expectNoBean(ctx, "naJacksonCustomizer");
            // Redis 未配置时本模块的 Redis 相关 Bean 不应存在。
            // 注意 redisTemplate 本身由 Spring Boot 的 RedisAutoConfiguration 无条件提供，
            // 所以只能断言它不归本模块管，不能断言它不存在
            expectNotOwnedByNaCommon(ctx, "redisTemplate");
            expectNoBean(ctx, "cacheTemplateUtil");
            expectNoBean(ctx, "redisMessageListenerContainer");
            expectNoBean(ctx, "noRepeatSubmitAOP");
            expectNoBean(ctx, "redissonClient");

            // 异常处理与 Feign
            expectBean(ctx, "apiExceptionHandler");
            expectBean(ctx, "asyncExceptionHandler");
            expectBean(ctx, "feignInnerRequestInterceptor");
            expectOwnedByNaCommon(ctx, "apiExceptionHandler", null);
            expectOwnedByNaCommon(ctx, "asyncExceptionHandler", "com.na.common.exceptions.ApiExceptionHandler");
            expectOwnedByNaCommon(ctx, "feignInnerRequestInterceptor", null);
        });
    }

    // === 夹具基础设施 ===

    /**
     * 显式开启默认关闭的开关后，对应 Bean 应出现。
     * <p>只验证"条件为真时会注册"；条件求值本身是否为假已在默认上下文中验证。
     */
    private static void checkDefaultOffGroupWhenEnabled() {
        withContext(ctx -> {
            expectBean(ctx, "naJacksonModule");
            expectBean(ctx, "naJacksonCustomizer");
            expectBean(ctx, "xssFilterRegistrationBean");
            expectBean(ctx, "csrfFilterRegistration");
            expectBean(ctx, "feignAop");
        }, "na.jackson.enable=true",
                "na.xss.enabled=true",
                "na.csrf.enabled=true",
                "na.feign.enable=true");
    }

    /**
     * 开启 Redis 相关开关后，Redis 家族的 Bean 应出现。
     * <p>其中只开 {@code na.redisMessage.enable}（不开 {@code na.redisListener.enable}）时，
     * 消息监听容器也必须注册 —— 修复前这种情况会因找不到容器而启动失败。
     */
    private static void checkRedisGroupWhenEnabled() {
        withContext(ctx -> {
            expectBean(ctx, "redisTemplate");
            expectBean(ctx, "cacheTemplateUtil");
            expectBean(ctx, "redisMessageListenerContainer");
            expectBean(ctx, "redisKeyExpirationListenerConfig");
            expectBean(ctx, "noRepeatSubmitAOP");
            expectBean(ctx, "noRepeatSubmitIPAOP");
        },
                // 本机没有 Redis 服务: 开启懒加载, 只验证 Bean 定义被正确注册, 不真的去连
                "spring.main.lazy-initialization=true",
                "spring.data.redis.host=127.0.0.1",
                "na.redis.enable=true",
                "na.redisMessage.enable=true",
                "na.submit.enable=true");
    }

    /**
     * 宿主同时标注 {@code @EnableNA} 与新自动装配时，不应重复注册或抛
     * {@code BeanDefinitionOverrideException}。
     * <p>这是升级过渡期的真实场景：老宿主会保留该注解。
     */
    private static void checkCoexistenceWithEnableNA() {
        try (ConfigurableApplicationContext ctx = new SpringApplicationBuilder(EnableNaHost.class)
                .web(WebApplicationType.NONE)
                .properties("spring.main.banner-mode=off", EXCLUDE_DATASOURCE)
                .run()) {
            expectBean(ctx, "springContextUtil");
            expectOwnedByNaCommon(ctx, "messageSource", "com.na.common.config.WebI18nConfig");
            expectOwnedByNaCommon(ctx, "corsFilter", "com.na.common.config.CorsConfig");
        } catch (Exception e) {
            fail("与 @EnableNA 共存时启动失败: " + e);
        }
    }

    /**
     * 只想用 IP 维度防重、宿主未提供 INoRepeatSubmitUserService 时必须能正常启动。
     * <p>这是修复的核心验收点：两个切面共用 {@code na.submit.enable} 开关，
     * 但依赖不同 —— 用户维度的切面缺 SPI 时应跳过并告警，而不是把整个应用拖垮。
     */
    private static void checkIpOnlySubmitWithoutUserService() {
        try (ConfigurableApplicationContext ctx = new SpringApplicationBuilder(NoUserServiceHost.class)
                .web(WebApplicationType.NONE)
                .properties("spring.main.banner-mode=off", EXCLUDE_DATASOURCE,
                        "spring.main.lazy-initialization=true",
                        "spring.data.redis.host=127.0.0.1",
                        "na.submit.enable=true")
                .run()) {
            // IP 维度切面与用户体系无关，必须注册
            expectBean(ctx, "noRepeatSubmitIPAOP");
            // 用户维度切面缺 SPI，必须跳过而不是让启动失败
            expectNoBean(ctx, "noRepeatSubmitAOP");
        } catch (Exception e) {
            fail("宿主未提供 INoRepeatSubmitUserService 时启动失败: " + e);
        }
    }

    /**
     * 启动上下文执行断言，结束后关闭
     *
     * @param assertions      断言体
     * @param extraProperties 额外属性
     */
    private static void withContext(ContextAssertions assertions, String... extraProperties) {
        // 用非 Web 上下文: 这里验证的是 Bean 注册与让位,
        // 而 spring-boot-starter-web 是 provided, 测试 classpath 上没有 servlet 容器实现
        SpringApplicationBuilder builder = new SpringApplicationBuilder(HostApplication.class)
                .web(WebApplicationType.NONE)
                .properties("spring.main.banner-mode=off", EXCLUDE_DATASOURCE);
        if (extraProperties.length > 0) {
            builder.properties(extraProperties);
        }
        try (ConfigurableApplicationContext ctx = builder.run()) {
            assertions.run(ctx);
        } catch (Exception e) {
            fail("上下文启动或断言阶段抛出异常: " + e);
        }
    }

    /** 断言 Bean 存在 */
    private static void expectBean(ConfigurableApplicationContext ctx, String beanName) {
        if (!ctx.containsBean(beanName)) {
            fail("期望存在 Bean [" + beanName + "]，实际不存在");
        } else {
            System.out.println("  [ok] Bean 存在: " + beanName);
        }
    }

    /** 断言 Bean 不存在 */
    private static void expectNoBean(ConfigurableApplicationContext ctx, String beanName) {
        if (ctx.containsBean(beanName)) {
            fail("期望不存在 Bean [" + beanName + "]，实际存在");
        } else {
            System.out.println("  [ok] Bean 正确地不存在: " + beanName);
        }
    }

    /**
     * 按方法名反射调用私有断言，便于按批次逐步启用检查项而不必改动 main
     *
     * @param ctx      上下文
     * @param method   方法名
     */
    @SuppressWarnings("unused")
    private static void invoke(ConfigurableApplicationContext ctx, String method) {
        try {
            Method m = NaCommonSmoke.class.getDeclaredMethod(method, ConfigurableApplicationContext.class);
            m.setAccessible(true);
            m.invoke(null, ctx);
        } catch (ReflectiveOperationException e) {
            fail("调用断言[" + method + "]失败: " + e);
        }
    }

    private static void fail(String message) {
        FAILURES.add(message);
        System.out.println("  [FAIL] " + message);
    }

    /**
     * 断言 Bean 由本模块提供。
     * <p>这是本次改造最容易出错的地方：注册器在全部配置类（含 Spring Boot 自动装配）之后运行，
     * 若让位策略写成"按类型让位"，本模块的实现会被 Boot 的默认实现顶掉，而 Bean 名仍然存在 ——
     * 只断言"Bean 存在"完全发现不了。
     *
     * @param ctx       上下文
     * @param beanName  Bean 名
     * @param ownerFqcn 期望的拥有者：工厂类 FQCN；为 null 表示该 Bean 自身就是实现类（按类名判断）
     */
    private static void expectOwnedByNaCommon(ConfigurableApplicationContext ctx, String beanName, String ownerFqcn) {
        BeanDefinition definition = beanDefinition(ctx, beanName);
        if (definition == null) {
            fail("Bean 不存在: " + beanName);
            return;
        }
        String actual = definition.getFactoryBeanName() != null
                ? definition.getFactoryBeanName()
                : definition.getBeanClassName();
        String expected = ownerFqcn != null ? ownerFqcn : definition.getBeanClassName();
        boolean ok = ownerFqcn != null ? ownerFqcn.equals(actual)
                : actual != null && actual.startsWith("com.na.common.");
        if (ok) {
            System.out.println("  [ok] Bean 由本模块提供: " + beanName);
        } else {
            fail("Bean [" + beanName + "] 未由本模块提供, 实际拥有者=" + actual + ", 期望=" + expected);
        }
    }

    /** 取 Bean 的定义拥有者（工厂类或实现类） */
    private static String ownerOf(ConfigurableApplicationContext ctx, String beanName) {
        BeanDefinition definition = beanDefinition(ctx, beanName);
        if (definition == null) {
            return null;
        }
        return definition.getFactoryBeanName() != null
                ? definition.getFactoryBeanName()
                : definition.getBeanClassName();
    }

    /**
     * 断言 Bean（若存在）不由本模块提供。
     * <p>用于 Spring Boot 会无条件提供的 Bean（如 {@code redisTemplate}）：
     * 不能断言它"不存在"，只能断言本模块的开关确实没让它接管
     *
     * @param ctx      上下文
     * @param beanName Bean 名
     */
    private static void expectNotOwnedByNaCommon(ConfigurableApplicationContext ctx, String beanName) {
        String actual = ownerOf(ctx, beanName);
        if (actual == null || !actual.startsWith("com.na.common.")) {
            System.out.println("  [ok] 本模块未接管: " + beanName + "（当前拥有者=" + actual + "）");
        } else {
            fail("开关未开启, 本模块却接管了 Bean: " + beanName);
        }
    }

    /**
     * 断言 Bean 定义上保留了指定的 @Qualifier 元注解
     *
     * @param ctx          上下文
     * @param beanName     Bean 名
     * @param qualifierType @Qualifier 元注解类型 FQCN
     */
    private static void expectLoadBalancedQualifier(ConfigurableApplicationContext ctx, String beanName) {
        BeanDefinition definition = beanDefinition(ctx, beanName);
        boolean present = definition instanceof AbstractBeanDefinition abstractDefinition
                && abstractDefinition.getQualifier("org.springframework.cloud.client.loadbalancer.LoadBalanced") != null;
        if (present) {
            System.out.println("  [ok] @LoadBalanced qualifier 保留: " + beanName);
        } else {
            fail("@LoadBalanced qualifier 丢失: " + beanName + "（服务名调用会在运行期静默失效）");
        }
    }

    /**
     * 取 Bean 定义
     *
     * @param ctx      上下文
     * @param beanName Bean 名
     * @return Bean 定义；不存在时返回 null
     */
    private static BeanDefinition beanDefinition(ConfigurableApplicationContext ctx, String beanName) {
        ConfigurableListableBeanFactory beanFactory = ctx.getBeanFactory();
        if (beanFactory instanceof BeanDefinitionRegistry registry
                && registry.containsBeanDefinition(beanName)) {
            return registry.getBeanDefinition(beanName);
        }
        return null;
    }

    /** 断言体 */
    @FunctionalInterface
    private interface ContextAssertions {
        void run(ConfigurableApplicationContext ctx);
    }
}
