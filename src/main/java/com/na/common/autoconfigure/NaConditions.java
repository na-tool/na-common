package com.na.common.autoconfigure;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.context.annotation.ConfigurationCondition.ConfigurationPhase;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.RepeatableContainers;
import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.util.ClassUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 条件求值垫片：把类上的 {@code @ConditionalOnXxx} 在 BeanDefinitionRegistryPostProcessor
 * 阶段求值，语义等价于 Spring 处理 {@code @Bean} 方法时的 {@code ConditionEvaluator.shouldSkip}。
 *
 * <p><b>为什么需要这个类</b>：{@code NaCommonBeanRegistrar} 注册的类是通过 BeanDefinition
 * 直接登记到注册表的，永远不会进入 {@code ConfigurationClassParser}，因此类上的
 * {@code @ConditionalOnProperty} / {@code @ConditionalOnExpression} 不会被任何人读取。
 *
 * <p><b>为什么不直接用 Spring 的实现</b>：{@code ConditionEvaluator} 与其内部类
 * {@code ConditionContextImpl} 都是包私有的（字节码 flags 只有 {@code ACC_SUPER}），
 * 库代码无法引用。Boot 侧的 {@code OnPropertyCondition} / {@code OnExpressionCondition}
 * 同样是包私有的，所以条件实现类只能从 {@code @Conditional.value()} 元注解里反射取出，
 * 不能硬编码类名 —— 这反而让本类对 Boot 内部改名免疫。
 *
 * <p>因此本类只使用公开 API 复刻那约二十行逻辑：注解始终是条件的唯一事实来源，
 * 由 Boot 自己的 Condition 实现来求值。
 */
final class NaConditions {

    /**
     * 与 Spring 内部 {@code ConditionContextImpl} 一致：按固定 Bean 名取环境，
     * 避免按类型解析出非预期的 Environment 实现
     */
    private static final String ENVIRONMENT_BEAN_NAME = "environment";

    private NaConditions() {
    }

    /**
     * 建立条件求值上下文
     *
     * @param beanFactory 当前 Bean 工厂
     * @return 条件上下文
     */
    static ConditionContext newContext(ConfigurableListableBeanFactory beanFactory) {
        return new SimpleConditionContext(beanFactory);
    }

    /**
     * 求值类上的全部 {@code @Conditional} 条件
     *
     * @param type    待求值类型
     * @param context 条件上下文
     * @return true=条件全部满足，可以注册
     */
    static boolean matches(Class<?> type, ConditionContext context) {
        List<Condition> conditions = collectConditions(type);
        if (conditions.isEmpty()) {
            return true;
        }
        AnnotationAwareOrderComparator.sort(conditions);
        AnnotationMetadata metadata = new StandardAnnotationMetadata(type);
        for (Condition condition : conditions) {
            // 与 ConditionEvaluator 一致：仅跳过"声明了其他阶段"的配置条件；
            // 普通 Condition 没有阶段概念，照常求值
            if (condition instanceof ConfigurationCondition configurationCondition
                    && configurationCondition.getConfigurationPhase() != ConfigurationPhase.REGISTER_BEAN) {
                continue;
            }
            if (!condition.matches(context, metadata)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 收集类（含父类与元注解）上声明的全部条件实例
     *
     * @param type 待求值类型
     * @return 条件实例列表
     */
    private static List<Condition> collectConditions(Class<?> type) {
        List<Condition> conditions = new ArrayList<>();
        MergedAnnotations.from(type,
                        MergedAnnotations.SearchStrategy.TYPE_HIERARCHY,
                        RepeatableContainers.standardRepeatables())
                .stream(Conditional.class)
                .forEach(annotation -> {
                    for (Class<?> conditionType : annotation.getClassArray("value")) {
                        conditions.add((Condition) BeanUtils.instantiateClass(conditionType));
                    }
                });
        return conditions;
    }

    /**
     * 探测某个类是否在类路径上
     * <p>本模块的依赖几乎全是 provided，宿主可能完全没有 mybatis-plus / redisson / feign。
     * 注册器因此全程用字符串类名 + 本方法做门禁，避免类加载期直接 NoClassDefFoundError。
     *
     * @param className 全限定类名
     * @return true=存在
     */
    static boolean present(String className) {
        return ClassUtils.isPresent(className, NaConditions.class.getClassLoader());
    }

    /**
     * 惰性解析类名
     *
     * @param className 全限定类名
     * @return 类对象
     */
    static Class<?> resolve(String className) {
        return ClassUtils.resolveClassName(className, NaConditions.class.getClassLoader());
    }

    /**
     * {@link ConditionContext} 的最小实现
     */
    private static final class SimpleConditionContext implements ConditionContext {

        private final ConfigurableListableBeanFactory beanFactory;
        private final ResourceLoader resourceLoader;
        private Environment environment;

        SimpleConditionContext(ConfigurableListableBeanFactory beanFactory) {
            this.beanFactory = beanFactory;
            ClassLoader classLoader = beanFactory.getBeanClassLoader();
            this.resourceLoader = classLoader != null
                    ? new DefaultResourceLoader(classLoader)
                    : new DefaultResourceLoader();
        }

        @Override
        public BeanDefinitionRegistry getRegistry() {
            return beanFactory instanceof BeanDefinitionRegistry registry ? registry : null;
        }

        @Override
        public ConfigurableListableBeanFactory getBeanFactory() {
            return beanFactory;
        }

        @Override
        public Environment getEnvironment() {
            if (environment == null) {
                environment = beanFactory.getBean(ENVIRONMENT_BEAN_NAME, Environment.class);
            }
            return environment;
        }

        @Override
        public ResourceLoader getResourceLoader() {
            return resourceLoader;
        }

        @Override
        public ClassLoader getClassLoader() {
            return beanFactory.getBeanClassLoader();
        }
    }
}
