package com.na.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI3 + Knife4j 配置
 *
 * @author na
 */
@Configuration
@ConditionalOnProperty(
        name = "na.swagger.enable",
        havingValue = "true",
        matchIfMissing = true
)
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("NA 平台 - 总服务平台模块")
                        .description("NA 平台接口文档")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("na")
                                .email("na_tool@163.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://www.apache.org/licenses/LICENSE-2.0.txt")));
    }
}
