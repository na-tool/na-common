package com.na.common.exceptions;

import com.na.common.result.enums.IStatusProvider;
import org.springframework.http.HttpStatus;

/**
 * 业务异常: 用于在业务逻辑中主动抛出, 携带 HTTP 状态码与消息
 * <p>
 * 继承 {@link AbstractHttpStatusException} → {@link RuntimeException}, 属非受检异常, 无需在方法签名声明
 * <p>
 * 示例:
 * <pre>{@code
 * // 自定义消息 + 指定 HTTP 状态
 * throw new BusinessException("订单状态不允许取消", HttpStatus.BAD_REQUEST);
 *
 * // 使用 Status 枚举消息 + 指定 HTTP 状态; 响应体会保留该枚举的业务码
 * throw new BusinessException(Status.USER_NOT_EXIST, HttpStatus.NOT_FOUND);
 *
 * // 由 ApiExceptionHandler 捕获后按 getStatus() 返回 ResponseEntity
 * }</pre>
 *
 * @see ApiExceptionHandler#handleBusinessException
 */
public class BusinessException extends AbstractHttpStatusException {

    private static final long serialVersionUID = -3804995326646218863L;

    /**
     * 构造时使用的状态枚举; 按字符串构造时为 null。
     * <p>保留它是为了让全局异常处理器能把业务码(carrier code)写回响应体——
     * 只取 {@code status.getMsg()} 会把业务码丢掉, 客户端就只能收到一个笼统的失败码。
     */
    private final IStatusProvider statusProvider;

    /**
     * 按自定义消息 + HTTP 状态构造
     *
     * @param ex         异常消息
     * @param httpStatus HTTP 状态码; 为 null 时兜底 200(业务异常通常 HTTP 200 + 业务码区分)
     */
    public BusinessException(String ex, HttpStatus httpStatus) {
        super(ex, httpStatus);
        this.statusProvider = null;
    }

    /**
     * 按状态枚举消息 + HTTP 状态构造
     *
     * @param status     状态枚举(取其 msg 作为异常消息)
     * @param httpStatus HTTP 状态码; 为 null 时兜底 200
     */
    public BusinessException(IStatusProvider status, HttpStatus httpStatus) {
        super(status.getMsg(), httpStatus);
        this.statusProvider = status;
    }

    /**
     * 获取构造时使用的状态枚举
     *
     * @return 状态枚举; 按字符串构造的异常返回 null
     */
    public IStatusProvider getStatusProvider() {
        return statusProvider;
    }
}
