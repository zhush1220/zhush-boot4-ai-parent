# zhush-module-pay 统一支付能力整合方案（微信支付 V2 / V3）

> 模块归属：`zhush-module-pay`（统一支付底座 + 微信双协议适配）、`zhush-module-union`（提现业务消费方）、`zhush-server`（装配）
> 外部依赖：`com.github.binarywang:weixin-java-pay:4.8.0`（`zhush-dependencies` 统一管控 `${weixin.version}`）
> 关联能力：`zhush-common`（异常/错误码）、`starter-mybatis`（`EncryptTypeHandler` SM4 字段加密）、`starter-satoken`（`@IgnoreAuth`）、`starter-protection`（`@Idempotent`）、`starter-web`（`@OperateLog`）、`starter-job`（对账轮询）、`starter-redis`（幂等锁/缓存）
> 文档状态：**待确认**（确认后再进入功能编码）

## 一、目标：从「提现专用」升级为「统一支付底座」

### 1.1 本次增量

原方案（V1）定位为「微信商家转账（V3）打通提现」。本次要求**上升为通用能力**：

| 维度 | V1（提现专用） | **V2（本次目标）** |
|---|---|---|
| 定位 | 打通一条提现链路 | **统一支付底座**，为后续所有支付业务打基础 |
| 抽象 | 直接用微信类 | **协议无关的统一门面 + 渠道 SPI** |
| 协议 | 仅 V3 商家转账 | **微信 V2 + V3 双栈**，业务无感知 |
| 业务面 | 出款（提现） | 出款 + **预留收款/退款/对账** |
| 扩展 | 无 | 新增渠道/协议**不改业务代码** |

### 1.2 范围

| 分期 | 内容 |
|---|---|
| **一期** | 统一抽象层（门面 + SPI + 统一模型/状态/错误码/回调）+ 微信 **V3** 出款（商家转账）+ 首业务接入（union 提现） |
| **二期** | 微信 **V2** 出款（企业付款到银行卡/零钱）+ 对账与差异处置 + 多渠道路由 |
| 三期（按需） | 收款（V2 统一下单 / V3 JSAPI）+ 退款 + 支付宝渠道 |

### 1.3 非目标

- 不做收银台 UI / 聚合支付产品；
- 不做资金清结算与分账；
- 一期不改造前端提现页面（除非状态机新增中间态需同步字典）。

## 二、现状核查

### 2.1 `zhush-module-pay` 当前是「空壳模块」

| 项 | 核查结果 |
|---|---|
| git 跟踪文件 | 仅 `pom.xml`、`.gitattributes`、`.gitignore` |
| `src/` 目录 | **不存在** |
| pom 依赖 | `zhush-common` + `com.github.binarywang:weixin-java-pay` |
| 父 POM 注册 | 根 `pom.xml` 已有 `<module>zhush-module-pay</module>` |
| 是否参与运行 | **否**——`zhush-server/pom.xml`、`zhush-module-union/pom.xml` 均未依赖 pay |
| 遗留物 | `target/classes` 保留上一轮编译产物 `com.zhush.pay.wx.*`（14 个类 + `WxBankPayClientTest`），含 `target/zhush-module-pay-1.0.0.jar` |

> 干扰项：`zhush-server/.flattened-pom.xml`、`zhush-module-union/.flattened-pom.xml` 残留 pay 依赖条目，是 flatten 插件历史遗留，与真实 pom 不一致。

### 2.2 历史实现痕迹（**该模块曾实现过完整能力**）

两条独立证据链互相印证：

**证据 A：编译产物签名（`javap` 还原）**

| 类 | 关键签名 |
|---|---|
| `WxPayConfiguration` | 装配 `WxPayService` + `wxTransferNotifyParser` + `wxTransferClient` + `wxBankPayClient`；私有 `validate`/`loadPrivateKey`/`loadResource`（**密钥缺失 fail-fast**） |
| `WxTransferClient` | `transfer` / `query` / `cancel`，内部构造 `bean.transfer.TransferBillsRequest`（V3 商家转账） |
| `WxBankPayClient` | `payBank(WxBankPayRequest)` / `queryPayBank(String)`（**V2 企业付款到银行卡**） |
| `WxTransferNotifyParser` | `parse(body, ...)` → 验签 + 解密 |
| `WxTransferResult` | `accepted` 标志 + `accepted()/failed()` + `success()/finalState()` |
| `WxTransferState` | `ACCEPTED/PROCESSING/WAIT_USER_CONFIRM/TRANSFERING/SUCCESS/FAIL/CANCELING/CANCELLED` |
| `WxPayProperties` | `appId/mchId/mchSerialNo/apiV3Key/privateKeyPath/notifyUrl/transferSceneId/...` + 内嵌 `Bank{mchKey,certPath,...}` |

