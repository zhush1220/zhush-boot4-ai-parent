package com.zhush.pay.wx.v2;

import cn.hutool.core.util.StrUtil;
import com.github.binarywang.wxpay.bean.entpay.EntPayBankQueryResult;
import com.github.binarywang.wxpay.bean.entpay.EntPayBankRequest;
import com.github.binarywang.wxpay.bean.entpay.EntPayBankResult;
import com.github.binarywang.wxpay.bean.entpay.EntPayQueryResult;
import com.github.binarywang.wxpay.bean.entpay.EntPayRequest;
import com.github.binarywang.wxpay.bean.entpay.EntPayResult;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.zhush.pay.core.channel.PayoutChannel;
import com.zhush.pay.core.model.PayChannelType;
import com.zhush.pay.core.model.PayStatus;
import com.zhush.pay.core.model.Payee;
import com.zhush.pay.core.model.PayoutCommand;
import com.zhush.pay.core.model.PayoutResult;
import com.zhush.pay.enums.PayErrorCode;
import com.zhush.pay.wx.WxPayProperties;
import com.zhush.pay.wx.exception.WxPayExceptionTranslator;
import lombok.extern.slf4j.Slf4j;

import static com.zhush.common.exception.ServiceExceptionUtil.assertTrue;
import static com.zhush.common.exception.ServiceExceptionUtil.exception;

/**
 * 微信 V2 企业付款出款渠道（到银行卡 / 到零钱，channelType={@link PayChannelType#WECHAT_V2}）
 *
 * <p>镜像 {@code WxV3PayoutChannel}，底层走 SDK {@code EntPayService}：
 * <ul>
 *     <li>{@code Payee.accountNo} 非空 → 到银行卡 {@code payBank}（卡号/姓名 RSA 预加密）；</li>
 *     <li>否则 {@code Payee.openid} 非空 → 到零钱 {@code entPay}。</li>
 * </ul>
 *
 * <p><b>红线</b>：企业付款同步 {@code result_code=SUCCESS} 仅代表<b>受理</b>（{@link PayStatus#ACCEPTED}），
 * <b>不等于到账</b>；V2 出款<b>无结果回调</b>，终态只能靠 {@link #query} 主动查单收敛。渠道异常一律
 * {@code accepted=false}，绝不伪装受理。企业付款不支持撤单（{@link #supportCancel()} 返回 false）。
 *
 * @author zhush
 */
@Slf4j
public class WxV2PayoutChannel implements PayoutChannel {

    /** 到零钱实名校验选项 */
    private static final String CHECK_NAME_NONE = "NO_CHECK";
    private static final String CHECK_NAME_FORCE = "FORCE_CHECK";
    private static final String DEFAULT_IP = "127.0.0.1";

    private final WxV2PayoutClient client;
    private final WxPayProperties properties;

