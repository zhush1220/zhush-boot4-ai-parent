# 定时任务、RocketMQ 与 WebSocket 使用指南

> 覆盖三个自研 starter：`zhush-spring-boot-starter-job`（Quartz）、`zhush-spring-boot-starter-mq`（RocketMQ）、`zhush-spring-boot-starter-websocket`（WebSocket 推送）。
>
> 三者均遵循统一启停范式：**`zhush.xxx.enabled` 总开关默认关闭，关闭时零装配、零副作用**；业务侧可通过 `@ConditionalOnMissingBean` 覆盖任意默认 Bean。

---

## 一、starter-job：定时任务（Quartz）

### 1.1 模块定位

基于 `spring-boot-starter-quartz`（Boot BOM 托管 Quartz 2.3.2）封装，提供：

| 能力 | 入口 |
|---|---|
| 任务增删改查、暂停/恢复、手动触发 | `JobUtils`（自动装配的 Bean） |
| 任务基类（耗时日志 + 异常包装） | 继承 `AbstractJob` |
| 总开关联动 Quartz 启停 | `JobEnvironmentPostProcessor`（开关关闭时自动补 `spring.quartz.auto-startup=false`） |

### 1.2 配置

```yaml
zhush:
  job:
    enabled: true              # 总开关（默认 false）
    # default-group: DEFAULT   # 未指定分组时的默认分组

# Quartz 原生配置（持久化/集群时放开）
spring:
  quartz:
    job-store-type: jdbc       # memory（内存，重启丢失）/ jdbc（持久化）
    jdbc:
      initialize-schema: never # 表结构由 sql/07_quartz.sql 维护
    properties:
      org.quartz.scheduler.instanceName: zhushScheduler
      org.quartz.scheduler.instanceId: AUTO
      org.quartz.threadPool.threadCount: 10
      org.quartz.jobStore.isClustered: true  # 多实例集群部署时开启
```

- **持久化**：先在目标库执行 `sql/07_quartz.sql`（MySQL InnoDB 16 张表），再配 `job-store-type: jdbc`。
- **内存模式**：不配 `spring.quartz` 即可，任务随 JVM 重启丢失，适合开发调试。
- **启停机制**：`zhush.job.enabled=false` 时不仅不装配 `JobUtils`，还会阻止 Boot 原生 Quartz 调度器启动（用户显式配置 `spring.quartz.auto-startup` 优先）。

### 1.3 定义任务

继承 `AbstractJob`，实现 `doExecute`；任务类由 Spring 管理，可直接 `@Autowired` 注入业务 Bean：

```java
@Component
public class OrderCloseJob extends AbstractJob {

    @Autowired
    private OrderService orderService;

    @Override
    protected void doExecute(JobExecutionContext context) {
        orderService.closeExpiredOrders();
        // 抛出异常会被包装为 JobExecutionException，不影响调度器运行
    }
}
```

需要携带运行时参数时，通过 `JobDataMap` 读取：

```java
@Override
protected void doExecute(JobExecutionContext context) {
    String shopId = context.getMergedJobDataMap().getString("shopId");
}
```

### 1.4 管理任务（JobUtils）

```java
@Autowired
private JobUtils jobUtils;

// 新增/替换任务（同名同组已存在时先删再建）
jobUtils.addCronJob(OrderCloseJob.class, "orderClose", "0 0/5 * * * ?");
// 指定分组 + 参数
jobUtils.addCronJob(OrderCloseJob.class, "orderClose", "order", "0 0/5 * * * ?",
        Map.of("shopId", "1001"));

jobUtils.updateCron("orderClose", null, "0 0 * * * ?");  // 改 Cron（不存在则静默返回）
jobUtils.pauseJob("orderClose", null);                    // 暂停
jobUtils.resumeJob("orderClose", null);                   // 恢复
jobUtils.triggerOnce("orderClose", null);                 // 立即触发一次
Date next = jobUtils.getNextFireTime("orderClose", null); // 下次执行时间（不存在返回 null）
jobUtils.deleteJob("orderClose", null);                   // 删除（连同触发器）
jobUtils.exists("orderClose", null);                      // 是否存在
jobUtils.getScheduler();                                  // 兜底：获取原生 Scheduler
```

分组传 `null` 或空串时自动回落 `zhush.job.default-group`。

### 1.5 注意事项

