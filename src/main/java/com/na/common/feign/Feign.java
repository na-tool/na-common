package com.na.common.feign;

import java.lang.annotation.*;

/**
 * 标记方法为 Feign 内部调用入口
 * <p>
 * 被 {@link FeignAop} 拦截, 校验请求头是否携带 Feign 标识(from=Y & na-feign=Y),
 * 防止外部直接访问本应只允许服务间调用的接口
 * <p>
 * 使用示例:
 * <pre>{@code
 * @Feign
 * @GetMapping("/internal/user/{id}")
 * public Result<User> getInternalUser(@PathVariable Long id) {
 *     // 仅允许 Feign 调用, 外部请求会被 FeignAop 拦截
 *     return Result.success(userService.getById(id));
 * }
 * }</pre>
 *
 * @author na
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Feign {
}
