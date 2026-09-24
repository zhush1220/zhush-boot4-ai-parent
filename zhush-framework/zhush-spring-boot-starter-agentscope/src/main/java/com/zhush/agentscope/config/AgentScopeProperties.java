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
 *     session-store: redis     # 会话存储：redis / memory
 *     session-ttl: 24h         # 会话 TTL
 *     request-timeout: 60s
 *     event-async: true        # 事件异步分发（避开审计/用量落库阻塞 SSE 尾包）
 *     safety: { approval-enabled: true, sensitive-keywords: [删除, 转账] }
 *     usage: { enabled: true }  # 用量日聚合落库开关
 *     chat: { max-message-length: 2000 }
 *     quota: { enabled: true, rate-per-minute: 10, max-active-sessions: 200 }
 *     generation: { enabled: false, image: { timeout-minutes: 5 }, video: { timeout-minutes: 30 } }
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
     * 事件异步分发开关（默认开）：关闭后监听器在 SSE 推送线程上同步执行
     */
    private boolean eventAsync = true;

    /**
     * 事件分发线程数
     */
    private int eventPoolSize = 2;

    /**
     * 事件分发队列容量（饱和后退化为同步分发，不丢事件）
     */
    private int eventQueueCapacity = 1000;

    /**
     * 安全审批配置
     */
    private Safety safety = new Safety();

    /**
     * 动态配置（模型/敏感词库表化）
     */
    private DynamicConfig dynamicConfig = new DynamicConfig();

    /**
     * 用量计量配置
     */
    private Usage usage = new Usage();

    /**
     * 原生会话状态（路线 B / P4 会话事实源收敛）
     */
    private NativeState nativeState = new NativeState();

    /**
     * 对话入参约束
     */
    private Chat chat = new Chat();

    /**
     * 配额与限流
     */
    private Quota quota = new Quota();

    /**
     * 图像/视频生成
     */
    private Generation generation = new Generation();

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

        /**
         * 原生确认门禁（路线 B）：敏感工具改由权限层 REQUIRE_USER_CONFIRM 拦截，
         * 禁用工具层 {@code ToolResultBlock.suspended}。仅在 {@code nativeState.enabled=true} 时生效，
         * 二者必须同时开启（默认 false，保持现有工具层挂起行为）。
         */
        private boolean nativeConfirm = false;

        /**
         * 无审批状态存储（无 Redis）时是否 fail-fast 抛错而非静默放行敏感工具。
         * 生产建议置 true；置 false 时降级为直接放行（与 {@code NullToolApprovalHandler} 语义一致）。
         */
        private boolean failOnApprovalStoreUnavailable = false;
    }

    /**
     * 原生会话状态配置（AgentScope AgentState 作为运行态唯一事实源）
     */
    @Data
    public static class NativeState {

        /**
         * 总开关（默认 false）：true 时新会话启用 AgentScope 原生状态持久化，
         * 停止每轮 {@code toMsgs} 全量重发历史（改为只发本轮增量），由框架从 state 恢复上下文。
         * 存量会话按其创建时的来源位继续走旧路径（会话级灰度隔离）。
         */
        private boolean enabled = false;

        /**
         * Redis 状态键前缀
         */
        private String keyPrefix = "agentscope:state:";
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

    /**
     * 用量计量配置（Token 真实计量与日聚合）
     */
    @Data
    public static class Usage {

        /**
         * 用量日聚合落库开关（{@code agent_token_usage}）
         */
        private boolean enabled = true;

        /**
         * 成本估算开关（依赖 {@code ai_model} 单价，缺价记 0）
         */
        private boolean costEnabled = true;
    }

    /**
     * 对话入参约束
     */
    @Data
    public static class Chat {

        /**
         * 单条用户消息长度上限（字符）：超出拒接并提示，避免超长上下文直接击穿模型窗口
         */
        private int maxMessageLength = 2000;
    }

    /**
     * 配额与限流配置（次数配额明细按 {@code ai_quota_rule} 库表配置，此处为全局开关与速率维度）
     */
    @Data
    public static class Quota {

        /**
         * 配额总开关（false 时不校验、不计数，用于灰度与故障隔离）
         */
        private boolean enabled = true;

        /**
         * 单用户每分钟请求上限（QPS 维度，与「按日/月次数配额」正交）
         */
        private int ratePerMinute = 10;

        /**
         * 单用户活跃会话上限（超出拒新建会话）
         */
        private int maxActiveSessions = 200;
    }

    /**
     * 图像/视频生成配置
     */
    @Data
    public static class Generation {

        /**
         * 生成能力总开关（默认关）：未配置供应商 Key 时保持关闭，工具直接拒接而非报错穿透
         */
        private boolean enabled = false;

        /**
         * 产物下载域名白名单（防 SSRF；需包含供应商对象存储域）
         */
        private List<String> allowedHosts = List.of(
                "dashscope-result-bj.oss-cn-beijing.aliyuncs.com",
                "dashscope-audio.oss-cn-beijing.aliyuncs.com",
                "cdn-wansx.cn-hangzhou.oss.aliyuncs.com");

        /**
         * 转存线程数（提交 + 轮询 + 下载的并发上限）
         */
        private int threads = 2;

        /**
         * 等待队列容量（满则置任务失败，不阻塞调用线程）
         */
        private int queueCapacity = 50;

        /**
         * 兜底扫描周期（秒）：扫悬挂任务主动 fetch，前端不轮询也能收敛
         */
        private int sweepIntervalSeconds = 15;

        /** 图像生成参数 */
        private Image image = new Image();

        /** 视频生成参数 */
        private Video video = new Video();
    }

    /**
     * 图像生成参数
     */
    @Data
    public static class Image {

        /**
         * 任务整体超时（分钟），超出置超时状态
         */
        private int timeoutMinutes = 5;

        /**
         * 线程内轮询间隔（秒）
         */
        private int pollIntervalSeconds = 2;

        /**
         * 线程内等待上限（秒），超出后交由兜底扫描接管，释放线程
         */
        private int maxWaitSeconds = 120;

        /**
         * 兜底判定阈值（秒）：提交超此刻仍未结束且无心跳时主动查询
         */
        private int staleSeconds = 90;

        /**
         * 默认尺寸（形如 1024*1024）
         */
        private String defaultSize = "1024*1024";

        /**
         * 单次默认生成张数
         */
        private int defaultCount = 1;

        /**
         * 单次生成张数上限（成本夹紧）
         */
        private int maxCount = 4;
    }

    /**
     * 视频生成参数
     */
    @Data
    public static class Video {

        /**
         * 任务整体超时（分钟）：视频常见 1~5 分钟，留冗余
         */
        private int timeoutMinutes = 30;

        /**
         * 线程内轮询间隔（秒）
         */
        private int pollIntervalSeconds = 5;

        /**
         * 线程内等待上限（秒）
         */
        private int maxWaitSeconds = 600;

        /**
         * 兜底判定阈值（秒）
         */
        private int staleSeconds = 900;

        /**
         * 默认分辨率
         */
        private String defaultSize = "1280*720";

        /**
         * 默认时长（秒）
         */
        private int defaultDuration = 5;

        /**
         * 时长下限（秒）
         */
        private int minDuration = 3;

        /**
         * 时长上限（秒）：防成本失控
         */
        private int maxDuration = 10;
    }
}
