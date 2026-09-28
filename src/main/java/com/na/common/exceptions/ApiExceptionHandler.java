package com.na.common.exceptions;

import com.na.common.result.Result;
import com.na.common.result.enums.Status;
import com.na.common.utils.LocalLangUtil;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.HandlerMethod;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 全局异常处理器: 统一拦截 Controller 层异常, 按类型返回标准化 {@link Result} 响应
 * <p>
 * 处理流程: 捕获异常 → 国际化消息 → 记录 slf4j 日志 → 返回统一响应
 * <p>
 * 异常落库不属于本类职责: 需要持久化异常日志的宿主接入 na-log, 由其 OperationLogAspect
 * 采集。本类只写 slf4j, 不依赖任何日志模块。
 * <p>
 * 生效条件: {@code na.api.exception.handler} 未显式置为 false(默认开启)
 * <p>
 * 支持的异常类型(按优先级):
 * <ul>
 *     <li>{@link BusinessException} — 业务异常, 按 httpStatus 返回</li>
 *     <li>{@link UnauthorizedException} — 401 未授权</li>
 *     <li>{@link ForbiddenException} — 403 禁止访问</li>
 *     <li>{@link MethodArgumentNotValidException} — @RequestBody 校验失败</li>
 *     <li>{@link BindException} — @Valid 表单校验失败</li>
 *     <li>{@link ConstraintViolationException} — @RequestParam 校验失败</li>
 *     <li>{@link MissingServletRequestParameterException} — 缺少必填参数</li>
 *     <li>{@link IllegalStateException} — 非法状态</li>
 *     <li>{@link NullPointerException} — 空指针</li>
 *     <li>{@link RuntimeException} — 运行时异常(解包嵌套 NaBusinessException)</li>
 *     <li>{@link Exception} — 兜底(读 @NaApiException 注决定返回)</li>
 * </ul>
 */
@Slf4j
@RestControllerAdvice
@ConditionalOnProperty(name = "na.api.exception.handler", havingValue = "true", matchIfMissing = true)
@Hidden
public class ApiExceptionHandler {

    @Autowired
    private Environment environment;

    // === 私有工具方法 ===

    /**
     * 国际化辅助: 根据配置判断是否启用国际化
     * <p>若启用且 message 形如 {key}, 提取 key 从资源查找; 找不到返回原始 message
     *
     * @param message 原始消息或国际化 key
     * @return 国际化后的消息
     */
    private String i18n(String message) {
        boolean isI18n = Boolean.parseBoolean(environment.getProperty("na.i18n.enable", "false"));
        if (!isI18n || message == null || message.isEmpty()) {
            return message;
        }
        String key = message.trim();
        if (key.startsWith("{") && key.endsWith("}")) {
            key = key.substring(1, key.length() - 1);
        }
        String localized = LocalLangUtil.get(key);
        return localized != null ? localized : message;
    }

