package com.zhush.ai.entity;

import com.mybatisflex.annotation.Table;
import com.zhush.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * AI 审计日志实体（agent_audit_log）
 *
 * <p>事件总线落库：会话/工具/错误/审批全量留痕。
 *
 * @author zhush
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("agent_audit_log")
public class AgentAuditLogEntity extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 链路追踪ID */
    private String traceId;

    /** 业务会话ID */
    private String sessionId;

    /** 智能体名 */
    private String agentName;

    /** 事件类型：STARTED FINISHED TOOL_CALLED ERROR APPROVAL */
    private String eventType;

    /** 工具名（TOOL_CALLED/APPROVAL 时有值） */
    private String toolName;

    /** 工具入参（脱敏，JSON） */
    private String toolArgs;

    /** 工具结果（JSON，超长截断） */
    private String toolResult;

    /** 耗时（毫秒） */
    private Integer duration;

    /** Token 消耗 */
    private Integer tokenUsage;

    /** 推理步数 */
    private Integer stepCount;

    /** 操作人ID */
    private Long userId;

    /** 操作人类型：admin-管理端 member-会员端 */
    private String userType;

    /** 租户ID（审计必带，用于隔离查询） */
    private Long tenantId;

    /** 结果码：0-成功，其余为错误码 */
    private Integer resultCode;

    /** 结果信息（失败时为异常信息） */
    private String resultMsg;
}
