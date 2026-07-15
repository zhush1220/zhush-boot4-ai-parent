package com.zhush.common.exception;

/**
 * 错误码接口
 *
 * <p>业务模块可通过实现该接口定义自己的错误码枚举，避免所有错误码集中在一处造成的耦合。
 *
 * @author zhush
 */
public interface IErrorCode {

    /**
     * 错误码
     */
    int getCode();

    /**
     * 错误描述，支持 {@link java.text.MessageFormat} 占位符（如 {@code {0}}）
     */
    String getMessage();

}