**证据 B：既有项目记忆（描述的是已实现态）**

- 能力边界：**「仅实现出账/转账：微信 V3 商家转账 + V2 企业付款到银行卡」；无收款/下单/退款；无支付宝（走 Mock 兜底）；回调强制验签 + 幂等已实现；对账为本地自校准**
- 银行卡通道（type=1）：V2 `/mmpaysptrans/pay_bank`，**无结果回调 → 依赖 `queryPayBank` 主动查单收敛**；卡号/姓名 **SM4 密文**存 `pay_account_cipher`（`EncryptTypeHandler`），展示脱敏；SDK 用 `EntPayService.payBank()` 自动 RSA 加密
- 凭证要求：V2 需 **APIv2 密钥（`mchKey`）+ PKCS12 商户证书（双向 TLS，密码=商户号）**，通过 `WxPayConfig.setMchKey()` / `setKeyContent()` 设置
- 资金安全红线：**生产环境无渠道实现时严禁返回成功/accepted**；**撤销仅渠道明确返回 CANCELLED 才解冻**
- 存在未实现功能清单 PAY-01 ~ PAY-13（批次一已落地：生产禁用 Mock 兜底、单日累计限额、管理端人工兜底）

**结论**：源码已丢失，但**设计契约完整可复原**；本项目**已有 V2 的企业付款实践认知**，本次要求将其升级为"统一 + 双协议"，正是对历史缺口的补齐。

### 2.3 业务侧与框架现状

- **union 提现域**：6 张表（`union_withdraw_apply`/`_config`/`_audit_log`/`_risk_record`/`_whitelist`、`union_account_transaction`），**仅有纯 CRUD，无渠道调用**。`union_withdraw_apply` 已有 `withdrawType`(1-银行卡 2-支付宝 3-微信支付)、`status`(1-待审核 2-审核通过 3-已打款 4-已驳回 5-已取消 6-打款失败)、`payChannelOrderNo`、`payFailReason`、`retryCount`、`transactionId/transactionNo`、`extraJson`；**缺 openid 与 SM4 密文字段**。
- **member**：`WxMiniClient.code2Session` → openid 落 `member_social_user`；**缺「按 userId 反查 openid」方法**。
- **框架**：启动类 `@SpringBootApplication(scanBasePackages="com.zhush")` → pay 只要上 classpath 即被扫描，**无需做成 starter**；SM4 加密基座已具备（`EncryptTypeHandler` + `zhush.encrypt.enabled/sm4-key`）。
- **SDK 已就位**：本地仓库 `D:\devtools\repository\...\weixin-java-pay\4.8.0` 存在（另有 4.5.0）。

## 三、统一支付能力架构

### 3.1 分层

```
业务层        union 提现 / 未来订单收款 / 退款 …
                │  只依赖统一门面，不感知 V2/V3/微信
────────────────┼──────────────────────────────────────────────
统一门面层     PayGateway / PayoutGateway / PaymentGateway（协议无关）
              ├─ 统一命令与结果模型（PayoutCommand / PayoutResult / …）
              ├─ 统一状态（PayStatus）与统一错误码（PayErrorCode）
              ├─ 渠道路由（按 PayChannelType 选实现）
              └─ 统一回调分发（PayNotifyHandler 扩展点）
────────────────┼──────────────────────────────────────────────
渠道 SPI 层    PayoutChannel / PaymentChannel（接口）
────────────────┼──────────────────────────────────────────────
协议适配层     WxPayV3Adapter（商家转账/JSAPI/退款）
              WxPayV2Adapter（企业付款/统一下单/退款/对账单）
              AlipayAdapter（预留）
────────────────┼──────────────────────────────────────────────
SDK            weixin-java-pay 4.8.0（WxPayService + EntPayService + MerchantTransferService）
```

