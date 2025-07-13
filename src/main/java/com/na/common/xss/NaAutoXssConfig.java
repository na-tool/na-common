package com.na.common.xss;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * XSS 配置属性类，用于绑定 application.yml 中的 na.xss.* 配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "na.xss")
public class NaAutoXssConfig {

    /**
     * XSS 过滤器排除路径（不拦截）
     */
    private List<String> excludePaths = new ArrayList<>();
}
