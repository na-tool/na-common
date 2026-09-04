package com.na.common.config;

import com.na.common.constant.IGlobalConst;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenApi 接口文档配置
 * na.swagger.enable=true 开启文档；缺失配置默认开启
 */
@Configuration
@ConditionalOnProperty(
        name = "na.swagger.enable",
        havingValue = "true",
        matchIfMissing = true
)
public class OpenApiConfig {

    /**
     * 文档基础信息 + Authorization(JWT)全局鉴权（右上角🔒按钮）
     */
    @Bean
    public OpenAPI customOpenAPI() {

        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");

        SecurityRequirement securityRequirement = new SecurityRequirement().addList(IGlobalConst.SECURITY.HEADER_STRING);

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
                                .url("http://www.apache.org/licenses/LICENSE-2.0.txt")))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes(IGlobalConst.SECURITY.HEADER_STRING, securityScheme))
                .security(List.of(securityRequirement));
    }

    /**
     * 全局追加 Request‑Source 请求头，所有接口参数列表展示
     */
    @Bean
    public OpenApiCustomizer globalHeaderOpenApiCustomizer() {
        return openApi -> openApi.getPaths().values().forEach(pathItem ->
                pathItem.readOperations().forEach(operation -> {

                    Parameter tokenHeader = new Parameter();
                    tokenHeader.setName(IGlobalConst.SECURITY.HEADER_STRING);
                    tokenHeader.setIn("header");
                    tokenHeader.setRequired(false);
                    tokenHeader.setDescription("用户登录令牌");
                    tokenHeader.setSchema(new StringSchema());
                    operation.addParametersItem(tokenHeader);

                    Parameter requestSourceHeader = new Parameter();
                    requestSourceHeader.setName(IGlobalConst.SECURITY.REQUEST_SOURCE);
                    requestSourceHeader.setIn("header");
                    requestSourceHeader.setRequired(false);
                    requestSourceHeader.setDescription("请求来源标识");
                    requestSourceHeader.setSchema(new StringSchema());
                    operation.addParametersItem(requestSourceHeader);
                })
        );
    }

}