### 3.2 依赖方向（严格单向）

```
zhush-module-union ──▶ zhush-module-pay ──▶ zhush-common + weixin-java-pay
zhush-server       ──▶ zhush-module-pay
```

- pay **不反向依赖** member/union；收款方标识（openid/卡号）由业务解析后传入统一命令；
- 资金状态机与流水**留在业务侧**，pay 只返回「渠道受理结果 + 渠道单号 + 渠道状态」。

### 3.3 统一模型与状态

| 模型 | 说明 |
|---|---|
| `PayChannelType` | `WECHAT_V2` / `WECHAT_V3` / `ALIPAY`（与 `withdrawType` 的 1/2/3 做**显式映射**，不混用） |
| `PayoutCommand` | 统一出款入参：`bizNo`（业务单号，**渠道幂等键**）、`amount`（分）、`channelType`、`payee`（openid / 卡号+姓名+行别）、`remark`、`notifyUrl`、`sceneReport` |
| `PayoutResult` | 统一出款结果：`accepted`、`finalState`、`channelOrderNo`、`channelState`、`failReason`、`errCode`、`rawResponse`（脱敏） |
| `PayStatus` | 统一状态：`ACCEPTED` / `PROCESSING` / `SUCCESS` / `FAIL` / `CANCELING` / `CANCELLED` / `WAIT_USER_CONFIRM` |
| `PaymentCommand/Result`、`RefundCommand/Result` | 收款/退款**预留**（一期仅定义接口，不实现） |
| `PayErrorCode` | 模块错误码，仿 `UnionErrorCode` 分段（模块号 `004`，`1004xxx`/`2004xxx`） |

> **红线**：`PayoutResult.accepted`（渠道已受理）与 `finalState`（到账终态）**必须分离**；**严禁**把同步返回的成功判定为资金到账。

### 3.4 渠道 SPI

```java
public interface PayoutChannel {
    PayChannelType channelType();                 // 唯一标识
    PayoutResult payout(PayoutCommand command);   // 出款（幂等：同一 bizNo）
    PayoutResult query(String bizNo);             // 查单（V2 无回调时唯一收敛手段）
    PayoutResult cancel(String bizNo);            // 撤单（仅部分渠道支持）
    boolean supportCancel();                      // 是否支持撤单
}
```

- 实现类注册为 Spring Bean，门面用 `List<PayoutChannel>` 自动聚合，按 `channelType()` 路由；
- **撤销红线**：仅当渠道明确返回 `CANCELLED` 才执行解冻/回滚，`CANCELING` 不得视为终态。

### 3.5 回调统一分发

- pay 提供**协议无关**的解析入口：`PayNotifyParser`（入参：原始报文 + 请求头），输出统一 `PayNotifyMessage`；
- 业务侧实现 `PayNotifyHandler`（`supports(bizType)` + `handle(PayNotifyMessage)`），由统一分发器按业务类型路由 → **新增业务无需改 pay**；
- 控制器由**业务侧**实现（或由 pay 提供抽象基类），pay 保持"无 web"以降低耦合。

### 3.6 多商户与凭证路由

SDK 原生支持多配置：`WxPayService.addConfig(mchId, config)` / `switchoverTo(mchId)` / `setMultiConfig(map, default)`，配合 `WxPayConfigHolder`。
方案：统一门面按 `PayoutCommand` 中的商户标识（默认取 primary）路由，为未来「多商户号/服务商模式」预留，**一期只启用单商户**。

## 四、微信 V2 / V3 双协议支持

### 4.1 能力对照矩阵

