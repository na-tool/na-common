package com.na.common.feign;

import java.lang.annotation.*;

/**
 * 服务无 Token 调用声明注解
 * <p>
 * 标注在方法上, 表示该方法允许无 Token 访问(如内部服务调用、健康检查等)
 * <p>
 * 使用示例:
 * <pre>{@code
 * @Token
 * @GetMapping("/health")
 * public String health() {
 *     return "OK";
 * }
 * }</pre>
 *
 * @author na
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Token {
}
