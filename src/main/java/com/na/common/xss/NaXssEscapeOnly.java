package com.na.common.xss;

import java.lang.annotation.*;

/**
 * 标记字段在 JSON 反序列化时只进行 XSS 转义处理，
 * 不进行清除或替换操作。
 *
 * 用于配合 {@link NaXssStringJsonDeserializer}，
 * 让该字段内容只执行转义，防止将内容清空或移除。
 *
 * 适用于需要保留用户输入内容但防止 XSS 攻击的场景。
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NaXssEscapeOnly {
}