| 维度 | 微信支付 **V2** | 微信支付 **V3** |
|---|---|---|
| 报文格式 | XML | JSON |
| 签名 | MD5 / HMAC-SHA256（APIv2 密钥） | SHA256-RSA（商户私钥 `apiclient_key.pem`） |
| 传输加密 | 依赖 HTTPS + PKCS12 双向 TLS | AES-256-GCM（APIv3 密钥） |
| 平台证书 | PKCS12 商户证书（密码=商户号） | 平台证书 / 微信支付公钥（`certSerialNo`、`publicKeyId`） |
| 回调格式 | XML，`parseOrderNotifyResult` 验签 | JSON + `SignatureHeader`，`parseOrderNotifyV3Result` 验签解密 |
| 回调应答 | `WxPayNotifyResponse`（XML） | `WxPayNotifyV3Response`（JSON） |
| 出款（零钱） | `EntPayService.entPay`（企业付款到零钱，**已逐步下线**） | `bean.transfer.TransferBills*`（商家转账，**主推**） |
| 出款（银行卡） | `EntPayService.payBank`（**仅 V2 提供**） | **无** |
| 收款下单 | `unifiedOrder` / `createOrder` | `unifiedOrderV3` / `createOrderV3` |
| 退款 | `refund` / `refundQuery` | `refundV3` / `refundQueryV3` |
| 对账单 | `downloadRawBill` | `getV3` + 账单下载接口 |
| 卡号加密 | `getPublicKey()` 取 RSA 公钥加密 | 不适用 |

### 4.2 V3 适配（一期）

- 出款：`WxTransferClient` → `TransferBillsRequest/Result/GetResult/CancelResult`（`/v3/fund-app/mch-transfer/transfer-bills`），支持 `transfer/query/cancel`；
- 回调：`TransferBillsNotifyResult`（或通用 `WxPayNotifyV3Result` + `SignatureHeader`）；
- 凭证：`apiV3Key` + `privateKeyPath`/`privateKeyContent` + `certSerialNo` + 平台证书；
- 备用：`MerchantTransferService`（旧商家转账到零钱批次），一期**不启用**，仅登记为可选。

### 4.3 V2 适配（二期）

- 出款到银行卡：`WxBankPayClient` → `EntPayService.payBank(EntPayBankRequest)` / `queryPayBank(String)`；
  - **无回调**：终态**只能**靠 `queryPayBank` 主动查单（`SUCCESS`/`FAIL`/`PROCESSING`）收敛；
  - 卡号/姓名由 SDK 自动 RSA 加密（`getPublicKey()`），业务传明文即可；
- 凭证：`mchKey`（APIv2 密钥）+ PKCS12 证书（`setKeyContent`，密码=商户号）+ 企业付款密钥 `entPayKey`；
- 出款到零钱：`EntPayService.entPay`（历史产品，**不建议新接入**，如需兼容可登记）。

### 4.4 SDK 映射表（4.8.0，已核对本地 jar）

| 能力 | SDK 入口 | 关键类 |
|---|---|---|
| 资金出口（V3 商家转账） | `WxPayService` → 直连 V3 | `bean.transfer.TransferBillsRequest/Result/GetResult/CancelResult/NotifyResult` |
| 商家转账批次（V3） | `getMerchantTransferService()` | `createTransfer` / `queryWxBatches` / `queryWxDetails` / 电子回单 |
| 企业付款到银行卡（V2） | `getEntPayService()` | `payBank` / `queryPayBank` / `getPublicKey` |
| 企业付款到零钱（V2） | `getEntPayService()` | `entPay` / `queryEntPay` |
| 统一下单 | `unifiedOrder`(V2) / `unifiedOrderV3`(V3) | — |
| 退款 | `refund`(V2) / `refundV3`(V3) | — |
| 回调解析 | `parseOrderNotifyResult`(V2) / `parseOrderNotifyV3Result`(V3) | `WxPayOrderNotifyResult` / `WxPayNotifyV3Result` + `SignatureHeader` |
| 多商户 | `addConfig` / `switchoverTo` / `setMultiConfig` | `WxPayConfig`、`WxPayConfigHolder` |

### 4.5 选型规则（写入 `PayChannelType` 与业务字典）

| 场景 | 选用 | 理由 |
|---|---|---|
| 提现到微信零钱 | **V3 商家转账** | 官方主推，有回调 + 可撤销 |
| 提现到银行卡 | **V2 企业付款到银行卡** | V3 无此能力（唯一途径） |
| 未来收款/退款 | 优先 **V3** | 新能力仅在 V3 |

## 五、配置设计

### 5.1 V3 凭证（`zhush.pay.wx.v3.*`）

