# zhush-module-pay 支付能力分析与退差价封装设计

> 面向对象：支付底座维护者、后续对接业务的后端同学
> 关联文档：[zhush-module-pay微信支付整合方案.md](./zhush-module-pay微信支付整合方案.md)
> 结论先行：**抽象层设计到位、资金安全红线守得住；但"同时支持微信 V2 / V3"目前只落地了 V3，V2 是空壳；退差价能力 V3 已具备但缺封装约定与业务消费方。**

---

## 一、模块定位

`zhush-module-pay` 的核心目的：**为未来各类业务（收款、退款、分润出款、商家转账…）提供"快速对接、协议无关"的支付解决方案**。业务侧只依赖统一门面 `PayGateway`，不感知微信 V2/V3、XML/JSON、签名算法等差异；新增业务不改 pay，新增渠道只实现 SPI。

据此，支付能力被拆成三类动作 × 两个协议版本：

| 动作 | 说明 | 门面方法 |
|---|---|---|
| **统一支付（付款/收款）** | 用户向商户付款（JSAPI/Native/H5/APP…） | `pay` / `queryPayment` |
| **退款** | 原路退回，支持全额与**部分/退差价** | `refund` / `queryRefund` |
| **商家转账（出款）** | 商户向用户付款（提现、佣金…） | `payout` / `queryPayout` / `cancelPayout` |

要求：**三类动作均需支持微信支付 V2 与 V3。**

---

## 二、能力矩阵（现状，基于 master 代码核实）

| 能力 | V3 | V2 | 证据 |
|---|:--:|:--:|---|
| 商家转账 / 出款 | ✅ 已实现 | ❌ **无实现** | `WxV3PayoutChannel`（transfer-bills）；V2 无任何渠道类 |
| 统一支付 / 收款 | ⚠️ 仅 **JSAPI** | ❌ **无实现** | `WxV3PaymentChannel.pay`（JSAPI）；无 Native/H5/APP；V2 无 |
| 退款（含退差价） | ✅ 能力具备（缺封装约定） | ❌ **无实现** | `WxV3PaymentChannel.refund`（`refund`/`total` 分离，天然支持部分退款）；V2 无 |
| 支付/退款回调 | ✅ 解析器具备，**无业务消费方** | ❌ | `WxV3PaymentNotifyParser`/`WxV3RefundNotifyParser`；业务侧仅 `UnionPayNotifyHandler` 处理 TRANSFER |
| 渠道路由 | `PayChannelType.WECHAT_V3` → 有 Bean | `WECHAT_V2` → **路由即抛 `PAY_CHANNEL_UNSUPPORTED`** | `DefaultPayGateway` |

> **关键偏差**：用户明确要求"支持 V2 和 V3"，但仓库现状 **V2 仅停留在枚举 `PayChannelType.WECHAT_V2` 与配置 `WxPayProperties.V2`/`validateV2()`**，没有任何 V2 客户端/渠道/回调实现。曾出现在 `WxPayConfiguration` 的 V2 引用指向的 `com.zhush.pay.wx.v2.*` 源码在 master 上根本不存在（见第七节分支问题），已按决策移除失效引用。

---

## 三、架构回顾（可取之处，分析基线）

```
业务(union/member/…) 
   │  仅依赖 PayGateway（门面）
   ▼
core：PayGateway / DefaultPayGateway（按 channelType 路由）
   ├── PayoutChannel  SPI（出款）      ← WxV3PayoutChannel
   ├── PaymentChannel SPI（收款/退款）  ← WxV3PaymentChannel
   └── notify：PayNotifyDispatcher 按 (channelType, PayNotifyType) 复合键路由
   ▼
wx/v3：WxTransferClient / WxPaymentClient / *NotifyParser / *StateMapper
   ▼
weixin-java-pay SDK（4.8.0）
```

设计正确点（应保留）：

1. **门面 + SPI 双抽象**：出款、收款/退款分别有 `PayoutChannel`/`PaymentChannel`，Spring 聚合 `List<PayoutChannel>`、`List<PaymentChannel>` 自动入路由表，未启用协议不装配 Bean。
2. **资金安全红线**：`accepted`（受理）与 `finalState`（终态）分离；下单/转账同步返回**只映射受理/PROCESSING，绝不判成功到账**；未知态一律保守 `PROCESSING`（`WxPayTradeStateMapper`、`WxTransferStateMapper`）；渠道异常一律 `accepted=false`，不伪装成功。
3. **回调撞键已解决**：微信 V3 转账/支付/退款通知同属 `WECHAT_V3`，用 `PayNotifyType(TRANSFER/PAYMENT/REFUND)` 与 `channelType` 组成复合键，`PayNotifyDispatcher` 二维路由。
4. **异步终态靠查单**：退款、商家转账均提供 `queryRefund`/`queryPayout`，符合"异步收敛"要求。
5. **异常脱敏**：`WxPayExceptionTranslator.describe` 只提取可读原因，不落密钥/证书/密文。
6. **pay 无 DB/无 web**：保持纯能力层，状态持久化归业务侧，符合"底座"定位。