- 任务类必须有**无参构造器**（Quartz 反射实例化），依赖注入走 Spring 的 JobFactory。
- JDBC 持久化模式下任务定义存库，重复 `addCronJob` 为「删除重建」语义，幂等安全。
- Cron 表达式非法时 `addCronJob`/`updateCron` 抛 `IllegalStateException`，请在上层校验。

---

## 二、starter-mq：消息队列（RocketMQ）

### 2.1 模块定位

因 rocketmq-spring 对 Spring Boot 4 暂无官方支持，本 starter **直接基于官方 `rocketmq-client 5.5.0`**（remoting 协议，与 Boot 版本解耦）自研封装：

| 能力 | 入口 |
|---|---|
| 同步/异步/oneway/延迟/顺序发送 | `RocketMQUtils`（自动装配的 Bean） |
| 注解式监听器 | `@ZhushMqListener` + 实现 `ZhushMessageListener<T>` |
| 消费者生命周期管理 | `MqListenerContainer`（SmartLifecycle，随容器启停） |
| ACL 鉴权 | 配置 `access-key`/`secret-key` 自动启用 |

### 2.2 配置

```yaml
zhush:
  mq:
    enabled: true                        # 总开关（默认 false）
    namesrv-addr: 127.0.0.1:9876         # 多个用分号分隔
    # access-key: xxx                    # broker 开启 ACL 时配置
    # secret-key: xxx
    producer:
      enabled: true                      # 仅消费场景可关闭生产者
      group: zhush_producer_group
      send-timeout: 3000                 # 毫秒
      retry-times: 2                     # 同步发送失败重试
      max-message-size: 4194304          # 4MB
    consumer:
      consume-thread-min: 4
      consume-thread-max: 16
      max-reconsume-times: 3             # 消费失败重试上限
```

> 生产者由 `@Bean(initMethod="start", destroyMethod="shutdown")` 托管；消费者由 `MqListenerContainer` 在容器刷新后统一启动、关闭时统一 shutdown。

### 2.3 发送消息（RocketMQUtils）

```java
@Autowired
private RocketMQUtils rocketMQUtils;

// 同步发送（对象自动 JSON 序列化，返回 SendResult）
SendResult result = rocketMQUtils.sendSync("order_topic", orderDTO);
// 带 Tag + 业务 Key（Key 用于消息轨迹查询）
rocketMQUtils.sendSync("order_topic", "create", orderNo, orderDTO);

// 异步发送
rocketMQUtils.sendAsync("order_topic", orderDTO, new SendCallback() {
    public void onSuccess(SendResult r) { /* ... */ }
    public void onException(Throwable e) { /* ... */ }
});

// 单向发送（不等确认，适合日志类可丢失消息）
rocketMQUtils.sendOneway("log_topic", logEvent);

// 延迟消息（delayLevel 1-18：1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h）
rocketMQUtils.sendDelay("order_topic", orderDTO, 4);  // 延迟 30 秒投递

// 顺序消息（同 hashKey 路由到同一队列，分区有序）
rocketMQUtils.sendOrderly("order_topic", orderDTO, String.valueOf(orderId));
```

payload 支持三种形态：`byte[]` 原样发送、`String` UTF-8 编码、其他对象走 `JacksonUtils.toJsonString` JSON 序列化。

### 2.4 消费消息（注解监听器）

```java
@Component
@ZhushMqListener(topic = "order_topic", consumerGroup = "order_group")
public class OrderListener implements ZhushMessageListener<OrderDTO> {

    @Override
    public void onMessage(OrderDTO message, MqMessage meta) {
        // 业务处理
        // meta.getMsgId() / meta.getTag() / meta.getKeys() / meta.getReconsumeTimes()
    }
}
```

**注解属性：**

| 属性 | 说明 | 默认 |
|---|---|---|
| `topic` | 订阅主题（必填） | - |
| `consumerGroup` | 消费者分组：不同分组独立消费，同分组多实例集群消费（必填） | - |
| `selectorExpression` | Tag 过滤，多个用 `||` 分隔 | `*` |
| `consumeThreadMin/Max` | 消费线程数，`<=0` 用 `zhush.mq.consumer` 全局默认 | 0 |

**序列化约定：** 泛型 `T` 为 `String` 时直传原文；其他类型按 JSON 反序列化为 `T`。与 `RocketMQUtils` 的 JSON 序列化天然对齐。

**重试语义：** `onMessage` 抛出异常时返回 `RECONSUME_LATER`，由 broker 按退避策略重投，超过 `max-reconsume-times` 进入死信队列（`%DLQ%分组名`）。

### 2.5 注意事项