```yaml
zhush:
  pay:
    wx:
      enabled: true
      primary-mch-id: ${WX_PAY_MCH_ID:}            # 默认商户号
      notify-url: ${WX_PAY_NOTIFY_URL:}            # 统一回调前缀，如 https://<域名>/api/pay/notify
      v3:
        enabled: true
        app-id: ${WX_PAY_V3_APP_ID:}
        mch-id: ${WX_PAY_MCH_ID:}
        api-v3-key: ${WX_PAY_API_V3_KEY:}                        # 回调解密
        cert-serial-no: ${WX_PAY_V3_CERT_SERIAL_NO:}             # 商户证书序列号
        private-key-path: ${WX_PAY_V3_PRIVATE_KEY_PATH:file:/data/certs/apiclient_key.pem}
        public-key-id: ${WX_PAY_V3_PUBLIC_KEY_ID:}               # 微信支付公钥 ID（公钥模式）
        public-key-path: ${WX_PAY_V3_PUBLIC_KEY_PATH:file:/data/certs/pub_key.pem}
        transfer-scene-id: ${WX_PAY_V3_TRANSFER_SCENE_ID:}
        transfer-remark: ${WX_PAY_V3_TRANSFER_REMARK:佣金提现}
        user-recv-perception: ${WX_PAY_V3_USER_RECV_PERCEPTION:}
        receipt-authorization-mode: ${WX_PAY_V3_RECEIPT_AUTH_MODE:}
        transfer-scene-report-infos:
          - info-type: ${WX_PAY_V3_SCENE_INFO_TYPE:}
            info-content: ${WX_PAY_V3_SCENE_INFO_CONTENT:}
```

### 5.2 V2 凭证（`zhush.pay.wx.v2.*`）

```yaml
      v2:
        enabled: true
        app-id: ${WX_PAY_V2_APP_ID:}
        mch-id: ${WX_PAY_MCH_ID:}
        mch-key: ${WX_PAY_V2_MCH_KEY:}                           # APIv2 密钥（签名）
        ent-pay-key: ${WX_PAY_V2_ENT_PAY_KEY:}                   # 企业付款专用密钥（可与 mchKey 同值）
        key-path: ${WX_PAY_V2_PKCS12_PATH:file:/data/certs/apiclient_cert.p12}  # PKCS12 双向 TLS
        key-password: ${WX_PAY_MCH_ID:}                          # 注意：PKCS12 密码 = 商户号
        sign-type: HMAC-SHA256                                   # 或 MD5
        use-sandbox-env: false
```

### 5.3 渠道级限额（统一抽象）

```yaml
      limit:
        wechat-v3: { min-amount: 1.00, max-amount: 50000.00 }
        wechat-v2: { min-amount: 1.00, max-amount: 50000.00 }
```

### 5.4 配置纪律

1. **凭证全部外置**：仅环境变量占位符；证书走容器挂载卷（`file:/data/certs/`），禁止打包进 jar；可叠加 jasypt。
2. **生产 fail-fast**：`enabled=true` 时校验必需项（V3：`apiV3Key`/私钥/`certSerialNo`；V2：`mchKey`/PKCS12），缺失**启动即失败**，禁止静默降级。
3. **条件装配**：`v2.enabled` / `v3.enabled` 各自独立开关（`@ConditionalOnProperty`），未启用的协议不装配 Bean。
4. **多环境**：`dev/test/prod/docker` 四份 yaml 各自补全；docker 需新增证书挂载卷。

## 六、模块骨架

