package com.zhush.pay.wx.v3;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.github.binarywang.wxpay.bean.transfer.TransferBillsCancelResult;
import com.github.binarywang.wxpay.bean.transfer.TransferBillsGetResult;
import com.github.binarywang.wxpay.bean.transfer.TransferBillsRequest;
import com.github.binarywang.wxpay.bean.transfer.TransferBillsResult;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

import static com.zhush.common.exception.ServiceExceptionUtil.assertTrue;

/**
 * 微信 V3 出款渠道：商家转账（transfer-bills）
 *
 * <p>实现 {@link PayoutChannel}，由 {@code PayGateway} 按 {@link PayChannelType#WECHAT_V3} 路由。
 *
 * <p><b>红线</b>：
 * <ul>
 *     <li>只把渠道「受理」映射为 {@link PayStatus#ACCEPTED}，绝不直接判定 {@link PayStatus#SUCCESS}；</li>
 *     <li>渠道异常一律返回 {@code accepted=false}，不得伪造成受理。</li>
 * </ul>
 *
 * @author zhush
 */
@Slf4j
public class WxV3PayoutChannel implements PayoutChannel {

    private final WxTransferClient client;
    private final WxPayProperties properties;

    public WxV3PayoutChannel(WxTransferClient client, WxPayProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public PayChannelType channelType() {
        return PayChannelType.WECHAT_V3;
    }

    @Override
    public PayoutResult payout(PayoutCommand command) {
        validate(command);
        validateLimit(command.getAmount());

        WxPayProperties.V3 v3 = properties.getV3();
        TransferBillsRequest request = new TransferBillsRequest();
        request.setAppid(StrUtil.blankToDefault(command.getAppId(), v3.getAppId()));
        request.setOutBillNo(command.getBizNo());
        request.setTransferSceneId(v3.getTransferSceneId());
        request.setOpenid(command.getPayee().getOpenid());
        request.setTransferAmount(command.getAmount());
        request.setTransferRemark(StrUtil.blankToDefault(command.getRemark(), v3.getTransferRemark()));
        request.setNotifyUrl(StrUtil.blankToDefault(command.getNotifyUrl(), properties.getNotifyUrl()));
        request.setUserRecvPerception(StrUtil.blankToDefault(command.getUserRecvPerception(), v3.getUserRecvPerception()));
        request.setReceiptAuthorizationMode(v3.getReceiptAuthorizationMode());
        request.setTransferSceneReportInfos(buildSceneReportInfos(command, v3));

        try {
            TransferBillsResult result = client.transfer(request);
            PayStatus state = WxTransferStateMapper.toPayStatus(result.getState());
            PayoutResult payoutResult = PayoutResult.builder()
                    .accepted(state != PayStatus.FAIL)
                    .finalState(state)
                    .channelOrderNo(result.getTransferBillNo())
                    .channelState(result.getState())
                    .packageInfo(result.getPackageInfo())
                    .failReason(result.getFailReason())
                    .build();
            log.info("[WxV3] 商家转账受理, bizNo={}, state={}, transferBillNo={}",
                    command.getBizNo(), state, result.getTransferBillNo());
            return payoutResult;
        } catch (WxPayException e) {
            log.error("[WxV3] 商家转账调用失败, bizNo={}, reason={}", command.getBizNo(), describeSafe(e));
            return WxPayExceptionTranslator.toFailResult(e);
        }
    }

    @Override
    public PayoutResult query(String bizNo) {
        try {
            TransferBillsGetResult result = client.queryByOutBillNo(bizNo);
            PayStatus state = WxTransferStateMapper.toPayStatus(result.getState());
            return PayoutResult.builder()
                    // 已取得渠道明确状态：accepted=true，是否终态/失败一律以 finalState 为准
                    .accepted(true)
                    .finalState(state)
                    .channelOrderNo(result.getTransferBillNo())
                    .channelState(result.getState())
                    .failReason(result.getFailReason())
                    .build();
        } catch (WxPayException e) {
            // 查单调用失败：结果未知，accepted=false，业务侧不得据此判定失败
            log.error("[WxV3] 商家转账查单失败, bizNo={}, reason={}", bizNo, describeSafe(e));
            return WxPayExceptionTranslator.toFailResult(e);
        }
    }

    @Override
    public PayoutResult cancel(String bizNo) {
        try {
            TransferBillsCancelResult result = client.cancel(bizNo);
            PayStatus state = WxTransferStateMapper.toPayStatus(result.getState());
            return PayoutResult.builder()
                    .accepted(true)
                    .finalState(state)
                    .channelOrderNo(result.getTransferBillNo())
                    .channelState(result.getState())
                    .build();
        } catch (WxPayException e) {
            log.error("[WxV3] 商家转账撤单失败, bizNo={}, reason={}", bizNo, describeSafe(e));
            return WxPayExceptionTranslator.toFailResult(e);
        }
    }

    @Override
    public boolean supportCancel() {
        return true;
    }

    // ================ 内部方法 ================

    private void validate(PayoutCommand command) {
        assertTrue(command != null && StrUtil.isNotBlank(command.getBizNo()),
                PayErrorCode.PAYEE_INVALID, "业务单号缺失");
        assertTrue(command.getAmount() != null && command.getAmount() > 0,
                PayErrorCode.PAY_AMOUNT_INVALID, command.getAmount());
        Payee payee = command.getPayee();
        assertTrue(payee != null && StrUtil.isNotBlank(payee.getOpenid()),
                PayErrorCode.PAYEE_INVALID, "openid 缺失");
    }

    /**
     * 单笔限额校验（按配置；未配置则不限制）
     */
    private void validateLimit(Integer amount) {
        WxPayProperties.AmountRange range = properties.getLimit().getWechatV3();
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

    /**
     * 组装转账场景报备信息：命令级优先，否则用配置
     */
    private List<TransferBillsRequest.TransferSceneReportInfo> buildSceneReportInfos(
            PayoutCommand command, WxPayProperties.V3 v3) {
        List<TransferBillsRequest.TransferSceneReportInfo> infos = new ArrayList<>();
        Map<String, String> commandInfos = command.getSceneReportInfos();
        if (CollUtil.isNotEmpty(commandInfos)) {
            commandInfos.forEach((type, content) ->
                    infos.add(new TransferBillsRequest.TransferSceneReportInfo(type, content)));
            return infos;
        }
        for (WxPayProperties.SceneReportInfo info : v3.getTransferSceneReportInfos()) {
            if (StrUtil.isNotBlank(info.getInfoType())) {
                infos.add(new TransferBillsRequest.TransferSceneReportInfo(info.getInfoType(), info.getInfoContent()));
            }
        }
        return infos;
    }

    /**
     * 脱敏描述渠道异常
     */
    private String describeSafe(WxPayException e) {
        return WxPayExceptionTranslator.describe(e);
    }

}
