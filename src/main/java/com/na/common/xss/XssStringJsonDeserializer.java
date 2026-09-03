package com.na.common.xss;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.na.common.utils.CommonUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 针对 JSON 字符串字段的 XSS 防护反序列化器
 * <p>
 * 在 Jackson 反序列化 {@code @RequestBody} 的 String 字段时自动清洗 XSS 攻击代码。
 * 支持两种模式:
 * <ul>
 *   <li>默认模式: 正则匹配并删除危险标签/事件(如 {@code <script>}, {@code onclick=})</li>
 *   <li>{@link XssEscapeOnly} 模式: 仅 HTML 转义特殊字符, 保留原文</li>
 * </ul>
 * 通过 {@link AutoXssConfig} 的 excludePaths 排除特定路径不做过滤
 */
public class XssStringJsonDeserializer extends JsonDeserializer<String> implements ContextualDeserializer {

    /** 全局 XSS 配置(含排除路径), 由 JacksonConfig 注入 */
    private static AutoXssConfig config;

    /**
     * 设置全局 XSS 配置对象(通常在 JacksonConfig 中注入)
     *
     * @param cfg XSS 配置对象, 用于控制排除路径等规则
     */
    public static void setConfig(AutoXssConfig cfg) {
        config = cfg;
    }

    /** 是否只转义不删除(由 @XssEscapeOnly 注解决定) */
    private final boolean escapeOnly;

    /**
     * 默认构造器(Jackson 反射需要)
     */
    public XssStringJsonDeserializer() {
        this(false);
    }

    /**
     * @param escapeOnly true 时只做 HTML 转义, 不做正则删除
     */
    public XssStringJsonDeserializer(boolean escapeOnly) {
        this.escapeOnly = escapeOnly;
    }

    // === XSS 正则规则 ===

    /** 原始正则表达式列表 */
    private static final List<String> REGEX_LIST = new ArrayList<>();

    /** 预编译的正则数组(避免每次调用重新编译, 性能提升明显) */
    private static final Pattern[] COMPILED_PATTERNS;

    /** 所有正则拼接的统一表达式(用于 containsXss 快速检测) */
    private static final Pattern XSS_COMBINED_PATTERN;

