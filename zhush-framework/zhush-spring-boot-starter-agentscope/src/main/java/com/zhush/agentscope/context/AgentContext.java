package com.zhush.agentscope.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 会话上下文：注入系统提示词与工具执行环境的用户/租户信息。
 *
 * @author zhush
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentContext implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 租户 ID（多租户隔离键）
     */
    private String tenantId;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 用户类型：admin-管理端 member-会员端
     */
    private String userType;

    /**
     * 用户名称
     */
    private String nickname;

    /**
     * 当前会话 ID（由 {@code DefaultAgentTemplate} 在构建 RuntimeContext 时回填）：
     * 供生成类工具把异步任务绑定到来源会话（产物回填、越权校验），非登录上下文可为 {@code null}
     */
    private String sessionId;

    /**
     * 本轮是否允许联网（由对话入参按轮次写入）：{@code null} 表示调用方未指定，
     * 由联网工具按 {@code zhush.agentscope.web-search.session-default-enabled} 判定
     */
    private Boolean webSearchEnabled;
}
