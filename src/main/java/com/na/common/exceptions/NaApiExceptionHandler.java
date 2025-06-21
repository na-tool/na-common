package com.na.common.exceptions;

import com.na.common.result.NaResult;
import com.na.common.result.enums.NaStatus;
import com.na.common.utils.NaLocalLangUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.HandlerMethod;

import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;


@Slf4j
@ControllerAdvice
@ResponseBody
@ConditionalOnProperty(name = {"na.api.exception.handler"}, matchIfMissing = true)
public class NaApiExceptionHandler {

    @Autowired
    private Environment environment;

    /**
     * 国际化辅助方法
     * 根据配置判断是否启用国际化，
     * 若启用且传入的 message 是形如 {key} 格式，则提取 key 并从国际化资源中查找对应的值，
     * 如果找不到则返回原始消息。
     *
     * @param message 原始消息或国际化 key
     * @return 国际化后的消息文本，或原始消息
     */
    private String i18n(String message) {
        boolean isI18n = Boolean.parseBoolean(environment.getProperty("na.i18n", "false"));
        if (!isI18n || message == null || message.isEmpty()) {
            return message; // 未启用国际化或消息为空，直接返回
        }
        String key = message.trim();
        if (key.startsWith("{") && key.endsWith("}")) {
            key = key.substring(1, key.length() - 1); // 去除大括号获取 key
        }
        String localized = NaLocalLangUtil.get(key); // 从国际化工具获取翻译
        return localized != null ? localized : message; // 找不到翻译则返回原始消息
    }

    /**
     * 处理 @RequestBody 参数校验失败异常（JSON 参数校验）
     * 组合所有校验错误信息并进行国际化处理
     * 处理参数验证异常
     * @param e 参数验证异常
     * @return 返回统一的错误响应对象
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public NaResult handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String errors = e.getBindingResult()
                .getAllErrors()
                .stream()
                .map(ObjectError::getDefaultMessage) // 获取错误信息
                .map(this::i18n)                     // 进行国际化转换
                .collect(Collectors.joining(", ")); // 用逗号拼接所有错误
        log.error("MethodArgumentNotValidException: {}", errors);
        return NaResult.errorWithArgs(errors);
    }

    /**
     * 处理 @Valid GET 请求参数校验失败异常
     * @param e 参数验证异常
     * @return 返回统一的错误响应对象
     */
    @ExceptionHandler(BindException.class)
    public NaResult handleBindException(BindException e) {
        String errors = e.getBindingResult()
                .getAllErrors()
                .stream()
                .map(ObjectError::getDefaultMessage)
                .map(this::i18n)
                .collect(Collectors.joining(", "));
        log.error("BindException: {}", errors);
        return NaResult.errorWithArgs(errors);
    }

    /**
     * 处理缺少请求参数异常
     * @param e 参数验证异常
     * @return 返回统一的错误响应对象
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public NaResult handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.error("Missing request parameter: {}", e.getParameterName(), e);
        String message = i18n("缺少必要的请求参数: " + e.getParameterName());
        return NaResult.errorWithArgs(message);
    }

    /**
     * 处理请求参数校验异常（@RequestParam 参数校验失败）
     * @param e 参数验证异常
     * @return 返回统一的错误响应对象
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public NaResult handleConstraintViolationException(ConstraintViolationException e) {
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        String errors = violations.stream()
                .map(ConstraintViolation::getMessage)
                .map(this::i18n)
                .collect(Collectors.joining(", "));
        log.error("ConstraintViolationException: {}", errors);
        return NaResult.errorWithArgs(errors);
    }

    /**
     * 处理非法状态异常，返回 403 Forbidden 状态码和国际化错误消息
     * @param ex 参数验证异常
     * @return 返回统一的错误响应对象
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleIllegalStateException(IllegalStateException ex) {
        String msg = i18n(ex.getMessage());
        log.error("IllegalStateException: {}", msg, ex);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(msg);
    }

    /**
     * 处理运行时异常，统一返回服务器内部错误提示（国际化）
     * @param e 参数验证异常
     * @return 返回统一的错误响应对象
     */
    @ExceptionHandler(RuntimeException.class)
    public NaResult handleRuntimeException(RuntimeException e) {
        log.error("RuntimeException: {}", e.getMessage(), e);
        String msg = i18n("服务器发生运行时错误，请稍后再试");
        return NaResult.errorWithArgs(NaStatus.INTERNAL_SERVER_ERROR_ARGS, msg);
    }