    static {
        REGEX_LIST.add("alert\\s*\\((.*?)\\)");
        REGEX_LIST.add("expression\\s*\\((.*?)\\)");
        REGEX_LIST.add("<script(\\s*)>(.*?)</script(\\s*)>");
        REGEX_LIST.add("<style(\\s*)>(.*?)</style(\\s*)>");
        REGEX_LIST.add("<applet(.*?)/>|<applet(.*?)>(.*?)</applet(\\s*)>");
        REGEX_LIST.add("<object(.*?)/>|<object(.*?)>(.*?)</object(\\s*)>");
        REGEX_LIST.add("<embed(.*?)/>|<embed(.*?)>(.*?)</embed(\\s*)>");
        REGEX_LIST.add("eval\\s*\\((.*?)\\)");
        REGEX_LIST.add("\\'javascript(\\s*):(.*?)\\'|\\\"javascript(\\s*):(.*?)\\\"");
        REGEX_LIST.add("\\'vbscript(\\s*):(.*?)\\'|\\\"vbscript(\\s*):(.*?)\\\"");
        REGEX_LIST.add("<iframe\\s+(.*?)/>|<iframe\\s+(.*?)>(.*?)</iframe(\\s*)>");
        REGEX_LIST.add("(\\s+)onmouseover\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onmouseover\\s*=(\\s*)\\\"(.*?)\\\"");
        REGEX_LIST.add("(\\s+)onmouseout\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onmouseout\\s*=(\\s*)\\\"(.*?)\\\"");
        REGEX_LIST.add("(\\s+)onmousedown\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onmousedown\\s*=(\\s*)\\\"(.*?)\\\"");
        REGEX_LIST.add("(\\s+)onmouseup\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onmouseup\\s*=(\\s*)\\\"(.*?)\\\"");
        REGEX_LIST.add("(\\s+)onmousemove\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onmousemove\\s*=(\\s*)\\\"(.*?)\\\"");
        REGEX_LIST.add("(\\s+)onclick\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onclick\\s*=(\\s*)\\\"(.*?)\\\"");
        REGEX_LIST.add("(\\s+)ondblclick\\s*=(\\s*)\\'(.*?)\\'|(\\s+)ondblclick\\s*=(\\s*)\\\"(.*?)\\\"");
        REGEX_LIST.add("(\\s+)onkeypress\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onkeypress\\s*=(\\s*)\\\"(.*?)\\\"");
        REGEX_LIST.add("(\\s+)onkeydown\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onkeydown\\s*=(\\s*)\\\"(.*?)\\\"");
        REGEX_LIST.add("(\\s+)onkeyup\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onkeyup\\s*=(\\s*)\\\"(.*?)\\\"");
        REGEX_LIST.add("(\\s+)ondragstart\\s*=(\\s*)\\'(.*?)\\'|(\\s+)ondragstart\\s*=(\\s*)\\\"(.*?)\\\"");
        REGEX_LIST.add("(\\s+)onerrorupdate\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onerrorupdate\\s*=(\\s*)\\\"(.*?)\\\"");
        REGEX_LIST.add("(\\s+)onhelp\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onhelp\\s*=(\\s*)\\\"(.*?)\\\"");
        REGEX_LIST.add("(\\s+)onreadystatechange\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onreadystatechange\\s*=(\\s*)\\\"(.*?)\\\"");
        REGEX_LIST.add("(\\s+)onrowenter\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onrowenter\\s*=(\\s*)\\\"(.*?)\\\"");
        REGEX_LIST.add("(\\s+)onrowexit\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onrowexit\\s*=(\\s*)\\\"(.*?)\\\"");
        REGEX_LIST.add("(\\s+)onselectstart\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onselectstart\\s*=(\\s*)\\\"(.*?)\\\"");
        REGEX_LIST.add("(\\s+)onload\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onload\\s*=(\\s*)\\\"(.*?)\\\"");
        REGEX_LIST.add("(\\s+)onunload\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onunload\\s*=(\\s*)\\\"(.*?)\\\"");
        REGEX_LIST.add("(\\s+)onbeforeunload\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onbeforeunload\\s*=(\\s*)\\\"(.*?)\\\"");
        REGEX_LIST.add("(\\s+)onblur\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onblur\\s*=(\\s*)\\\"(.*?)\\\"");
        REGEX_LIST.add("(\\s+)onerror\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onerror\\s*=(\\s*)\\\"(.*?)\\\"");
        REGEX_LIST.add("(\\s+)onfocus\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onfocus\\s*=(\\s*)\\\"(.*?)\\\"");
        REGEX_LIST.add("(\\s+)onresize\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onresize\\s*=(\\s*)\\\"(.*?)\\\"");
        REGEX_LIST.add("(\\s+)onscroll\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onscroll\\s*=(\\s*)\\\"(.*?)\\\"");
        REGEX_LIST.add("(\\s+)oncontextmenu\\s*=(\\s*)\\'(.*?)\\'|(\\s+)oncontextmenu\\s*=(\\s*)\\\"(.*?)\\\"");
        REGEX_LIST.add("(\\s+)onbounce\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onbounce\\s*=(\\s*)\\\"(.*?)\\\"");
        REGEX_LIST.add("(\\s+)onfinish\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onfinish\\s*=(\\s*)\\\"(.*?)\\\"");
        REGEX_LIST.add("(\\s+)onstart\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onstart\\s*=(\\s*)\\\"(.*?)\\\"");

        // 预编译所有正则, 避免每次调用重新编译
        COMPILED_PATTERNS = new Pattern[REGEX_LIST.size()];
        StringBuilder combined = new StringBuilder();
        for (int i = 0; i < REGEX_LIST.size(); i++) {
            String regex = REGEX_LIST.get(i);
            COMPILED_PATTERNS[i] = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            if (combined.length() > 0) combined.append("|");
            combined.append("(").append(regex).append(")");
        }
        XSS_COMBINED_PATTERN = Pattern.compile(combined.toString(),
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
    }

    /**
     * 实际的 JSON 字段反序列化逻辑
     *
     * @param p    Jackson 解析器
     * @param ctxt 反序列化上下文
     * @return 清洗后的字符串
     */
    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        String source = p.getText();
        if (StringUtils.isBlank(source)) {
            return source;
        }

        // 获取请求路径用于排除判断
        String requestPath = "";
        HttpServletRequest request = CommonUtil.getCurrentHttpRequest();
        if (request != null) {
            // 去除 contextPath, 得到应用内真实路径
            String contextPath = request.getContextPath();
            requestPath = contextPath.isEmpty()
                    ? request.getRequestURI()
                    : request.getRequestURI().replace(contextPath, "");
        }

        // 排除路径不做 XSS 过滤
        if (config != null && CommonUtil.isExcluded(config.getExcludePaths(), requestPath)) {
            return source;
        }

        if (escapeOnly) {
            // 只转义 HTML 特殊字符, 不删除内容
            return HtmlUtils.htmlEscape(source);
        } else {
            // 正则匹配并删除危险标签/事件
            return xssScriptReplace(source);
        }
    }

    /**
     * 用预编译正则删除 XSS 攻击代码
     *
     * @param value 原始字符串
     * @return 清洗后的字符串; null 输入返回 null
     */
    public static String xssScriptReplace(String value) {
        if (value == null) {
            return null;
        }
        for (Pattern pattern : COMPILED_PATTERNS) {
            value = pattern.matcher(value).replaceAll("");
        }
        return value;
    }

    /**
     * 判断输入字符串是否包含可能的 XSS 攻击代码
     *
     * @param input 待检测的输入字符串
     * @return true 包含 XSS 攻击代码; false 不包含或输入为 null
     */
    public static boolean containsXss(String input) {
        if (input == null) {
            return false;
        }
        return XSS_COMBINED_PATTERN.matcher(input).find();
    }

    /**
     * 根据 @XssEscapeOnly 注解创建上下文化的反序列化器实例
     *
     * @param ctxt     反序列化上下文
     * @param property 当前字段属性
     * @return 带 escapeOnly 标记的新实例
     */
    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
        if (property != null) {
            boolean escapeOnlyFlag = property.getAnnotation(XssEscapeOnly.class) != null
                    || property.getContextAnnotation(XssEscapeOnly.class) != null;
            return new XssStringJsonDeserializer(escapeOnlyFlag);
        }
        return this;
    }
}
