package com.zhush.common.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zhush.common.exception.GlobalErrorCode;
import com.zhush.common.exception.IErrorCode;
import com.zhush.common.exception.ServiceException;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 统一返回结果
 *
 * @author zhush
 */
@Data
@Accessors(chain = true)
public class Result<T> {

    /**
     * 状态码，见 {@link GlobalErrorCode}；{@code 0} 表示成功
     */
    private Integer code;

    /**
     * 提示信息
     */
    private String message;

    /**
     * 业务数据
     */
    private T data;

    private Result() {
    }

    // ================ build ================

    public static <T> Result<T> build(T data, Integer code, String message) {
        Result<T> result = new Result<>();
        result.setData(data);
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    public static <T> Result<T> build(T data, IErrorCode errorCode) {
        return build(data, errorCode.getCode(), errorCode.getMessage());
    }

    // ================ success ================

    public static <T> Result<T> success() {
        return build(null, GlobalErrorCode.SUCCESS);
    }

    public static <T> Result<T> success(T data) {
        return build(data, GlobalErrorCode.SUCCESS);
    }

    // ================ error ================

    public static <T> Result<T> error(IErrorCode errorCode) {
        return build(null, errorCode);
    }

    public static <T> Result<T> error(IErrorCode errorCode, String message) {
        return build(null, errorCode.getCode(), message);
    }

    public static <T> Result<T> error(int code, String message) {
        return build(null, code, message);
    }

    @SuppressWarnings("unchecked")
    public static <T> Result<T> error(ServiceException e) {
        Result<T> result = build(null, e.getCode(), e.getMessage());
        if (e.getData() != null) {
            result.setData((T) e.getData());
        }
        return result;
    }

    // ================ helpers ================

    @JsonIgnore
    public boolean isSuccess() {
        return code != null && code == GlobalErrorCode.SUCCESS.getCode();
    }

}
