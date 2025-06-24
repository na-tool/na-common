package com.na.common.config;

import com.google.common.collect.Lists;
import com.na.common.constant.INaGlobalConst;
import com.na.common.result.enums.NaStatus;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import springfox.documentation.RequestHandler;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.builders.ResponseMessageBuilder;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.service.*;
import springfox.documentation.spi.service.contexts.SecurityContext;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Configuration
public class NaSwaggerConfig {
    @Autowired
    private Environment environment;

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
        String property = environment.getProperty("na.swagger.package", "");
        List<String> packages = new ArrayList<>();
        packages.add("com.na"); // 默认基础包

        if (StringUtils.isNotBlank(property)) {
            packages.addAll(Arrays.stream(property.split(","))
                    .map(String::trim)
                    .collect(Collectors.toList()));
        }

        return packages.stream()
                .map(RequestHandlerSelectors::basePackage)
                .reduce(x -> false, Predicate::or);
    }
}