    public WxV2PayoutChannel(WxV2PayoutClient client, WxPayProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public PayChannelType channelType() {
        return PayChannelType.WECHAT_V2;
    }

    @Override
    public PayoutResult payout(PayoutCommand command) {
        validate(command);
        validateLimit(command.getAmount());

        Payee payee = command.getPayee();
        // 有银行卡信息走 payBank；否则按 openid 走到零钱
        if (StrUtil.isNotBlank(payee.getAccountNo())) {
            return payoutToBank(command, payee);
        }
        return payoutToBalance(command, payee);
    }

    @Override
    public PayoutResult query(String bizNo) {
        // V2 出款无回调：到银行卡/到零钱查单接口不同且仅凭 bizNo 无法区分，
        // 先查银行卡、未命中再查零钱，均失败则结果未知（accepted=false，保守非终态）
        try {
            EntPayBankQueryResult bank = client.queryPayBank(bizNo);
            return fromBankQuery(bizNo, bank);
        } catch (WxPayException bankError) {
            log.debug("[WxV2] 出款到银行卡查单未命中，转查零钱, bizNo={}, reason={}",
                    bizNo, WxPayExceptionTranslator.describe(bankError));
        }
        try {
            EntPayQueryResult balance = client.queryEntPay(bizNo);
            return fromBalanceQuery(bizNo, balance);
        } catch (WxPayException e) {
            log.error("[WxV2] 企业付款查单失败, bizNo={}, reason={}", bizNo, WxPayExceptionTranslator.describe(e));
            return WxPayExceptionTranslator.toFailResult(e);
        }
    }

    @Override
    public PayoutResult cancel(String bizNo) {
        // 企业付款不支持撤单：明确未受理，不伪装成功（V3 商家转账才支持撤单）
        throw exception(PayErrorCode.PAY_NOT_SUPPORT_CANCEL, "WECHAT_V2");
    }

    @Override
    public boolean supportCancel() {
        return false;
    }

    // ================ 内部方法 ================

    private PayoutResult payoutToBank(PayoutCommand command, Payee payee) {
        WxPayProperties.V2 v2 = properties.getV2();
        EntPayBankRequest request = new EntPayBankRequest();
        request.setAppid(StrUtil.blankToDefault(command.getAppId(), v2.getAppId()));
        request.setMchId(StrUtil.blankToDefault(command.getMchId(), v2.getMchId()));
        request.setPartnerTradeNo(command.getBizNo());
        request.setAmount(command.getAmount());
        request.setBankCode(resolveBankCode(payee));
        request.setDescription(StrUtil.blankToDefault(command.getRemark(), "企业付款"));
        try {
            // 敏感字段用微信支付公钥 RSA-OAEP 预加密（SDK 不自动加密）
            request.setEncBankNo(client.encryptSensitive(payee.getAccountNo()));
            request.setEncTrueName(client.encryptSensitive(payee.getAccountName()));

            EntPayBankResult result = client.payBank(request);
            boolean accepted = WxV2ResultChecker.businessSuccess(result);
            log.info("[WxV2] 付款到银行卡受理, bizNo={}, accepted={}, paymentNo={}",
                    command.getBizNo(), accepted, result.getPaymentNo());
            return PayoutResult.builder()
                    .accepted(accepted)
                    .finalState(accepted ? PayStatus.ACCEPTED : PayStatus.FAIL)
                    .channelOrderNo(result.getPaymentNo())
                    .channelState(result.getResultCode())
                    .failReason(accepted ? null : WxV2ResultChecker.failReason(result))
                    .build();
        } catch (WxPayException e) {
            log.error("[WxV2] 付款到银行卡调用失败, bizNo={}, reason={}", command.getBizNo(),
                    WxPayExceptionTranslator.describe(e));
            return WxPayExceptionTranslator.toFailResult(e);
        }
    }

    private PayoutResult payoutToBalance(PayoutCommand command, Payee payee) {
        WxPayProperties.V2 v2 = properties.getV2();
        EntPayRequest request = new EntPayRequest();
        request.setMchAppid(StrUtil.blankToDefault(command.getAppId(), v2.getAppId()));
        request.setAppid(StrUtil.blankToDefault(command.getAppId(), v2.getAppId()));
        request.setMchId(StrUtil.blankToDefault(command.getMchId(), v2.getMchId()));
        request.setPartnerTradeNo(command.getBizNo());
        request.setOpenid(payee.getOpenid());
        request.setAmount(command.getAmount());
        request.setDescription(StrUtil.blankToDefault(command.getRemark(), "企业付款"));
        request.setSpbillCreateIp(DEFAULT_IP);
        // 提供收款人姓名则强制校验实名，否则跳过
        boolean verifyName = StrUtil.isNotBlank(payee.getAccountName());
        request.setCheckName(verifyName ? CHECK_NAME_FORCE : CHECK_NAME_NONE);
        if (verifyName) {
            request.setReUserName(payee.getAccountName());
        }
        try {
            EntPayResult result = client.entPay(request);
            // 到零钱以 ret_code 为准（SUCCESS 即受理），终态仍靠查单
            boolean accepted = WxV2ResultChecker.businessSuccess(result)
                    && (result.getRetCode() == null || "SUCCESS".equals(result.getRetCode()));
            log.info("[WxV2] 付款到零钱受理, bizNo={}, accepted={}, paymentNo={}, retCode={}",
                    command.getBizNo(), accepted, result.getPaymentNo(), result.getRetCode());
            return PayoutResult.builder()
                    .accepted(accepted)
                    .finalState(accepted ? PayStatus.ACCEPTED : PayStatus.FAIL)
                    .channelOrderNo(result.getPaymentNo())
                    .channelState(result.getRetCode())
                    .failReason(accepted ? null
                            : StrUtil.blankToDefault(result.getRetMsg(), WxV2ResultChecker.failReason(result)))
                    .build();
        } catch (WxPayException e) {
            log.error("[WxV2] 付款到零钱调用失败, bizNo={}, reason={}", command.getBizNo(),
                    WxPayExceptionTranslator.describe(e));
            return WxPayExceptionTranslator.toFailResult(e);
        }
    }

    private PayoutResult fromBankQuery(String bizNo, EntPayBankQueryResult result) {
        PayStatus state = WxV2PayoutStateMapper.toPayStatus(result.getStatus());
        return PayoutResult.builder()
                .accepted(true)
                .finalState(state)
                .channelOrderNo(result.getPaymentNo())
                .channelState(result.getStatus())
                .failReason(state == PayStatus.FAIL ? result.getFailReason() : null)
                .build();
    }

    private PayoutResult fromBalanceQuery(String bizNo, EntPayQueryResult result) {
        PayStatus state = WxV2PayoutStateMapper.toPayStatus(result.getStatus());
        return PayoutResult.builder()
                .accepted(true)
                .finalState(state)
                .channelState(result.getStatus())
                .failReason(state == PayStatus.FAIL ? result.getReason() : null)
                .build();
    }

    /**
     * 银行卡渠道编码：优先取业务侧显式 bankCode；缺失则拒单（不做名称→编码猜测，避免错发）
     */
    private String resolveBankCode(Payee payee) {
        assertTrue(StrUtil.isNotBlank(payee.getBankCode()),
                PayErrorCode.PAYEE_INVALID, "到银行卡缺少 bankCode");
        return payee.getBankCode();
    }

    private void validate(PayoutCommand command) {
        assertTrue(command != null && StrUtil.isNotBlank(command.getBizNo()),
                PayErrorCode.PAYEE_INVALID, "业务单号缺失");
        assertTrue(command.getAmount() != null && command.getAmount() > 0,
                PayErrorCode.PAY_AMOUNT_INVALID, command.getAmount());
        Payee payee = command.getPayee();
        assertTrue(payee != null
                        && (StrUtil.isNotBlank(payee.getAccountNo()) || StrUtil.isNotBlank(payee.getOpenid())),
                PayErrorCode.PAYEE_INVALID, "收款方银行卡号或 openid 至少其一");
    }

    private void validateLimit(Integer amount) {
        WxPayProperties.AmountRange range = properties.getLimit().getWechatV2();
        if (range == null || amount == null) {
            return;
        }
        if (range.getMinAmount() != null) {
            assertTrue(amount >= range.getMinAmount(), PayErrorCode.PAY_AMOUNT_INVALID,
                    "低于单笔下限 " + range.getMinAmount() + " 分");
        }
        if (range.getMaxAmount() != null) {
            assertTrue(amount <= range.getMaxAmount(), PayErrorCode.PAY_AMOUNT_INVALID,
                    "超过单笔上限 " + range.getMaxAmount() + " 分");
        }
    }

}
