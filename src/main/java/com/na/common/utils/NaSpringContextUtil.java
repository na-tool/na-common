package com.na.common.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Spring 上下文工具, 可用于获取spring 容器中的Bean
 */
@Component("naApplicationUtil")
public class NaSpringContextUtil implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        NaSpringContextUtil.applicationContext = applicationContext;
    }

    /**
     * 获取spring容器中的bean,通过bean名称获取
     * @param beanName bean名称
     * @return: Object 返回Object,需要做强制类型转换
     */
    public static Object getBean(String beanName){
        return applicationContext.getBean(beanName);
    }

    /**
     * @param <T>       Bean 类型
     * @param beanClass Bean 的类对象
     * @return 指定类型的 Bean 实例
     */
    public static <T> T getBean(Class<T> beanClass) {
        return applicationContext.getBean(beanClass);
    }

    /**
     * @param <T> 泛型类型
     * @param beanName Bean 名称
     * @param beanClass Bean 类型
     * @return 指定类型的 Bean 实例
     */
    public static <T> T getBean(String beanName, Class<T> beanClass){
        return applicationContext.getBean(beanName,beanClass);
    }
}