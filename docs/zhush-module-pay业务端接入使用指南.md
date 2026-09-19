# zhush-module-pay 业务端接入使用指南

> 类型：使用文档 / 接入手册（面向业务模块开发者）
> 关联：[支付能力分析与退差价设计](./zhush-module-pay支付能力分析与退差价设计.md) · [V2 与退差价修复实现方案](./zhush-module-pay支付V2与退差价修复实现方案.md) · [微信支付整合方案](./zhush-module-pay微信支付整合方案.md)
> 目标：业务侧**只依赖 `PayGateway` 门面**即可完成收款、退款（含退差价）、商家转账（出款），不感知 V2/V3/微信协议差异。

---

## 一、能力总览

`zhush-module-pay` 是**协议无关的统一支付能力底座**（无 DB、无 Web 层），对业务暴露三类动作 × 双协议：

| 动作 | 门面方法 | V3（`WECHAT_V3`） | V2（`WECHAT_V2`） |
|---|---|---|---|
| 收款（下单） | `pay` / `queryPayment` | ✅ JSAPI | ✅ JSAPI |
| 退款 / **退差价** | `refund` / `refundDifference` / `queryRefund` | ✅ | ✅ `total_fee`/`refund_fee` 分离 |
| 出款（商家转账） | `payout` / `queryPayout` / `cancelPayout` | ✅ 商家转账（可撤单） | ✅ 企业付款到零钱/银行卡（**无回调、不可撤单**） |

> 渠道是否可用取决于运行环境是否启用对应协议（`zhush.pay.wx.v2.enabled` / `v3.enabled`）。未启用的渠道被调用会抛 `PAY_CHANNEL_UNSUPPORTED`，**绝不静默返回成功**。

---

## 二、引入依赖

业务模块 `pom.xml` 增加（版本由父 `dependencyManagement` 统一管理，无需写死）：

```xml
<dependency>
    <groupId>com.zhush</groupId>
    <artifactId>zhush-module-pay</artifactId>
</dependency>
```

`PayGateway`、`PayNotifyDispatcher` 均为 Spring `@Component`，被组件扫描后**直接注入即可**：

```java
private final PayGateway payGateway;   // 构造注入
```

---

## 三、必须牢记的资金安全红线

1. **「受理」≠「到账」**：`payout/pay/refund` 的同步返回只代表渠道**已受理**（`accepted=true`、`finalState` 多为 `ACCEPTED`/`PROCESSING`），**不能**据此记账为成功。终态必须靠 **回调** 或 **主动查单**（`queryPayout/queryPayment/queryRefund`）收敛。
2. **终态判定统一用 `PayStatus.isFinal()`**：仅 `SUCCESS`/`FAIL`/`CANCELLED` 为终态；其余（`ACCEPTED`/`PROCESSING`/`TRANSFERING`/`WAIT_USER_CONFIRM`/`CANCELING`）为中间态，**不得**解冻资金或判成功。
3. **查单返回 `accepted=false` = 结果未知**（网络/系统失败），此时**不得判失败**，应保持原状态并下次对账重试。
4. **幂等键 = `bizNo`**：下单/出款用业务单号；退款用退款业务单号（`out_refund_no`）。重试、并发必须复用同一单号。
5. **金额单位一律「分」（`Integer`）**：边界用 `MoneyUtil` 转换，禁止浮点。

---

## 四、收款（下单 + 拉起支付 + 收敛）

### 4.1 下单

```java
PaymentResult result = payGateway.pay(PaymentCommand.builder()
        .bizNo(orderNo)                              // 业务订单号（幂等键）
        .amount(MoneyUtil.yuanToFen(new BigDecimal("286.00"))) // 28600 分
        .channelType(PayChannelType.WECHAT_V3)       // 或 WECHAT_V2
        .payerId(openid)                             // JSAPI 付款人 openid
        .description("会员套餐")
        // .notifyUrl(...)  .appId(...)              // 不传则用渠道默认配置
        .build());

if (!result.isAccepted()) {
    // 下单明确失败（参数/渠道拒绝），可安全提示用户重试
    throw new ServiceException("下单失败：" + result.getFailReason());
}
// accepted=true：仅受理，finalState=PROCESSING，此时订单应置「待支付」，不能置「已支付」
String payParams = result.getPayParams();            // 前端拉起支付参数（JSON），透传给小程序/公众号
```

`payParams` 为渠道签名后的支付包（V2/V3 均为 JSON 字符串），后端**原样透传**给前端调起支付，前端一套逻辑解析。

### 4.2 收敛终态

- **回调**：见第七节，支付结果通知（`notifyType=PAYMENT`）。
- **主动查单**（无回调或兜底）：

