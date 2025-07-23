package com.na.common.config;

import com.github.xiaoymin.knife4j.spring.annotations.EnableKnife4j;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RequestMethod;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebMvc;


@Configuration
@EnableSwagger2WebMvc
@EnableKnife4j
@ConditionalOnProperty(name = "na.swagger.group", havingValue = "false", matchIfMissing = false)
public class NaSwaggerConfiguration extends NaSwaggerConfig {
    @Autowired
    private Environment environment;

    @Bean
    public Docket groupRestApi() {
        boolean swaggerEnabled = Boolean.parseBoolean(environment.getProperty("na.swagger.enabled", "true"));
        if (!swaggerEnabled) {
            return new Docket(DocumentationType.SWAGGER_2)
                    .select().apis(RequestHandlerSelectors.none()).build();
        }

        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("na-default") // ✅ 加上这句，避免默认值 default
                .apiInfo(groupApiInfo())
                .select()
                .apis(multiplePackagePredicate())
                .paths(PathSelectors.any())
                .build()
                .globalResponseMessage(RequestMethod.GET, responseMessages())
                .globalResponseMessage(RequestMethod.POST, responseMessages())
                .globalResponseMessage(RequestMethod.PUT, responseMessages())
                .globalResponseMessage(RequestMethod.DELETE, responseMessages())
                .securityContexts(Lists.newArrayList(securityContext()))
                .securitySchemes(Lists.newArrayList(apiKey()));
    }
}

