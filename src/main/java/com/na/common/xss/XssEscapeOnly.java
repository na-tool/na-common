package com.na.common.xss;

import java.lang.annotation.*;

/**
 * 标记字段在 JSON 反序列化时只进行 XSS 转义处理, 不进行清除或替换操作
 * <p>
 * 配合 {@link XssStringJsonDeserializer} 使用, 让该字段只执行 HTML 转义, 防止内容被清空
 * <p>
 * 使用示例:
 * <pre>{@code
 * public class ArticleDTO {
 *     private String title;  // 默认: 正则删除危险标签
 *
 *     // 内容字段只转义不删除(保留富文本内容)
 *     @XssEscapeOnly
 *     private String content;
 * }
 * }</pre>
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface XssEscapeOnly {
}
