package com.na.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.na.common.config.model.NaJacksonModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(
        name = {"na.jackson"},
        matchIfMissing = false
)
public class NaJacksonConfig {

    @Bean
    public NaJacksonModule naJacksonModule() {
        return new NaJacksonModule();
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer customizer(NaJacksonModule module) {
        return builder -> builder.modulesToInstall(module);
    }
}
