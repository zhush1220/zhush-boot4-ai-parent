package com.zhush.agentscope.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * 会话生命周期事件基类。
 *
 * <p>类型化事件与真实 {@code eventType} 字段双重标识，便于流式 SSE 按类型分发，
 * 同时保留泛型监听（{@link AgentEventListener}）。
 *
 * @author zhush
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class AgentEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 事件类型
     */
    private AgentEventType eventType;

    /**
     * 智能体名
     */
    private String agentName;

    /**
     * 业务会话 ID
     */
    private String sessionId;

    /**
     * 租户 ID（多租户隔离）
     */
    private String tenantId;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 事件发生时间
     */
    private Instant time;
}