---

## 四、问题清单（按优先级）

### P0 —— 违反核心要求，必须补齐

- **P0-1 V2 全量缺实现。** 出款/收款/退款三类动作均无 V2 渠道。需新增：
  - `WxV2PayoutChannel`（企业付款到银行卡/代发，`entPay`/`payBank`，**无回调、靠查单**）；
  - `WxV2PaymentChannel`（统一下单 `unifiedorder`，至少覆盖 JSAPI/Native）+ V2 退款 `/secapi/pay/refund`；
  - V2 回调/查单解析（V2 用 MD5/HMAC-SHA256 + XML，与 V3 完全不同）。
  - 依赖前置：**V2 退款/企业付款需 PKCS12 商户证书做双向 TLS**，`WxPayProperties.V2.keyPath` 已预留但无人消费。
- **P0-2 V2 与 union 业务代码的分支完整性冲突（详见第七节）。** 不解决则整个 reactor 无法编译，V2/退款都无从验证。

### P1 —— 能力不完整，尽快补

- **P1-1 收款支付方式只有 JSAPI。** "统一支付"若含 PC 扫码（Native）、H5、APP，当前 `WxV3PaymentChannel` 不支持。`payParams` 契约也只覆盖 JSAPI。
- **P1-2 退差价缺少封装与约定（见第五节）**：能力可用，但没有"部分/多次退款"的语义封装、单位换算约定与前置校验说明。
- **P1-3 收款/退款回调无业务消费方、无回调端点**：`UnionPayNotifyController` 仅有 `/wechat/transfer`，缺 `/wechat/pay`、`/wechat/refund`；`UnionPayNotifyHandler.supports` 只放行 `TRANSFER`。收款业务落地时必须补对应 handler + 端点，否则钱到账无人收敛。

### P2 —— 一致性/健壮性

- **P2-1 限额校验只在出款侧**（`WxV3PayoutChannel.validateLimit`）。收款/退款单笔限额缺失（`WxPayProperties.Limit` 已有配置位）。
- **P2-2 多商户/多 AppID 路由未实现**：`primaryMchId` 预留，但全链路单 `WxPayService` 实例；多渠道主体需扩展（当前非必需，登记）。
- **P2-3 重复 `pay` 同 `bizNo` 语义**：V3 以 `out_trade_no` 幂等，同单号改金额会被微信拒绝；需在集成文档明确"改价须换新单号或先撤销"。
- **P2-4 状态映射可读性**：`toPayStatus("REFUND") → SUCCESS`（站在"支付"视角正确），但需文档标注，避免业务把"已退款"误解为"可再次全额退"。

---

## 五、退差价（部分退款）封装设计 【重点】

### 5.1 业务场景

用户实付 **286.00 元**，商家降价后需**退差价 86.00 元**（原订单不整单退，只退差额）。要求：
- 同一原支付单，退**部分**金额；
- 允许多次退差价，**累计退款额 ≤ 原单实付额**；
- 每次退款有独立可追溯的单号与状态。

### 5.2 现状能力评估（好消息）

微信 V3 退款 API 的 `amount` 同时携带 `refund`（本次退款额）与 `total`（**原单总额**），二者分离——这正是部分退款/退差价的底层支撑。当前实现已具备：

`WxV3PaymentChannel.refund(RefundCommand)` 关键逻辑：
```java
// 先查原单，拿到 transactionId 与原单总额 total
WxPayOrderQueryV3Result origin = client.queryOrder(command.getOriginBizNo());
assertTrue(origin.getTransactionId() != null && origin.getAmount() != null,
        PayErrorCode.PAY_ORIGINAL_ORDER_MISSING, command.getOriginBizNo());

WxPayRefundV3Request request = new WxPayRefundV3Request();
request.setOutTradeNo(command.getOriginBizNo());   // 原支付单
request.setOutRefundNo(command.getBizNo());        // 本次退款单（每次唯一）
request.setAmount(new WxPayRefundV3Request.Amount()
        .setRefund(command.getAmount())            // 8600 分 = 86.00（本次退差价）
        .setTotal(origin.getAmount().getTotal())   // 28600 分 = 286.00（原单总额，现场查得）
        .setCurrency("CNY"));
```