```
zhush-module-pay/
├── pom.xml                                        # zhush-common + weixin-java-pay（保持）；测试补 starter-test
└── src/main/java/com/zhush/pay/
    ├── core/                                      # 【新增】协议无关层
    │   ├── PayGateway.java                        # 统一门面（按渠道路由）
    │   ├── channel/PayoutChannel.java             # 出款 SPI
    │   ├── channel/PaymentChannel.java            # 收款 SPI（预留）
    │   ├── model/PayoutCommand.java / PayoutResult.java
    │   ├── model/PaymentCommand.java / PaymentResult.java   # 预留
    │   ├── model/RefundCommand.java / RefundResult.java     # 预留
    │   ├── model/PayChannelType.java / PayStatus.java
    │   └── notify/PayNotifyParser.java / PayNotifyMessage.java / PayNotifyHandler.java
    ├── wx/                                        # 微信双协议适配
    │   ├── WxPayProperties.java                   # 前缀 zhush.pay.wx，含 v2/v3/limit 子配置
    │   ├── WxPayConfiguration.java                # 装配 + fail-fast 校验
    │   ├── v3/
    │   │   ├── WxTransferClient.java              # 商家转账 transfer/query/cancel
    │   │   ├── WxTransferNotifyParser.java        # V3 验签解密
    │   │   ├── WxV3PayoutChannel.java             # 实现 PayoutChannel
    │   │   └── model/（WxTransferRequest/Result/Notify/State）
    │   ├── v2/
    │   │   ├── WxBankPayClient.java               # 企业付款到银行卡 payBank/queryPayBank
    │   │   ├── WxV2PayoutChannel.java             # 实现 PayoutChannel（无回调，靠查单）
    │   │   └── model/（WxBankPayRequest）
    │   └── exception/WxPayExceptionTranslator.java # 渠道异常 → PayErrorCode
    ├── alipay/                                    # 【预留】空包，三期填充
    └── enums/PayErrorCode.java
```

## 七、核心链路

### 7.1 出款两阶段与幂等

| 步骤 | 事务 | 说明 |
|---|---|---|
| ① 受理 | **事务内** | 校验业务状态/限额/收款方；落「处理中」+ `bizNo`（= 业务单号，渠道幂等键） |
| ② 调用渠道 | **事务外** | `PayoutChannel.payout(cmd)`；**禁止**在事务内调外部接口 |
| ③ 记录结果 | **事务内** | `accepted=true` → 记 `channelOrderNo`，保持处理中；`accepted=false` → 置失败 + `failReason` + `retryCount++` |
| ④ 终态收敛 | 回调 / 定时查单 | 见 7.2、7.3 |
| ⑤ 落账 | **事务内** | 见 7.5 |

**幂等**：`bizNo` 固定复用（重试/并发同值）+ `@Idempotent` 或 Redis 锁按 `bizNo` 加锁 + 渠道侧幂等。

### 7.2 V2 无回调 → 主动查单（银行卡关键差异）

V2 企业付款到银行卡**不推送结果通知**，必须：

1. 调用后立即置「处理中」；
2. 由定时任务按 `bizNo` 调 `queryPayBank`，按返回 `SUCCESS`/`FAIL`/`PROCESSING` 收敛（`PROCESSING` 保持处理中，继续下轮）；
3. **禁止**把 `payBank` 同步返回当作终态。

### 7.3 V3 回调 + 查单（双保险）

- 回调：强制验签 + 解密；`@IgnoreAuth` 且加入 Sa-Token 白名单；**处理失败必须返回 HTTP 500** 触发微信重试；按 `bizNo + channelOrderNo` 幂等；校验 `mchId` 与配置一致；
- 查单：定时任务兜底收敛「已受理但回调丢失」的单据；
- 二者靠**状态 CAS**（`update ... where status=处理中`）保证只生效一次。

### 7.4 撤单与解冻红线

- 支持条件：V3 商家转账在 `WAIT_USER_CONFIRM`（用户未确认）等状态可 `cancel`；V2 银行卡不支持撤单（`supportCancel()=false`）；
- **红线**：仅当渠道明确返回 `CANCELLED` 才执行解冻/回滚，`CANCELING` 视为非终态。

### 7.5 资金流水与余额口径

- 终态**成功**后写 `union_account_transaction`：`transactionType=2`（提现扣减）、`status=1`、金额取负、`sourceId=bizNo`；
- **余额扣减时机（决策点）**：建议**受理时冻结**、终态成功转扣减、失败/取消解冻；若沿用现有表结构（无冻结字段），需明确"终态扣减"并接受中间态未扣。

### 7.6 敏感信息加密（已具备基座）

- 银行卡号、姓名等落库**必须 SM4 密文**：使用 `zhush.mybatis.core.handler.EncryptTypeHandler`（`@TableField(typeHandler=...)`），展示列脱敏；
- 对应字段建议 `pay_account_cipher`、`pay_account_name_cipher`；
- 日志**禁止**打印卡号/姓名/密钥明文。

