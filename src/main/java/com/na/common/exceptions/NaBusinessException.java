package com.na.common.exceptions;

import com.na.common.result.enums.IStatusProvider;
import org.springframework.http.HttpStatus;

/**
 * 业务异常: 用于在业务逻辑中主动抛出, 携带 HTTP 状态码与消息
 * <p>
 * 继承 {@link RuntimeException} 属于非受检异常, 无需在方法签名声明
 * <p>
 * 示例:
 * <pre>{@code
 * // 自定义消息 + 指定 HTTP 状态
 * throw new NaBusinessException("订单状态不允许取消", HttpStatus.BAD_REQUEST);
 *
 * // 使用 Status 枚举消息 + 指定 HTTP 状态
 * throw new NaBusinessException(Status.USER_NOT_EXIST, HttpStatus.NOT_FOUND);
 *
 * // 由 NaApiExceptionHandler 捕获后按 httpStatus 返回 ResponseEntity
 * }</pre>
 *
 * @see NaApiExceptionHandler#handleBusinessException
 */
public class NaBusinessException extends RuntimeException {
    private static final long serialVersionUID = -3804995326646218863L;

    /** HTTP 状态码, 不为 null */
    private final HttpStatus httpStatus;

    /**
     * 按自定义消息 + HTTP 状态构造
     *
     * @param ex         异常消息
     * @param httpStatus HTTP 状态码; 为 null 时兜底 200(业务异常通常 HTTP 200 + 业务码区分)
     */
    public NaBusinessException(String ex, HttpStatus httpStatus) {
        super(ex);
        this.httpStatus = resolveHttpStatus(httpStatus);
    }

    /**
     * 按状态枚举消息 + HTTP 状态构造
     *
     * @param status     状态枚举(取其 msg 作为异常消息)
     * @param httpStatus HTTP 状态码; 为 null 时兜底 200
     */
    public NaBusinessException(IStatusProvider status, HttpStatus httpStatus) {
        super(status.getMsg());
        this.httpStatus = resolveHttpStatus(httpStatus);
    }

    /**
     * 获取 HTTP 状态码(保证非 null)
     *
     * @return HTTP 状态码
     */
    public HttpStatus getStatus() {
        return httpStatus;
    }

    /**
     * 解析 HTTP 状态: null 兜底 200(业务异常通用做法: HTTP 200 + 业务码区分错误)
     *
     * @param status 传入的状态
     * @return 非空状态
     */
    private HttpStatus resolveHttpStatus(HttpStatus status) {
        return status != null ? status : HttpStatus.OK;
    }
}
