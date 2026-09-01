package com.zhush.ai.entity;

import com.mybatisflex.annotation.Table;
import com.zhush.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 对话会话实体（ai_chat_session）
 *
 * @author zhush
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("ai_chat_session")
public class AiChatSessionEntity extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 业务会话ID（AgentSessionManager 生成，全局唯一） */
    private String sessionId;

    /** 智能体名 */
    private String agentName;

    /** 智能体描述快照 */
    private String agentDesc;

    /** 模型名快照 */
    private String model;

    /** 会话标题（取首条用户消息截断） */
    private String title;

    /** 发起用户ID */
    private Long userId;

    /** 用户类型：admin-管理端 member-会员端 */
    private String userType;

    /** 租户ID（多租户隔离） */
    private Long tenantId;

    /** 消息数 */
    private Integer messageCount;

    /** 累计 Token 消耗 */
    private Long tokenCost;

    /** 状态：1-进行中 2-已结束 3-审批挂起 4-异常 */
    private Integer status;

    /** 会话开始时间 */
    private LocalDateTime startTime;

    /** 会话结束时间 */
    private LocalDateTime finishTime;

    /** 最近交互时间（列表排序） */
    private LocalDateTime lastActiveTime;
}
