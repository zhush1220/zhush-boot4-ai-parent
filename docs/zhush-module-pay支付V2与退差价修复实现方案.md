# zhush-module-pay 支付 V2 支持与退差价 修复实现方案

> 类型：实现方案 / 施工设计（decision-complete，编码前无需再作架构决策）
> 关联：[能力分析](./zhush-module-pay支付能力分析与退差价设计.md) · [整合方案](./zhush-module-pay微信支付整合方案.md)
> 前提：**本方案聚焦 pay 模块自身**，假设有一个可编译的基线；不考虑 union 分支代码/分支完整性问题。
> SDK：`com.github.binarywang:weixin-java-pay:4.8.0`（下列方法签名均已用 `javap` 核实）。

---

## 一、目标与范围

补齐分析文档中的 **P0（V2 全量缺实现）** 与 **P1-2（退差价缺封装）**，使三类动作在 **V2、V3 双协议** 下均可用：

| 动作 | V3（现状） | V2（本方案新增） |
|---|---|---|
| 统一支付 / 收款 | ✅ JSAPI | ✅ JSAPI（Native/H5 预留位） |
| 退款（含**退差价**） | ✅ 能力具备 | ✅ `total_fee`/`refund_fee` 分离 |
| 商家转账 / 出款 | ✅ transfer-bills | ✅ 企业付款（到银行卡 `payBank` / 到零钱 `entPay`） |
| 支付 / 退款回调解析 | ✅ | ✅（XML + `req_info` 解密） |

**不做**：不新增平行抽象层（沿用 `PayGateway` + `PaymentChannel`/`PayoutChannel` + `PayNotifyType` 复合键）；`core` 契约仅做**向后兼容的增量扩展**（新增语义方法），不改动既有签名。

---

## 二、设计总则

1. **V2 只是新增适配器**：全部落在 `com.zhush.pay.wx.v2` 包，仿 `wx/v3` 结构；`WxPayConfiguration` 以 `@ConditionalOnProperty(zhush.pay.wx.v2.enabled=true)` 条件装配，与 V3 互不影响。
2. **共享协议无关件**：把 `WxPayTradeStateMapper`（V2/V3 的 `trade_state` 词表完全一致）从 `wx/v3` **上移到 `wx`** 供两版复用；V2 额外提供 `WxV2ResultChecker`（判 `return_code`/`result_code`）。
3. **资金安全红线不破**：V2 与 V3 同规则——同步返回仅代表受理，终态靠查单/回调；渠道异常一律 `accepted=false`。
4. **金额单位为分（Integer）**：与既有模型一致；提供 `MoneyUtil` 做元↔分安全转换（避免 286.00 误传 286）。
5. **V2 关键差异处理**：
   - 报文为 **XML**、签名 **MD5/HMAC-SHA256**（非 V3 的 JSON + AES-GCM）；
   - 退款/企业付款/银行卡需 **PKCS12 商户证书做双向 TLS**（`WxPayProperties.V2.keyPath` 已预留）；
   - 企业付款到银行卡的**卡号/姓名需先用微信 RSA 公钥加密**（`EntPayService.getPublicKey()`）；
   - **企业付款无结果回调，只能靠查单收敛**。

---

## 三、包与类清单（新增/改动）

```
com.zhush.pay.wx
├── WxPayTradeStateMapper            ← 由 wx/v3 上移（复用），逻辑不变
├── WxPayConfiguration               ← 改：新增 V2 beans
├── WxPayProperties                  ← 不变（V2 配置已就绪）
└── v2/
    ├── WxV2PaymentClient            ← 新：unifiedOrder/queryOrder/refund/refundQuery
    ├── WxV2PayoutClient             ← 新：payBank/queryPayBank/entPay/queryEntPay（+RSA加密）
    ├── WxV2ResultChecker            ← 新：return_code/result_code 判定
    ├── WxV2PaymentChannel           ← 新：implements PaymentChannel（收款/退款/查单）
    ├── WxV2PayoutChannel            ← 新：implements PayoutChannel（出款，无回调）
    ├── WxV2PaymentNotifyParser      ← 新：implements PayNotifyParser（PAYMENT）
    └── WxV2RefundNotifyParser       ← 新：implements PayNotifyParser（REFUND，req_info 解密）

com.zhush.pay.core
├── PayGateway                       ← 改：新增 refundDifference 语义（default 方法，兼容）
└── util/MoneyUtil                   ← 新：元↔分（若 zhush-common 已有则复用，见 5.4）
```

