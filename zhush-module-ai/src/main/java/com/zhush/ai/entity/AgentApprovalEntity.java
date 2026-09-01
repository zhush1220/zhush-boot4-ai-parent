package com.zhush.ai.entity;

import com.mybatisflex.annotation.Table;
import com.zhush.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 智能体审批任务实体（agent_approval）
 *
 * @author zhush
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("agent_approval")
public class AgentApprovalEntity extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 审批编号（Redis key 后缀） */
    private String approvalId;

    /** 业务会话ID */
    private String sessionId;

    /** 智能体名 */
    private String agentName;

    /** 敏感工具名 */
    private String toolName;

    /** 原始入参（JSON） */
    private String toolArgs;

    /** 改参通过后的入参（JSON，改参时写入） */
    private String modifiedArgs;

    /** 提交人用户ID */
    private Long proposerId;

    /** 提交人类型：admin-管理端 member-会员端 */
    private String proposerType;

    /** 提交人名称 */
    private String proposerName;

    /** 状态：0-待审批 1-已通过 2-已驳回 3-已过期 */
    private Integer status;

    /** 审批人用户ID */
    private Long approveUserId;

    /** 审批时间 */
    private LocalDateTime approveTime;

    /** 驳回原因 */
    private String rejectReason;

    /** 过期时间（对齐 Redis TTL） */
    private LocalDateTime expireTime;
}
