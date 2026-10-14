package com.na.common.csrf;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

/**
 * CSRF 过滤器注册配置
 * <p>
 * 仅当 {@code na.csrf.enabled=true} 时生效, 将 {@link CsrfFilter} 注册为 Servlet Filter
 * <p>
 * 配置示例:
 * <pre>{@code
 * na:
 *   csrf:
 *     enabled: true   # 开启 CSRF 过滤器
 * }</pre>
 */
@ConditionalOnProperty(
        name = "na.csrf.enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class CsrfFilterConfig {

    /**
     * 注册 CSRF 过滤器 Bean(拦截所有 HTTP 请求)
     *
     * @param config CSRF 配置属性
     * @return 过滤器注册对象, 拦截 {@code /*} 所有路径, order=10
     */
    @Bean
    public FilterRegistrationBean<CsrfFilter> csrfFilterRegistration(AutoCsrfConfig config) {
        FilterRegistrationBean<CsrfFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new CsrfFilter(config));
        registration.addUrlPatterns("/*");
        registration.setName("naCsrfFilter");
        // 顺序: 建议晚于 XSS 过滤器, 早于业务 Filter
        registration.setOrder(10);
        return registration;
    }
}
