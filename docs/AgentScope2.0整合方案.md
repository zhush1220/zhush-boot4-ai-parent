# AgentScope 2.0 整合方案（智能体接入与公共封装）

> 目标模块：`zhush-spring-boot-starter-agentscope`（框架层，新建）+ `zhush-module-ai`（业务层，实体化）+ 各业务系统接入（以 `zhush-module-union` 电商联盟推广系统为例）
>
> 版本基准：AgentScope Java 2.0（`io.agentscope:agentscope-harness:2.0.0`）· JDK 21 · Spring Boot 4.1.0
>
> 状态：**设计方案**（本文档仅给出方案与类设计，代码暂不实现）

---

## 一、背景与目标

### 1.1 背景

AgentScope 是阿里巴巴通义实验室推出的生产级智能体框架。Java 2.0 版本将分布式部署、多租户隔离、安全权限、事件可观测等能力做成框架原生特性，可让 Java 后端直接构建生产级智能体应用。

本项目（zhush-boot4-parent）是 Spring Boot 4.1.0 多模块单体，框架层已有 16 个 starter 的成熟封装范式（`zhush.xxx.enabled` 开关 + `AutoConfiguration.imports` 自动装配 + BOM 统一版本），`zhush-module-ai` 目前仅占位，`zhush-module-union`（电商联盟推广系统）等业务模块具备丰富的智能体落地场景。

### 1.2 目标

| 编号 | 目标 | 说明 |
| --- | --- | --- |
| G1 | 统一整合 | 将 AgentScope Java 2.0 封装为框架层 starter，任何业务模块引入即用，**不与具体业务耦合** |
| G2 | 多系统复用 | 同一套封装可供 system / member / union / 未来新增系统复用，业务侧仅需声明 Agent 定义与工具 |
| G3 | 可扩展 | 模型供应商、Agent 定义、工具、会话存储、事件监听、安全审批六大扩展点全部 SPI 化 |
| G4 | 生产可用 | 开箱包含：多模型切换、Redis 会话持久化、流式输出、执行安全（人工审批）、审计日志、多租户上下文 |
| G5 | 渐进落地 | 先跑通最小链路（Hello Agent），再逐步覆盖电商联盟推广系统业务场景 |

---

## 二、AgentScope Java 2.0 核心概念

### 2.1 定位与关键特性

| 概念 | 说明 | 对应能力 |
| --- | --- | --- |
| `ReActAgent` | 核心推理循环（思考 → 调用工具 → 观察 → 继续思考） | 一次请求 → 推理 → 工具 → 回复 |
| `HarnessAgent` | **推荐入口**，在 ReActAgent 之上的工程化包装 | 工作区、记忆、子 Agent、技能、沙箱、Plan Mode |
| `Middleware` | 中间件体系，可插拔拦截推理链路 | 权限校验、限流、审计、安全拦截 |
| `Msg` | 统一消息模型（用户/助手/工具消息） | 多轮对话 |
| `Toolkit` / `@Tool` | 工具注册机制（注解式声明） | Agent 调用业务能力 |
| `AgentState` / `AgentStateStore` | 状态持久化抽象 | 多副本恢复、长任务断点续跑 |
| 事件系统 | 类型化事件暴露文本/思考/工具调用/结果 | 可观测、审计、前端流式推送 |
| 执行安全 | 敏感操作拦截、人工确认/修改参数 | 支付、下单等高风险工具审批 |

### 2.2 模型扩展（模块拆分）

2.0 起模型供应商拆分为独立扩展模块，按需引入：

| 扩展模块 | 适用模型 |
| --- | --- |
| `agentscope-extensions-model-dashscope` | 通义千问（qwen-max 等） |
| `agentscope-extensions-model-openai` | OpenAI 及兼容服务（DeepSeek 等） |
| `agentscope-extensions-model-ollama` | 本地 Ollama 模型 |
| `agentscope-extensions-redis` 等 | 状态持久化后端 |

### 2.3 为什么自建 starter 而非直接用官方 starter

与 `starter-captcha`（自建封装、不用官方 Boot starter）同一决策逻辑：

1. **版本兼容**：官方 `agentscope-spring-boot-starter` 主要适配 Spring Boot 3.x，本项目为 Boot 4.1.0，需隔离兼容性风险；
2. **统一范式**：项目所有中间件均遵循 `zhush.xxx.enabled` 开关 + 自动装配范式，接入方心智一致；
3. **默认能力**：可直接复用项目已有的 `RedisCache`、Sa-Token 账号体系、`@OperateLog` 审计、SkyWalking TID 等公共设施；
4. **可控升级**：AgentScope 版本升级只动 BOM 一处。

---

## 三、总体架构设计

### 3.1 模块归属

```
zhush-boot4-parent
├── zhush-dependencies                        [改] BOM：新增 agentscope 版本管理
├── zhush-framework
│   ├── zhush-spring-boot-starter-agentscope  [新建] 智能体框架封装（零业务依赖）
│   └── ...（其余 16 个 starter 不动）
├── zhush-module-ai                           [实体化] 公共 AI 业务层：通用 Agent 定义 + 对外接口
├── zhush-module-union                        [接入示例] 电商联盟推广系统业务 Agent
└── zhush-server                              [改] 聚合引入 zhush-module-ai
```

### 3.2 三层职责边界

```
┌─────────────────────────────────────────────────────────────────┐
│  业务系统层（union / member / 未来系统）                           │
│  · 实现 AgentDefinitionProvider：声明业务 Agent（提示词/工具/模型）  │
│  · @AgentTool 工具 Bean：商品查询 / 佣金测算 / 文案生成 / 合规审核    │
│  · 监听 AgentEventListener：业务侧异步处理（如会话落库）             │
├─────────────────────────────────────────────────────────────────┤
│  公共 AI 业务层（zhush-module-ai）                                 │
│  · 通用 Agent 定义（客服/翻译/摘要等与系统无关的 Agent）             │
│  · REST + SSE 流式接口：/ai/agent/chat、/ai/agent/stream           │
│  · AI 会话记录落库（可选，复用操作日志体系）                         │
├─────────────────────────────────────────────────────────────────┤
│  框架封装层（zhush-spring-boot-starter-agentscope）               │
│  · 模型工厂：配置驱动多供应商（DashScope/OpenAI/Ollama）            │
│  · AgentRegistry：Agent 定义注册中心（按名称获取，支持动态注册）      │
│  · AgentTemplate：统一门面（同步 / 流式 / 异步 / 多轮）              │
│  · ToolkitRegistry：@AgentTool 自动收集 + 手动注册                  │
│  · AgentSessionManager：会话管理（Redis 持久化 + TTL）              │
│  · 安全中间件：敏感工具审批（人工介入）、执行审计                     │
│  · 事件总线：AgentStarted/Finished/ToolCalled/Error 事件            │
│  · 上下文解析：从 Sa-Token 提取用户/租户注入会话上下文               │
└─────────────────────────────────────────────────────────────────┘
```

