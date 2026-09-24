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
 * <p>百位分段：0xx 会话、1xx 模型、2xx 知识库、3xx 敏感词、4xx 文档、5xx 配额限流、6xx 生成任务。
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
    SENSITIVE_CONTENT_BLOCKED(1003303, "内容包含敏感信息，已拦截：{0}"),

    // ================ 文档生成/解析 1003 4xx ================
    DOCUMENT_CONTENT_EMPTY(1003401, "文档内容不能为空"),
    DOCUMENT_FORMAT_UNSUPPORTED(1003402, "不支持的文档格式：{0}"),
    DOCUMENT_PARSE_TARGET_EMPTY(1003403, "文件地址不能为空，请提供上传后返回的 URL"),

    // ================ 配额与限流 1003 5xx ================
    AI_QUOTA_DAILY_EXCEED(1003501, "今日调用次数已用尽，请明日再试"),
    AI_QUOTA_MONTHLY_EXCEED(1003502, "本月调用次数已用尽"),
    AI_RATE_LIMITED(1003503, "操作过于频繁，请稍后再试"),
    SESSION_FORBIDDEN(1003504, "无权访问该会话"),
    QUOTA_RULE_NOT_FOUND(1003505, "配额规则不存在：{0}"),
    QUOTA_RULE_DUPLICATE(1003506, "该用户类型与任务类型的配额规则已存在：{0}"),
    QUOTA_RULE_CONFIG_INVALID(1003507, "配额规则配置非法：{0}"),

    // ================ 生成任务 1003 6xx ================
    GENERATION_MODEL_UNAVAILABLE(1003601, "未配置可用的生成模型，请联系管理员"),
    GENERATION_TASK_FORBIDDEN(1003602, "无权访问该生成任务"),
    GENERATION_TASK_NOT_FOUND(1003603, "生成任务不存在：{0}"),
    GENERATION_DISABLED(1003604, "生成能力未开启"),

    // ================ 上游调用 2003 0xx ================
    UPSTREAM_TIMEOUT(2003001, "AI 服务响应超时"),
    UPSTREAM_ERROR(2003002, "AI 服务调用异常：{0}"),
    UPSTREAM_RATE_LIMITED(2003003, "AI 服务限流，请稍后再试"),
    DOCUMENT_RENDER_FAIL(2003005, "文档渲染失败：{0}"),
    DOCUMENT_FETCH_FAIL(2003006, "文件获取失败：{0}"),
    GENERATION_SUBMIT_FAIL(2003601, "生成任务提交失败：{0}"),
    GENERATION_FETCH_FAIL(2003602, "生成任务查询失败：{0}"),
    GENERATION_TRANSFER_FAIL(2003603, "生成产物转存失败：{0}"),
    ;

    private final int code;
    private final String message;

}