### 7.7 对账与差异处置（二期）

- 一期为**本地自校准**（回调 + 查单收敛）；
- 二期：拉取微信对账单（V2 `downloadRawBill` / V3 账单接口）与本地流水核对 → 差异落表 + **管理端处置接口** + 告警（补齐记忆中的 PAY 清单缺口）。

## 八、为后续业务打基础

### 8.1 接入范式（业务侧只需三步）

1. **定义业务单号**：`bizNo` 保证全局唯一且可复用于重试（如 `applyNo`）；
2. **组装统一命令**：`PayoutCommand.builder().bizNo(...).amount(...).channelType(WECHAT_V3).payee(...)`；
3. **调用统一门面**：`payGateway.payout(cmd)` → 拿 `PayoutResult`；终态由回调/查单驱动。

新增业务（如订单分润出款、会员提现）**无需**改 pay；新增渠道（支付宝）只需实现 `PayoutChannel`。

### 8.2 扩展检查清单

- [ ] 新增渠道 → 实现 `PayoutChannel` + 声明 `channelType()`；
- [ ] 新增协议版本 → 在 `wx/v2`、`wx/v3` 下加适配器，**不改 core**；
- [ ] 新增回调业务 → 实现 `PayNotifyHandler`，**不改 pay 分发器**；
- [ ] 新增业务场景 → 复用统一模型，**不新增平行抽象**。

## 九、安全与合规

| 风险 | 缓解措施 |
|---|---|
| 密钥/证书泄露 | 全部外置 + 挂载卷；生产 fail-fast；禁止入库/入代码 |
| 伪造回调 | 强制验签 + 解密；校验 `mchId`；失败返回 500；V3 校验 `SignatureHeader` |
| 重复打款 | `bizNo` 渠道幂等 + Redis 锁 + 状态 CAS |
| 越权/篡改收款方 | openid / 卡号**服务端反查**，不信任前端；SM4 密文落库 |
| 金额越界 | 服务端 `min/max` + **单日累计限额** |
| 生产误成功 | **无渠道实现时严禁返回成功/accepted**（红线），杜绝 Mock 兜底 |
| 误解冻资损 | 撤销仅 `CANCELLED` 才解冻（红线） |
| 敏感信息展示 | 卡号/姓名脱敏；日志脱敏 |
| 事务与外部调用耦合 | 渠道调用移出事务，先落中间态再收敛 |
| 定时任务并发 | Redis 锁 / `@Idempotent` 保证单次执行 |

## 十、构建与接入改造清单

| # | 文件 | 改动 |
|---|---|---|
| 1 | `zhush-module-pay/src/main/java/com/zhush/pay/**` | **重建** core（统一层）+ wx（v2/v3 适配）+ `PayErrorCode` |
| 2 | `zhush-module-pay/pom.xml` | 保持依赖；补 `starter-test`(test) |
| 3 | `zhush-module-union/pom.xml` | **新增** `zhush-module-pay` |
| 4 | `zhush-server/pom.xml` | **新增** `zhush-module-pay`（否则不参与运行） |
| 5 | `application-{dev,test,prod,docker}.yaml` | 新增 `zhush.pay.wx.*`；回调路径入 `ignore-paths` |
| 6 | `deploy/backend/docker-compose.yaml` | 新增证书挂载卷（`./certs:/data/certs`）+ `WX_PAY_*` 环境变量 |
| 7 | union service/controller | 新增出款编排（`WithdrawPayoutService`）、回调控制器、定时查单任务、状态机扩展 |
| 8 | union entity/query/vo/param | 视决策新增 `pay_openid`、SM4 密文字段、`status` 中间态（同步 VO 与前端字典） |
| 9 | member mapper | 补「按 userId + socialType 查 social_user」 |
| 10 | `sql/` | 新增字段 DDL；渠道/限额配置种子数据 |

## 十一、分期实施计划

### 一期：统一抽象 + V3 出款