### 3.3 一次调用时序（设计）

```
业务 Service ──► AgentTemplate.chat(sessionId, userMsg)
                    │
                    ├─ 1. SessionContextResolver：解析当前用户/租户（Sa-Token）
                    ├─ 2. AgentRegistry.get(agentName) → AgentDefinition
                    ├─ 3. 模型工厂构建 ChatModel（按 definition.model 选择供应商）
                    ├─ 4. HarnessAgent.builder() 组装（模型/工具/记忆/中间件/状态存储）
                    ├─ 5. 会话加载：RedisStateStore 恢复历史（无则新建）
                    ├─ 6. 执行推理 → 触发事件（思考/工具调用/结果 → 事件总线）
                    │       └─ 安全中间件：敏感工具 → 挂起等待人工审批（可恢复执行）
                    ├─ 7. 结果返回（同步）或 SSE 推送（流式）
                    └─ 8. 会话保存 + 审计日志（@OperateLog 或独立 ai_chat_log）
```

---

## 四、工程改造清单

### 4.1 zhush-dependencies（BOM）

```xml
<!-- 新增属性 -->
<agentscope.version>2.0.0</agentscope.version>

<!-- dependencyManagement 新增 -->
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>agentscope-harness</artifactId>
    <version>${agentscope.version}</version>
</dependency>
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>agentscope-extensions-model-dashscope</artifactId>
    <version>${agentscope.version}</version>
</dependency>
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>agentscope-extensions-model-openai</artifactId>
    <version>${agentscope.version}</version>
</dependency>
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>agentscope-extensions-model-ollama</artifactId>
    <version>${agentscope.version}</version>
</dependency>
<!-- 状态后端（如官方提供） -->
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>agentscope-extensions-redis</artifactId>
    <version>${agentscope.version}</version>
</dependency>
```

> 坐标与扩展模块清单以 Maven 中央仓库 / 官方仓库实际发布为准，引入前先 `mvn dependency:get` 验证。

### 4.2 zhush-framework（聚合 POM）

```xml
<modules>
    <!-- 新增 -->
    <module>zhush-spring-boot-starter-agentscope</module>
</modules>
```

### 4.3 新增 starter 的依赖设计

```xml
<dependencies>
    <!-- 基础 -->
    <dependency>com.zhush:zhush-common</dependency>
    <dependency>com.zhush:zhush-spring-boot-starter-redis</dependency>   <!-- RedisCache：会话/审批状态 -->
    <!-- AgentScope 核心（harness 自动引入 core） -->
    <dependency>io.agentscope:agentscope-harness</dependency>
    <!-- 模型扩展：默认全量引入，运行时按配置启用（与 starter-search 同思路） -->
    <dependency>io.agentscope:agentscope-extensions-model-dashscope</dependency>
    <dependency>io.agentscope:agentscope-extensions-model-openai</dependency>
    <dependency>io.agentscope:agentscope-extensions-model-ollama</dependency>
    <!-- 可选：Redis 状态后端 -->
    <dependency>io.agentscope:agentscope-extensions-redis</dependency>
    <!-- 自动装配支持 -->
    <dependency>org.springframework.boot:spring-boot-autoconfigure</dependency>
    <dependency>org.springframework.boot:spring-boot-configuration-processor (optional)</dependency>
</dependencies>
```

### 4.4 zhush-module-ai（实体化）

- 引入 `zhush-spring-boot-starter-agentscope` + `starter-web` + `starter-mybatis`（会话落库时）；
- 提供通用 Agent 定义与 `/ai/**` 对外接口（REST + SSE）；
- 维护 `AiErrorCode` 扩展。

### 4.5 zhush-server

- 引入 `zhush-module-ai`（启动类 `scanBasePackages = "com.zhush"` 已覆盖，无需改动扫描配置）；
- `application-{env}.yaml` 增加 `zhush.agentscope` 配置段。

---

## 五、公共封装 API 设计（starter 内部）

> 以下为**设计签名**（含关键方法），实现阶段按此落地；包根 `com.zhush.agentscope`。

### 5.1 配置类

```java
/**
 * 总开关：zhush.agentscope.enabled=true 时装配（默认关闭，零开销），
 * 与其他 starter 完全同范式
 */
@AutoConfiguration
@ConditionalOnClass({HarnessAgent.class, RedisCache.class})
@ConditionalOnProperty(prefix = "zhush.agentscope", name = "enabled",
        havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AgentScopeProperties.class)
public class AgentScopeAutoConfiguration { ... }
```

### 5.2 属性类（AgentScopeProperties）

```java
public class AgentScopeProperties {
    /** 默认模型名（如 qwen-max） */
    private String defaultModel = "qwen-max";
    /** 多供应商配置 */
    private Map<String, ModelProperties> models = new HashMap<>();
    /** 会话存储：redis / memory，默认 redis */
    private String sessionStore = "redis";
    /** 会话 TTL（默认 24h） */
    private Duration sessionTtl = Duration.ofHours(24);
    /** 流式输出缓冲区大小 / 超时 */
    private int streamBufferSize = 1024;
    private Duration requestTimeout = Duration.ofSeconds(60);
    /** 安全审批开关 */
    private Safety safety = new Safety();

    public static class ModelProperties {
        private String type;          // dashscope | openai | ollama
        private String apiKey;        // 支持 ${ENV} 占位
        private String baseUrl;
        private String model;         // 默认模型名
        private Map<String, Object> options = new HashMap<>();  // 温度等透传参数
    }

    public static class Safety {
        private boolean approvalEnabled = true;   // 人工审批总开关
        private List<String> sensitiveKeywords;   // 敏感词触发审批
    }
}
```

