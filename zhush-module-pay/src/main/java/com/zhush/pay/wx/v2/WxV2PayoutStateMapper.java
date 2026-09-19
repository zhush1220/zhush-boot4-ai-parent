package com.zhush.pay.wx.v2;

import cn.hutool.core.util.StrUtil;
import com.zhush.pay.core.model.PayStatus;

/**
 * 微信 V2 企业付款查单状态 → 统一 {@link PayStatus} 映射
 *
 * <p>V2 出款<b>无结果回调</b>，终态只能靠 {@code queryEntPay}/{@code queryPayBank} 收敛。
 * 两条接口状态取值不完全一致，统一按下表归并（未知/空一律 PROCESSING，保守非终态）：
 * <ul>
 *     <li>SUCCESS → {@link PayStatus#SUCCESS}（到账终态）；</li>
 *     <li>FAILED / FAIL / DELETED / CHANGE（退票资金已退回商户）→ {@link PayStatus#FAIL}；</li>
 *     <li>ING / PROCESSING / 其他 → {@link PayStatus#PROCESSING}。</li>
 * </ul>
 *
 * @author zhush
 */
public final class WxV2PayoutStateMapper {

    private WxV2PayoutStateMapper() {
    }

    public static PayStatus toPayStatus(String rawStatus) {
        if (StrUtil.isBlank(rawStatus)) {
            return PayStatus.PROCESSING;
        }
        switch (rawStatus.trim().toUpperCase()) {
            case "SUCCESS":
                return PayStatus.SUCCESS;
            case "FAILED":
            case "FAIL":
            case "DELETED":
            case "CHANGE":
                return PayStatus.FAIL;
            default:
                // ING / PROCESSING / 未知态：保守视为非终态，交查单/对账继续收敛
                return PayStatus.PROCESSING;
        }
    }

}
