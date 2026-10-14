package com.na.common.xss;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

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
@ConfigurationProperties(prefix = "na.xss")
public class AutoXssConfig {

    /**
     * 是否开启 XSS 过滤器(默认关闭)
     * <p>声明为字段后, 开关、属性绑定与 IDE 配置提示三者归一;
     * {@code XssFilterConfig} 的 {@code @ConditionalOnProperty} 读的是同一个键
     */
    private Boolean enabled = false;

    /**
     * XSS 过滤器排除路径（不拦截）
     */
    private List<String> excludePaths = new ArrayList<>();
}
