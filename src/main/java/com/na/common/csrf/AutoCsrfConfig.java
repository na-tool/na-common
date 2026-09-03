package com.na.common.csrf;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * CSRF 防护配置属性
 * <p>
 * 通过 application.yml 中 {@code na.csrf} 前缀配置, 控制 CSRF 过滤器的白名单与空 Referer 策略
 * <p>
 * 配置示例:
 * <pre>{@code
 * na:
 *   csrf:
 *     enabled: true                        # 开启 CSRF 过滤器(由 CsrfFilterConfig 控制)
 *     allow-blank-referer: true            # 允许 Referer 为空的请求(默认 true)
 *     csrf-white-paths:                   # 路径白名单
 *       - /login
 *       - /health
 *     csrf-white-domains:                  # 域名白名单(支持通配/端口范围/IP段)
 *       - www.example.com:8080
 *       - www.example.com:8000-9000
 *       - 192.168.1.*
 * }</pre>
 */
@Data
@Component
@ConfigurationProperties(prefix = "na.csrf")
public class AutoCsrfConfig {

    /**
     * 是否允许 Referer 为空的请求(默认 true = 不拦截)
     * <p>浏览器地址栏直接访问时 Referer 为空, 设为 false 会拦截此类请求
     */
    private Boolean allowBlankReferer = true;

    /**
     * 请求路径白名单(如 /health、/login)
     * <p>Referer 不可信但路径在白名单中时, 仍放行请求
     */
    private List<String> csrfWhitePaths = new ArrayList<>();

    /**
     * 域名白名单(如 www.example.com:8080)
     * <p>支持以下格式:
     * <ul>
     *   <li>www.example.com:8080 — 精确端口匹配</li>
     *   <li>www.example.com:8000-9000 — 端口范围匹配</li>
     *   <li>192.168.1.* — IP 段通配</li>
     *   <li>192.168.1.10-192.168.1.20 — IP 范围匹配</li>
     *   <li>h5.example.com — 纯域名匹配(任意端口)</li>
     * </ul>
     */
    private List<String> csrfWhiteDomains = new ArrayList<>();
}
