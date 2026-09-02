package com.zhush.agentscope.template;

import com.zhush.agentscope.agent.AgentReply;
import com.zhush.agentscope.event.AgentEvent;
import reactor.core.publisher.Flux;

import java.util.concurrent.CompletableFuture;

/**
 * 统一门面：业务侧唯一入口（同步 / 流式 / 异步 / 一次性调用 / 会话管理）。
 *
 * @author zhush
 */
public interface AgentTemplate {

    /**
     * 同步单轮/多轮对话（自动携带会话历史）。
     *
     * @param agentName 智能体名
     * @param sessionId 会话 ID（null 时自动新建）
     * @param userText  用户输入
     * @return 助手回复
     */
    AgentReply chat(String agentName, String sessionId, String userText);

    /**
     * 流式对话：返回事件流（文本增量 / 工具调用 / 结束），可对接 SSE。
     *
     * @param agentName 智能体名
     * @param sessionId 会话 ID
     * @param userText  用户输入
     * @return 事件流
     */
    Flux<AgentEvent> stream(String agentName, String sessionId, String userText);

    /**
     * 异步对话（内部线程池，提交即返回）。
     *
     * @param agentName 智能体名
     * @param sessionId 会话 ID
     * @param userText  用户输入
     * @return 异步回复
     */
    CompletableFuture<AgentReply> chatAsync(String agentName, String sessionId, String userText);

    /**
     * 无会话的一次性调用（工具 / 定时任务场景）。
     *
     * @param agentName 智能体名
     * @param userText  用户输入
     * @return 助手回复
     */
    AgentReply callOnce(String agentName, String userText);

    /**
     * 新建会话（返回会话 ID）。
     *
     * @param agentName 智能体名
     * @return 会话 ID
     */
    String newSession(String agentName);

    /**
     * 清空会话。
     *
     * @param sessionId 会话 ID
     */
    void clearSession(String sessionId);

    /**
     * 失效指定智能体的运行时缓存（模型配置变更后需重建）。
     *
     * @param agentName 智能体名（null 时失效全部）
     */
    void evictAgentCache(String agentName);

    /**
     * 失效全部智能体运行时缓存。
     */
    void evictAllAgentCache();
}
