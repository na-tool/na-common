package com.na.common.result;

import com.na.common.result.enums.INaStatusProvider;
import com.na.common.result.enums.NaStatus;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.text.MessageFormat;

/**
 * 通用响应封装类
 */
@ApiModel
@Data
public class NaResult<T> implements INaResultInfoProvider, Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("响应状态码")
    private Integer code;

    @ApiModelProperty("响应信息")
    private String msg;

    @ApiModelProperty("响应数据")
    private T data;

    public NaResult() {}

    public NaResult(INaStatusProvider status) {
        if (status != null) {
            this.code = status.getCode();
            this.msg = status.getMsg();
        }
    }

    public NaResult(INaStatusProvider status, T data) {
        this(status);
        this.data = data;
    }

    public NaResult(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public NaResult(Integer code, String msg, T data) {
        this(code, msg);
        this.data = data;
    }

    // === 成功响应 ===

    public static <T> NaResult<T> success() {
        return new NaResult<>(NaStatus.SUCCESS);
    }

    public static <T> NaResult<T> success(T data) {
        return new NaResult<>(NaStatus.SUCCESS, data);
    }

    public static <T> NaResult<T> success(String msg, T data) {
        return new NaResult<>(NaStatus.SUCCESS.getCode(), msg, data);
    }

    public static <T> NaResult<T> successNotMsg( T data) {
        return new NaResult<>(NaStatus.DEFAULT.getCode(), null, data);
    }

    public static <T> NaResult<T> successWithStatus(INaStatusProvider status, T data) {
        return new NaResult<>(status, data);
    }

    // === 错误响应 ===

    public static <T> NaResult<T> error(INaStatusProvider status) {
        return new NaResult<>(status);
    }

    public static <T> NaResult<T> error(String msg) {
        return new NaResult<>(NaStatus.ERROR.getCode(), msg);
    }

    public static <T> NaResult<T> error(T data) {
        return new NaResult<>(NaStatus.ERROR.getCode(), NaStatus.ERROR.getMsg(), data);
    }

    // === 异常响应 ===

    public static <T> NaResult<T> exception(String msg) {
        return new NaResult<>(NaStatus.EXCEPTION.getCode(), msg);
    }

    public static <T> NaResult<T> exception(T data) {
        return new NaResult<>(NaStatus.EXCEPTION.getCode(), NaStatus.EXCEPTION.getMsg(), data);
    }

    public static <T> NaResult<T> exception(INaStatusProvider status) {
        return new NaResult<>(status);
    }

    // === 自定义格式化消息响应 ===

    public static NaResult<?> errorWithArgs(INaStatusProvider status, Object... args) {
        String formattedMessage = MessageFormat.format(status.getMsg(), args);
        return new NaResult<>(status.getCode(), formattedMessage);
    }

    public static NaResult<?> errorWithArgs(Object... args) {
        String pattern = String.join(" ", argsToPlaceholders(args.length));
        String formattedMessage = MessageFormat.format(pattern, args);
        return new NaResult<>(NaStatus.ERROR.getCode(), formattedMessage);
    }

    private static String[] argsToPlaceholders(int count) {
        String[] placeholders = new String[count];
        for (int i = 0; i < count; i++) {
            placeholders[i] = "{" + i + "}";
        }
        return placeholders;
    }
}
