package com.na.common.xss;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.web.util.HtmlUtils;

/**
 * XSS 安全防护包装类(包装原始 HttpServletRequest)
 * <p>
 * 通过继承 HttpServletRequestWrapper, 在常见获取参数方法中统一做 HTML 特殊字符转义,
 * 有效防止前端提交脚本注入(如 {@code <script>})被直接执行
 * <p>
 * 适用范围: 表单参数、Query 参数、Header 参数
 * <br>不适用于 JSON 请求体(@RequestBody), JSON 需通过 Jackson 模块处理
 */
public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {

    /**
     * @param request 原始请求对象
     */
    public XssHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    /**
     * 对 Header 中的值进行 HTML 转义
     *
     * @param name header 名称
     * @return 转义后的 header 值; 原值为 null 时返回 null
     */
    @Override
    public String getHeader(String name) {
        String value = super.getHeader(name);
        return value != null ? HtmlUtils.htmlEscape(value) : null;
    }

    /**
     * 对 URL 查询参数字符串进行 HTML 转义
     *
     * @return 转义后的 queryString; 原值为 null 时返回 null
     */
    @Override
    public String getQueryString() {
        String value = super.getQueryString();
        return value != null ? HtmlUtils.htmlEscape(value) : null;
    }

    /**
     * 对单个参数值进行 HTML 转义
     *
     * @param name 参数名
     * @return 转义后的参数值; 原值为 null 时返回 null
     */
    @Override
    public String getParameter(String name) {
        String value = super.getParameter(name);
        return value != null ? HtmlUtils.htmlEscape(value) : null;
    }

    /**
     * 对多个参数值数组进行 HTML 转义
     *
     * @param name 参数名
     * @return 转义后的参数值数组; 原值为 null 时返回 null
     */
    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (values == null) {
            return null;
        }
        for (int i = 0; i < values.length; i++) {
            values[i] = HtmlUtils.htmlEscape(values[i]);
        }
        return values;
    }
}
