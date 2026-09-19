package com.zhush.pay.core.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * 金额工具：元 ↔ 分 安全转换
 *
 * <p>支付全链路金额统一以「分」为单位（{@code Integer}），本工具负责业务边界的元/分换算，
 * 禁止在 pay 内部出现浮点金额，杜绝 {@code 286.00} 被误传为 {@code 286} 或精度丢失。
 *
 * @author zhush
 */
public final class MoneyUtil {

    private MoneyUtil() {
    }

    /**
     * 元 → 分（四舍五入，溢出即抛异常）
     *
     * @param yuan 金额（元），如 {@code new BigDecimal("86.00")}
     * @return 金额（分），如 {@code 8600}
     */
    public static int yuanToFen(BigDecimal yuan) {
        Objects.requireNonNull(yuan, "金额不可为空");
        return yuan.movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValueExact();
    }

    /**
     * 分 → 元
     *
     * @param fen 金额（分）
     * @return 金额（元），保留两位小数
     */
    public static BigDecimal fenToYuan(int fen) {
        return BigDecimal.valueOf(fen, 2);
    }

}