### 5.3 模型工厂（可扩展点 1：模型供应商）

```java
/** 模型供应商 SPI：新增供应商只需实现并注册为 Spring Bean */
public interface ChatModelFactory {
    String type();                                   // 供应商标识：dashscope/openai/...
    ChatModel create(AgentScopeProperties.ModelProperties props);
}

/** 默认实现：根据配置 type 分发到内置工厂（DashScope/OpenAI/Ollama） */
public class DefaultModelFactory implements ChatModelFactory { ... }
```

### 5.4 Agent 定义与注册中心（可扩展点 2：Agent 定义）

```java
/** 一个 Agent 的完整描述（业务侧声明式定义） */
public class AgentDefinition {
    private String name;              // 唯一标识，如 "union-selector"
    private String description;       // 功能说明
    private String systemPrompt;      // 系统提示词
    private String model;             // 模型名，缺省用默认模型
    private List<String> toolNames;   // 允许使用的工具
    private boolean streaming;        // 是否支持流式
    private int maxSteps;             // 最大推理步数
    private List<String> skills;      // 技能（HarnessAgent 能力）
}

/** 业务侧扩展点：向注册中心提供 Agent 定义 */
public interface AgentDefinitionProvider {
    List<AgentDefinition> getDefinitions();
}

/** 注册中心：按名称获取 / 动态注册 / 查询 */
public interface AgentRegistry {
    AgentDefinition get(String name);
    boolean contains(String name);
    void register(AgentDefinition definition);   // 支持运行时动态注册
    Collection<AgentDefinition> all();
}
```

### 5.5 统一门面 AgentTemplate（业务唯一入口）

```java
public interface AgentTemplate {

    /** 同步单轮/多轮对话（自动携带会话历史） */
    AgentReply chat(String agentName, String sessionId, String userText);

    /** 流式对话：返回事件流（文本增量/工具调用/结束），可对接 SSE */
    Flux<AgentEvent> stream(String agentName, String sessionId, String userText);

    /** 异步对话（内部线程池，提交即返回） */
    CompletableFuture<AgentReply> chatAsync(String agentName, String sessionId, String userText);

    /** 无会话的一次性调用（工具/定时任务场景） */
    AgentReply callOnce(String agentName, String userText);

    /** 新建/清空会话 */
    String newSession(String agentName);
    void clearSession(String sessionId);
}
```

### 5.6 工具注册（可扩展点 3：工具）

```java
/**
 * 业务工具注解：标注在 Spring Bean 方法上，starter 启动时自动收集到 ToolkitRegistry
 * （也可通过实现 ToolProvider 手动注册）
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AgentTool {
    String name();           // 工具名，缺省取方法名
    String description();    // 给 LLM 看的说明
    boolean requiresApproval() default false;  // 敏感工具：需人工审批
}

/** 手动注册扩展点 */
public interface ToolProvider {
    List<RegisteredTool> getTools();
}

/** 工具注册中心（启动时扫描 + 运行时注册） */
public interface ToolkitRegistry {
    void register(Object bean, Method method, AgentTool anno);
    Toolkit buildToolkit(AgentDefinition definition);  // 按定义裁剪工具集
}
```

### 5.7 会话与状态（可扩展点 4：会话存储）

```java
/** 会话状态存储 SPI：默认 Redis 实现，可替换为 DB/内存 */
public interface AgentSessionStore {
    void save(String sessionId, SessionSnapshot snapshot);
    Optional<SessionSnapshot> load(String sessionId);
    void delete(String sessionId);
    /** 会话维度超时回收由 Redis TTL 承担 */
}

/** 会话管理器：负责会话创建、恢复、上下文注入、TTL */
public class AgentSessionManager { ... }
```

### 5.8 事件与审计（可扩展点 5：事件监听）

```java
/** 会话生命周期事件（类型化，与 AgentScope 事件系统桥接） */
public abstract class AgentEvent {                    // 基类
    private String agentName;
    private String sessionId;
    private Instant time;
}
public class AgentStartedEvent extends AgentEvent { ... }
public class AgentFinishedEvent extends AgentEvent {   // 含耗时/Token 消耗
    private String reply;
    private int stepCount;
}
public class AgentToolCalledEvent extends AgentEvent { // 工具名/参数/结果
    private String toolName;
    private String arguments;
    private boolean approved;
}
public class AgentErrorEvent extends AgentEvent {      // 异常/重试信息
    private String error;
}

/** 业务侧扩展点：监听会话事件（落库、推送、告警） */
public interface AgentEventListener {
    default void onEvent(AgentEvent event) { }
}

/** 事件发布器：同步 + 异步（可配置线程池）双模式 */
public class AgentEventPublisher { ... }
```

### 5.9 执行安全（可扩展点 6：人工审批）

```java
/**
 * 敏感工具审批处理器 SPI：
 * 默认实现将待审批任务写入 Redis（key: agentscope:approval:{sessionId}:{tool}），
 * 并提供 REST 接口供管理端审批/驳回/修改参数；审批后精确恢复执行。
 */
public interface ToolApprovalHandler {
    ApprovalStatus submit(ApprovalRequest request);   // 提交审批
    void approve(String approvalId, Map<String, Object> modifiedArgs);  // 通过（可改参）
    void reject(String approvalId, String reason);    // 驳回
}
```

### 5.10 上下文解析（多租户隔离）

```java
/** 从当前请求上下文解析用户/租户信息（默认对接 Sa-Token admin/member 双账号体系） */
public interface SessionContextResolver {
    AgentContext resolve();
}
public class AgentContext {   // 注入会话的系统提示词与工具执行环境
    private String tenantId;      // 多租户隔离键
    private Long userId;
    private String userType;      // admin | member
    private String nickname;
}
```

### 5.11 自动装配清单（AutoConfiguration 注册 Bean）

