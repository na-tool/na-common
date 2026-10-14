package com.na.common.exceptions;

import org.springframework.http.HttpStatus;

/**
 * 禁止访问异常(403): 用户已认证但无权限访问目标资源
 * <p>
 * 示例:
 * <pre>{@code
 * if (!user.hasPermission("order:delete")) {
 *     throw new ForbiddenException("无权删除订单", HttpStatus.FORBIDDEN);
 * }
 * }</pre>
 *
 * @see ApiExceptionHandler#handleForbiddenException
 */
public class ForbiddenException extends AbstractHttpStatusException {

    private static final long serialVersionUID = -3804995326646218863L;

    /**
     * 按消息 + HTTP 状态构造
     *
     * @param ex     异常消息
     * @param status HTTP 状态码; 为 null 时兜底 200
     */
    public ForbiddenException(String ex, HttpStatus status) {
        super(ex, status);
    }
}