**结论：`refund` 与 `total` 分离即天然支持退差价，V3 链路无需改造即可退 86.00。** 缺的是"用法约定 + 防错封装"。

### 5.3 调用参数约定（业务侧）

| 字段 | 值（示例） | 说明 |
|---|---|---|
| `RefundCommand.originBizNo` | 原支付业务单号（当初 `pay.bizNo`） | 用于查原单 + 作为渠道 `out_trade_no` |
| `RefundCommand.bizNo` | `RF` + 日期 + 序号（**每次唯一**） | 渠道 `out_refund_no`；多次退差价必须不同，微信按此幂等 |
| `RefundCommand.amount` | `8600` | 本次退款额，**单位：分**（86.00 元） |
| `RefundCommand.reason` | "价格调整退差价" | 用户可见 |
| `channelType` | `WECHAT_V3` | 与原支付同协议 |

> 原单总额 `total` 不需业务传，底座查单获取；业务只需保证 `bizNo` 唯一、`amount` 为分。

### 5.4 需要补的"封装"（落地建议，二选一或组合）

1. **门面语义糖（推荐）**：在 `PayGateway` 增加
   ```java
   /** 退差价/部分退款：对同一原单退指定金额，幂等以 refundBizNo 为键 */
   RefundResult refundDifference(String originBizNo, String refundBizNo, long amountFen, String reason);
   ```
   内部组装 `RefundCommand` 复用 `refund`，避免业务每次手写查单细节。
2. **前置累计校验（业务侧持有账本）**：pay 无 DB，符合底座定位。业务侧应记录"原单已退累计额"，发起退差价前校验
   `已退累计 + 本次 ≤ 原单实付`，超限直接拒绝（微信也会拒，但本地拦截更快、可给友好提示、防重复提交）。
3. **金额单位工具**：统一提供 `元 ↔ 分` 转换（如 `MoneyUtil.yuanToFen(BigDecimal)`），杜绝 `286.00` 被误传为 `286` 或 `28600.0`。当前全链路 `Integer`（分），业务侧务必先转分。
4. **异步收敛**：退款受理仅代表渠道接受，`RefundResult.finalState` 多为 `PROCESSING`；终态必须靠 `queryRefund(refundBizNo)` 或退款回调（`WxV3RefundNotifyParser` → 业务 handler）收敛。**退差价成功与否以收敛后的 `SUCCESS` 为准。**

### 5.5 完整时序（286 → 退差价 86）

```
① 收款：pay(bizNo=P001, amount=28600)  → PROCESSING（拿到 payParams 前端拉起）
② 用户支付成功 → 支付回调/查单收敛 P001 = SUCCESS（原单实付 28600，total 记录在案）
③ 降价退差价：refundDifference(originBizNo=P001, refundBizNo=RF001, amount=8600)
     底座内部：queryOrder(P001) 得 total=28600 & transactionId → refundV3(refund=8600,total=28600)
     返回 PROCESSING
④ 退款回调/查单：queryRefund(RF001) → SUCCESS，业务落"已退 8600"
⑤ 若后续再退（例如又降 30）：refundBizNo 换新(RF002)，amount=3000，
   本地校验 已退8600 + 3000 ≤ 28600 → 通过后调用；累计仍走同一原单
```

---

## 六、V2 补齐方案（要点，供排期）

| 动作 | V2 接口（weixin-java-pay） | 回调/收敛 | 证书 |
|---|---|---|---|
| 出款（企业付款到银行卡/零钱） | `EntPayService`（`payBank`/`transfers`） | **无回调，靠查单** | PKCS12 双向 TLS |
| 统一支付 | `unifiedOrder`（`TRADE_TYPE`: JSAPI/NATIVE/H5/APP） | V2 支付通知（XML+MD5/HMAC） | 部分接口需证书 |
| 退款（含退差价） | `refund`（`total_fee`/`refund_fee` 分离，天然支持部分退款） | V2 退款通知（需证书） | **强制双向证书** |

- V2 与 V3 报文差异大（XML vs JSON、签名算法、回调格式），应在 `wx/v2` 下建 `WxV2PaymentChannel`/`WxV2PayoutChannel` + 各自 `*NotifyParser`，**不改 core**（符合"新增协议版本只加适配器"的扩展原则）。
- 金额单位：V2 用**分（int）**，与现有 `Integer amount`（分）一致，`total_fee=28600 / refund_fee=8600` 同样支持退差价。
- 配置：`WxPayProperties.V2`（`mchKey`/`keyPath`/`signType`）与 `validateV2()` 已就绪，实现落地后即可启用。

