package com.zhush.pay.core;

import com.zhush.pay.core.model.PayChannelType;
import com.zhush.pay.core.model.PaymentCommand;
import com.zhush.pay.core.model.PaymentResult;
import com.zhush.pay.core.model.PayoutCommand;
import com.zhush.pay.core.model.PayoutResult;
import com.zhush.pay.core.model.RefundCommand;
import com.zhush.pay.core.model.RefundResult;

/**
 * 统一支付门面（协议无关）
 *
 * <p>业务侧只依赖本接口：按 {@link PayoutCommand#getChannelType()} 自动路由到对应
 * {@code PayoutChannel} 实现，业务代码不感知 V2/V3/微信。
 *
 * <p>新增业务（分润出款、会员提现…）无需改动 pay；新增渠道只需实现渠道 SPI。
 *
 * @author zhush
 */
public interface PayGateway {

    /**
     * 发起出款（幂等：同一 {@code bizNo} 只产生一笔渠道账）
     */
    PayoutResult payout(PayoutCommand command);

    /**
     * 查询出款结果（终态收敛；V2 无回调时唯一手段）
     */
    PayoutResult queryPayout(PayChannelType channelType, String bizNo);

    /**
     * 撤销出款（返回状态为终态「已撤销」时方可解冻/回滚）
     */
    PayoutResult cancelPayout(PayChannelType channelType, String bizNo);

    /**
     * 指定渠道是否支持撤单
     */
    boolean supportCancel(PayChannelType channelType);

    // ==================== 收款 / 退款（三期） ====================

    /**
     * 发起收款（下单，幂等：同一 {@code bizNo} 只产生一笔渠道预支付）。
     * <p>返回结果中的 {@code payParams} 为前端拉起支付所需参数（如 JSAPI 签名包）。
     */
    PaymentResult pay(PaymentCommand command);

    /**
     * 查询收款结果（终态收敛）
     */
    PaymentResult queryPayment(PayChannelType channelType, String bizNo);

    /**
     * 发起退款（幂等：同一退款业务单号）
     */
    RefundResult refund(RefundCommand command);

    /**
     * 查询退款结果（退款为异步，需查单收敛）
     */
    RefundResult queryRefund(PayChannelType channelType, String refundBizNo);

    /**
     * 退差价 / 部分退款：对同一原支付单退指定金额（单位：分）。
     *
     * <p>幂等以 {@code refundBizNo}（渠道 out_refund_no）为键，V2/V3 语义一致；底座内部查原单总额
     * 并组装退款请求，业务侧无需重复查单。同一原单可多次退差价，累计不超过原单实付额（由业务侧账本校验）。
     *
     * @param channelType  渠道协议（与原支付同协议）
     * @param originBizNo  原支付业务单号
     * @param refundBizNo  本次退款业务单号（唯一）
     * @param amountFen    本次退款额（分）
     * @param reason       退款原因（用户可见）
     */
    default RefundResult refundDifference(PayChannelType channelType, String originBizNo,
                                          String refundBizNo, int amountFen, String reason) {
        return refund(RefundCommand.builder()
                .channelType(channelType)
                .originBizNo(originBizNo)
                .bizNo(refundBizNo)
                .amount(amountFen)
                .reason(reason)
                .build());
    }

}
