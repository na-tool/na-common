package com.na.common.config;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.na.common.config.model.JacksonModule;
import com.na.common.xss.AutoXssConfig;
import com.na.common.xss.XssStringJsonDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * Jackson 自定义配置
 * <p>
 * 注册自定义 {@link JacksonModule} 统一日期/Long/BigDecimal 序列化格式,
 * 并可选启用 JSON 层 XSS 防护(通过 {@code na.xss.enabled=true} 控制)
 * <p>
 * 配置示例:
 * <pre>{@code
 * na:
 *   jackson:
 *     enable: true   # 开启 Jackson 自定义配置
 *   xss:
 *     enabled: true  # 同时开启 JSON XSS 防护(可选)
 * }</pre>
 */
@ConditionalOnProperty(
        name = "na.jackson.enable",
        havingValue = "true",
        matchIfMissing = false
)
public class JacksonConfig {

    @Autowired
    private Environment environment;

    @Autowired
    private AutoXssConfig autoXssConfig;

    /**
     * 注册自定义 Jackson 模块(日期格式、Long→String、BigDecimal 等)
     *
     * @return JacksonModule 实例
     */
    @Bean
    public JacksonModule naJacksonModule() {
        return new JacksonModule();
    }

    /**
     * 自定义 Jackson ObjectMapper 构建器
     * <p>
     * 安装通用模块 + 可选的 XSS 反序列化模块
     *
     * @param module 自定义 Jackson 模块
     * @return Jackson2ObjectMapperBuilderCustomizer
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer naJacksonCustomizer(JacksonModule module) {
        return builder -> {
            // 安装已有的通用模块
            builder.modulesToInstall(module);

            boolean jsonXssEnabled = Boolean.parseBoolean(
                    environment.getProperty("na.xss.enabled", "false")
            );

            if (jsonXssEnabled) {
                // 设置 XSS 配置(静态注入给 XssStringJsonDeserializer)
                XssStringJsonDeserializer.setConfig(autoXssConfig);

                // 创建并注册 XSS Module
                SimpleModule xssModule = new SimpleModule("XssDeserializerModule");
                xssModule.addDeserializer(String.class, new XssStringJsonDeserializer());
                builder.modules(xssModule);
            }
        };
    }
}
