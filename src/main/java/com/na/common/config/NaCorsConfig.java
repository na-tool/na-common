package com.na.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ConditionalOnProperty(
        name = {"na.cors"}, matchIfMissing = false
)
public class NaCorsConfig implements WebMvcConfigurer {
    // 当前跨域请求最大有效时长。这里默认1天
    private static final long MAX_AGE = 24 * 60 * 60;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 所有接口
                .allowedOriginPatterns("*") // ✅ 允许所有域
                .allowedMethods("*")        // ✅ 所有请求方式
                .allowedHeaders("*")        // ✅ 所有请求头
                .allowCredentials(true)     // ✅ 支持带 Cookie
                .exposedHeaders("Content-Disposition") // ✅ 暴露头
                .maxAge(MAX_AGE);           // 预检请求缓存时间
    }
}

