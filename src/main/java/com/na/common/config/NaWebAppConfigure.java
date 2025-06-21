package com.na.common.config;

import com.na.common.config.service.INaLanguageService;
import com.na.common.utils.NaLocalLangUtil;
import com.na.common.utils.NaSpringContextUtil;
import org.apache.poi.ss.formula.functions.Na;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.Locale;

@Configuration
@ConditionalOnProperty(
        name = {"na.i18n"},
        matchIfMissing = false
)
public class NaWebAppConfigure implements WebMvcConfigurer {

    @Autowired
    private Environment environment;
    @Autowired
    private INaLanguageService naLanguageService;

    /**
     * 默认解析器 其中locale表示默认语言
     * @return 地区解析器实例
     */
    @Bean
    public LocaleResolver localeResolver() {
        SessionLocaleResolver localeResolver = new SessionLocaleResolver();
        localeResolver.setDefaultLocale(Locale.CHINA);
        return localeResolver;
    }

    /**
     * 默认拦截器 其中lang表示切换语言的参数名
     * 添加国际化拦截器
     * @return WebMvcConfigurer 实例
     */
    @Bean
    public WebMvcConfigurer localeInterceptor() {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                LocaleChangeInterceptor localeInterceptor = new LocaleChangeInterceptor() {
                    @Override
                    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws ServletException {
                        // 获取 Accept-Language 请求头并解析
                        String lang = request.getHeader("Accept-Language");
                        if (lang != null && !lang.isEmpty()) {
                            String language = lang.split(",")[0].toLowerCase();  // 获取第一个语言
                            NaLocalLangUtil.setLocale(language);
                        }else {
                            naLanguageService.setLanguage();
                        }
                        return super.preHandle(request, response, handler);
                    }
                };
                localeInterceptor.setParamName("lang");  // 保留对查询参数 lang 的支持
                registry.addInterceptor(localeInterceptor);
            }
        };
    }

    /**
     * 如果不加，则静态资源会被拦截，导致访问不到静态资源
     * @param registry registry
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        boolean staticEnabled = Boolean.parseBoolean(environment.getProperty("na.staticWebSource", "false"));
        if (staticEnabled) {
            registry.addResourceHandler("/**")
                    .addResourceLocations("classpath:/static/");  // Serve static files

            registry.addResourceHandler("/swagger-ui.html")
                    .addResourceLocations("classpath:/META-INF/resources/");  // Swagger UI

            registry.addResourceHandler("doc.html")
                    .addResourceLocations("classpath:/META-INF/resources/");  // Knife4j doc.html

            registry.addResourceHandler("/webjars/**")
                    .addResourceLocations("classpath:/META-INF/resources/webjars/");  // Webjars for Swagger UI
        }
    }
}