> 复用不改动的既有件：`PaymentCommand/Result`、`RefundCommand/Result`、`PayoutCommand/Result`、`PayStatus`、`PayChannelType(WECHAT_V2)`、`PayNotifyDispatcher`（复合键路由天然支持 V2）、`WxPayExceptionTranslator`。

---

## 四、V2 各能力实现要点

### 4.1 出款：`WxV2PayoutChannel`（企业付款，`channelType=WECHAT_V2`）

镜像 `WxV3PayoutChannel`，底层走 `EntPayService`。到银行卡示例：

```java
public PayoutResult payout(PayoutCommand command) {
    validate(command); validateLimit(command.getAmount());
    EntPayBankRequest req = new EntPayBankRequest();
    req.setPartnerTradeNo(command.getBizNo());        // 幂等键 = 业务单号
    req.setAmount(command.getAmount());               // 分
    // 卡号/姓名 RSA 加密（entPayClient 内用 getPublicKey() 缓存公钥处理）
    client.encryptAndFill(req, command.getPayee());   // bankName/账号/姓名密文 + 序列号
    req.setDesc(StrUtil.blankToDefault(command.getRemark(), v2defaultRemark));
    try {
        EntPayBankResult r = client.payBank(req);
        // 企业付款“受理”不等于到账：非明确 SUCCESS 一律非终态
        PayStatus state = WxV2ResultChecker.toPayoutState(r);   // SUCCESS→ACCEPTED；FAIL→FAIL
        return PayoutResult.builder().accepted(state != PayStatus.FAIL)
                .finalState(state).channelState(r.getResultCode())
                .failReason(WxV2ResultChecker.failMsg(r)).build();
    } catch (WxPayException e) {
        return WxPayExceptionTranslator.toFailResult(e);   // 绝不伪装受理
    }
}
public PayoutResult query(String bizNo) {              // 无回调→查单收敛终态
    EntPayBankQueryResult r = client.queryPayBank(bizNo);
    return PayoutResult.builder().accepted(true)
            .finalState(WxV2ResultChecker.toBankPayState(r.getStatus())) // 银行付款态→PayStatus
            .channelOrderNo(r.getDetailId()).build();
}
public boolean supportCancel() { return false; }       // 企业付款不支持撤单
```

- 到零钱变体：`entPay(EntPayRequest)` / `queryEntPay`，同一渠道内按 `Payee` 是否有银行卡信息分派（默认到银行卡，保持与提现业务一致）。
- **状态映射**：V2 出款无 `trade_state`，用 `return_code/result_code` + 查单态映射到 `PayStatus`（`WxV2ResultChecker`）；未确认一律非终态。

### 4.2 收款：`WxV2PaymentChannel.pay`（统一支付 JSAPI）

```java
public PaymentResult pay(PaymentCommand command) {
    validatePay(command);
    WxPayUnifiedOrderRequest req = new WxPayUnifiedOrderRequest();
    req.setTradeType("JSAPI");                          // 预留 NATIVE/H5/APP
    req.setAppid(blankToDefault(command.getAppId(), v2.getAppId()));
    req.setMchId(v2.getMchId());
    req.setBody(command.getDescription());
    req.setOutTradeNo(command.getBizNo());              // 幂等
    req.setTotalFee(command.getAmount());               // 分
    req.setOpenid(command.getPayerId());
    req.setNotifyUrl(blankToDefault(command.getNotifyUrl(), properties.getNotifyUrl()));
    req.setSpbillCreateIp("127.0.0.1");
    try {
        WxPayUnifiedOrderResult r = client.unifiedOrder(req);   // 返回 prepay_id
        String payParams = WxV2JsapiSigner.build(client, req, r.getPrepayId()); // 见下
        return PaymentResult.accepted(PayStatus.PROCESSING, r.getPrepayId(), payParams);
    } catch (WxPayException e) { return PaymentResult.fail(describe(e)); }
}
```

