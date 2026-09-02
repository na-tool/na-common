package com.na.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 全局跨域配置（na-upms 服务兜底）
 * <p>
 * 架构说明：
 *   1. 前端请求经过 Gateway(8861) 时 → Gateway globalcors 已统一处理跨域 + DedupeResponseHeader 去重
 *      → na-upms 侧即使也写了 ACAO 头，Gateway 会丢弃重复的，不会冲突
 *   2. 前端直连 na-upms(8886) 时 → 本配置生效，独立处理跨域
 * <p>
 * 本配置同时提供：
 *   - WebMvcConfigurer#addCorsMappings：覆盖 MVC 层接口的跨域
 *   - CorsFilter Bean：兜底，确保 Filter 链最前端能处理跨域（含 OPTIONS 预检）
 *
 * @author na
 */
@Configuration
@ConditionalOnProperty(
        name = "na.cors.enable",
        havingValue = "true",
        matchIfMissing = true
)
public class CorsConfig {

    /**
     * 方式一：WebMvcConfigurer 方式（覆盖 Spring MVC 接口层）
     * 推荐做法，注解驱动、作用域清晰
     */
//    @Bean
//    public WebMvcConfigurer corsWebMvcConfigurer() {
//        return new WebMvcConfigurer() {
//            @Override
//            public void addCorsMappings(CorsRegistry registry) {
//                registry.addMapping("/**")
//                        // 推荐用 allowedOriginPatterns 替代 allowedOrigins
//                        // allowedOrigins 不支持通配符且与 allowCredentials 同时使用会报错
//                        .allowedOriginPatterns("*")
//                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
//                        .allowedHeaders("*")
//                        // 前端 JS 可读取的响应头（如拿到 token 做鉴权）
//                        .exposedHeaders(
//                                "Authorization",
//                                "Content-Disposition",
//                                "X-Total-Count",
//                                "X-Request-Id",
//                                "X-Tenant-Id"
//                        )
//                        // 允许携带 Cookie / Authorization 头
//                        .allowCredentials(true)
//                        // 预检请求缓存 1 小时，浏览器在缓存期内对同一路径不再重复发 OPTIONS
//                        .maxAge(3600);
//            }
//        };
//    }

    /**
     * 方式二：CorsFilter Bean（兜底覆盖整个 Filter 链）
     * 与 WebMvcConfigurer 同时存在不冲突，Spring 内部会做合并处理
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