| Bean | 说明 |
| --- | --- |
| `AgentScopeProperties` | 配置绑定 |
| `DefaultModelFactory` + 各内置供应商工厂 | 模型构建 |
| `AgentRegistry` | 收集所有 `AgentDefinitionProvider` Bean 初始化注册表 |
| `ToolkitRegistry` | 后处理器扫描 `@AgentTool` Bean 方法 |
| `AgentTemplate`（默认实现） | 门面实现，聚合以上组件 |
| `AgentSessionManager` + `RedisAgentSessionStore` | 会话管理 |
| `AgentEventPublisher` | 事件总线，收集 `AgentEventListener` |
| `DefaultToolApprovalHandler` | 安全审批（`approval-enabled` 为 true 时） |
| `DefaultSessionContextResolver` | 对接 Sa-Token |

注册文件：`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`（与既有 starter 一致）。

---

## 六、配置项设计（application.yaml）

```yaml
zhush:
  agentscope:
    enabled: true                    # 总开关，默认 false（零装配）
    default-model: qwen-max
    models:
      dashscope:                     # 通义千问
        type: dashscope
        api-key: ${DASHSCOPE_API_KEY}
        model: qwen-max
      deepseek:                      # OpenAI 兼容（DeepSeek 示例）
        type: openai
        api-key: ${DEEPSEEK_API_KEY}
        base-url: https://api.deepseek.com/v1
        model: deepseek-chat
      local:                         # 本地 Ollama
        type: ollama
        base-url: http://localhost:11434
        model: qwen2.5:7b
    session:
      store: redis                   # redis | memory
      ttl: 24h
    request-timeout: 60s
    safety:
      approval-enabled: true         # 敏感工具人工审批
      sensitive-keywords: [删除, 转账, 下单, 退款]
```

---

## 七、可扩展点一览（六横一纵）

| # | 扩展点 | 扩展方式 | 适用场景 |
| --- | --- | --- | --- |
| 1 | 模型供应商 | 实现 `ChatModelFactory`，注册 Bean | 接入新大模型服务商 |
| 2 | Agent 定义 | 实现 `AgentDefinitionProvider` | 各业务系统声明自己的 Agent |
| 3 | 工具 | `@AgentTool` 注解 Bean / 实现 `ToolProvider` | 业务能力开放给 LLM（查库/算佣金/发通知） |
| 4 | 会话存储 | 实现 `AgentSessionStore` | 替换为 DB、ES 等持久化方案 |
| 5 | 事件监听 | 实现 `AgentEventListener` | 会话落库、消息推送、监控告警 |
| 6 | 安全审批 | 实现 `ToolApprovalHandler` | 对接企业 OA/审批流 |
| 纵 | 上下文 | 实现 `SessionContextResolver` | 多系统多租户上下文注入（默认 Sa-Token） |

**约束**：框架层仅依赖 SPI 接口，业务模块通过 Spring Bean 机制被自动收集，**新增业务系统零改动框架**。

---

## 八、业务接入指南

### 8.1 通用流程（任何业务系统三步接入）

1. **pom 引入**：`zhush-spring-boot-starter-agentscope`；
2. **声明 Agent**：实现 `AgentDefinitionProvider`（提示词 + 模型 + 工具清单）；
3. **实现工具**：`@AgentTool` 标注业务方法（查库、计算、推送）。

### 8.2 电商联盟推广系统落地场景（zhush-module-union）

| 场景 | Agent 名 | 挂载工具（union 现有表/服务） | 说明 |
| --- | --- | --- | --- |
| 选品推荐助手 | `union-selector` | `@AgentTool queryHotGoods`（union_goods 热度/佣金排序）、`queryCategoryTree` | 自然语言选品，如"推荐 100 元内高佣美妆品" |
| 佣金政策问答 | `union-policy` | `@AgentTool calcCommission`（佣金测算）、`queryPolicyRules` | 佣金比例、结算规则问答 |
| 推广文案生成 | `union-copywriter` | `@AgentTool getGoodsDetail`、`queryPlatformTemplate` | 按平台（淘宝/拼多多/京东）风格生成标题/小红书文案 |
| 素材合规审核 | `union-compliance` | `@AgentTool checkAdvertWords`（广告法敏感词库）、`checkImageMeta` | 上架前自动审核 + 人工审批兜底 |
| 经营数据洞察 | `union-insight` | `@AgentTool queryDailyStat`、`queryTrend`（现有报表服务） | 自然语言问数：如"上周联盟佣金 Top10" |

**union 接入示意**（设计方案，非最终代码）：

```java
@Component
public class UnionAgentDefinitionProvider implements AgentDefinitionProvider {

    @Override
    public List<AgentDefinition> getDefinitions() {
        return List.of(
            AgentDefinition.builder()
                .name("union-selector")
                .description("电商联盟选品推荐助手")
                .systemPrompt("你是资深电商选品顾问，基于工具返回的商品数据，"
                        + "按用户预算与需求推荐高性价比商品，并给出推荐理由。")
                .model("qwen-max")
                .toolNames(List.of("queryHotGoods", "queryCategoryTree", "calcCommission"))
                .maxSteps(6)
                .build()
        );
    }
}

@Component
public class UnionGoodsTools {

    @AgentTool(name = "queryHotGoods", description = "按条件查询热门商品列表（价格区间/类目/佣金率排序）")
    public List<GoodsVO> queryHotGoods(@ToolParam(name = "priceMax", description = "最高价") BigDecimal priceMax,
                                       @ToolParam(name = "category", description = "类目名") String category) {
        return goodsMapper.selectHotList(...);   // 复用 union 现有 Mapper
    }

    @AgentTool(name = "calcCommission", description = "计算商品预估佣金", requiresApproval = false)
    public String calcCommission(@ToolParam(name = "goodsId") Long goodsId) {
        return commissionService.calc(goodsId).toString();
    }
}
```

**调用方**（Controller 或 Service）：

```java
@RestController
@RequestMapping("/union/agent")
public class UnionAgentController {

    @Resource
    private AgentTemplate agentTemplate;

    @PostMapping("/chat")
    public Result<AgentReply> chat(@RequestBody UnionChatReq req) {
        // sessionId 缺省时由 AgentTemplate 新建；用户/租户上下文由 Sa-Token 自动注入
        return Results.success(agentTemplate.chat("union-selector", req.getSessionId(), req.getText()));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<AgentEvent> stream(@RequestParam String sessionId, @RequestParam String text) {
        return agentTemplate.stream("union-selector", sessionId, text);
    }
}
```

### 8.3 zhush-module-ai 公共 Agent（跨系统通用）

