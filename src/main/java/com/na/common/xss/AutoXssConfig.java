package com.na.common.xss;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * XSS 配置属性类, 绑定 application.yml 中 {@code na.xss.*} 配置
 * <p>
 * 配置示例:
 * <pre>{@code
 * na:
 *   xss:
 *     enabled: true
 *     exclude-paths:   # 排除路径(不拦截)
 *       - /health
 *       - /api/public/**
 * }</pre>
 */
@Data
@Component
@ConfigurationProperties(prefix = "na.xss")
public class AutoXssConfig {

    /**
     * XSS 过滤器排除路径（不拦截）
     */
    private List<String> excludePaths = new ArrayList<>();
}