    /**
     * 递归获取最底层原始异常
     *
     * @param throwable 异常
     * @return 根因异常
     */
    private Throwable getRootCause(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null && root != root.getCause()) {
            root = root.getCause();
        }
        return root;
    }

    // === 参数校验异常 ===

    /**
     * 处理 @RequestBody 参数校验失败(JSON 参数校验)
     * <p>组合所有校验错误信息并进行国际化处理
     *
     * @param e 参数验证异常
     * @return 返回统一的错误响应对象
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        try {
            String errors = e.getBindingResult().getAllErrors().stream()
                    .map(ObjectError::getDefaultMessage)
                    .map(this::i18n)
                    .collect(Collectors.joining(", "));
            log.error("MethodArgumentNotValidException: {}", errors);
            return Result.errorWithArgs(errors);
        } catch (Exception ex) {
            log.error("异常处理链路自身出错", ex);
            return Result.errorWithArgs(ex);
        }
    }

    /**
     * 处理 @Valid GET 请求参数校验失败异常
     *
     * @param e 参数验证异常
     * @return 返回统一的错误响应对象
     */
    @ExceptionHandler(BindException.class)
    public Result<?> handleBindException(BindException e) {
        try {
            String errors = e.getBindingResult().getAllErrors().stream()
                    .map(ObjectError::getDefaultMessage)
                    .map(this::i18n)
                    .collect(Collectors.joining(", "));
            log.error("BindException: {}", errors);
            return Result.errorWithArgs(errors);
        } catch (Exception ex) {
            log.error("异常处理链路自身出错", ex);
            return Result.errorWithArgs(ex);
        }
    }

    /**
     * 处理缺少请求参数异常
     *
     * @param e 缺少参数异常
     * @return 返回统一的错误响应对象
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<?> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        try {
            log.error("Missing request parameter: {}", e.getParameterName(), e);
            String message = i18n("缺少必要的请求参数: " + e.getParameterName());
            return Result.errorWithArgs(message);
        } catch (Exception ex) {
            log.error("异常处理链路自身出错", ex);
            return Result.errorWithArgs(ex);
        }
    }

    /**
     * 处理请求参数校验异常（@RequestParam 参数校验失败）
     *
     * @param e 参数验证异常
     * @return 返回统一的错误响应对象
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<?> handleConstraintViolationException(ConstraintViolationException e) {
        try {
            Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
            String errors = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .map(this::i18n)
                    .collect(Collectors.joining(", "));
            log.error("ConstraintViolationException: {}", errors);
            return Result.errorWithArgs(errors);
        } catch (Exception ex) {
            log.error("异常处理链路自身出错", ex);
            return Result.errorWithArgs(ex);
        }
    }

    // === 系统异常 ===

    /**
     * 处理非法状态异常, 返回 403 Forbidden 状态码和国际化错误消息
     *
     * @param e 非法状态异常
     * @return 返回统一的错误响应对象
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Result<?>> handleIllegalStateException(IllegalStateException e) {
        try {
            String msg = i18n(e.getMessage());
            log.error("IllegalStateException: {}", msg, e);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(Result.errorWithArgs(Status.USER_NO_ACCESS, msg));
        } catch (Exception ex) {
            log.error("异常处理链路自身出错", ex);
            return ResponseEntity.status(HttpStatus.OK).body(Result.errorWithArgs(ex));
        }
    }

    /**
     * 处理空指针异常, 返回国际化提示信息
     *
     * @param e 空指针异常
     * @return 返回统一的错误响应对象
     */
    @ExceptionHandler(NullPointerException.class)
    public Result<?> handleNullPointerException(NullPointerException e) {
        try {
            log.error("NullPointerException: {}", e.getMessage(), e);
            String msg = i18n("空值异常，请联系管理员");
            return Result.errorWithArgs(Status.INTERNAL_SERVER_ERROR_ARGS, msg);
        } catch (Exception ex) {
            log.error("异常处理链路自身出错", ex);
            return Result.errorWithArgs(ex);
        }
    }

    // === 自定义业务异常 ===

    /**
     * 处理未经授权异常, 返回 401 Unauthorized 和国际化错误消息
     *
     * @param e 未授权异常
     * @return 返回统一的错误响应对象
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Result<?>> handleUnauthorizedException(UnauthorizedException e) {
        try {
            String msg = i18n(e.getMessage());
            log.error("UnauthorizedException: {}", msg);
            return ResponseEntity.status(e.getStatus())
                    .body(Result.errorWithArgs(Status.USER_NEED_AUTHORITIES, msg));
        } catch (Exception ex) {
            log.error("异常处理链路自身出错", ex);
            return ResponseEntity.status(e.getStatus()).body(Result.errorWithArgs(ex));
        }
    }

    /**
     * 处理权限不足异常, 返回 403 Forbidden 和国际化错误消息
     *
     * @param e 禁止访问异常
     * @return 返回统一的错误响应对象
     */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Result<?>> handleForbiddenException(ForbiddenException e) {
        try {
            String msg = i18n(e.getMessage());
            log.error("ForbiddenException: {}", msg);
            return ResponseEntity.status(e.getStatus())
                    .body(Result.errorWithArgs(Status.USER_NO_ACCESS, msg));
        } catch (Exception ex) {
            log.error("异常处理链路自身出错", ex);
            return ResponseEntity.status(e.getStatus()).body(Result.errorWithArgs(ex));
        }
    }

    /**
     * 处理业务异常, 返回业务异常状态码和国际化消息
     *
     * @param e 业务异常
     * @return 返回统一的错误响应对象
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<?>> handleBusinessException(BusinessException e) {
        try {
            String msg = i18n(e.getMessage());
            log.error("BusinessException: {}", msg, e);
            return ResponseEntity.status(e.getStatus()).body(Result.exception(msg));
        } catch (Exception ex) {
            log.error("异常处理链路自身出错", ex);
            return ResponseEntity.status(e.getStatus()).body(Result.errorWithArgs(ex));
        }
    }

    /**
     * 处理运行时异常, 统一返回服务器内部错误提示(国际化)
     * <p>先解包嵌套异常识别内部 NaBusinessException, 否则按通用错误返回
     *
     * @param e 运行时异常
     * @return 返回统一的错误响应对象
     */
    @ExceptionHandler(RuntimeException.class)
    public Result<?> handleRuntimeException(RuntimeException e) {
        try {
            // 解包嵌套异常, 识别内部 NaBusinessException
            Throwable rootCause = getRootCause(e);
            if (rootCause instanceof BusinessException) {
                BusinessException bizEx = (BusinessException) rootCause;
                String msg = i18n(bizEx.getMessage());
                log.error("BusinessException(wrapped): {}", msg, bizEx);
                return Result.errorWithArgs(msg);
            }

            log.error("RuntimeException: {}", e.getMessage(), e);
            String msg = i18n("服务器发生运行时错误，请稍后再试");
            return Result.errorWithArgs(Status.INTERNAL_SERVER_ERROR_ARGS, msg);
        } catch (Exception ex) {
            log.error("异常处理链路自身出错", ex);
            return Result.errorWithArgs(ex);
        }
    }

    /**
     * 兜底处理所有未捕获异常
     * <p>根据方法上 @NaApiException 注解返回不同状态码和错误信息, 错误信息同样支持国际化
     *
     * @param e        未捕获异常
     * @param webRequest   当前处理请求的方法(可能为 null)
     * @param response HttpServletResponse
     * @return 返回统一的错误响应对象
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e, WebRequest webRequest, HttpServletResponse response) {
        try {
            log.error("Unhandled Exception: {}", e.getMessage(), e);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            HandlerMethod method = null;
            Object handlerAttr = webRequest.getAttribute(HandlerMethod.class.getName(), WebRequest.SCOPE_REQUEST);
            if (handlerAttr instanceof HandlerMethod hm) {
                method = hm;
            }

            if (method != null) {
                ApiException apiException = method.getMethodAnnotation(ApiException.class);
                if (apiException != null) {
                    if ("不允许访问".equals(e.getMessage())) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        return Result.error(Status.USER_NO_ACCESS);
                    }
                    if ("未登录".equals(e.getMessage())) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        return Result.error(Status.USER_NEED_AUTHORITIES);
                    }

                    Status status = apiException.value();
                    return (status.getCode() == Status.DEFAULT.getCode()
                            || status.getCode() == Status.SUCCESS.getCode())
                            ? Result.error(status)
                            : Result.exception(status);
                }
            }

            String i18nMsg = i18n(e.getMessage());
            return Result.errorWithArgs(Status.INTERNAL_SERVER_ERROR_ARGS, i18nMsg);
        } catch (Exception ex) {
            log.error("异常处理链路自身出错", ex);
            return Result.errorWithArgs(ex);
        }
    }

    /**
     * 异步异常处理器 Bean: 记录 @Async 方法未捕获异常
     *
     * @return 异步异常处理器
     */
    @Bean
    public AsyncUncaughtExceptionHandler asyncExceptionHandler() {
        return (ex, method, params) -> log.error(
                "Async Exception in method: {} with params: {}", method.getName(), params, ex);
    }
}
