package com.na.common.xss;

import com.na.common.utils.CommonUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * XSS 防护过滤器
 * <p>
 * 拦截所有 HTTP 请求, 包装为 {@link XssHttpServletRequestWrapper},
 * 统一对请求参数(header、queryString、form 参数)做 HTML 特殊字符转义, 防止 XSS 攻击
 * <p>
 * 仅作用于非 JSON 类型请求数据(@RequestParam、@RequestHeader、表单提交、URL 参数)
 * <br>JSON 类型(@RequestBody)需通过 Jackson 反序列化器单独处理
 */
@Slf4j
public class XssFilter implements Filter {

    private final List<String> excludePaths;

    /**
     * @param excludePaths XSS 过滤器排除路径(不拦截)
     */
    public XssFilter(List<String> excludePaths) {
        this.excludePaths = excludePaths != null ? excludePaths : new ArrayList<>();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String requestUri = httpRequest.getRequestURI();

        // 去除 contextPath, 用 replace 而非 replaceFirst(contextPath 不是正则)
        String contextPath = httpRequest.getContextPath();
        String cleanUri = contextPath.isEmpty()
                ? requestUri
                : requestUri.replace(contextPath, "");

        if (CommonUtil.isExcluded(excludePaths, cleanUri)) {
            // 排除路径不包装
            chain.doFilter(request, response);
        } else {
            chain.doFilter(new XssHttpServletRequestWrapper(httpRequest), response);
        }
    }
}
