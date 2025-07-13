package com.na.common.xss;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.na.common.utils.NaCommonUtil;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 针对 JSON 字符串字段的 XSS 防护反序列化器
 *
 * 用于在反序列化时自动清洗含有潜在 XSS 攻击代码的字符串字段内容。
 * 支持通过配置类 {@link NaAutoXssConfig} 设置排除路径。
 */
public class NaXssStringJsonDeserializer extends JsonDeserializer<String> implements ContextualDeserializer {

    /**
     * 配置类（包含路径排除规则）
     */
    private static NaAutoXssConfig config;

    /**
     * 设置配置对象（在 JacksonConfig 中注入）
     * @param cfg
     */
    public static void setConfig(NaAutoXssConfig cfg) {
        config = cfg;
    }

    private boolean escapeOnly = false; // 标记是否只转义，不替换为空

    public NaXssStringJsonDeserializer(boolean escapeOnly) {
        this.escapeOnly = escapeOnly;
    }

    // 构造器（必须）
    public NaXssStringJsonDeserializer() {

    }

    // 常用 XSS 正则表达式集合
    private static final List<String> regexList = new ArrayList<>();

    // 所有正则拼接的统一表达式（用于 containsXss 方法）
    private static final String XSS_PATTERN;

    static {
        regexList.add("alert\\s*\\((.*?)\\)");
        regexList.add("expression\\s*\\((.*?)\\)");
        regexList.add("<script(\\s*)>(.*?)</script(\\s*)>");
        regexList.add("<style(\\s*)>(.*?)</style(\\s*)>");
        regexList.add("<applet(.*?)/>|<applet(.*?)>(.*?)</applet(\\s*)>");
        regexList.add("<object(.*?)/>|<object(.*?)>(.*?)</object(\\s*)>");
        regexList.add("<embed(.*?)/>|<embed(.*?)>(.*?)</embed(\\s*)>");
        regexList.add("eval\\s*\\((.*?)\\)");
        regexList.add("\\'javascript(\\s*):(.*?)\\'|\\\"javascript(\\s*):(.*?)\\\"");
        regexList.add("\\'vbscript(\\s*):(.*?)\\'|\\\"vbscript(\\s*):(.*?)\\\"");
        regexList.add("<iframe\\s+(.*?)/>|<iframe\\s+(.*?)>(.*?)</iframe(\\s*)>");
        regexList.add("(\\s+)onmouseover\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onmouseover\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)onmouseout\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onmouseout\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)onmousedown\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onmousedown\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)onmouseup\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onmouseup\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)onmousemove\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onmousemove\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)onclick\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onclick\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)ondblclick\\s*=(\\s*)\\'(.*?)\\'|(\\s+)ondblclick\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)onkeypress\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onkeypress\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)onkeydown\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onkeydown\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)onkeyup\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onkeyup\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)ondragstart\\s*=(\\s*)\\'(.*?)\\'|(\\s+)ondragstart\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)onerrorupdate\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onerrorupdate\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)onhelp\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onhelp\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)onreadystatechange\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onreadystatechange\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)onrowenter\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onrowenter\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)onrowexit\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onrowexit\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)onselectstart\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onselectstart\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)onload\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onload\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)onunload\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onunload\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)onbeforeunload\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onbeforeunload\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)onblur\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onblur\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)onerror\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onerror\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)onfocus\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onfocus\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)onresize\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onresize\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)onscroll\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onscroll\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)oncontextmenu\\s*=(\\s*)\\'(.*?)\\'|(\\s+)oncontextmenu\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)onbounce\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onbounce\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)onfinish\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onfinish\\s*=(\\s*)\\\"(.*?)\\\"");
        regexList.add("(\\s+)onstart\\s*=(\\s*)\\'(.*?)\\'|(\\s+)onstart\\s*=(\\s*)\\\"(.*?)\\\"");

        StringBuilder combined = new StringBuilder();
        for (String regex : regexList) {
            if (combined.length() > 0) combined.append("|");
            combined.append("(").append(regex).append(")");
        }
        XSS_PATTERN = combined.toString();
    }

    /**
     * 实际的 JSON 字段反序列化逻辑
     */
    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        String source = p.getText();
        if (StringUtils.isNotBlank(source)) {
            String requestPath = "";
            HttpServletRequest request = NaCommonUtil.getCurrentHttpRequest();
            if (request != null) {
                /**
                 * 获取真实的请求路径（去除 contextPath）
                 */
                requestPath = request.getRequestURI().replaceFirst(request.getContextPath(), "");
            }
            if(NaCommonUtil.isExcluded(config.getExcludePaths(),requestPath)){
                return source;
            }
            if (escapeOnly) {
                /**
                 * 只转义HTML特殊字符，不删除任何内容
                 *
                 * 转义成HTML
                 * StringEscapeUtils.unescapeHtml4(转以后的字符串)
                 */
                return StringEscapeUtils.escapeHtml4(source);
            } else {
                /**
                 * 原有正则替换，替换为" "，防止XSS攻击
                 */
                return xssScriptReplace(source);
            }
        }
        return source;
    }

    public static String xssScriptReplace(String value) {
        if (value != null) {
            for (String regex : regexList) {
                Pattern scriptPattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
                value = scriptPattern.matcher(value).replaceAll(" ");
            }
        }
        return value;
    }

    /**
     * 判断输入字符串是否包含 XSS 攻击代码（用于单元测试或手动检测）
     */
    public static boolean containsXss(String input) {
        Pattern pattern = Pattern.compile(XSS_PATTERN, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(input);
        return matcher.find();
    }

    /**
     * 支持 ContextualDeserializer 以确保字段类型一致
     */
    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
        if (property != null) {
            // 判断字段是否有 @NaXssEscapeOnly 注解
            boolean escapeOnlyFlag = property.getAnnotation(NaXssEscapeOnly.class) != null
                    // 或者上下文中也有该注解
                    || property.getContextAnnotation(NaXssEscapeOnly.class) != null;

            // 根据是否有注解创建新的反序列化器实例，并传入标记
            return new NaXssStringJsonDeserializer(escapeOnlyFlag);
        }
        return this; // 保持当前实例
    }


}
