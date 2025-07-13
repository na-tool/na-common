package com.na.common.config;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.na.common.config.model.NaJacksonModule;
import com.na.common.xss.NaAutoXssConfig;
import com.na.common.xss.NaXssStringJsonDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
@ConditionalOnProperty(
        name = {"na.jackson"},
        havingValue = "true",
        matchIfMissing = false
)
public class NaJacksonConfig {

    @Autowired
    private Environment environment;

    @Autowired
    private NaAutoXssConfig naAutoXssConfig;

    @Bean
    public NaJacksonModule naJacksonModule() {
        return new NaJacksonModule();
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer customizer(NaJacksonModule module) {
        return builder -> {
            // 安装已有的通用模块
            builder.modulesToInstall(module);

            boolean jsonXssEnabled = Boolean.parseBoolean(
                    environment.getProperty("na.xss.enabled", "false")
            );

            if (jsonXssEnabled) {
                // 设置 XSS 配置（静态注入）
                NaXssStringJsonDeserializer.setConfig(naAutoXssConfig);

                // 创建并注册 XSS Module
                SimpleModule xssModule = new SimpleModule("XssDeserializerModule");
                xssModule.addDeserializer(String.class, new NaXssStringJsonDeserializer());
                builder.modules(xssModule);
            }
        };
    }
}
