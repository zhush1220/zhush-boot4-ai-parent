package com.zhush.ai.entity;

import com.mybatisflex.annotation.Table;
import com.zhush.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * AI 对话消息实体（ai_chat_message）
 *
 * @author zhush
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("ai_chat_message")
public class AiChatMessageEntity extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 业务会话ID（关联 ai_chat_session.session_id） */
    private String sessionId;

    /** 会话内消息序号（自增可恢复顺序） */
    private Integer seq;

    /** 角色：user 用户 assistant 助手 tool 工具 */
    private String role;

    /** 内容类型：text / markdown */
    private String contentType;

    /** 消息正文 */
    private String content;

    /** 思考过程（assistant，流式累积） */
    private String thinking;

    /** 工具调用（JSON 数组：name/arguments/result/status） */
    private String toolCalls;

    /** Token 用量（JSON：input/output/total） */
    private String tokenUsage;

    /** 状态：1-正常 2-流式中 3-已中断 4-失败 */
    private Integer status;

    /** 失败/中断原因（列非空，默认空串避免显式 NULL 触发约束） */
    private String errorMsg = "";
}
