package com.na.common.config;

import com.github.xiaoymin.knife4j.spring.annotations.EnableKnife4j;
import com.google.common.collect.Lists;
import com.na.common.constant.INaGlobalConst;
import com.na.common.result.enums.NaStatus;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RequestMethod;
import springfox.documentation.RequestHandler;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.builders.ResponseMessageBuilder;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.service.*;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebMvc;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Configuration
@EnableSwagger2WebMvc
@EnableKnife4j
@ConditionalOnProperty(name = "na.swagger.package", matchIfMissing = false)
public class NaSwaggerConfiguration {
    @Autowired
    private Environment environment;

    @Bean
//    @Order(1)
    public Docket groupRestApi()
    {
        // 通过读取配置文件中的布尔值来决定是否启用Swagger
        boolean swaggerEnabled = Boolean.parseBoolean(environment.getProperty("na.swagger.enabled", "true"));
        boolean swaggerGroup = Boolean.parseBoolean(environment.getProperty("na.swagger.group", "false"));
        // 完全禁用Swagger
        if(!swaggerEnabled && !swaggerGroup){
            return new Docket(DocumentationType.SWAGGER_2).select().apis(RequestHandlerSelectors.none()).build();
        }

        // 如果启用了 Swagger 分组模式，返回一个 enable(false) 的 Docket，Docket 对象会注册，但实际不启用文档生成。可能是为了留出分组结构，在别处再设置启用的 Docket 分组
        if(swaggerGroup){
            return new Docket(DocumentationType.SWAGGER_2).select().apis(RequestHandlerSelectors.none()).build().enable(false);
        }

        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(groupApiInfo())
                .select()
                .apis(multiplePackagePredicate())
                .paths(PathSelectors.any())
                .build()
                .globalResponseMessage(RequestMethod.GET,responseMessages())
                .globalResponseMessage(RequestMethod.POST,responseMessages())
                .globalResponseMessage(RequestMethod.PUT,responseMessages())
                .globalResponseMessage(RequestMethod.DELETE,responseMessages())
                .securityContexts(Lists.newArrayList(securityContext()))
                .securitySchemes(Lists.<SecurityScheme>newArrayList(apiKey()));
    }

    protected List<ResponseMessage> responseMessages() {
        return Arrays.stream(NaStatus.values()).map(a -> new ResponseMessageBuilder().code(a.getCode())
                .responseModel(new ModelRef("非http响应状态,此状态对应ResponseBody中的code字段:" + a.getMsg()))
                .build()
        ).collect(Collectors.toList());
    }


    protected ApiInfo groupApiInfo() {
        InetAddress localHost = null;
        try {
            localHost = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            System.out.println(e);
        }
        Contact contact = new Contact(environment.getProperty("na.swagger.contact.name", "na"),
                environment.getProperty("na.swagger.contact.url", ""),
                environment.getProperty("na.swagger.contact.email", "na_tool@163.com"));
        return new ApiInfoBuilder()
                .title(environment.getProperty("na.swagger.title", environment.getProperty("spring.application.name") + "模块"))
                .description(environment.getProperty("na.swagger.description", "<div style='font-size:14px;color:red;'>na RESTful Apis</div>"))
                .termsOfServiceUrl(environment.getProperty("na.swagger.termsOfServiceUrl", localHost == null ? "127.0.0.1" : localHost.getHostAddress()))
                .contact(contact)
                .license(environment.getProperty("na.swagger.license", ""))
                .licenseUrl(environment.getProperty("na.swagger.licenseUrl", ""))
                .version(environment.getProperty("na.swagger.version", "v1.0.0"))
                .build();
    }

    protected List<ApiKey> apiKey() {
        ApiKey apiKey1 = new ApiKey("认证令牌", INaGlobalConst.SECURITY.HEADER_STRING, "header");
        ApiKey apiKey2 = new ApiKey("请求来源",INaGlobalConst.SECURITY.REQUEST_SOURCE, "header");
        ArrayList<ApiKey> apiKeys = new ArrayList<>();
        apiKeys.add(apiKey1);
        apiKeys.add(apiKey2);
        return apiKeys;
    }

    protected SecurityContext securityContext() {
        return SecurityContext.builder()
                .securityReferences(defaultAuth())
                .forPaths(PathSelectors.regex("/.*"))
                .build();
    }

    protected List<SecurityReference> defaultAuth() {
        AuthorizationScope authorizationScope = new AuthorizationScope("global", "accessEverything");
        AuthorizationScope[] authorizationScopes = new AuthorizationScope[1];
        authorizationScopes[0] = authorizationScope;
        return Lists.newArrayList(new SecurityReference("认证令牌", authorizationScopes), new SecurityReference("请求来源", authorizationScopes));
    }

    protected Predicate<RequestHandler> multiplePackagePredicate() {
        // 从一个空谓词开始
        Predicate<RequestHandler> requestHandlerPredicate = Predicate.isEqual(null);
        // 始终包括基础包
        requestHandlerPredicate = requestHandlerPredicate.or(RequestHandlerSelectors.basePackage("com.na"));

        String property = environment.getProperty("na.swagger.package", "");
        if (StringUtils.isNotEmpty(property)) {
            for (String api : property.split(",")) {
                requestHandlerPredicate = requestHandlerPredicate.or(RequestHandlerSelectors.basePackage(api.trim()));
            }
        }

        return requestHandlerPredicate;
    }
}