    /**
     * 处理空指针异常，返回国际化提示信息
     * @param e 参数验证异常
     * @return 返回统一的错误响应对象
     */
    @ExceptionHandler(NullPointerException.class)
    public NaResult handleNullPointerException(NullPointerException e) {
        log.error("NullPointerException: {}", e.getMessage(), e);
        String msg = i18n("空值异常，请联系管理员");
        return NaResult.errorWithArgs(NaStatus.INTERNAL_SERVER_ERROR_ARGS, msg);
    }

    /**
     * 处理未经授权异常，返回 401 Unauthorized 和国际化错误消息
     * @param ex 参数验证异常
     * @return 返回统一的错误响应对象
     */
    @ExceptionHandler(NaUnauthorizedException.class)
    public ResponseEntity<String> handleUnauthorizedException(NaUnauthorizedException ex) {
        String msg = i18n(ex.getMessage());
        log.error("UnauthorizedException: {}", msg);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(msg);
    }

    /**
     * 处理权限不足异常，返回 403 Forbidden 和国际化错误消息
     * @param ex 参数验证异常
     * @return 返回统一的错误响应对象
     */
    @ExceptionHandler(NaForbiddenException.class)
    public ResponseEntity<String> handleForbiddenException(NaForbiddenException ex) {
        String msg = i18n(ex.getMessage());
        log.error("ForbiddenException: {}", msg);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(msg);
    }

    /**
     * 处理业务异常，返回业务异常状态码和国际化消息
     * @param ex 参数验证异常
     * @return 返回统一的错误响应对象
     */
    @ExceptionHandler(NaBusinessException.class)
    public ResponseEntity<NaResult> handleBusinessException(NaBusinessException ex) {
        String msg = i18n(ex.getMessage());
        log.error("BusinessException: {}", msg, ex);
        return ResponseEntity.status(ex.getStatus())
                .body(NaResult.exception(msg));
    }

    /**
     * 处理所有未捕获异常
     * 根据方法上 @NaApiException 注解返回不同状态码和错误信息，
     * 错误信息同样支持国际化处理。
     * @param e 未捕获异常
     * @param method 当前处理请求的方法
     * @param response HttpServletResponse
     * @return 返回统一的错误响应对象
     */
    @ExceptionHandler(Exception.class)
    public NaResult handleException(Exception e, HandlerMethod method, HttpServletResponse response) {
        log.error("Unhandled Exception: {}", e.getMessage(), e);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        NaApiException naApiException = method.getMethodAnnotation(NaApiException.class);
        if (naApiException != null) {
            if ("不允许访问".equals(e.getMessage())) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return NaResult.error(NaStatus.USER_NO_ACCESS);
            }
            if ("未登录".equals(e.getMessage())) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return NaResult.error(NaStatus.USER_NEED_AUTHORITIES);
            }

            NaStatus status = naApiException.value();
            return (status.getCode() == NaStatus.DEFAULT.getCode()
                    || status.getCode() == NaStatus.SUCCESS.getCode())
                    ? NaResult.error(status)
                    : NaResult.exception(status);
        }

        return NaResult.errorWithArgs(NaStatus.INTERNAL_SERVER_ERROR_ARGS, i18n(e.getMessage()));
    }

    /**
     * 异步异常处理器
     * 记录异步方法执行过程中未捕获的异常，方便排查
     * @return 异步异常处理器
     */
    @Bean
    public AsyncUncaughtExceptionHandler asyncExceptionHandler() {
        return (ex, method, params) -> log.error("Async Exception in method: {} with params: {}", method.getName(), params, ex);
    }
}
