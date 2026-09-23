package com.zhush.agentscope.session;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * 会话快照：多轮对话的状态载体（由存储层持久化以恢复上下文）。
 *
 * <p>存储实现决定序列化方式（Redis 用 JSON）；本类仅描述会话可恢复的最小状态。
 *
 * @author zhush
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionSnapshot implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 业务会话 ID（全局唯一）
     */
    private String sessionId;

    /**
     * 智能体名
     */
    private String agentName;

    /**
     * 租户 ID（多租户隔离键）
     */
    private String tenantId;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 用户类型（admin / member）
     */
    private String userType;

    /**
     * 会话内消息（user/assistant/tool），按序恢复上下文
     */
    @Builder.Default
    private List<SessionMessage> messages = new LinkedList<>();

    /**
     * 消息数
     */
    private int messageCount;

    /**
     * 累计 Token 消耗
     */
    private long tokenCost;

    /**
     * 会话来源位（路线 B 会话级灰度）：true=本会话创建时已启用 AgentScope 原生状态（只发增量、由 state 恢复）；
     * false=旧路径（每轮 toMsgs 全量重发）。创建时按全局开关钉定，之后不随全局开关变化，避免同一会话双事实源。
     */
    private boolean nativeState;
}