- **`WxV2JsapiSigner`（工具）**：V2 不自动产出前端支付包，需用 `appId/timeStamp/nonceStr/package=prepay_id=xxx/signType` 以 mchKey 生成 `paySign`，组装为 JSON（对齐 V3 的 `payParams` 契约，前端一套逻辑解析）。
- 查询 `query(bizNo)`：`queryOrder(null, bizNo)` → `WxPayOrderQueryResult.getTradeState()` → `WxPayTradeStateMapper.toPayStatus`。

### 4.3 退款：`WxV2PaymentChannel.refund`（**天然支持退差价**）

```java
public RefundResult refund(RefundCommand command) {
    validateRefund(command);
    // 退款需原单总额：V2 查单现场取 total_fee（=原实付），与 V3 同策略
    WxPayOrderQueryResult origin = client.queryOrder(null, command.getOriginBizNo());
    assertTrue(origin.getTotalFee() != null, PAY_ORIGINAL_ORDER_MISSING, command.getOriginBizNo());

    WxPayRefundRequest req = new WxPayRefundRequest();
    req.setOutTradeNo(command.getOriginBizNo());
    req.setOutRefundNo(command.getBizNo());             // 每次唯一（退差价幂等键）
    req.setTotalFee(origin.getTotalFee());              // 原单总额（如 28600）
    req.setRefundFee(command.getAmount());              // 本次退款额（如 8600）← 退差价
    req.setRefundDesc(command.getReason());
    req.setNotifyUrl(blankToDefault(command.getNotifyUrl(), properties.getNotifyUrl()));
    // 需 PKCS12 双向证书：由 wxPayService.config.keyPath 提供，无需逐请求设置
    WxPayRefundResult r = client.refund(req);
    // V2 退款返回即受理；终态靠 refundQuery / 退款回调
    return RefundResult.of(PayStatus.PROCESSING, r.getRefundId());
}
public RefundResult queryRefund(String refundBizNo) {
    WxPayRefundQueryResult r = client.refundQuery(refundBizNo);
    return RefundResult.of(WxPayTradeStateMapper.toRefundStatus(r.getRefundStatus_0()), r.getRefundId_0());
}
```

> V2 `total_fee=原单总额 / refund_fee=本次退款额` 与 V3 `total/refund` 语义等价 → **退差价在 V2 同样直接支持，无需新增表结构**。

### 4.4 回调解析器：`WxV2PaymentNotifyParser` / `WxV2RefundNotifyParser`

- `channelType()=WECHAT_V2`；`notifyType()` 分别 `PAYMENT`/`REFUND`，天然进 `PayNotifyDispatcher` 的 `(WECHAT_V2, PAYMENT/REFUND)` 复合键，与 V3、转账互不撞键。
- 支付通知（V2 仅支付成功时推送）：

```java
public PayNotifyMessage parse(String body, Map<String,String> headers) {
    assertTrue(StrUtil.isNotBlank(body), WX_PAY_NOTIFY_INVALID, "报文为空");
    WxPayOrderNotifyResult r = wxPayService.parseOrderNotifyResult(body); // SDK 内部完成 XML 解析+验签
    PayStatus state = "SUCCESS".equals(r.getReturnCode()) && "SUCCESS".equals(r.getResultCode())
            ? PayStatus.SUCCESS : PayStatus.PROCESSING;                   // V2 通知无 trade_state
    return PayNotifyMessage.builder().channelType(WECHAT_V2).notifyType(PAYMENT)
        .bizNo(r.getOutTradeNo()).channelOrderNo(r.getTransactionId())
        .amount(r.getTotalFee()).payStatus(state)
        .mchId(v2.getMchId())                                             // 归属校验按配置商户号
        .successTime(parseTime(r.getTimeEnd())).build();
}
```

