package com.zhush.common.exception;

/**
 * 业务异常工具类
 *
 * <p>建议在 Service 中静态导入：
 * <pre>{@code
 * import static com.zhush.common.exception.ServiceExceptionUtil.*;
 *
 * User user = userMapper.selectOneById(id);
 * assertNotNull(user, SystemErrorCode.USER_NOT_FOUND, id);
 * }</pre>
 *
 * @author zhush
 */
public final class ServiceExceptionUtil {

    private ServiceExceptionUtil() {
    }

    // ================ 构造 ================

    public static ServiceException exception(IErrorCode errorCode) {
        return new ServiceException(errorCode);
    }

    public static ServiceException exception(IErrorCode errorCode, Object... args) {
        return new ServiceException(errorCode, args);
    }

    public static ServiceException exception(int code, String message) {
        return new ServiceException(code, message);
    }

    // ================ 断言 ================

    /**
     * 条件为 {@code false} 时抛异常
     */
    public static void assertTrue(boolean expression, IErrorCode errorCode, Object... args) {
        if (!expression) {
            throw exception(errorCode, args);
        }
    }

    /**
     * 条件为 {@code true} 时抛异常
     */
    public static void assertFalse(boolean expression, IErrorCode errorCode, Object... args) {
        if (expression) {
            throw exception(errorCode, args);
        }
    }

    /**
     * 对象为 {@code null} 时抛异常
     */
    public static void assertNotNull(Object obj, IErrorCode errorCode, Object... args) {
        if (obj == null) {
            throw exception(errorCode, args);
        }
    }

    /**
     * 对象不为 {@code null} 时抛异常
     */
    public static void assertNull(Object obj, IErrorCode errorCode, Object... args) {
        if (obj != null) {
            throw exception(errorCode, args);
        }
    }

}