```java
PaymentResult q = payGateway.queryPayment(PayChannelType.WECHAT_V3, orderNo);
if (q.isAccepted() && q.getFinalState().isFinal()) {
    if (q.getFinalState() == PayStatus.SUCCESS) { /* 置已支付、发货 */ }
    else { /* FAIL：置关闭 */ }
}
// accepted=false 或 非终态：不改变订单状态，等待下次查单/回调
```

---

## 五、退款与退差价

### 5.1 全额退款

```java
RefundResult r = payGateway.refund(RefundCommand.builder()
        .bizNo(refundNo)                             // 退款单号（幂等键，唯一）
        .originBizNo(orderNo)                        // 原支付业务单号
        .amount(28600)
        .channelType(PayChannelType.WECHAT_V3)       // 与原支付同协议
        .reason("用户取消")
        .build());
// accepted=true 且 finalState=PROCESSING：已受理，终态靠 queryRefund/退款回调
```

### 5.2 退差价（推荐用门面语义方法）

商家降价、部分退差等场景，**无需自己查原单总额**，直接调：

```java
// 用户已付 286.00，降价后退差价 86.00
RefundResult r = payGateway.refundDifference(
        PayChannelType.WECHAT_V3,   // 渠道（与原支付一致）
        orderNo,                    // 原支付业务单号
        "DIFF" + orderNo,           // 本次退款单号（唯一，幂等键）
        MoneyUtil.yuanToFen(new BigDecimal("86.00")), // 8600 分
        "价格调整");
```

底座内部按 `total_fee=原单总额 / refund_fee=本次额`（V3 `total/refund` 同义）组装，天然支持部分退款。

### 5.3 多次退差价规则

- 每次用**唯一** `refundBizNo`；同一原单可多次退差；
- **累计退款额 ≤ 原单实付额**：由业务侧账本校验（pay 无库）；超额渠道也会拒；
- 每次独立收敛终态：`queryRefund(channelType, refundNo)` 或退款回调。

---

## 六、出款（商家转账 / 企业付款）

### 6.1 到微信零钱（V3 推荐 / V2 到零钱）

```java
PayoutResult r = payGateway.payout(PayoutCommand.builder()
        .bizNo(withdrawNo)                           // 提现单号（幂等键）
        .amount(10000)
        .channelType(PayChannelType.WECHAT_V3)       // 或 WECHAT_V2
        .payee(Payee.builder().openid(openid).build())
        .remark("佣金提现")
        .build());
// accepted=true：受理，finalState 通常 TRANSFERING/ACCEPTED，非终态
```

### 6.2 到银行卡（仅 V2 企业付款到银行卡）

```java
PayoutResult r = payGateway.payout(PayoutCommand.builder()
        .bizNo(withdrawNo)
        .amount(10000)
        .channelType(PayChannelType.WECHAT_V2)
        .payee(Payee.builder()
                .accountNo("622202...")              // 明文卡号（渠道内 RSA 加密后送网关）
                .accountName("张三")
                .bankCode("ICBC")                    // 到银行卡必填渠道编码
                .build())
        .remark("提现")
        .build());
```

> **V2 出款分派规则**：`Payee.accountNo` 非空 → 到银行卡（需 `bankCode`，缺失抛 `PAYEE_INVALID`）；否则按 `openid` → 到零钱。

### 6.3 查单收敛（V2 无回调 → **必须**定时查单）

```java
PayoutResult q = payGateway.queryPayout(PayChannelType.WECHAT_V2, withdrawNo);
if (q.isAccepted() && q.getFinalState().isFinal()) {
    // SUCCESS：确认到账；FAIL：解冻/回滚并可重试（换新单号或按业务规则）
} else {
    // accepted=false（未知）或非终态：不动账，下次对账再查
}
```

### 6.4 撤单

```java
if (payGateway.supportCancel(PayChannelType.WECHAT_V3)) {
    PayoutResult c = payGateway.cancelPayout(PayChannelType.WECHAT_V3, withdrawNo);
    // 仅当 finalState == CANCELLED（终态）才解冻；CANCELING 为撤销中，不得解冻
}
// WECHAT_V2 企业付款不支持撤单：supportCancel=false，调用 cancelPayout 抛 PAY_NOT_SUPPORT_CANCEL
```

---

## 七、回调接入（业务侧实现）

pay 层不含 Controller。业务侧需：① 提供回调 HTTP 端点接收微信请求；② 实现 `PayNotifyHandler` 消费统一消息。

### 7.1 回调端点：交给分发器解析 + 路由

```java
@PostMapping("/api/pay/notify/wechat")
public String wechatNotify(@RequestBody String body,
                           @RequestHeader Map<String, String> headers,
                           @RequestParam PayChannelType channel,
                           @RequestParam PayNotifyType type) {
    // 复合键 (channelType, notifyType) 自动选择解析器并路由到已注册的 Handler
    PayNotifyMessage msg = payNotifyDispatcher.dispatch(channel, type, body, headers);
    return "SUCCESS";   // 处理成功按渠道要求应答；失败务必抛异常 → HTTP 500 触发重试
}
```

