package com.na.common.xss;

import lombok.extern.slf4j.Slf4j;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * XSS 防护过滤器
 *
 * 此过滤器用于拦截所有 HTTP 请求，并将其包装为自定义的 {@link NaXssHttpServletRequestWrapper}，
 * 以便统一对请求中的参数（如 header、queryString、form 参数等）进行 HTML 特殊字符转义处理，防止 XSS 攻击。
 *
 * 该过滤器仅作用于非 JSON 类型的请求数据（即：@RequestParam、@RequestHeader、表单提交、URL 参数等）。
 * 对于 JSON 类型（@RequestBody）的参数，需要使用 Jackson 反序列化器单独处理。
 */
@Slf4j
public class NaXssFilter implements Filter {

    private final List<String> excludePaths;

    public NaXssFilter(List<String> excludePaths) {
        this.excludePaths = excludePaths != null ? excludePaths : new ArrayList<>();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String requestUri = httpRequest.getRequestURI();

        requestUri = requestUri.replaceFirst(httpRequest.getContextPath(), "");
        if (isExcluded(excludePaths,requestUri)) {
            chain.doFilter(request, response); // 不包装
        } else {
            chain.doFilter(new NaXssHttpServletRequestWrapper(httpRequest), response);
        }
    }

    /**
     * 支持简单通配符和正则匹配的路径判断
     * 判断路径是否在 xss 路径白名单中
     */
    private boolean isExcluded(List<String> requestUris, String requestUri) {
        for (String uri : requestUris) {
            // 将 uriPattern 转换为正则表达式
            String regex = uri.replace("/*", "/[^/]*");

            // 使用 Pattern 进行匹配
            if (Pattern.matches(regex, requestUri)) {
                log.debug("[XSS] 命中路径白名单: {}", requestUri);
                return true;
            }
        }
        return false;
    }

    @Override public void init(FilterConfig filterConfig) {}
    @Override public void destroy() {}
}
