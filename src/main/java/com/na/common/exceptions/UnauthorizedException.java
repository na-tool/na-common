package com.na.common.exceptions;

import org.springframework.http.HttpStatus;

/**
 * 未授权异常(401): 用户未登录或 token 失效
 * <p>
 * 示例:
 * <pre>{@code
 * if (token == null || token.isExpired()) {
 *     throw new UnauthorizedException("请先登录", HttpStatus.UNAUTHORIZED);
 * }
 * }</pre>
 *
 * @see ApiExceptionHandler#handleUnauthorizedException
 */
public class UnauthorizedException extends AbstractHttpStatusException {

    private static final long serialVersionUID = -3804995326646218863L;

    /**
     * 按消息 + HTTP 状态构造
     *
     * @param ex     异常消息
     * @param status HTTP 状态码; 为 null 时兜底 200
     */
    public UnauthorizedException(String ex, HttpStatus status) {
        super(ex, status);
    }
}
