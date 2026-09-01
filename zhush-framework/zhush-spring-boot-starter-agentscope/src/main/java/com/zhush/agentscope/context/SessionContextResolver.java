package com.zhush.agentscope.context;

/**
 * 从当前请求上下文解析用户/租户信息（默认对接 Sa-Token admin/member 双账号体系）。
 *
 * <p>多租户隔离是安全底线：解析失败时调用方应拒绝执行（安全兜底），避免上下文串扰。
 *
 * @author zhush
 */
public interface SessionContextResolver {

    /**
     * 解析当前请求上下文。
     *
     * @return 会话上下文（解析失败返回 null 或抛出异常由调用方决定）
     */
    AgentContext resolve();
}
