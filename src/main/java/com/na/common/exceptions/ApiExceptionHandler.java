package com.na.common.exceptions;

import com.na.common.result.Result;
import com.na.common.result.enums.IStatusProvider;
import com.na.common.result.enums.Status;
import com.na.common.utils.LocalLangUtil;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
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
 *     <li>{@link BusinessException} — 业务异常, 按 httpStatus 返回并保留业务码</li>
 *     <li>{@link UnauthorizedException} — 401 未授权</li>
 *     <li>{@link ForbiddenException} — 403 禁止访问</li>
 *     <li>{@link MethodArgumentNotValidException} — @RequestBody 校验失败</li>
 *     <li>{@link BindException} — @Valid 表单校验失败</li>
 *     <li>{@link ConstraintViolationException} — @RequestParam 校验失败</li>
 *     <li>{@link MissingServletRequestParameterException} — 缺少必填参数</li>
 *     <li>{@link IllegalStateException} — 非法状态</li>
 *     <li>{@link NullPointerException} — 空指针</li>
 *     <li>{@link RuntimeException} — 运行时异常(解包嵌套 BusinessException)</li>
 *     <li>{@link Exception} — 兜底(读 {@link ApiException} 注解决定返回)</li>
 * </ul>
 */
@Slf4j
@RestControllerAdvice
@ConditionalOnProperty(name = "na.api.exception.handler", havingValue = "true", matchIfMissing = true)
@Hidden
public class ApiExceptionHandler {

    /**
     * 异常处理链路自身出错时对外的固定文案。
     * <p>不回传异常类名与内部消息 —— {@code Result.errorWithArgs(ex)} 会把
     * {@code java.lang.XxxException: 内部信息} 原样写进响应体, 属信息泄露。
     * 具体异常写服务端日志。
     */
    private static final String FALLBACK_MSG = "服务端异常，请稍后重试";

    /**
     * 配置环境。构造器注入：注册器以 AUTOWIRE_CONSTRUCTOR 注册本类，
     * 必填依赖在构造期就解析，缺依赖时报错位置更明确
     */
    private final Environment environment;

    /**
     * @param environment 配置环境
     */
    public ApiExceptionHandler(Environment environment) {
        this.environment = environment;
    }

    // === 私有工具方法 ===

    /**
     * 兜底响应体(每次新建, 避免共享可变对象)
     *
     * @return 固定文案的失败响应
     */
    private static Result<?> fallbackBody() {
        return new Result<>(Status.INTERNAL_SERVER_ERROR_ARGS.getCode(), FALLBACK_MSG);
    }

    /**
     * 记录异常处理链路自身的失败
     *
     * @param where 出错位置描述
     * @param ex    二次异常
     */
    private static void logFallback(String where, Exception ex) {
        log.error("异常处理链路自身出错: {}", where, ex);
    }

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
            logFallback("MethodArgumentNotValidException", ex);
            return fallbackBody();
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
            logFallback("BindException", ex);
            return fallbackBody();
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
            logFallback("MissingServletRequestParameterException", ex);
            return fallbackBody();
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
            logFallback("ConstraintViolationException", ex);
            return fallbackBody();
        }
    }

    // === 系统异常 ===

    /**
     * 处理非法状态异常
     * <p>按项目约定返回 HTTP 200 + 业务码 {@code USER_NO_ACCESS}(无权限访问),
     * 与 {@link BusinessException} 的"业务异常用 HTTP 200 + 业务码区分"一致
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
            logFallback("IllegalStateException", ex);
            return ResponseEntity.status(HttpStatus.OK).body(fallbackBody());
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
            logFallback("NullPointerException", ex);
            return fallbackBody();
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
            logFallback("UnauthorizedException", ex);
            return ResponseEntity.status(e.getStatus()).body(fallbackBody());
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
            logFallback("ForbiddenException", ex);
            return ResponseEntity.status(e.getStatus()).body(fallbackBody());
        }
    }

    /**
     * 处理业务异常, 返回业务异常状态码和国际化消息
     * <p>响应体保留构造异常时传入的状态枚举的业务码; 按字符串构造的异常没有业务码, 回退为通用失败码
     *
     * @param e 业务异常
     * @return 返回统一的错误响应对象
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<?>> handleBusinessException(BusinessException e) {
        try {
            String msg = i18n(e.getMessage());
            log.error("BusinessException: {}", msg, e);
            IStatusProvider provider = e.getStatusProvider();
            Result<?> body = provider != null
                    ? new Result<>(provider.getCode(), msg)
                    : Result.error(msg);
            return ResponseEntity.status(e.getStatus()).body(body);
        } catch (Exception ex) {
            logFallback("BusinessException", ex);
            return ResponseEntity.status(e.getStatus()).body(fallbackBody());
        }
    }

    /**
     * 处理运行时异常, 统一返回服务器内部错误提示(国际化)
     * <p>先解包嵌套异常识别内部 BusinessException, 否则按通用错误返回
     *
     * @param e 运行时异常
     * @return 返回统一的错误响应对象
     */
    @ExceptionHandler(RuntimeException.class)
    public Result<?> handleRuntimeException(RuntimeException e) {
        try {
            // 解包嵌套异常, 识别内部 BusinessException
            Throwable rootCause = getRootCause(e);
            if (rootCause instanceof BusinessException bizEx) {
                String msg = i18n(bizEx.getMessage());
                log.error("BusinessException(wrapped): {}", msg, bizEx);
                IStatusProvider provider = bizEx.getStatusProvider();
                return provider != null
                        ? new Result<>(provider.getCode(), msg)
                        : Result.error(msg);
            }

            log.error("RuntimeException: {}", e.getMessage(), e);
            String msg = i18n("服务器发生运行时错误，请稍后再试");
            return Result.errorWithArgs(Status.INTERNAL_SERVER_ERROR_ARGS, msg);
        } catch (Exception ex) {
            logFallback("RuntimeException", ex);
            return fallbackBody();
        }
    }

    /**
     * 兜底处理所有未捕获异常
     * <p>根据方法上 {@link ApiException} 注解返回不同状态码和错误信息, 错误信息同样支持国际化
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
                    // 用 int 比较: Integer 的 == 只在缓存范围内成立, 依赖它是巧合
                    int code = status.getCode();
                    return (code == Status.DEFAULT.getCode() || code == Status.SUCCESS.getCode())
                            ? Result.error(status)
                            : Result.exception(status);
                }
            }

            String i18nMsg = i18n(e.getMessage());
            return Result.errorWithArgs(Status.INTERNAL_SERVER_ERROR_ARGS, i18nMsg);
        } catch (Exception ex) {
            logFallback("Exception", ex);
            return fallbackBody();
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