---

## 七、分支完整性问题（阻断验证，须先决策）

三期在 **master** 工作树推进时发现：master 存在**二期业务/协议源码缺失导致的编译不自洽**，且与 **union 分支为两套不同架构**：

- **pay**：`WxPayConfiguration` 曾引用 `com.zhush.pay.wx.v2.*`（master 上不存在）→ 已移除失效引用，**pay 模块现可编译、单测 24/24 通过**。
- **union**：master 跟踪的 `UnionWithdrawPayoutService`/`UnionWithdrawReconcileTask` 引用的 `UnionWithdrawLimitService`/`UnionWithdrawReconcileService` **仅存在于 union 分支** → **union 模块在 master 上编译不过**（非三期引入）。
- **架构分叉**：union 分支已重构为"扁平 `wx/` 包、无 `PayGateway` 抽象"的另一套设计，与本 master 抽象网关架构不兼容。

影响：无法对三期 V3 收款/退款做端到端验证；V2 补齐与退差价落地也都要求一个可编译的基线。

待决策（同前）：**(A)** 把二期业务类补齐/移植到 master，保持抽象网关架构并继续在其上补 V2；**(B)** 改以 union 为业务基线，三期/退差价/V2 按 union 架构重写；**(C)** 先只在 pay 层独立推进，union 分支问题另立任务。

---

## 八、落地优先级建议

1. **P0-2 先解**：定分支基线（A/B/C），恢复 reactor 可编译，否则一切验证悬空。
2. **P0-1**：在选定基线补 V2 出款/收款/退款（含退差价、证书装配、V2 回调解析）。
3. **P1-2 + P1-3**：退差价语义糖 `refundDifference` + 金额工具 + 回调端点/ handler（收款、退款业务消费方）。
4. **P1-1**：按需扩 Native/H5/APP 收款方式。
5. **P2**：限额一致性、幂等/改价约定文档化、多商户路由预留位。

### 验收补充（在整合方案原标准上新增）

- [ ] V2 出款无回调场景由查单正确收敛；V2 退款退差价 `total_fee/refund_fee` 正确。
- [ ] 同一原单可**多次退差价**，累计不超过原单实付；每次退款单号唯一、状态独立收敛。
- [ ] 金额全程"分"（`Integer`），提供元→分转换工具且单测覆盖。
- [ ] 收款、退款回调各有业务 handler 与端点，验签失败/串商户号被拒、处理失败返 HTTP 500。
- [ ] `PayGateway` 路由 `WECHAT_V2`/`WECHAT_V3` 均能命中实现（不再抛 unsupported）。

---

## 附录 A：退差价最小调用样例（V3，业务侧伪代码）

```java
// 元 → 分（务必用工具，避免精度/单位错误）
long paid   = MoneyUtil.yuanToFen(new BigDecimal("286.00")); // 28600（记录在原单）
long refund = MoneyUtil.yuanToFen(new BigDecimal("86.00"));  // 8600（本次差价）

// 业务侧账本校验：已退累计 + 本次 ≤ 原单实付
refundLedger.assertWithinPaid(originBizNo, refund);

RefundResult r = payGateway.refund(RefundCommand.builder()
        .channelType(PayChannelType.WECHAT_V3)
        .originBizNo(originBizNo)          // 原支付业务单号
        .bizNo("RF" + dateSeq())           // 本次退款单号（唯一）
        .amount((int) refund)              // 8600 分
        .reason("价格调整退差价")
        .build());
// r.accepted=true 且 r.finalState=PROCESSING：仅受理，终态待 queryRefund/回调收敛
```

## 附录 B：当前 pay 模块 V3 文件清单（便于对照）

`core`：`PayGateway`、`DefaultPayGateway`、`channel/PayoutChannel`、`channel/PaymentChannel`、`notify/{PayNotifyDispatcher,PayNotifyParser,PayNotifyHandler,PayNotifyMessage,PayNotifyType}`、`model/{PayChannelType,PayStatus,Payee,PaymentCommand,PaymentResult,RefundCommand,RefundResult,PayoutCommand,PayoutResult}`
`wx`：`WxPayProperties`、`WxPayConfiguration`、`exception/WxPayExceptionTranslator`
`wx/v3`：`WxTransferClient`、`WxV3PayoutChannel`、`WxTransferNotifyParser`、`WxTransferStateMapper`、`WxPaymentClient`、`WxV3PaymentChannel`、`WxV3PaymentNotifyParser`、`WxV3RefundNotifyParser`、`WxPayTradeStateMapper`
`wx/v2`：**（缺，待补）**