1. 重建 `com.zhush.pay.core`（门面/SPI/统一模型/状态/错误码/回调扩展点）；
2. 重建 `com.zhush.pay.wx` V3 部分（`WxPayProperties`/`WxPayConfiguration`/`WxTransferClient`/`WxTransferNotifyParser`）+ `WxV3PayoutChannel`；
3. 打通依赖（union→pay、server→pay）+ 四环境配置（仅启用 V3）；
4. union：出款编排 + 回调控制器 + 定时查单；
5. member：openid 反查；union：`pay_openid` 字段；
6. 单测（Mock 微信）+ 真实商户号小额联调。

### 二期：V2 出款 + 对账

1. `wx/v2`：`WxBankPayClient` + `WxV2PayoutChannel`（**查单收敛**，无回调）；
2. 银行卡号 SM4 密文字段落库与脱敏展示；
3. 对账拉单 + 差异处置接口 + 告警；
4. 多渠道路由与限额（含单日累计）。

### 三期（按需）：收款 + 退款 + 支付宝

V2 统一下单 / V3 JSAPI、退款、支付宝渠道实现（替换现有 Mock 兜底）。

## 十二、验收标准

- [ ] `PayGateway` 可编译、可通过 `channelType` 路由到正确渠道实现；未启用协议不装配 Bean。
- [ ] 配置缺失时**启动失败**且报错明确（V2/V3 分别校验）。
- [ ] 同一 `bizNo` 重复出款**只产生一笔**渠道账。
- [ ] V3：`accepted=true` 落「处理中」，回调/查单收敛终态；`accepted=false` 落失败。
- [ ] V2 银行卡：**无回调**场景下由查单正确收敛（`SUCCESS`/`FAIL`/`PROCESSING`）。
- [ ] 回调：伪造签名被拒；成功后状态正确收敛；**处理失败返回 HTTP 500**。
- [ ] 撤单：非 `CANCELLED` 不解冻。
- [ ] 卡号/姓名以 SM4 密文落库，列表脱敏，日志无明文。
- [ ] 无渠道实现时**不返回** accepted/成功。
- [ ] 日志无 `apiV3Key`/`mchKey`/私钥明文。

## 十三、风险与待确认项

| # | 待确认项 | 建议 |
|---|---|---|
| 1 | **源码缺失**：`com.zhush.pay` 是否另有备份/分支？ | 有则恢复；无则按 2.2 契约 + 本方案重建 |
| 2 | **统一抽象粒度**：出款足矣，还是本期即建 `PaymentGateway`（收款）空实现？ | 建议**只建接口骨架**（预留），一期实现出款 |
| 3 | **`PayChannelType` 与 `withdrawType` 映射**：是否需要扩展 `withdrawType` 区分微信 V2/V3？ | 建议用 `pay_channel` 字段表达协议，`withdrawType` 仅表业务方式 |
| 4 | **V3 产品选型**：用 `transfer-bills`（新商家转账）还是 `MerchantTransferService`（批次）？ | 建议 `transfer-bills`（对齐历史产物与既有记忆） |
| 5 | **商户资质**：是否已开通「商家转账」（V3）与「企业付款到银行卡」（V2）产品权限/场景 ID？ | 需商户平台确认，否则调用必失败 |
| 6 | **提现状态机**：是否新增 `status=7 打款处理中`？ | 建议新增（同步前端字典与 VO） |
| 7 | **余额口径**：受理冻结 vs 终态扣减 | 建议「受理冻结 + 终态落账」，需评估是否加冻结字段/流水类型 |
| 8 | **V2 企业付款到零钱**是否保留（历史产品，逐步下线） | 建议不新接入，仅登记兼容位 |
| 9 | **回调控制器归属** | 业务侧实现（pay 保持无 web）；多业务涌现时再升级 pay 抽象基类 |
| 10 | **对账触发**：`starter-job` 轮询 vs `starter-mq` 延迟消息 | 一期 job 轮询（简单可靠），量大再引 MQ |
| 11 | **多商户**是否规划（服务商/多商户号） | 一期单商户，配置与门面预留 `mchId` 路由位 |

---

> 下一步：待上表确认项（尤其 #1 源码、#2 抽象粒度、#5 商户资质、#6 状态机、#7 余额口径）明确后，按「十一、分期实施计划」一期清单进入编码。
