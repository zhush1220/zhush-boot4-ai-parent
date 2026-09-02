package com.zhush.ai.enums;

import com.zhush.common.exception.IErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * ai 模块错误码
 *
 * <p>模块号：{@code 003}
 * <ul>
 *     <li>{@code 1003xxx} —— 客户端错误</li>
 *     <li>{@code 2003xxx} —— 服务端错误</li>
 * </ul>
 *
 * @author zhush
 */
@Getter
@AllArgsConstructor
public enum AiErrorCode implements IErrorCode {

    // ================ 会话 1003 0xx ================
    CONVERSATION_NOT_FOUND(1003001, "会话不存在：{0}"),
    CONVERSATION_LIMIT_EXCEED(1003002, "会话数量已达上限"),
    MESSAGE_CONTENT_EMPTY(1003003, "消息内容不能为空"),
    MESSAGE_TOO_LONG(1003004, "消息长度超出限制：{0}"),

    // ================ 模型 1003 1xx ================
    MODEL_NOT_FOUND(1003101, "模型不存在：{0}"),
    MODEL_NOT_SUPPORTED(1003102, "暂不支持该模型：{0}"),
    MODEL_QUOTA_EXCEED(1003103, "模型调用次数已用尽"),
    MODEL_CODE_DUPLICATE(1003104, "模型编码已存在：{0}"),
    MODEL_REFERENCED(1003105, "模型被启用智能体引用，不能停用/删除：{0}"),
    MODEL_DEFAULT_CANNOT_DISABLE(1003106, "默认模型不能停用，请先切换默认模型"),
    MODEL_TEST_FAIL(2003004, "模型连接测试失败：{0}"),

    // ================ 知识库 1003 2xx ================
    KNOWLEDGE_NOT_FOUND(1003201, "知识库不存在：{0}"),
    KNOWLEDGE_DOC_PARSE_FAIL(1003202, "知识文档解析失败：{0}"),
    EMBEDDING_FAIL(1003203, "向量化处理失败"),

    // ================ 敏感词 1003 3xx ================
    SENSITIVE_WORD_NOT_FOUND(1003301, "敏感词不存在：{0}"),
    SENSITIVE_WORD_DUPLICATE(1003302, "敏感词已存在：{0}"),

    // ================ 上游调用 2003 0xx ================
    UPSTREAM_TIMEOUT(2003001, "AI 服务响应超时"),
    UPSTREAM_ERROR(2003002, "AI 服务调用异常：{0}"),
    UPSTREAM_RATE_LIMITED(2003003, "AI 服务限流，请稍后再试"),
    ;

    private final int code;
    private final String message;

}
