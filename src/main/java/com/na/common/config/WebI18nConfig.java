package com.na.common.config;

import com.na.common.config.service.ILanguageService;
import com.na.common.utils.LocalLangUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.util.Locale;

/**
 * 国际化配置(通过 application.yml 中 na.i18n=true 开启)
 * <p>
 * <b>整体机制</b>:
 * <ul>
 *   <li>默认语言为中文(zh-CN); 翻译文件位于 classpath:static/i18n/ 目录,
 *       命名约定 messages.properties(默认兜底) / messages_zh_CN.properties / messages_en_US.properties ...
 *       </li>
 *   <li>语言优先级: 请求头 Accept-Language(取第一段) &gt; 业务方 {@link ILanguageService} 自定义逻辑 &gt; 默认中文;
 *       查询参数 ?lang=en-US 仍受支持(由 LocaleChangeInterceptor 基础能力提供, 优先级最高)</li>
 *   <li>翻译取值: {@link LocalLangUtil#get(String, String)} 或 {@code Status.getLanguageMsg()};
 *       资源文件中 key 不存在时回退内置英文, 保证文案永不为空</li>
 * </ul>
 * <p>
 * 开启示例(application.yml):
 * <pre>{@code
 * na:
 *   i18n: true              # 开启国际化
 *   staticWebSource: true   # 可选: 放行静态资源
 * }</pre>
 */
@ConditionalOnProperty(
        name = {"na.i18n.enable"},
        havingValue = "true",
        matchIfMissing = true
)
public class WebI18nConfig implements WebMvcConfigurer {

    /** 翻译文件 basename: 对应 classpath:static/i18n/messages*.properties(排在前优先匹配); 第二个兼容 Spring Boot 默认位置 */
    private static final String[] MESSAGE_BASENAMES = {"static/i18n/messages", "messages"};

    @Autowired
    private Environment environment;

    /**
     * 业务方自定义语言逻辑 SPI(无请求头时的兜底, 如从用户配置/租户配置读取语言)
     * required=false: 业务方未提供实现时优雅降级为默认中文, 而不是启动失败
     */
    @Autowired(required = false)
    private ILanguageService languageService;

    /**
     * i18n 资源文件 MessageSource: 读取 classpath:static/i18n/messages*.properties
     * <p>
     * 说明:
     * <ul>
     *   <li>默认 bundle(messages.properties)内容为中文, 作为所有语言的最终兜底 → 未配置的语言回落为中文默认</li>
     *   <li>fallbackToSystemLocale=false: 禁止回落到操作系统语言, 保证行为只取决于请求语言与默认文件</li>
     *   <li>@ConditionalOnMissingBean: 业务方已自定义 MessageSource 时不覆盖</li>
     * </ul>
     *
     * @return MessageSource 实例
     */
    @Bean
    @ConditionalOnMissingBean(MessageSource.class)
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames(MESSAGE_BASENAMES);
        // properties 文件统一按 UTF-8 解析, 支持直接书写中文
        messageSource.setDefaultEncoding("UTF-8");
        // 关闭系统语言回落, 避免 zh_TW 等未配置语言随机使用部署机器的系统语言
        messageSource.setFallbackToSystemLocale(false);
        return messageSource;
    }

    /**
     * 地区解析器: 基于 Session 存储语言, 默认中文(zh-CN)
     * <p>
     * 未命中任何语言来源(请求头/参数/业务 SPI)时, 按 zh-CN 返回翻译
     *
     * @return 地区解析器实例
     */
    @Bean
    public LocaleResolver localeResolver() {
        SessionLocaleResolver localeResolver = new SessionLocaleResolver();
        localeResolver.setDefaultLocale(Locale.CHINA);
        return localeResolver;
    }

    /**
     * 国际化拦截器: 每个请求开始时确定当前语言
     * <p>
     * 处理顺序: Accept-Language 请求头 → 业务方 {@link ILanguageService} → 最后执行父类逻辑
     * 处理 ?lang= 查询参数(如 /api/user?lang=en-US), 参数存在时覆盖前面的语言设置
     *
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
                        // 从 Accept-Language 请求头解析语言, 如 "zh-CN,zh;q=0.9" 取第一段 "zh-cn"
                        String acceptLanguage = request.getHeader("Accept-Language");
                        if (StringUtils.hasText(acceptLanguage)) {
                            String language = acceptLanguage.split(",")[0].trim().toLowerCase();
                            LocalLangUtil.setLocale(language);
                        } else if (languageService != null) {
                            // 无语言头时由业务方自定义语言逻辑(SPI 实现, 如按用户偏好设置)
                            languageService.setLanguage();
                        }
                        // 父类逻辑支持 ?lang= 查询参数覆盖语言设置, 必须最后调用
                        return super.preHandle(request, response, handler);
                    }
                };
                // 支持 ?lang= 查询参数切换语言
                localeInterceptor.setParamName("lang");
                registry.addInterceptor(localeInterceptor);
            }
        };
    }

    /**
     * 静态资源放行: na.staticWebSource=true 时生效
     * <p>
     * 不配置时拦截器会拦截静态资源请求导致 404, 因此需要显式注册资源映射
     *
     * @param registry 资源处理器注册器
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        boolean staticEnabled = Boolean.parseBoolean(environment.getProperty("na.staticWebSource", "false"));
        if (staticEnabled) {
            // 应用自身静态资源
            registry.addResourceHandler("/**")
                    .addResourceLocations("classpath:/static/");

            // Swagger UI
            registry.addResourceHandler("/swagger-ui.html")
                    .addResourceLocations("classpath:/META-INF/resources/");

            // Knife4j 文档
            registry.addResourceHandler("doc.html")
                    .addResourceLocations("classpath:/META-INF/resources/");

            // Swagger 依赖的 webjars
            registry.addResourceHandler("/webjars/**")
                    .addResourceLocations("classpath:/META-INF/resources/webjars/");
        }
    }
}
