package com.zhush.pay.wx.v3;

import com.zhush.pay.core.model.PayStatus;
import java.util.Locale;

/**
 * 微信 V3 商家转账状态 → 统一状态 映射
 *
 * <p>渠道状态取值：{@code ACCEPTED / PROCESSING / WAIT_USER_CONFIRM / TRANSFERING /
 * SUCCESS / FAIL / CANCELING / CANCELLED}（未知值一律按「处理中」保守处置，绝不误判成功）。
 *
 * @author zhush
 */
public final class WxTransferStateMapper {

    private WxTransferStateMapper() {
    }

    public static PayStatus toPayStatus(String state) {
        if (state == null || state.isBlank()) {
            // 渠道未返回状态：保守视为处理中，等待回调/查单收敛
            return PayStatus.PROCESSING;
        }
        return switch (state.trim().toUpperCase(Locale.ROOT)) {
            case "ACCEPTED" -> PayStatus.ACCEPTED;
            case "PROCESSING" -> PayStatus.PROCESSING;
            case "WAIT_USER_CONFIRM" -> PayStatus.WAIT_USER_CONFIRM;
            case "TRANSFERING" -> PayStatus.TRANSFERING;
            case "SUCCESS" -> PayStatus.SUCCESS;
            case "FAIL" -> PayStatus.FAIL;
            case "CANCELING" -> PayStatus.CANCELING;
            case "CANCELLED" -> PayStatus.CANCELLED;
            default -> PayStatus.PROCESSING;
        };
    }

}
