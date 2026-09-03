package com.na.common.result;

import com.na.common.result.enums.IStatusProvider;
import com.na.common.result.enums.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.text.MessageFormat;

/**
 * 通用响应封装类: 统一接口返回结构 {code, msg, data}
 * <p>
 * 示例:
 * <pre>{@code
 * return Result.success(userList);                        // 成功 + 数据
 * return Result.error("参数不能为空");                      // 失败 + 自定义消息
 * return Result.error(Status.USER_NOT_EXIST);              // 失败 + 状态枚举(按当前语言取内置文案)
 * return Result.errorWithArgs(Status.INTERNAL_SERVER_ERROR_ARGS, e.getMessage()); // 占位符格式化
 * }</pre>
 *
 * @param <T> 响应数据类型
 */
@Schema(description = "通用响应封装类")
@Data
public class Result<T> implements IResultInfoProvider, Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "响应状态码", example = "200")
    private Integer code;

    @Schema(description = "响应信息", example = "成功")
    private String msg;

    @Schema(description = "响应数据")
    private T data;

    /** 空构造(Jackson/框架反射需要) */
    public Result() {}

    /**
     * 按状态枚举构造(消息按当前语言取内置文案)
     *
     * @param status 状态枚举; 为 null 时 code/msg 保持 null
     */
    public Result(IStatusProvider status) {
        if (status != null) {
            this.code = status.getCode();
            this.msg = status.getMsg();
        }
    }

    /**
     * 按状态枚举 + 数据构造
     *
     * @param status 状态枚举
     * @param data   响应数据
     */
    public Result(IStatusProvider status, T data) {
        this(status);
        this.data = data;
    }

    /**
     * 按自定义状态码与消息构造
     *
     * @param code 状态码
     * @param msg  消息
     */
    public Result(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    /**
     * 按自定义状态码、消息与数据构造
     *
     * @param code 状态码
     * @param msg  消息
     * @param data 响应数据
     */
    public Result(Integer code, String msg, T data) {
        this(code, msg);
        this.data = data;
    }

    // === 成功响应 ===

    /**
     * 成功响应(无数据)
     * <p>
     * 示例: {@code return Result.success(); // {code:1, msg:"成功", data:null}}
     *
     * @param <T> 响应数据类型
     * @return 成功响应
     */
    public static <T> Result<T> success() {
        return new Result<>(Status.SUCCESS);
    }

    /**
     * 成功响应(带数据)
     * <p>
     * 示例: {@code return Result.success(user);}
     *
     * @param data 响应数据
     * @param <T>  响应数据类型
     * @return 成功响应
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(Status.SUCCESS, data);
    }

    /**
     * 成功响应(自定义消息 + 数据)
     * <p>
     * 示例: {@code return Result.success("登录成功", token);}
     *
     * @param msg  自定义消息
     * @param data 响应数据
     * @param <T>  响应数据类型
     * @return 成功响应
     */
    public static <T> Result<T> success(String msg, T data) {
        return new Result<>(Status.SUCCESS.getCode(), msg, data);
    }

    /**
     * 成功响应(无消息, 仅数据; 使用默认状态码 0, msg 为 null)
     * <p>
     * 示例: {@code return Result.successNotMsg(list); // {code:0, msg:null, data:[...]}}
     *
     * @param data 响应数据
     * @param <T>  响应数据类型
     * @return 成功响应
     */
    public static <T> Result<T> successNotMsg(T data) {
        return new Result<>(Status.DEFAULT.getCode(), null, data);
    }

    /**
     * 成功响应(指定状态枚举 + 数据, 适用于非 SUCCESS 状态但业务视为成功的场景)
     *
     * @param status 状态枚举
     * @param data   响应数据
     * @param <T>    响应数据类型
     * @return 响应封装
     */
    public static <T> Result<T> successWithStatus(IStatusProvider status, T data) {
        return new Result<>(status, data);
    }

    // === 错误响应 ===

    /**
     * 失败响应(状态枚举, 消息按当前语言取内置文案)
     * <p>
     * 示例: {@code return Result.error(Status.USER_NOT_EXIST);}
     *
     * @param status 状态枚举
     * @param <T>    响应数据类型
     * @return 失败响应
     */
    public static <T> Result<T> error(IStatusProvider status) {
        return new Result<>(status);
    }

    /**
     * 失败响应(自定义消息, 状态码取 ERROR 的 -1)
     * <p>
     * 示例: {@code return Result.error("参数不能为空");}
     *
     * @param msg 自定义消息
     * @param <T> 响应数据类型
     * @return 失败响应
     */
    public static <T> Result<T> error(String msg) {
        return new Result<>(Status.ERROR.getCode(), msg);
    }

    /**
     * 失败响应(ERROR 状态 + 数据)
     * <p>
     * 示例: {@code return Result.error(failedIds);}
     *
     * @param data 响应数据(如失败明细)
     * @param <T>  响应数据类型
     * @return 失败响应
     */
    public static <T> Result<T> error(T data) {
        return new Result<>(Status.ERROR.getCode(), Status.ERROR.getMsg(), data);
    }

    // === 异常响应 ===

    /**
     * 异常响应(EXCEPTION 状态 + 自定义消息, 状态码 -2)
     * <p>
     * 示例: {@code return Result.exception("系统繁忙请稍后再试");}
     *
     * @param msg 自定义消息
     * @param <T> 响应数据类型
     * @return 异常响应
     */
    public static <T> Result<T> exception(String msg) {
        return new Result<>(Status.EXCEPTION.getCode(), msg);
    }

    /**
     * 异常响应(EXCEPTION 状态 + 数据)
     *
     * @param data 响应数据(如异常上下文)
     * @param <T>  响应数据类型
     * @return 异常响应
     */
    public static <T> Result<T> exception(T data) {
        return new Result<>(Status.EXCEPTION.getCode(), Status.EXCEPTION.getMsg(), data);
    }

    /**
     * 异常响应(状态枚举)
     * <p>
     * 示例: {@code return Result.exception(Status.SERVER_ERROR);}
     *
     * @param status 状态枚举
     * @param <T>    响应数据类型
     * @return 异常响应
     */
    public static <T> Result<T> exception(IStatusProvider status) {
        return new Result<>(status);
    }

    // === 自定义格式化消息响应 ===

    /**
     * 失败响应(状态枚举消息中含 MessageFormat 占位符 {0} {1} 时, 用 args 依次填充)
     * <p>
     * 示例:
     * <pre>{@code
     * // INTERNAL_SERVER_ERROR_ARGS 消息模板: "服务端异常: {0}"
     * return Result.errorWithArgs(Status.INTERNAL_SERVER_ERROR_ARGS, e.getMessage());
     * }</pre>
     *
     * @param status 状态枚举(msg 作为 MessageFormat 模板)
     * @param args   占位符参数, 与 {0}{1}... 顺序对应, 不允许为 null
     * @return 失败响应(消息已格式化)
     */
    public static Result<?> errorWithArgs(IStatusProvider status, Object... args) {
        String formattedMessage = MessageFormat.format(status.getMsg(), args);
        return new Result<>(status.getCode(), formattedMessage);
    }

    /**
     * 失败响应(把全部参数按空格拼接输出, 状态码取 ERROR 的 -1)
     * <p>
     * 示例: {@code return Result.errorWithArgs("订单", orderId, "不存在");}
     *
     * @param args 任意参数, 逐个以空格拼接为消息; 不允许为 null
     * @return 失败响应
     */
    public static Result<?> errorWithArgs(Object... args) {
        String pattern = String.join(" ", argsToPlaceholders(args.length));
        String formattedMessage = MessageFormat.format(pattern, args);
        return new Result<>(Status.ERROR.getCode(), formattedMessage);
    }

    /**
     * 生成 MessageFormat 占位符数组: count=3 时返回 {"{0}", "{1}", "{2}"}
     *
     * @param count 占位符数量
     * @return 占位符数组
     */
    private static String[] argsToPlaceholders(int count) {
        String[] placeholders = new String[count];
        for (int i = 0; i < count; i++) {
            placeholders[i] = "{" + i + "}";
        }
        return placeholders;
    }
}
