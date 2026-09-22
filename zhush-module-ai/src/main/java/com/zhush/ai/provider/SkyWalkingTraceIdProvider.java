package com.zhush.ai.provider;

import com.zhush.agentscope.event.AgentTraceIdSupplier;
import org.apache.skywalking.apm.toolkit.trace.TraceContext;
import org.springframework.stereotype.Component;

/**
 * 链路 ID 提供者（SkyWalking 实现）。
 *
 * <p>Agent 事件改为异步分发后，监听器所在线程已无调用方的链路上下文；框架在发布线程采集 traceId
 * 并写入事件，本类提供采集能力，保证审计日志（{@code agent_audit_log.trace_id}）仍可关联原始请求链路。
 *
 * @author zhush
 */
@Component
public class SkyWalkingTraceIdProvider implements AgentTraceIdSupplier {

    @Override
    public String get() {
        String traceId = TraceContext.traceId();
        // SkyWalking 无 agent / 无链路时返回 "N/A" 或空串，统一归一为 null 交由监听器兜底
        return traceId == null || traceId.isBlank() || "N/A".equals(traceId) ? null : traceId;
    }
}
