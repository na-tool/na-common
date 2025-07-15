package com.na.common.csrf;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * CSRF 白名单配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "na.csrf")
public class NaAutoCsrfConfig {

    /**
     * 是否允许 Referer 为空的请求（默认 true = 不拦截）
     */
    private Boolean allowBlankReferer = true;

    /**
     * 请求路径白名单（如：/health、/login）
     */
    private List<String> csrfWhitePaths = new ArrayList<>();

    /**
     * 域名白名单（如：www.example.com:8080）
     *   - www.example.com:8080          # 精确端口匹配
     *   - www.example.com:8000-9000     # 端口范围匹配
     *   - 192.168.1.*                   # IP段通配
     *   - 192.168.1.10-192.168.1.20    # IP范围匹配
     *   - h5.lianshangtv.com            # 纯域名匹配
     */
    private List<String> csrfWhiteDomains = new ArrayList<>();
}
