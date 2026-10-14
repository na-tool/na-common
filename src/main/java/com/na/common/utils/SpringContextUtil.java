package com.na.common.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;

/**
 * Spring 上下文工具, 用于在非 Spring 托管对象中获取容器里的 Bean / 配置项
 * <p>
 * <b>生效条件</b>: 本类必须以 Spring Bean 方式被组件扫描注册(通过 @Component 与
 * ApplicationContextAware 回调注入上下文), 且调用需发生在容器启动完成后。
 * 容器启动前调用会抛出 {@link IllegalStateException}
 * <p>
 * 示例:
 * <pre>{@code
 * // 按类型获取(推荐)
 * OrderService orderService = SpringContextUtil.getBean(OrderService.class);
 * // 按名称获取(需强转)
 * OrderService orderService = (OrderService) SpringContextUtil.getBean("orderService");
 * // 按名称+类型获取(名称与类型双重校验, 最安全)
 * OrderService orderService = SpringContextUtil.getBean("orderService", OrderService.class);
 * // 读取配置
 * String appId = SpringContextUtil.getProperty("wx.appid");
 * }</pre>
 */
// 注册由 NaCommonAutoConfiguration 的注册器接管, Bean 名 springContextUtil
// (原名 applicationUtil 过于通用, 极易与宿主自定义 Bean 撞名)
public class SpringContextUtil implements ApplicationContextAware {

    /** 容器启动时由 Spring 回调注入; ApplicationContext 本身线程安全 */
    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringContextUtil.applicationContext = applicationContext;
    }

    /**
     * 获取容器中的 Bean, 通过 Bean 名称获取
     *
     * @param beanName Bean 名称
     * @return Bean 实例, 需调用方自行强制类型转换
     * @throws IllegalStateException Spring 上下文尚未初始化
     * @throws org.springframework.beans.factory.NoSuchBeanDefinitionException Bean 不存在时抛出
     */
    public static Object getBean(String beanName) {
        return getRequiredContext().getBean(beanName);
    }

    /**
     * 获取容器中的 Bean, 通过类型获取
     *
     * @param beanClass Bean 的类对象
     * @param <T>       Bean 类型
     * @return 指定类型的 Bean 实例; 同类型存在多个 Bean 时抛出 NoUniqueBeanDefinitionException
     * @throws IllegalStateException Spring 上下文尚未初始化
     */
    public static <T> T getBean(Class<T> beanClass) {
        return getRequiredContext().getBean(beanClass);
    }

    /**
     * 获取容器中的 Bean, 通过名称 + 类型获取(名称与类型双重校验, 推荐使用)
     *
     * @param beanName  Bean 名称
     * @param beanClass Bean 类型
     * @param <T>       泛型类型
     * @return 指定类型的 Bean 实例
     * @throws IllegalStateException Spring 上下文尚未初始化
     */
    public static <T> T getBean(String beanName, Class<T> beanClass) {
        return getRequiredContext().getBean(beanName, beanClass);
    }

    /**
     * 获取 Spring 应用上下文, 用于 getBean 之外的高级操作(如获取 Environment、发布事件、动态注册 Bean)
     *
     * @return 应用上下文
     * @throws IllegalStateException Spring 上下文尚未初始化
     */
    public static ApplicationContext getApplicationContext() {
        return getRequiredContext();
    }

    /**
     * 读取配置项(application.yml / properties / 环境变量等 Environment 数据源)
     * <p>
     * 示例: {@code SpringContextUtil.getProperty("wx.appid")}
     *
     * @param key 配置键
     * @return 配置值; 不存在时返回 null
     * @throws IllegalStateException Spring 上下文尚未初始化
     */
    public static String getProperty(String key) {
        return getRequiredContext().getEnvironment().getProperty(key);
    }

    /**
     * 读取配置项, 不存在时返回默认值
     *
     * @param key          配置键
     * @param defaultValue 默认值
     * @return 配置值; 不存在时返回 defaultValue
     * @throws IllegalStateException Spring 上下文尚未初始化
     */
    public static String getProperty(String key, String defaultValue) {
        return getRequiredContext().getEnvironment().getProperty(key, defaultValue);
    }

    /**
     * 获取已初始化的上下文, 未初始化时给出可定位的错误信息(原实现此处为 NPE)
     *
     * @return 应用上下文
     * @throws IllegalStateException Spring 上下文尚未初始化
     */
    private static ApplicationContext getRequiredContext() {
        if (applicationContext == null) {
            throw new IllegalStateException(
                    "Spring 上下文尚未初始化: 请确认 SpringContextUtil 已被组件扫描注册为 Bean, 且调用发生在容器启动完成后");
        }
        return applicationContext;
    }
}