- 退款通知：`parseRefundNotifyResult(body).getReqInfo()`（SDK 用 apiV2 密钥**自动解密 req_info**）→ `getOutRefundNo/getRefundStatus/getSuccessTime` → `toRefundStatus`；`notifyType=REFUND`。
- 统一约定：验签/解密失败抛 `WX_PAY_INVALID_SIGNATURE`/`WX_PAY_NOTIFY_INVALID`；处理失败由回调 Controller 返 **HTTP 500** 触发微信重试（沿用既有幂等收敛）。

### 4.5 装配与配置：`WxPayConfiguration`（V2 分支）

在既有 `wxPayService`（已按 `v2.isEnabled()` 载入 mchKey/entPayKey/keyPath/signType）基础上追加：

```java
@Bean @ConditionalOnProperty(prefix="zhush.pay.wx.v2", name="enabled", havingValue="true")
public WxV2PaymentClient wxV2PaymentClient(WxPayService s, WxPayProperties p){ return new WxV2PaymentClient(s,p); }
@Bean @ConditionalOnProperty(prefix="zhush.pay.wx.v2", name="enabled", havingValue="true")
public WxV2PayoutClient  wxV2PayoutClient (WxPayService s, WxPayProperties p){ return new WxV2PayoutClient(s,p); }
@Bean @ConditionalOnProperty(prefix="zhush.pay.wx.v2", name="enabled", havingValue="true")
public WxV2PaymentChannel wxV2PaymentChannel(WxV2PaymentClient c, WxPayProperties p){ return new WxV2PaymentChannel(c,p); }
@Bean @ConditionalOnProperty(prefix="zhush.pay.wx.v2", name="enabled", havingValue="true")
public WxV2PayoutChannel  wxV2PayoutChannel (WxV2PayoutClient  c, WxPayProperties p){ return new WxV2PayoutChannel(c,p); }
@Bean @ConditionalOnProperty(prefix="zhush.pay.wx.v2", name="enabled", havingValue="true")
public WxV2PaymentNotifyParser wxV2PaymentNotifyParser(WxPayService s, WxPayProperties p){ return new WxV2PaymentNotifyParser(s,p); }
@Bean @ConditionalOnProperty(prefix="zhush.pay.wx.v2", name="enabled", havingValue="true")
public WxV2RefundNotifyParser  wxV2RefundNotifyParser (WxPayService s, WxPayProperties p){ return new WxV2RefundNotifyParser(s,p); }
```

`WxV2*Client` 被 `List<PaymentChannel>`/`List<PayoutChannel>`/`List<PayNotifyParser>` 自动聚合进路由，`PayGateway` 路由 `WECHAT_V2` 不再抛 `PAY_CHANNEL_UNSUPPORTED`。

- **证书**：`zhush.pay.wx.v2.key-path`（PKCS12，口令=商户号，SDK 内解析）为退款/企业付款必需；缺失时 `validateV2()` 启动即失败（fail-fast，禁止静默降级）。证书经环境变量/挂载卷外置，禁止入库入代码。

### 4.6 状态与错误处理

- 复用 `WxV2ResultChecker`：`return_code!=SUCCESS` → 通信/参数失败（`accepted=false`）；`result_code=FAIL` → 业务失败，取 `err_code_des`。
- 复用 `WxPayTradeStateMapper`（`trade_state`：SUCCESS/REFUND→SUCCESS；CLOSED/REVOKED/PAYERROR→FAIL；其余→PROCESSING）。
- 复用 `WxPayExceptionTranslator` 脱敏；`PayErrorCode.WX_V2_*`（20005xx）已存在，按需补充"证书缺失""加密失败"码。

---

## 五、退差价封装（P1-2）落地设计

### 5.1 门面语义方法（向后兼容，default 实现）

