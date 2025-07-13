package com.na.common.xss;

import org.apache.commons.lang3.StringEscapeUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.Objects;

/**
 * XSS 安全防护包装类（用于包装原始 HttpServletRequest）
 *
 * 原理：
 * 通过继承 HttpServletRequestWrapper，在常见获取参数方法中统一做 XSS 特殊字符转义（HTML 转义），
 * 有效防止前端提交脚本注入（如 {@code <script>}）被直接执行。
 *
 * 适用范围：
 * - 表单参数（form-data / application/x-www-form-urlencoded）
 * - Query 参数（URL?key=value）
 * - Header 参数
 * 不适用于 JSON 请求体（@RequestBody），JSON 需要通过 Jackson 模块额外处理。
 */
public class NaXssHttpServletRequestWrapper extends HttpServletRequestWrapper {

    public NaXssHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    /**
     * 对 Header 中的值进行 HTML 转义
     */
    @Override
    public String getHeader(String name) {
        return StringEscapeUtils.escapeHtml4(super.getHeader(name));
    }

    /**
     * 对 URL 查询参数字符串进行 HTML 转义
     */
    @Override
    public String getQueryString() {
        return StringEscapeUtils.escapeHtml4(super.getQueryString());
    }

    /**
     * 对单个参数值进行 HTML 转义
     */
    @Override
    public String getParameter(String name) {
        return StringEscapeUtils.escapeHtml4(super.getParameter(name));
    }

    /**
     * 对多个参数值数组进行 HTML 转义
     */
    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (Objects.isNull(values)) return null;

        for (int i = 0; i < values.length; i++) {
            values[i] = StringEscapeUtils.escapeHtml4(values[i]);
        }

        return values;
    }
}
