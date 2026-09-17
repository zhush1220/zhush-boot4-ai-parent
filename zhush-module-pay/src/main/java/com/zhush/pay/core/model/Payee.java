package com.zhush.pay.core.model;

import lombok.Builder;
import lombok.Data;

/**
 * 统一收款方信息（协议无关）
 *
 * <p>由业务侧<b>服务端反查</b>后传入，禁止直接信任前端提交：
 * <ul>
 *     <li>{@link #openid}：出款到微信零钱时使用（V3 商家转账）；</li>
 *     <li>{@link #accountNo}/{@link #accountName}/{@link #bankName}：出款到银行卡时使用（V2 企业付款到银行卡）。</li>
 * </ul>
 *
 * @author zhush
 */
@Data
@Builder
public class Payee {

    /** 微信 openid（出款到零钱必填） */
    private String openid;

    /** 银行卡卡号（出款到银行卡必填；落库前须 SM4 密文，调用渠道时传明文由 SDK 自动 RSA 加密） */
    private String accountNo;

    /** 收款人姓名（银行卡必填） */
    private String accountName;

    /** 开户银行名称（银行卡必填，如「工商银行」） */
    private String bankName;

    /**
     * 渠道银行编码（V2 企业付款到银行卡必填，如 {@code ICBC}）
     *
     * <p>优先取业务侧显式值；为空时由渠道按 {@link #bankName} best-effort 解析。
     */
    private String bankCode;

    /** 开户支行名称（选填） */
    private String bankBranch;

}