> V3 支付/退款/转账通知同属 `WECHAT_V3`，靠 `PayNotifyType`（`PAYMENT`/`REFUND`/`TRANSFER`）区分；V2 支付/退款为 `(WECHAT_V2, PAYMENT/REFUND)`，互不撞键。V2 企业付款**无转账回调**。

### 7.2 业务处理器：实现 `PayNotifyHandler`

```java
@Component
@RequiredArgsConstructor
public class WithdrawPayNotifyHandler implements PayNotifyHandler {

    @Override
    public boolean supports(PayChannelType channelType, PayNotifyType notifyType) {
        return notifyType == PayNotifyType.TRANSFER;   // 只关心转账结果
    }

    @Override
    public void handle(PayNotifyMessage message) {
        // 幂等：按 bizNo + channelOrderNo 去重；仅当 message.payStatus.isFinal() 才终态化
        // 处理失败必须抛异常（控制器转 HTTP 500），触发微信重试
    }
}
```

`PayNotifyMessage` 归一字段：`channelType`/`notifyType`/`bizNo`/`channelOrderNo`/`payStatus`/`amount`/`successTime`/`failReason`。

---

## 八、金额工具 MoneyUtil

```java
int fen  = MoneyUtil.yuanToFen(new BigDecimal("286.00")); // 28600（四舍五入，溢出抛异常）
BigDecimal yuan = MoneyUtil.fenToYuan(8600);              // 86.00
```

传入 `BigDecimal`（**不要**用 `double`），避免 `286.00` 被误传为 `286`。

---

## 九、状态语义速查（PayStatus）

| 状态 | 终态? | 含义 | 业务处理 |
|---|---|---|---|
| `ACCEPTED` | 否 | 渠道已受理 | 置「处理中」，等回调/查单 |
| `PROCESSING`/`TRANSFERING` | 否 | 处理中/转账中 | 保持，勿改账 |
| `WAIT_USER_CONFIRM` | 否 | 待用户确认收款（V3） | 引导用户确认 |
| `CANCELING` | 否 | 撤销中 | **不得**解冻 |
| `SUCCESS` | ✅ | 到账/退款成功 | 记账终态 |
| `FAIL` | ✅ | 明确失败 | 解冻/回滚 |
| `CANCELLED` | ✅ | 已撤销 | 解冻/回滚 |

---

## 十、错误码（PayErrorCode）

| 码 | 触发场景 |
|---|---|
| `PAY_CHANNEL_UNSUPPORTED` | 渠道未启用/未实现（如未开 V2 却路由 `WECHAT_V2`） |
| `PAY_AMOUNT_INVALID` | 金额缺失/≤0/超出渠道单笔限额 |
| `PAYEE_INVALID` | 收款方信息不完整（缺 openid/卡号/`bankCode` 等） |
| `PAY_NOT_SUPPORT_CANCEL` | 对不支持撤单渠道（V2）调 `cancelPayout` |
| `PAY_ORIGINAL_ORDER_MISSING` | 退款时原支付单不存在/不可退 |
| `WX_PAY_CALL_FAIL` | 渠道调用异常/银行卡敏感信息加密失败 |
| `WX_PAY_NOTIFY_INVALID` | 回调报文非法/验签解密失败/商户号不匹配 |

---

## 十一、端到端示例：收款 → 退差价

```
pay(bizNo=P001, amount=28600, V3)           → PROCESSING，订单「待支付」，前端用 payParams 拉起
支付回调(PAYMENT, P001, SUCCESS) / queryPayment 收敛 → 订单「已支付」，记账 paid=28600
refundDifference(V3, P001, "D001", 8600, "价格调整") → PROCESSING，退款单「处理中」
退款回调(REFUND, D001, SUCCESS) / queryRefund 收敛     → 记账 refunded=8600
（可继续）refundDifference(V3, P001, "D002", 3000, …)  → 业务校验 8600+3000 ≤ 28600 通过
```

---

## 十二、接入检查清单

- [ ] 依赖 `zhush-module-pay`，注入 `PayGateway`（回调解耦再注入 `PayNotifyDispatcher`）。
- [ ] 运行环境已启用目标协议并配齐凭证（V2 退款/企业付款需 PKCS12 `key-path`）。
- [ ] 全链路金额单位为「分」，边界用 `MoneyUtil`。
- [ ] 幂等：`bizNo`/退款单号唯一且复用；回调按 `bizNo+channelOrderNo` 去重。
- [ ] **不**以同步返回判成功；终态一律靠回调/查单，`isFinal()` 才落终态。
- [ ] V2 出款配置**定时查单/对账**任务（无回调）。
- [ ] 退差价在业务侧维护累计退款额账本，保证 ≤ 原单实付。
- [ ] 回调处理失败抛异常（HTTP 500）触发微信重试；应答格式符合渠道要求。