`PayGateway` 新增（不破坏既有实现）：

```java
/**
 * 退差价 / 部分退款：对同一原单退指定金额（单位：分）。
 * 幂等以 refundBizNo（渠道 out_refund_no）为键，V2/V3 一致。
 */
default RefundResult refundDifference(PayChannelType channelType, String originBizNo,
                                      String refundBizNo, int amountFen, String reason) {
    return refund(RefundCommand.builder()
            .channelType(channelType).originBizNo(originBizNo).bizNo(refundBizNo)
            .amount(amountFen).reason(reason).build());
}
```

底座内部照 4.3/对应 V3 逻辑查原单总额并调渠道退款——**业务侧不再手写查单/总额**。

### 5.2 多次退差价规则

- 每次退差价用**唯一** `refundBizNo`；同一原单可多次；
- **累计退款额 ≤ 原单实付额**：由业务侧账本校验（pay 无 DB，符合底座定位）；渠道侧微信也会拒超额，本地先拦更快、提示更友好；
- 每次独立收敛终态（`queryRefund`/退款回调），互不影响。

### 5.3 286 → 退差价 86 端到端（V3 或 V2 同构）

```
pay(bizNo=P001, amount=28600)              → PROCESSING（原单实付 28600）
支付回调/查单收敛 P001 = SUCCESS            → 记账：paid=28600, refunded=0
refundDifference(WECHAT_V3, P001, RF001, 8600, "价格调整")
  → 查原单 total=28600 → refund(refund=8600,total=28600) → PROCESSING
退款回调/queryRefund(RF001) = SUCCESS       → 记账：refunded=8600
（可继续）refundDifference(..., RF002, 3000, ...)  校验 8600+3000≤28600 通过
```

### 5.4 金额单位工具

新增 `com.zhush.pay.core.util.MoneyUtil`（若 `zhush-common` 已有等价工具则直接复用，避免重复）：

```java
public static int yuanToFen(BigDecimal yuan){ 
    return yuan.movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValueExact(); }
public static BigDecimal fenToYuan(int fen){ return BigDecimal.valueOf(fen).movePointLeft(2); }
```

全链路金额统一"分（Integer）"，业务边界处用 `MoneyUtil` 转换，禁止在 pay 内部出现浮点金额。

---

## 六、施工阶段（建议顺序，每阶段独立可编译可测）

1. **共享件上移**：`WxPayTradeStateMapper` v3→wx；新增 `WxV2ResultChecker`、`MoneyUtil`；跑回归（V3 单测不破）。
2. **V2 退款 + 收款查单**（先退款，验证证书与部分退款链路）：`WxV2PaymentClient` + `WxV2PaymentChannel.refund/queryRefund/query` + `refundDifference` 门面方法。
3. **V2 收款下单**：`WxV2JsapiSigner` + `WxV2PaymentChannel.pay`。
4. **V2 出款**：`WxV2PayoutClient`（含 RSA 加密）+ `WxV2PayoutChannel`（无回调，靠查单）。
5. **V2 回调解析**：`WxV2PaymentNotifyParser` / `WxV2RefundNotifyParser`。
6. **装配**：`WxPayConfiguration` V2 beans；`zhush.pay.wx.v2.enabled` 灰度开启。
7. 全量单测 + 真实/仿真小额联调（JSAPI 支付、退差价、企业付款查单）。

---

## 七、测试方案（Mock，仿既有 V3 测试）

- `WxV2ResultCheckerTest`：return/result_code → `PayStatus` 映射矩阵。
- `WxV2PaymentChannelTest`：
  - `pay` 受理仅 PROCESSING（mock `unifiedOrder`→prepay_id）；
  - **退差价**：`queryOrder` 返回 total=28600，`refundFee=8600` → 断言 `WxPayRefundRequest` 的 `total_fee/refund_fee` 正确（`ArgumentCaptor`）；
  - 证书/调用异常 → accepted=false；原单缺失 → `ServiceException`。
