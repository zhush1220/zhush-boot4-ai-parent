package com.zhush.web.core.handler;

import com.zhush.common.exception.GlobalErrorCode;
import com.zhush.common.exception.ServiceException;
import com.zhush.common.response.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理器
 *
 * <p>负责将业务异常、参数校验异常、Spring MVC 异常统一翻译为 {@link Result} 返回。
 * Sa-Token 与数据访问相关异常由独立的 Advice 处理（见 {@code WebAutoConfiguration}）。
 *
 * @author zhush
 */
@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ================ 业务异常 ================

    @ExceptionHandler(ServiceException.class)
    public Result<?> handleServiceException(ServiceException e, HttpServletRequest request) {
        log.warn("[ServiceException] uri={}, code={}, msg={}", request.getRequestURI(), e.getCode(), e.getMessage());
        return Result.error(e);
    }

    // ================ 参数校验异常 ================

    /**
     * {@code @Valid @RequestBody} 校验失败
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String detail = fieldError == null
                ? e.getMessage()
                : fieldError.getField() + " " + fieldError.getDefaultMessage();
        log.warn("[ParamInvalid] {}", detail);
        return Result.error(GlobalErrorCode.PARAM_INVALID.getCode(), detail);
    }

    /**
     * {@code @Valid @ModelAttribute} / form 表单校验失败
     */
    @ExceptionHandler(BindException.class)
    public Result<?> handleBindException(BindException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String detail = fieldError == null
                ? e.getMessage()
                : fieldError.getField() + " " + fieldError.getDefaultMessage();
        log.warn("[ParamInvalid] {}", detail);
        return Result.error(GlobalErrorCode.PARAM_INVALID.getCode(), detail);
    }

    /**
     * {@code @Validated} 方法参数校验失败
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<?> handleConstraintViolation(ConstraintViolationException e) {
        String detail = e.getConstraintViolations().stream()
                .findFirst()
                .map(ConstraintViolation::getMessage)
                .orElse(e.getMessage());
        log.warn("[ParamInvalid] {}", detail);
        return Result.error(GlobalErrorCode.PARAM_INVALID.getCode(), detail);
    }

    /**
     * 缺少必填请求参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<?> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("[ParamMissing] name={}", e.getParameterName());
        return Result.error(new ServiceException(GlobalErrorCode.PARAM_MISSING, e.getParameterName()));
    }

    /**
     * 参数类型不匹配（如路径变量应为 Long，传入非数字）
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<?> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("[ParamTypeMismatch] name={}, value={}", e.getName(), e.getValue());
        return Result.error(new ServiceException(GlobalErrorCode.PARAM_TYPE_MISMATCH, e.getName()));
    }

    /**
     * 请求体不可读（JSON 解析失败）
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<?> handleMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("[RequestBodyInvalid] {}", e.getMessage());
        return Result.error(GlobalErrorCode.REQUEST_BODY_INVALID);
    }

    // ================ Spring MVC 异常 ================

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<?> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("[MethodNotAllowed] {}", e.getMessage());
        return Result.error(new ServiceException(GlobalErrorCode.METHOD_NOT_ALLOWED, e.getMethod()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Result<?> handleNoResourceFound(NoResourceFoundException e) {
        log.warn("[NotFound] {}", e.getResourcePath());
        return Result.error(GlobalErrorCode.NOT_FOUND);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<?> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        log.warn("[PayloadTooLarge] maxSize={}", e.getMaxUploadSize());
        return Result.error(GlobalErrorCode.PAYLOAD_TOO_LARGE);
    }

    // ================ 兜底 ================

    @ExceptionHandler(Throwable.class)
    public Result<?> handleThrowable(Throwable e, HttpServletRequest request) {
        log.error("[UnhandledException] uri={}", request.getRequestURI(), e);
        return Result.error(GlobalErrorCode.INTERNAL_ERROR);
    }

}