| Agent 名 | 用途 | 建议归属 |
| --- | --- | --- |
| `ai-chat` | 通用对话助手（默认提示词） | zhush-module-ai |
| `ai-translator` | 多语言翻译 | zhush-module-ai |
| `ai-summary` | 长文本摘要 | zhush-module-ai |

由 `zhush-module-ai` 提供 `AgentDefinitionProvider` 与 `/ai/**` REST + SSE 接口，各系统可复用。

---

## 九、安全与审计设计

| 安全项 | 方案 |
| --- | --- |
| 敏感工具审批 | `requiresApproval=true` 工具挂起执行 → Redis 待审队列 → 管理端审批/改参/驳回 → 精确恢复 |
| 执行隔离 | 生产环境以容器（Docker/K8s）天然隔离；必要时启用 AgentScope Sandbox 扩展 |
| 提示词注入 | Agent 定义白名单注册（仅注册中心内的 Agent 可被调用），拒绝任意名称调用 |
| 租户隔离 | `SessionContextResolver` 注入 tenantId，工具执行上下文携带租户过滤条件 |
| 成本控制 | `maxSteps` 上限 + 单会话消息数上限 + 超时熔断（配置驱动） |
| 审计 | 事件总线对接 `@OperateLog` / 操作日志表，记录会话、Token 消耗、工具调用与审批记录 |
| 密钥管理 | API Key 走环境变量/配置中心（jasypt 加密），禁止硬编码 |

---

## 十、实施计划（里程碑）

| 阶段 | 内容 | 验收标准 |
| --- | --- | --- |
| M1 骨架 | BOM + 新建 starter 模块 + 配置类 + 模型工厂 + AgentTemplate 最小实现 | `zhush.agentscope.enabled=true` 下跑通 Hello Agent（DashScope） |
| M2 能力补齐 | ToolkitRegistry 自动扫描 + AgentRegistry + Redis 会话持久化 + 事件发布 | 多轮对话携带历史；工具可被 LLM 调用 |
| M3 生产化 | 安全审批 + SSE 流式 + Sa-Token 上下文 + 审计落库 + 单元测试 | union 选品 Agent 全链路可用（含审批） |
| M4 业务落地 | zhush-module-ai 公共 Agent + union 五个场景 + 使用指南文档更新 | 电商联盟推广系统完整接入，测试全绿 |

**依赖关系**：M1 → M2 → M3 → M4 串行；M4 场景可按优先级单独排期。

---

## 十一、风险与注意事项

| 风险 | 等级 | 应对 |
| --- | --- | --- |
| AgentScope 2.0 为较新版本，API 可能迭代 | 中 | BOM 锁定版本；封装层集中隔离，升级只改 starter 内部 |
| 官方 starter 与 Spring Boot 4.1.0 兼容性未知 | 高 | **不使用官方 starter**，自建封装（与 captcha 同策略）；引入前先做最小依赖验证 |
| 扩展模块坐标/版本以官方仓库为准 | 中 | M1 前 `mvn dependency:get` 验证全部坐标 |
| 模型 API Key 泄露风险 | 高 | 环境变量 + jasypt，审批/审计链路禁止输出 Key |
| 长任务稳定性（上下文膨胀） | 中 | 启用 HarnessAgent 上下文压缩（Compaction）+ maxSteps 上限 |
| 与既有 `zhush-module-ai` 空壳模块的关系 | 低 | 将 AI 公共业务落在该模块，避免新建重复模块 |

---

## 十二、附：Maven 坐标速查

| 用途 | 坐标 | 版本 |
| --- | --- | --- |
| 核心 + Harness | `io.agentscope:agentscope-harness` | 2.0.0 |
| 核心（仅 ReAct） | `io.agentscope:agentscope-core` | 2.0.0 |
| 通义千问扩展 | `io.agentscope:agentscope-extensions-model-dashscope` | 2.0.0 |
| OpenAI 兼容扩展 | `io.agentscope:agentscope-extensions-model-openai` | 2.0.0 |
| Ollama 本地扩展 | `io.agentscope:agentscope-extensions-model-ollama` | 2.0.0 |
| Redis 状态后端 | `io.agentscope:agentscope-extensions-redis` | 2.0.0 |

> 官方文档：https://java.agentscope.io/ · 代码仓库：https://github.com/agentscope-ai/agentscope-java

---
---

# 深化分析（第二轮）
> 以下章节在总体方案之上，对**技术选型、核心能力映射、扩展机制、业务场景、横切关注点、风险**做进一步论证，仍为纯设计分析，不含实现代码。
>
## 十三、技术选型对比与决策论证（AgentScope / Spring AI / LangChain4j）

