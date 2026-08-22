package com.zhush.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 全局错误码
 *
 * <p>编码规则：7 位整型 {类别 1 位}{模块 3 位}{序号 3 位}
 * <ul>
 *     <li>类别：{@code 0} 成功；{@code 1} 客户端错误；{@code 2} 服务端错误</li>
 *     <li>模块：{@code 000} 通用；{@code 001~} 各业务模块分配（例如 001=system、002=member、003=ai）</li>
 * </ul>
 *
 * <p>此处仅存放跨模块通用的错误码，业务模块请在自身包内定义 {@link IErrorCode} 枚举。
 *
 * @author zhush
 */
@Getter
@AllArgsConstructor
public enum GlobalErrorCode implements IErrorCode {

    SUCCESS(0, "成功"),

    // ================ 1xxxxxx 客户端错误 ================
    BAD_REQUEST(1000000, "请求参数错误"),
    PARAM_MISSING(1000001, "缺少必填参数：{0}"),
    PARAM_INVALID(1000002, "参数不合法：{0}"),
    PARAM_TYPE_MISMATCH(1000003, "参数类型不匹配：{0}"),
    REQUEST_BODY_INVALID(1000004, "请求体格式错误"),
    UNAUTHORIZED(1000401, "未登录或登录已过期"),
    FORBIDDEN(1000403, "无权访问该资源"),
    NOT_FOUND(1000404, "资源不存在"),
    METHOD_NOT_ALLOWED(1000405, "不支持的请求方式：{0}"),
    REQUEST_TIMEOUT(1000408, "请求超时"),
    PAYLOAD_TOO_LARGE(1000413, "上传文件过大"),
    UNSUPPORTED_MEDIA_TYPE(1000415, "不支持的媒体类型"),
    REPEATED_REQUEST(1000409, "重复请求，请勿重复提交"),
    TOO_MANY_REQUESTS(1000429, "操作过于频繁，请稍后再试"),
    FILE_EMPTY(1000430, "上传文件不能为空"),
    FILE_TYPE_UNSUPPORTED(1000431, "不支持的文件类型：{0}"),
    FILE_NOT_FOUND(1000432, "文件不存在：{0}"),

    // ================ 2xxxxxx 服务端错误 ================
    INTERNAL_ERROR(2000000, "系统繁忙，请稍后重试"),
    DB_ERROR(2000001, "数据库异常"),
    DB_DUPLICATE_KEY(2000002, "数据已存在"),
    DB_INTEGRITY_VIOLATION(2000003, "数据完整性约束冲突"),
    RPC_ERROR(2000004, "远程调用失败"),
    FILE_UPLOAD_ERROR(2000005, "文件上传失败：{0}"),
    FILE_STORAGE_ERROR(2000006, "文件存储配置错误：{0}"),
    FILE_PARSE_ERROR(2000007, "文件解析失败：{0}"),
    ;

    private final int code;
    private final String message;

}
