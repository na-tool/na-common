package com.na.common.exceptions;

import com.na.common.result.enums.Status;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口异常注解: 标注在 Controller 方法上, 指定该方法抛出未捕获异常时返回的状态枚举
 * <p>
 * 由 {@link NaApiExceptionHandler} 在兜底处理 {@code Exception.class} 时读取,
 * 根据 value 决定返回给前端的 Status(状态码 + 消息)
 * <p>
 * 示例:
 * <pre>{@code
 * @NaApiException(Status.USER_NOT_EXIST)
 * @GetMapping("/user/{id}")
 * public Result<User> getUser(@PathVariable Long id) {
 *     // 若此处抛出未捕获异常, 前端收到 Status.USER_NOT_EXIST 的响应
 * }
 * }</pre>
 *
 * @see NaApiExceptionHandler
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface NaApiException {
    /**
     * 异常时返回的状态枚举, 默认 DEFAULT
     *
     * @return 状态枚举
     */
    Status value() default Status.DEFAULT;
}
