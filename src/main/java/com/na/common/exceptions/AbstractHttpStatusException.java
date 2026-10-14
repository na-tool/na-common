package com.na.common.exceptions;

import org.springframework.http.HttpStatus;

/**
 * 携带 HTTP 状态码的异常基类。
 *
 * <p>{@link BusinessException}、{@link ForbiddenException}、{@link UnauthorizedException}
 * 原本各自实现了一份逐字相同的 {@code resolveHttpStatus} 与 {@code getStatus}（连
 * serialVersionUID 都相同）。抽到基类后这段语义只有一处，不会再出现"改了一个忘了另一个"。
 *
 * <p><b>状态码兜底约定</b>：传入的 {@code status} 为 null 时统一兜底为 HTTP 200 ——
 * 本模块的约定是"业务异常用 HTTP 200 + 业务码区分"，而不是靠 HTTP 状态码表达业务失败。
 * 请显式传入 {@link HttpStatus#UNAUTHORIZED} / {@link HttpStatus#FORBIDDEN} 之类
 * 真正需要体现在 HTTP 层的状态，否则客户端拿到的是 200。
 *
 * <p>{@link ApiExceptionHandler} 会读取 {@link #getStatus()} 决定响应的 HTTP 状态码。
 */
public abstract class AbstractHttpStatusException extends RuntimeException {

    private static final long serialVersionUID = -3804995326646218863L;

    /** HTTP 状态码，保证非 null */
    private final HttpStatus status;

    /**
     * @param message 异常消息
     * @param status  HTTP 状态码；为 null 时兜底 200
     */
    protected AbstractHttpStatusException(String message, HttpStatus status) {
        super(message);
        this.status = status != null ? status : HttpStatus.OK;
    }

    /**
     * 获取 HTTP 状态码（保证非 null）
     *
     * @return HTTP 状态码
     */
    public HttpStatus getStatus() {
        return status;
    }
}