### 13.1 候选框架对比（按项目关键维度）
| 维度 | AgentScope Java 2.0 | Spring AI | LangChain4j |
| --- | --- | --- | --- |
| 定位 | 生产级智能体框架 | LLM 应用抽象层 | LLM 应用工具库 |
| 多智能体工作流（Workflow） | ✅ 原生（分支/循环/状态流转/多 Agent 协作） | ⚠️ 需基于 Advisor 自研编排 | ⚠️ 需基于 AiServices 自研 |
| 执行安全（敏感工具拦截/人工审批） | ✅ 原生（暂停→精确恢复） | ❌ 需自建拦截链 | ⚠️ 半原生（需自建） |
| 多租户隔离 | ✅ 原生支持 | ❌ 自建 | ❌ 自建 |
| 事件可观测（类型化事件流） | ✅ 原生 | ⚠️ 部分（ChatResponse 回调） | ⚠️ 部分 |
| 状态持久化/断点续跑 | ✅ 原生（AgentStateStore） | ⚠️ ChatMemory 有限 | ⚠️ ChatMemory 有限 |
| 上下文压缩 | ✅ 原生（Compaction） | ❌ | ⚠️ |
| Spring Boot 生态融合度 | ⚠️ 官方 starter 适配 Boot 3.x，需自建封装 | ✅ 官方深度集成 | ✅ 有 starter |
| 成熟度/社区 | 🟡 较新（阿里背书，迭代快） | ✅ 高（Spring 官方） | ✅ 高 |
| 学习成本 | 中（新 API） | 低（Spring 心智） | 中 |
| 与现有基础设施可结合度 | 高（依赖少，易隔离） | 高（原生） | 高 |
| 多系统/多场景扩展性 | 高 | 中 | 中 |
| 未来演进风险 | 中（2.0 新发，API 仍演进） | 低 | 低 |
| 适用场景 | 多智能体、工具密集、需审批/隔离的生产级系统 | 快速 LLM 接入（问答/摘要/翻译） | 原型/中等复杂度 |
### 13.2 决策结论与理由（项目视角）
| 评估点 | 结论 |
| --- | --- |
| 1. 多系统、多智能体诉求 | 电商联盟推广系统需要多角色协作（选品/文案/合规/审核），AgentScope 2.0 的 Workflow + 子 Agent 编排更匹配 |
| 2. 敏感工具审批（佣金调整、订单退款） | AgentScope 原生支持"暂停 → 人工确认 → 精确恢复"，避免自建中断恢复链路 |
| 3. 多租户/数据权限融合 | AgentScope 原生多租户，与项目 Sa-Token + 数据权限 starter 结合成本低 |
| 4. 事件可观测 | AgentScope 事件系统可直接桥接项目 SSE / WebSocket / 操作日志体系 |
| 5. Spring Boot 4.1.0 兼容性 | 三者均不直接保证，但 AgentScope 走自建封装路线，隔离性最好 |
| 6. 成本与风险 | 版本较新存在演进风险，通过 BOM 锁定 + 封装隔离，升级成本集中在 starter 内部 |
**结论**：以 AgentScope Java 2.0 为主框架，通过自建 `zhush-spring-boot-starter-agentscope` 落地；Spring AI / LangChain4j 不作为本期引入项，保留观察位（若后续仅做轻量问答，可用 AgentScope 的 ReActAgent 直接承载）。
### 13.3 选型边界（何时不应使用）
- 简单文本生成/摘要 → 直接调模型 API 即可，无需 Agent 编排；
- 纯检索增强（RAG）→ 可独立引入向量库方案，不与本封装耦合；
- 需要极致吞吐的批处理任务 → Agent 循环开销大，建议拆分为定时任务（starter-job）+ 批量 API 调用。
---
## 十四、AgentScope 2.0 核心能力与项目基础设施映射（逐项论证）
### 14.1 能力映射矩阵（每项标注落地路径与可行性）
| AgentScope 能力 | 说明 | 项目映射 / 落地点 | 可行性 |
| --- | --- | --- | --- |
| 事件系统 | 文本/思考/工具调用/结果均以类型化事件暴露 | 桥接到事件总线 → SSE（starter-web）/ WebSocket（starter-websocket）/ 操作日志（`@OperateLog`） | ✅ 高 |
| 执行安全 | 敏感工具白名单拦截 | `@AgentTool` + `requiresApproval` + Redis 审批队列 | ✅ 高 |
| 人工介入 | 暂停 → 人工审批 → 精确恢复 | 复用现有审批流程（管理端 + starter-web REST） | ✅ 高 |
| 多租户隔离 | 会话/工作区按租户隔离 | `SessionContextResolver` 对接 Sa-Token；租户 ID 参与会话 Key 与工具执行过滤 | ✅ 高 |
| 状态持久化 | 会话快照可恢复 | 复用 `zhush-spring-boot-starter-redis` 的 RedisCache；Key 带租户前缀 | ✅ 高 |
| 上下文压缩（Compaction） | 长对话自动摘要 | 直接启用框架能力，结合 `maxSteps` 防上下文膨胀 | ✅ 中（依赖框架行为，需实测） |
| 沙箱（Sandbox） | 工具隔离执行 | 生产容器化天然隔离；必要时启用官方 sandbox 扩展 | ✅ 中 |
| Plan Mode | 规划模式 | 用于复杂任务（数据洞察、多步文案生成） | ✅ 中 |
| Skill | 可复用技能 | 封装"佣金计算""敏感词检查"等复用技能 | ✅ 中 |
| Middleware | 中间件拦截 | 注入审计/限流/租户过滤，链式可插拔 | ✅ 高 |
### 14.2 映射原则（框架封装层的约束）
1. **不重复造轮子**：项目已有的能力（Redis、日志、SSE、操作日志、SkyWalking TID）优先复用，避免在 starter 内部重复实现；
2. **可观测先行**：所有关键动作（工具调用、审批、模型请求）必须落到事件总线，确保审计完整；
3. **安全默认关闭敏感工具**：`requiresApproval` 默认按配置保守策略执行，敏感操作默认走审批；
4. **租户贯穿**：从会话创建 → 工具执行 → 日志记录，租户 ID 全程可见可追溯。
---
## 十五、可扩展性细化（四级扩展机制）
在第六节"六大扩展点"基础上，按扩展**层级**进一步细化，明确不同场景的扩展方式与约束。
### 15.1 扩展层级总览（横向分层）
| 层级 | 扩展方式 | 典型场景 | 生效方式 | 扩展者心智成本 |
| --- | --- | --- | --- | --- |
| L1 配置级 | yaml / 配置中心 | 切换模型、调整提示词、改阈值 | 重启生效（配置中心可热更） | 低 |
| L2 代码级 | SPI 实现 / `@AgentTool` 注解 / 监听器 | 新增业务 Agent、注册业务工具、监听事件 | 重启生效 | 中 |
| L3 插件级 | 独立 jar / 独立 starter | 跨团队复用、平台化工具集 | 重启生效 | 中高 |
| L4 热插拔级 | 运行时动态注册 | 运营侧临时新增 Agent / 工具、A/B 实验 | 无需重启 | 高 |
### 15.2 每级扩展点的对应机制（落地路径）
| 层级 | 扩展点 | 落地机制 |
| --- | --- | --- |
| L1 | 模型/提示词/超时/阈值 | `zhush.agentscope.models.*`、`session.ttl`、`request-timeout`、`safety.*` |
| L1 | Agent 提示词热更 | 提示词外置到配置中心（Nacos 类）+ 版本号；运行时读取最新版 |
| L2 | Agent 定义 | `AgentDefinitionProvider`（框架启动时自动收集） |
| L2 | 工具 | `@AgentTool` 注解 + Bean 后处理器自动扫描 |
| L2 | 会话存储 | `AgentSessionStore` SPI（默认 Redis，可替换 DB/ES） |
| L2 | 事件监听 | `AgentEventListener`（同步+异步双模式） |
| L2 | 安全审批 | `ToolApprovalHandler`（默认 Redis 队列，可对接企业审批流） |
| L2 | 模型供应商 | `ChatModelFactory`（默认三供应商，可扩展） |
| L3 | 跨团队工具集 | 以独立 jar 发布 `zhush-agentscope-tools-xxx`，仅依赖注解层 |
| L3 | 平台级 Agent 包 | 独立 `zhush-module-ai` 子模块（公共 Agent 库），供多系统引用 |
| L4 | 动态注册 | `AgentRegistry.register(definition)` / 工具动态注入；版本化防冲突 |
| L4 | 运营配置 | 管理端提供"Agent 管理"页面（可选，二期） |
### 15.3 扩展契约（稳定性保障）
- SPI 接口一经发布即视为稳定契约，变更遵循"新增不破坏"原则；
- 所有扩展点默认有兜底实现，业务侧不实现也能跑通最小链路；
- 扩展点文档与样例落在 `zhush-module-ai` 模块的 README，与框架层解耦。
---
## 十六、电商联盟推广系统业务场景深化（union 五场景全链路）
对第 8.2 节的五个场景做**输入 → 处理 → 输出 → 审批 → 监控**的完整链路描述。
### 16.1 场景一：选品推荐助手（union-selector）
| 维度 | 内容 |
| --- | --- |
| 用户输入 | "帮我推荐 100 元以内高佣金的美妆类商品" |
| 触发工具 | `queryHotGoods`（按类目/价格/佣金排序）、`queryCategoryTree`（类目校验） |
| Agent 处理 | ReAct 循环：解析意图 → 调工具取数 → 综合排序 → 生成推荐理由 |
| 输出 | 结构化推荐列表（商品名/价格/佣金率/推荐理由） |
| 审批点 | 无（只读查询） |
| 监控 | 记录工具调用耗时 / Token 消耗 |
| 数据权限 | 工具执行时通过 `@DataPermission` 过滤当前用户可见范围 |
### 16.2 场景二：佣金政策问答（union-policy）
| 维度 | 内容 |
| --- | --- |
| 用户输入 | "淘宝联盟的佣金结算规则是什么？" |
| 触发工具 | `calcCommission`（预估计算）、`queryPolicyRules`（规则库） |
| Agent 处理 | 直接命中规则库 + 按商品实时估算 |
| 输出 | 规则说明 + 举例测算 |
| 审批点 | 无 |
| 监控 | 命中率、错误率（用于运营优化提示词） |
### 16.3 场景三：推广文案生成（union-copywriter）
| 维度 | 内容 |
| --- | --- |
| 用户输入 | "给这款商品生成小红书种草文案，突出性价比" |
| 触发工具 | `getGoodsDetail`、`queryPlatformTemplate`（平台模板库） |
| Agent 处理 | 读取商品详情 + 按平台风格（淘宝/拼多多/京东）生成 |
| 输出 | 标题 / 正文 / 话题标签（多风格） |
| 审批点 | 高（涉及对外推广，需人工确认，避免违规） |
| 监控 | 生成次数、人工修改率（优化模板） |
### 16.4 场景四：素材合规审核（union-compliance）
| 维度 | 内容 |
| --- | --- |
| 用户输入 | 素材（文本/图片元数据）提交审核 |
| 触发工具 | `checkAdvertWords`（广告法敏感词）、`checkImageMeta`（图片元数据） |
| Agent 处理 | 自动扫描 → 风险分级（低/中/高） |
| 输出 | 审核结论 + 风险清单 + 建议修改 |
| 审批点 | 高（高风险必须人工复核） |
| 监控 | 审核通过率 / 误杀率 |
### 16.5 场景五：经营数据洞察（union-insight）
| 维度 | 内容 |
| --- | --- |
| 用户输入 | "上周联盟佣金收入 Top10 商品是哪些？" |
| 触发工具 | `queryDailyStat`、`queryTrend`（现有报表服务） |
| Agent 处理 | NL2SQL 或调现有报表 API → 聚合 → 总结 |
| 输出 | 数据摘要 + 趋势说明（可对接图表） |
| 审批点 | 低（只读） |
| 监控 | 查询耗时、命中率 |
### 16.6 场景共性约束（所有场景必须满足）
1. **数据权限**：工具执行必须通过 `@DataPermission` 过滤（复用现有行级权限）；
2. **租户隔离**：会话与数据均按租户隔离；
3. **审计**：所有工具调用与审批落操作日志（含 SkyWalking TID）；
4. **降级**：工具调用失败或超时 → 返回明确提示（如"查询超时，请重试"），避免空结果误导。
---
## 十七、横切关注点深度设计（安全/多租户/可观测/成本）
### 17.1 安全设计（四层防护）
| 层级 | 防护项 | 实现方案 |
| --- | --- | --- |
| L1 提示词注入 | 拒绝执行系统级指令 | Agent 提示词白名单 + 用户输入消毒（关键词过滤） |
| L2 敏感工具 | 拦截高风险操作 | `@AgentTool(requiresApproval = true)` + 审批队列 |
| L3 数据权限 | 工具执行时过滤数据 | 复用 `@DataPermission` + 租户过滤 |
| L4 密钥安全 | API Key 不落日志/审计 | jasypt 加密 + 日志脱敏切面 |
**关键设计**：`ToolApprovalHandler` 与现有审批流（管理端）对接，审批任务持久化到 Redis（可替换为 DB），支持"通过（可改参）/ 驳回"，驳回后精确返回错误信息给用户。
### 17.2 多租户设计（会话隔离 + 数据权限融合）
| 维度 | 设计 |
| --- | --- |
| 会话 Key | `agentscope:session:{tenantId}:{sessionId}`，天然按租户隔离 |
| 上下文注入 | `SessionContextResolver` 从 Sa-Token 解析 `tenantId`、`userId`、`userType` |
| 工具执行 | 工具方法通过 `@DataPermission` 或显式传入租户过滤条件 |
| 日志 | 会话/工具调用日志必带 `tenantId` 字段，便于审计与查询 |
| 隔离粒度 | 租户级会话空间（可进一步细化到用户级，按需） |
### 17.3 事件系统与日志/监控集成（可观测性）
| 事件 | 桥接目标 | 用途 |
| --- | --- | --- |
| `AgentStartedEvent` | 操作日志（`@OperateLog`） | 记录会话开始 |
| `AgentToolCalledEvent` | 操作日志 + SkyWalking Span | 工具调用追踪 |
| `AgentFinishedEvent` | 会话落库 + 指标 | 耗时 / Token 消耗 / 步数 |
| `AgentErrorEvent` | 异常告警 | 失败率监控 |
| 文本增量 | SSE / WebSocket | 前端流式展示 |
**集成方案**：`AgentEventPublisher` 内部统一将事件分发到（1）项目事件总线、（2）SSE 端点、（3）操作日志、（4）指标采集器，避免业务代码散落监听。
### 17.4 性能、并发与成本估算（运营视角）
| 维度 | 估算 / 设计 |
| --- | --- |
| 单次会话平均耗时 | 3~10s（含推理 + 工具调用），流式首字延迟目标 < 2s |
| Token 消耗（示例） | 单会话约 1~3k Token，qwen-max 按量计费可估算月度成本 |
| 并发限流 | 单用户 5 次/分钟、单租户 100 次/分钟、全局 500 并发（配置驱动） |
| 超时策略 | 单工具 30s、单会话 180s、单步推理 60s |
| 错误重试 | 工具失败默认重试 2 次（可配置），失败后返回明确提示 |
| 熔断 | 工具连续失败 5 次 → 熔断 1 分钟，防止级联故障 |
| 成本上限 | 按租户配置月度 Token 上限（超限降级或告警） |
### 17.5 部署与运维（多副本、弹性伸缩）
| 维度 | 方案 |
| --- | --- |
| 多副本部署 | 会话状态走 Redis，实例无状态，可水平扩容 |
| 长任务断点续跑 | `AgentStateStore`（Redis）持久化，任务失败可从最近状态恢复 |
| 沙箱隔离 | 生产环境容器化（Docker/K8s）天然隔离，可选官方 sandbox |
| 可观测 | SkyWalking TID 贯穿 + Agent 事件流 + 指标（耗时/Token/失败率） |
| 弹性伸缩 | 按"排队会话数 / Token 消耗"指标触发扩容 |
---
## 十八、技术风险深度分析与应对策略（细化）
| 风险 | 等级 | 触发条件 | 应对策略 |
| --- | --- | --- | --- |
| AgentScope 2.0 API 不稳定 | 中 | 官方 2.0 刚发布，接口可能迭代 | BOM 锁定版本 + 封装隔离（升级只改 starter 内部） |
| 官方 starter 与 Boot 4.1.0 不兼容 | 高 | 官方适配目标为 Boot 3.x | **不使用官方 starter**，自建封装（与 captcha 同策略） |
| 扩展模块坐标/版本不准 | 中 | 扩展模块可能未全部发布 | M1 前 `mvn dependency:get` 全部坐标验证，缺失的降级为"仅核心"模式 |
| API Key 泄露 | 高 | 日志/审计未脱敏 | jasypt + 日志切面强制脱敏，审计禁用输出 Key |
| 长任务上下文膨胀 | 中 | 会话消息过多导致上下文溢出 | 启用 Compaction + `maxSteps` 上限 + 会话消息数上限 |
| 并发过高拖垮模型服务 | 中 | 多租户同时高并发 | 限流 + 队列排队 + 熔断 |
| 工具执行超时拖慢会话 | 中 | 工具调用外部服务慢 | 单工具超时 + 异步化 + 明确提示 |
| 提示词被恶意注入 | 中 | 用户输入含指令 | 输入消毒 + 提示词白名单 + 敏感词拦截 |
| 多租户数据串扰 | 高 | 租户上下文注入失败 | 上下文解析失败时拒绝执行（安全兜底） |
| 与现有 `zhush-module-ai` 冲突 | 低 | 占位模块命名/包冲突 | 明确 AI 业务归属该模块，统一包名 `com.zhush.ai` |
### 18.1 风险优先处理顺序（建议）
1. **高优**：官方 starter 兼容性（M1 前必须验证）→ API Key 安全 → 多租户串扰；
2. **中优**：依赖坐标验证 → 上下文膨胀 → 并发限流；
3. **低优**：模块命名冲突（可在 M1 前一次性解决）。
---
## 十九、总体结论与下一步建议（第二轮分析汇总）
### 19.1 核心结论（第二轮分析沉淀）
1. **选型成立**：AgentScope Java 2.0 与项目多智能体、多租户、审批、可观测的诉求高度匹配，优于 Spring AI / LangChain4j；
2. **自建封装路线正确**：通过 `zhush-spring-boot-starter-agentscope` 隔离 Boot 4.1.0 兼容风险，同时复用项目已有能力（Redis/日志/SSE/SkyWalking）；
3. **扩展机制完备**：四级扩展（配置/代码/插件/热插拔）覆盖不同团队与场景，契约化接口保证稳定；
4. **电商联盟推广系统场景已全链路打通**：五场景（选品/政策/文案/合规/洞察）均明确输入→处理→输出→审批→监控链路；
5. **横切能力齐全**：安全、多租户、可观测、成本、部署运维均有明确落地方案。
### 19.2 待确认事项（实施前）
| 事项 | 负责方 | 说明 |
| --- | --- | --- |
| AgentScope 2.0 全部扩展坐标验证 | 开发 | `mvn dependency:get` 逐一验证 |
| 模型选型（DashScope / DeepSeek / Ollama） | 业务 | 确定默认模型与计费预算 |
| 审批流对接方式 | 产品 | 复用现有管理端审批还是独立审批模块 |
| 会话落库表结构 | 开发 | 复用操作日志还是新建 `ai_chat_session` |
| SSE 前端接入 | 前端 | 流式展示协议与错误处理 |
### 19.3 建议下一步（按里程碑推进）
1. **先做 M1 验证**：用最小依赖跑通 Hello Agent，确认坐标与 Boot 4.1.0 兼容性（这是最大风险点）；
2. **并行准备业务侧**：union 侧梳理五场景的工具清单与数据表映射（不需等 M1）；
3. **文档先行**：本文档作为实施蓝图，M1 完成后补充"实测结论"与"配置示例"。
