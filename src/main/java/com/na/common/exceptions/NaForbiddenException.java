package com.na.common.exceptions;

import org.springframework.http.HttpStatus;

/**
 * 禁止访问异常(403): 用户已认证但无权限访问目标资源
 * <p>
 * 示例:
 * <pre>{@code
 * if (!user.hasPermission("order:delete")) {
 *     throw new NaForbiddenException("无权删除订单", HttpStatus.FORBIDDEN);
 * }
 * }</pre>
 *
 * @see NaApiExceptionHandler#handleForbiddenException
 */
public class NaForbiddenException extends RuntimeException {
    private static final long serialVersionUID = -3804995326646218863L;

    /** HTTP 状态码, 不为 null */
    private final HttpStatus status;

    /**
     * 按消息 + HTTP 状态构造
     *
     * @param ex     异常消息
     * @param status HTTP 状态码; 为 null 时兜底 200
     */
    public NaForbiddenException(String ex, HttpStatus status) {
        super(ex);
        this.status = resolveHttpStatus(status);
    }

    /**
     * 获取 HTTP 状态码(保证非 null)
     *
     * @return HTTP 状态码
     */
    public HttpStatus getStatus() {
        return status;
    }

    /**
     * 解析 HTTP 状态: null 兜底 200
     *
     * @param status 传入的状态
     * @return 非空状态
     */
    private HttpStatus resolveHttpStatus(HttpStatus status) {
        return status != null ? status : HttpStatus.OK;
    }
}
