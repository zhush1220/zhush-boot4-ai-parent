package com.zhush.union.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 提现申请状态（对应 union_withdraw_apply.status）
 *
 * <p>{@link #PAYING}（7-打款处理中）为出款编排新增的中间态：渠道已受理但结果未定，
 * <b>不得</b>据此落账/解冻；只有收敛到 {@link #PAID}（3）、{@link #PAY_FAILED}（6）、
 * {@link #CANCELLED}（5）等终态才允许做资金处理。
 *
 * @author zhush
 */
@Getter
@AllArgsConstructor
public enum UnionWithdrawStatus {

    /** 待审核 */
    WAIT_AUDIT(1, "待审核"),

    /** 审核通过（可发起打款） */
    AUDITED(2, "审核通过"),

    /** 已打款（终态：资金已到账） */
    PAID(3, "已打款"),

    /** 已驳回（终态） */
    REJECTED(4, "已驳回"),

    /** 已取消（终态） */
    CANCELLED(5, "已取消"),

    /** 打款失败（终态，可重试） */
    PAY_FAILED(6, "打款失败"),

    /** 打款处理中（非终态：渠道已受理，等待回调/查单收敛） */
    PAYING(7, "打款处理中"),

    ;

    private final int code;
    private final String description;

    /**
     * 按状态码解析，未匹配返回 {@code null}
     */
    public static UnionWithdrawStatus of(Integer code) {
        if (code == null) {
            return null;
        }
        for (UnionWithdrawStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }

    /**
     * 是否允许发起/重试打款（审核通过、打款失败）
     */
    public static boolean canPayout(Integer code) {
        return AUDITED.is(code) || PAY_FAILED.is(code);
    }

    /**
     * 是否等于指定状态码
     */
    public boolean is(Integer code) {
        return code != null && code == this.code;
    }

}
