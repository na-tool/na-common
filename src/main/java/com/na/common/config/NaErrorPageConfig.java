package com.na.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

@Configuration
@ConditionalOnProperty(
        name = {"na.errorPage"},
        matchIfMissing = false
)
public class NaErrorPageConfig {
    @Bean
    public WebServerFactoryCustomizer<ConfigurableWebServerFactory> webServerFactoryCustomizer() {
        return (factory -> {
            ErrorPage errorPage401 = new ErrorPage(HttpStatus.UNAUTHORIZED, "/401.html");
            ErrorPage errorPage403 = new ErrorPage(HttpStatus.FORBIDDEN, "/403.html");
            ErrorPage errorPage404 = new ErrorPage(HttpStatus.NOT_FOUND, "/404.html");
            ErrorPage errorPage500 = new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR, "/500.html");
            factory.addErrorPages(errorPage401,errorPage403,
                    errorPage404,errorPage500);
        });
    }
}
