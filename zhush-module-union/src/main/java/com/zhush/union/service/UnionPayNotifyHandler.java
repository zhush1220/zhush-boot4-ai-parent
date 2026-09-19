package com.zhush.union.service;

import com.zhush.pay.core.model.PayChannelType;
import com.zhush.pay.core.notify.PayNotifyHandler;
import com.zhush.pay.core.notify.PayNotifyMessage;
import com.zhush.pay.core.notify.PayNotifyType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 提现出款回调处理器（对接统一支付底座）
 *
 * <p>由 {@code PayNotifyDispatcher} 在验签/解密后按渠道路由到本处理器，将渠道终态
 * 收敛到提现申请单。收敛动作在 {@link UnionWithdrawPayoutService#converge} 内做 CAS +
 * 幂等，重复回调安全。
 *
 * @author zhush
 */
@Slf4j
@Component
public class UnionPayNotifyHandler implements PayNotifyHandler {

    private final UnionWithdrawPayoutService payoutService;

    public UnionPayNotifyHandler(UnionWithdrawPayoutService payoutService) {
        this.payoutService = payoutService;
    }

    @Override
    public boolean supports(PayChannelType channelType, PayNotifyType notifyType) {
        // 仅处理出款（商家转账）结果回调；支付/退款回调归后续收款业务
        return channelType == PayChannelType.WECHAT_V3 && notifyType == PayNotifyType.TRANSFER;
    }

    @Override
    public void handle(PayNotifyMessage message) {
        log.info("[UnionWithdraw] 收到渠道出款回调 applyNo={}，state={}，channelOrderNo={}",
                message.getBizNo(), message.getPayStatus(), message.getChannelOrderNo());
        payoutService.converge(message.getBizNo(), message.getPayStatus(),
                message.getChannelOrderNo(), message.getSuccessTime(), message.getFailReason());
    }

}
