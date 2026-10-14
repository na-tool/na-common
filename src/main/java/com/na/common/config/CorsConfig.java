package com.na.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 全局跨域配置（na-upms 服务兜底）
 * <p>
 * 架构说明：
 *   1. 前端请求经过 Gateway(8861) 时 → Gateway globalcors 已统一处理跨域 + DedupeResponseHeader 去重
 *      → na-upms 侧即使也写了 ACAO 头，Gateway 会丢弃重复的，不会冲突
 *   2. 前端直连 na-upms(8886) 时 → 本配置生效，独立处理跨域
 * <p>
 * 本配置通过注册 CorsFilter Bean 覆盖整个 Filter 链（含 OPTIONS 预检）。
 * 若你的项目更希望走 Spring MVC 的注解驱动方式，自行声明
 * {@link WebMvcConfigurer} 并实现 addCorsMappings 即可，两者同时存在不会冲突。
 *
 * @author na
 */
@ConditionalOnProperty(
        name = "na.cors.enable",
        havingValue = "true",
        matchIfMissing = true
)
public class CorsConfig {

    /**
     * 注册 CorsFilter Bean，兜底覆盖整个 Filter 链（含 OPTIONS 预检）
     * <p>与宿主自行声明的 {@code WebMvcConfigurer#addCorsMappings} 同时存在不冲突，
     * Spring 内部会做合并处理；两种方式按需选用即可
     *
     * @return 全局跨域过滤器
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // 通配符来源（生产环境建议改成具体域名列表）
        config.addAllowedOriginPattern("*");
        // 允许的 HTTP 方法
        config.addAllowedMethod("GET");
        config.addAllowedMethod("POST");
        config.addAllowedMethod("PUT");
        config.addAllowedMethod("DELETE");
        config.addAllowedMethod("OPTIONS");
        config.addAllowedMethod("PATCH");
        // 允许的请求头（* 表示全部）
        config.addAllowedHeader("*");
        // 前端 JS 可读取的响应头
        config.addExposedHeader("Authorization");
        config.addExposedHeader("Content-Disposition");
        config.addExposedHeader("X-Total-Count");
        config.addExposedHeader("X-Request-Id");
        config.addExposedHeader("X-Tenant-Id");
        // 允许携带凭证
        config.setAllowCredentials(true);
        // 预检缓存 1 小时
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}

