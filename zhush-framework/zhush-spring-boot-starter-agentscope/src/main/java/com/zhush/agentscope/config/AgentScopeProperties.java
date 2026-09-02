package com.zhush.agentscope.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AgentScope 智能体框架配置
 *
 * <p>配置前缀 {@code zhush.agentscope}，示例：
 * <pre>
 * zhush:
 *   agentscope:
 *     enabled: true            # 总开关（false 时不装配任何 Agent Bean）
 *     default-model: qwen-max  # 默认模型名
 *     models:                  # 多供应商模型配置
 *       dashscope: { type: dashscope, api-key: ${DASHSCOPE_API_KEY}, model: qwen-max }
 *       deepseek:  { type: openai, api-key: ${DEEPSEEK_API_KEY}, base-url: https://api.deepseek.com/v1, model: deepseek-chat }
 *       local:     { type: ollama, base-url: http://localhost:11434, model: qwen2.5:7b }
 *     session: { store: redis, ttl: 24h }
 *     request-timeout: 60s
 *     safety: { approval-enabled: true, sensitive-keywords: [删除, 转账] }
 * </pre>
 *
 * @author zhush
 */
@Data
@ConfigurationProperties(prefix = "zhush.agentscope")
public class AgentScopeProperties {

    /**
     * 总开关（false 时不装配任何智能体相关 Bean）
     */
    private boolean enabled = false;

    /**
     * 默认模型名（如 qwen-max，Agent 定义未指定 model 时使用）
     */
    private String defaultModel = "qwen-max";

    /**
     * 多供应商模型配置（key 为逻辑名，如 dashscope / deepseek / local）
     */
    private Map<String, ModelProperties> models = new HashMap<>();

    /**
     * 会话存储实现：redis / memory，默认 redis
     */
    private String sessionStore = "redis";

    /**
     * 会话 TTL（默认 24 小时，驱动 Redis 过期回收）
     */
    private Duration sessionTtl = Duration.ofHours(24);

    /**
     * 流式输出缓冲区大小（SSE 分片）
     */
    private int streamBufferSize = 1024;

    /**
     * 单次调用超时（默认 60 秒）
     */
    private Duration requestTimeout = Duration.ofSeconds(60);

    /**
     * 安全审批配置
     */
    private Safety safety = new Safety();

    /**
     * 动态配置（模型/敏感词库表化）
     */
    private DynamicConfig dynamicConfig = new DynamicConfig();

    /**
     * 模型供应商配置项
     */
    @Data
    public static class ModelProperties {

        /**
         * 供应商标识：dashscope / openai / ollama
         */
        private String type;

        /**
         * API Key（支持 ${ENV} 占位，禁止硬编码）
         */
        private String apiKey;

        /**
         * 服务地址（openai 兼容服务需指定，如 https://api.deepseek.com/v1）
         */
        private String baseUrl;

        /**
         * 默认模型名（如 qwen-max / deepseek-chat / qwen2.5:7b）
         */
        private String model;

        /**
         * 透传给模型的其它参数（温度、top_p 等）
         */
        private Map<String, Object> options = new HashMap<>();
    }

    /**
     * 安全审批配置
     */
    @Data
    public static class Safety {

        /**
         * 敏感工具人工审批总开关
         */
        private boolean approvalEnabled = true;

        /**
         * 触发审批的敏感关键词（命中工具名/入参即挂起）
         */
        private List<String> sensitiveKeywords = List.of();
    }

    /**
     * 动态配置项（模型/敏感词从库表加载）
     */
    @Data
    public static class DynamicConfig {

        /**
         * 动态配置总开关（false 时回退 yaml 配置）
         */
        private boolean enabled = true;

        /**
         * 定时兜底刷新周期（默认 5 分钟）
         */
        private Duration refreshInterval = Duration.ofMinutes(5);

        /**
         * 多实例 Redis pub/sub 广播开关
         */
        private boolean broadcast = true;
    }
}
