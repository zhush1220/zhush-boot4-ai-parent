package com.zhush.pay.core.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一支付状态（协议无关）
 *
 * <p><b>红线</b>：{@link #ACCEPTED}（渠道已受理）与 {@link #SUCCESS}（资金到账终态）
 * 必须严格区分——同步返回的「受理成功」<b>不得</b>当作资金到账。
 *
 * @author zhush
 */
@Getter
@AllArgsConstructor
public enum PayStatus {

    /** 渠道已受理（非终态，等待结果） */
    ACCEPTED("已受理"),

    /** 渠道处理中（非终态） */
    PROCESSING("处理中"),

    /** 等待收款用户确认收款（V3 商家转账特有，非终态） */
    WAIT_USER_CONFIRM("待用户确认"),

    /** 转账中（非终态） */
    TRANSFERING("转账中"),

    /** 成功（终态，资金已到账） */
    SUCCESS("成功"),

    /** 失败（终态） */
    FAIL("失败"),

    /** 撤销中（非终态，<b>不得</b>据此解冻） */
    CANCELING("撤销中"),

    /** 已撤销（终态，可据此解冻/回滚） */
    CANCELLED("已撤销"),

    ;

    private final String description;

    /**
     * 是否为终态（成功/失败/已撤销）
     */
    public boolean isFinal() {
        return this == SUCCESS || this == FAIL || this == CANCELLED;
    }

}
