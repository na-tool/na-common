package com.na.common.xss;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * XSS 防护过滤器配置类（Servlet Filter 方式）
 *
 * 用于注册自定义的 XssFilter，它会对所有进入的 HTTP 请求进行包装，
 * 拦截并清理常见的 XSS 攻击代码，适用于表单提交、URL 参数、Header 等非 JSON 数据。
 */
@Configuration
@ConditionalOnProperty(
        name = {"na.xss.http"},
        havingValue = "true",
        matchIfMissing = false
)
public class NaXssFilterConfig {

    /**
     * 注册 XSS 过滤器 Bean
     *
     * @return FilterRegistrationBean，用于拦截所有 HTTP 请求并执行 XSS 清理逻辑。
     */
    @Bean
    public FilterRegistrationBean<NaXssFilter> xssFilterRegistrationBean(NaAutoXssConfig config) {
        FilterRegistrationBean<NaXssFilter> registrationBean = new FilterRegistrationBean<>();

        registrationBean.setFilter(new NaXssFilter(config.getExcludePaths()));             // 设置过滤器逻辑
        registrationBean.addUrlPatterns("/*");                   // 拦截所有 URL 路径（你也可以自定义指定路径）
        registrationBean.setName("naXssFilter");                   // 设置过滤器名称
        registrationBean.setOrder(20);                            // 设置过滤器执行顺序（越小越优先执行）

        return registrationBean;
    }
}
