package com.na.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 内嵌 Web 容器全局错误页配置
 * <p>
 * 将常见 HTTP 错误状态码统一转发到静态错误页(默认指向 classpath:static 下的同名 html),
 * 避免用户看到容器原生的堆栈式 Whitelabel 错误页, 提升产品体验一致性。
 * <p>
 * 生效条件:
 * <ul>
 *     <li>项目 static 目录下需存在对应错误页文件: static/401.html、static/403.html、static/404.html、static/500.html</li>
 *     <li>配置项 na.errorPage.enable 未显式置为 false(默认开启)</li>
 * </ul>
 * <p>
 * 注意事项:
 * <ul>
 *     <li>仅对内嵌容器(Tomcat/Jetty/Undertow)生效; 若前端与后端分离部署, 404 等错误通常由前端网关处理, 可通过 na.errorPage.enable=false 关闭</li>
 *     <li>错误页路径必须以 "/" 开头, 否则容器无法正确转发</li>
 *     <li>新增错误码只需在 {@link #ERROR_PAGES} 中加一行映射, 无需改动注册逻辑</li>
 * </ul>
 * <p>
 * 示例:
 * <pre>{@code
 * // application.yml 中关闭该功能(如前端自行处理错误页时):
 * na:
 *   error-page:
 *     enable: false
 * }</pre>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        name = {"na.errorPage.enable"},
        havingValue = "true",
        matchIfMissing = true
)
public class ErrorPageConfig {

    /**
     * HTTP 状态码 -> 错误页路径 映射表
     * <p>
     * 使用 LinkedHashMap 保持声明顺序; 新增错误页支持时在此添加映射即可, 不可修改(只读视图)
     */
    private static final Map<HttpStatus, String> ERROR_PAGES;

    static {
        Map<HttpStatus, String> pages = new LinkedHashMap<>();
        pages.put(HttpStatus.UNAUTHORIZED, "/401.html");
        pages.put(HttpStatus.FORBIDDEN, "/403.html");
        pages.put(HttpStatus.NOT_FOUND, "/404.html");
        pages.put(HttpStatus.INTERNAL_SERVER_ERROR, "/500.html");
        // 包裹为只读视图, 防止运行期被意外修改
        ERROR_PAGES = Collections.unmodifiableMap(pages);
    }

    /**
     * 注册错误页的自定义器
     * <p>
     * 容器启动前遍历 {@link #ERROR_PAGES}, 将每个状态码对应的错误页注册到内嵌容器
     * <p>
     * 示例: {@code // 由 Spring 容器自动装配调用, 无需手动调用}
     *
     * @return 错误页工厂定制器, 用于向内嵌 Web 容器注册错误页映射
     */
    @Bean
    public WebServerFactoryCustomizer<ConfigurableWebServerFactory> webServerFactoryCustomizer() {
        return factory -> ERROR_PAGES.forEach(
                (status, path) -> factory.addErrorPages(new ErrorPage(status, path)));
    }
}
