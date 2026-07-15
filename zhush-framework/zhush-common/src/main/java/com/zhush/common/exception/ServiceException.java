package com.zhush.common.exception;

import lombok.Getter;

import java.io.Serial;
import java.text.MessageFormat;

/**
 * 业务异常
 *
 * <p>项目中所有可预期的业务错误统一抛出该异常，由 GlobalExceptionHandler 翻译为
 * {@code Result} 返回给前端。为避免异常类型爆炸，业务侧通过 {@link IErrorCode}
 * 区分具体场景，而不是为每种场景新建异常类。
 *
 * <p>为提升性能，覆写 {@link #fillInStackTrace()} 关闭堆栈填充；如需堆栈调试，
 * 可通过 {@link ServiceException#ServiceException(IErrorCode, Throwable)} 传入 cause。
 *
 * @author zhush
 */
@Getter
public class ServiceException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private final int code;

    /**
     * 需透传给前端的附加数据（如字段级校验详情），可选
     */
    private transient Object data;

    public ServiceException(IErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    /**
     * 支持 {@link MessageFormat} 占位符（如 {@code {0}}）的构造
     */
    public ServiceException(IErrorCode errorCode, Object... args) {
        super(format(errorCode.getMessage(), args));
        this.code = errorCode.getCode();
    }

    public ServiceException(int code, String message) {
        super(message);
        this.code = code;
    }

    public ServiceException(IErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.code = errorCode.getCode();
    }

    /**
     * 链式设置附加数据
     */
    public ServiceException withData(Object data) {
        this.data = data;
        return this;
    }

    /**
     * 业务异常无需填充堆栈信息，覆写以提升性能
     */
    @Override
    public Throwable fillInStackTrace() {
        return this;
    }

    private static String format(String template, Object... args) {
        if (template == null || args == null || args.length == 0) {
            return template;
        }
        try {
            return MessageFormat.format(template, args);
        } catch (IllegalArgumentException ignore) {
            return template;
        }
    }

}
