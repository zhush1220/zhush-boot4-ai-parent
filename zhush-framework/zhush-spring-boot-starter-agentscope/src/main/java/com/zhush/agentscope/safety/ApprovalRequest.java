package com.zhush.agentscope.safety;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

/**
 * 待审批请求。
 *
 * @author zhush
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 审批编号（全局唯一，Redis key 后缀）
     */
    private String approvalId;

    /**
     * 业务会话 ID
     */
    private String sessionId;

    /**
     * 智能体名
     */
    private String agentName;

    /**
     * 敏感工具名
     */
    private String toolName;

    /**
     * 原始入参（JSON 字符串，脱敏后落库）
     */
    private Map<String, Object> arguments;

    /**
     * 提交人用户 ID
     */
    private Long proposerId;

    /**
     * 提交人类型（admin / member）
     */
    private String proposerType;

    /**
     * 提交人名称
     */
    private String proposerName;

    /**
     * 租户 ID
     */
    private String tenantId;

    /**
     * 过期时间（对齐 Redis TTL）
     */
    private Instant expireTime;
}