- `WxV2PayoutChannelTest`：受理非终态；`payBank` 异常不伪装；查单收敛。
- `WxV2PaymentNotifyParserTest`/`WxV2RefundNotifyParserTest`：mock `parseOrderNotifyResult`/`parseRefundNotifyResult`（含 `ReqInfo`），验签失败/空报文抛异常，字段归一正确；`channelType=WECHAT_V2`、`notifyType=PAYMENT/REFUND`。
- `PayNotifyDispatcherTest`（若缺）：`(WECHAT_V2,PAYMENT)`、`(WECHAT_V2,REFUND)`、`(WECHAT_V3,…)`、`(WECHAT_V3,TRANSFER)` 四路互不撞键。

---

## 八、配置（四环境，`zhush.pay.wx.v2.*`）

```yaml
zhush.pay.wx:
  enabled: true
  v2:
    enabled: ${WX_V2_ENABLED:false}      # 灰度：按环境开启
    app-id: ${WX_V2_APPID:}
    mch-id: ${WX_V2_MCHID:}
    mch-key: ${WX_V2_MCHKEY:}            # APIv2 密钥
    ent-pay-key: ${WX_V2_ENTPAYKEY:}
    key-path: file:/data/certs/apiclient_cert.p12   # PKCS12，退款/企业付款必需
    sign-type: HMAC-SHA256
```

凭证全部环境变量/挂载卷外置；启用而缺凭证 → `validateV2()` 启动失败。**四环境结构已具备**（V2 字段在 `WxPayProperties` 已定义），仅需按需填值与证书卷。

---

## 九、风险与注意事项

| 风险 | 处置 |
|---|---|
| V2 退款/企业付款**强制 PKCS12 双向证书** | 证书卷挂载 + `validateV2()` fail-fast；无证书不得开 V2 开关 |
| 企业付款到银行卡**卡号/姓名须 RSA 加密** | `WxV2PayoutClient` 内 `getPublicKey()` 缓存 + 加密；卡号 SM4 密文落库仍归业务侧（沿用二期脱敏约束） |
| **V2 出款无回调** | 只能定时查单收敛终态；勿因"无回调"误判失败 |
| 退差价**超额/重复** | 业务侧累计账本 + `refundBizNo` 唯一；渠道超额会拒 |
| 金额单位/精度 | 全链路分（Integer），边界用 `MoneyUtil`，禁浮点 |
| V2 通知无 `trade_state` | 成功通知即 SUCCESS；需精确态走查单 |
| 与 V3 并存撞键 | 复合键 `(channelType, notifyType)` 已隔离；回调端点按 channel+type 分派 |
| V2 API 官方逐步收敛、部分商户无权限 | 上线前核对商户 V2 企业付款/退款权限与限额；优先 V3 收款，V2 出款按业务需要启用 |

---

## 十、验收标准（DoD）

- [ ] `PayGateway` 路由 `WECHAT_V2` 的 `payout/queryPayout/pay/queryPayment/refund/queryRefund` 全部命中实现，不再抛 unsupported。
- [ ] V2 收款 JSAPI 下单成功仅返回 PROCESSING + `payParams`；`queryOrder` 正确收敛终态。
- [ ] **V2 退差价**：同一原单可多次部分退款，`total_fee` 取原单总额、`refund_fee` 取本次额，`refundDifference` 语义方法可用；超额/重复被拦或幂等安全。
- [ ] V2 出款无回调下由查单正确收敛；`accepted` 与终态严格区分；异常不伪装受理。
- [ ] V2 支付/退款回调解析验签/解密失败被拒、字段归一正确；与 V3/转账回调互不撞键。
- [ ] `zhush.pay.wx.v2.enabled=true` 且缺证书时启动失败、报错明确。
- [ ] 上述 Mock 单测全绿；`mvn -pl zhush-module-pay test` 通过；真实/仿真小额联调记录留档。
- [ ] 本方案不新增平行抽象、不改既有 `core` 契约签名（仅 `default` 扩展），V3 既有测试回归通过。
