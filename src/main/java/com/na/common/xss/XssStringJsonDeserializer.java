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
import java.util.regex.Pattern;

/**
 * 针对 JSON 字符串字段的 XSS 防护反序列化器
 * <p>
 * 在 Jackson 反序列化 {@code @RequestBody} 的 String 字段时自动清洗 XSS 攻击代码。
 * 支持两种模式, 两者都是安全且无损于语义的:
 * <ul>
 *   <li><b>默认模式</b>: 移除全部 HTML 标签 —— 事件处理器必须依附在标签内,
 *       标签整体消失后就没有可执行的向量, 因此不存在"绕过"</li>
 *   <li>{@link XssEscapeOnly} 模式: 把 HTML 特殊字符转义为实体, 原样保留文本</li>
 * </ul>
 * <p><b>为什么不再按事件名做黑名单匹配</b>: 黑名单只能穷举已知写法,
 * {@code <img src=x onerror=alert(1)>}、{@code <svg onload=...>} 这类无引号属性
 * 可以直接穿过, 属可绕过的防护。默认模式改为"整标签移除"后不再依赖枚举攻击特征。
 * <p><b>富文本字段</b>：默认模式会去掉格式标记。若某字段确实要保留 HTML 并在前端渲染,
 * 请在应用层做白名单净化（如 OWASP Java HTML Sanitizer）；本类不做不安全的放行。
 * <p>通过 {@link AutoXssConfig} 的 excludePaths 排除特定路径不做过滤
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

    /**
     * 匹配任意 HTML 标签。
     * <p>闭合的 {@code >} 是可选的: 未闭合的写法（如 {@code <img src=x onerror=alert(1)}）
     * 在浏览器容错解析下同样会形成标签并触发事件处理器，因此必须一并移除。
     * <p>{@code [^<>]*} 保证匹配不跨越标签边界, 不会把两段正常文本一起吃掉。
     */
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^<>]*>?", Pattern.DOTALL);

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
     * 移除字符串中的全部 HTML 标签
     * <p>标签成对消失后标签之间的纯文本会保留（如 {@code <b>粗体</b>} → {@code 粗体}）；
     * 裸的 {@code <} 也会被移除，这是"宁可多删不可漏放"的取舍。
     *
     * @param value 原始字符串
     * @return 移除标签后的字符串; null 输入返回 null
     */
    public static String xssScriptReplace(String value) {
        if (value == null) {
            return null;
        }
        return HTML_TAG_PATTERN.matcher(value).replaceAll("");
    }

    /**
     * 判断输入字符串中是否含有 HTML 标签
     * <p>判定口径与 {@link #xssScriptReplace(String)} 完全一致:
     * "含有标签"即"需要清洗"。名字沿用历史 API，语义以本注释为准。
     *
     * @param input 待检测的输入字符串
     * @return true 含 HTML 标签; false 不含或输入为 null
     */
    public static boolean containsXss(String input) {
        if (input == null) {
            return false;
        }
        return HTML_TAG_PATTERN.matcher(input).find();
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
