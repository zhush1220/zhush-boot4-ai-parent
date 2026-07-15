package com.zhush.common.enums;

import com.zhush.common.exception.GlobalErrorCode;
import com.zhush.common.exception.IErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @deprecated 请使用 {@link GlobalErrorCode}，或在业务模块中定义 {@link IErrorCode} 枚举。
 * 保留此枚举仅为向前兼容，将在后续版本移除。
 */
@Deprecated
@Getter
@AllArgsConstructor
public enum ErrorCodeEnum implements IErrorCode {

    SUCCESS(0, "成功"),
    FAIL(2000000, "失败"),
    PARAM_ERROR(1000002, "参数错误"),
    BUSINESS_ERROR(2000000, "业务错误"),
    SYSTEM_ERROR(2000000, "系统错误");

    private final int code;
    private final String message;

}
