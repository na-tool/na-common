package com.na.common.csrf;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * CSRF 过滤器注册配置
 *
 * 仅当配置项 na.csrf.enabled=true 时生效
 */
@Configuration
@ConditionalOnProperty(
        name = "na.csrf.enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class NaCsrfFilterConfig {

    /**
     * 注册 CSRF 过滤器 Bean（拦截所有 HTTP 请求）
     *
     * @param config CSRF 配置类
     * @return 过滤器注册对象
     */
    @Bean
    public FilterRegistrationBean<NaCsrfFilter> csrfFilterRegistration(NaAutoCsrfConfig config) {
        FilterRegistrationBean<NaCsrfFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new NaCsrfFilter(config));
        registration.addUrlPatterns("/*");      // 拦截所有路径
        registration.setName("naCsrfFilter");
        registration.setOrder(10);              // 顺序可调（建议晚于 XSS 过滤器）
        return registration;
    }
}