- Topic 与消费者分组需提前在 broker 创建（或开启 `autoCreateTopicEnable`）。
- 监听器 Bean 支持 CGLIB 代理（注解经 `AnnotationUtils.findAnnotation` 解析），可放心加 `@Transactional` 等注解。
- 一个监听器 = 一个独立消费者实例；多个 topic 请定义多个监听器。

---

## 三、starter-websocket：实时推送

### 3.1 模块定位

基于 `spring-boot-starter-websocket`（Spring 原生 `TextWebSocketHandler`）封装：

| 能力 | 入口 |
|---|---|
| 端点注册 + 跨域 + userId 握手提取 | `WebSocketAutoConfiguration` 自动完成 |
| 会话注册表（同用户重连自动关旧会话） | `WebSocketSessionHolder` |
| 定向推送 / 广播 / 在线查询 | `WebSocketUtils`（自动装配的 Bean） |
| 入站消息接管（可选） | 实现 `WebSocketMessageHandler` 并注册为 Bean |

### 3.2 配置

```yaml
zhush:
  websocket:
    enabled: true                # 总开关（默认 false）
    # endpoint: /ws              # 端点路径
    # allowed-origins: "*"       # 允许的来源，生产建议收敛如 https://*.zhush.com
    # heartbeat-interval: 30     # 心跳间隔（秒），预留项，当前未接入主动 Ping
```

### 3.3 客户端连接

```
ws://localhost:9166/ws?userId=10001
```

- 握手时自动从 `userId` 查询参数提取用户标识写入会话属性；**未传 userId 时降级为 sessionId**（无法定向推送，仅能收到广播）。
- 同一 userId 重复连接时，旧会话被主动关闭，保证「一个用户一个活跃会话」。

前端示例：

```javascript
const ws = new WebSocket('ws://localhost:9166/ws?userId=10001');
ws.onmessage = (e) => console.log(JSON.parse(e.data));
```

### 3.4 服务端推送（WebSocketUtils）

```java
@Autowired
private WebSocketUtils webSocketUtils;

// 定向推送（对象自动 JSON 序列化）
boolean ok = webSocketUtils.sendToUser("10001", noticeDTO);
// 广播全部在线用户，返回成功送达数
int count = webSocketUtils.sendToAll("系统维护通知");

webSocketUtils.isOnline("10001");   // 是否在线
webSocketUtils.onlineCount();       // 在线人数
```

推送失败（不在线/IO 异常）仅记录日志并返回 `false`，**不抛异常、不影响主业务**。发送对会话加锁，多线程并发推送安全。

### 3.5 接收入站消息（可选）

不实现则入站消息被忽略。实现接口并注册为 Bean 即可接管：

```java
@Component
public class ChatMessageHandler implements WebSocketMessageHandler {

    @Override
    public void handleMessage(String userId, String message, WebSocketSession session) {
        // 处理客户端上行消息；抛异常由框架捕获记日志，不会断开连接
    }
}
```

### 3.6 注意事项

- 当前为**单机会话注册表**（内存 `ConcurrentHashMap`）；多实例部署需要跨节点推送时，建议结合 Redis Pub/Sub 转发（后续可扩展）。
- 会话清理依赖连接关闭回调与传输异常回调；网络异常未触发回调的极端场景可后续接入心跳 Ping 主动清理。
- 鉴权建议：握手阶段通过 `userId` + Token 校验（可自定义 `WebSocketConfigurer` Bean 覆盖默认的，替换内置拦截器）。

---

## 四、依赖引入与开关速查

业务模块按需引入：

```xml
<dependency>
    <groupId>com.zhush</groupId>
    <artifactId>zhush-spring-boot-starter-job</artifactId>
</dependency>
<dependency>
    <groupId>com.zhush</groupId>
    <artifactId>zhush-spring-boot-starter-mq</artifactId>
</dependency>
<dependency>
    <groupId>com.zhush</groupId>
    <artifactId>zhush-spring-boot-starter-websocket</artifactId>
</dependency>
```

| Starter | 开关 | 默认 | 关键配置 |
|---|---|---|---|
| job | `zhush.job.enabled` | false | `spring.quartz.*`、`sql/07_quartz.sql` |
| mq | `zhush.mq.enabled` | false | `zhush.mq.namesrv-addr` |
| websocket | `zhush.websocket.enabled` | false | `zhush.websocket.endpoint` |

dev 环境（`application-dev.yaml`）已预置三套默认关闭的配置块与注释版启用指引，启用时按注释放开即可。
