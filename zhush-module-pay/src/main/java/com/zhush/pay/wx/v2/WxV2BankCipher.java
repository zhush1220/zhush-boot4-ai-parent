package com.zhush.pay.wx.v2;

import cn.hutool.core.util.StrUtil;
import com.zhush.pay.enums.PayErrorCode;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.Cipher;

import static com.zhush.common.exception.ServiceExceptionUtil.exception;

/**
 * 微信 V2 企业付款到银行卡敏感字段加密（RSA/ECB/OAEPWithSHA-1AndMGF1Padding）
 *
 * <p><b>重要</b>：SDK 的 {@code EntPayBankRequest} 只暴露 {@code encBankNo}/{@code encTrueName}
 * 两个密文字段，<b>不会</b>自动加密；调用方必须用「微信支付公钥」（{@code getPublicKey()}）
 * 对银行卡号、收款人姓名做 RSAES-OAEP(SHA-1, MGF1-SHA1) 加密后 Base64 传入，否则网关直接拒单。
 *
 * <p>安全纪律：明文卡号/姓名仅在方法内瞬时存在，禁止落日志；公钥字符串同样禁止输出。
 *
 * @author zhush
 */
public final class WxV2BankCipher {

    private static final String ALGORITHM = "RSA";
    private static final String TRANSFORMATION = "RSA/ECB/OAEPWithSHA-1AndMGF1Padding";
    private static final String PEM_BEGIN = "-----BEGIN PUBLIC KEY-----";
    private static final String PEM_END = "-----END PUBLIC KEY-----";

    private WxV2BankCipher() {
    }

    /**
     * 用微信支付公钥（PEM 或纯 Base64 DER）加密敏感字段，返回 Base64 密文。
     *
     * @param publicKeyPem 微信支付公钥（{@code EntPayService#getPublicKey()} 返回值）
     * @param plain        明文（银行卡号 / 收款人姓名）
     */
    public static String encrypt(String publicKeyPem, String plain) {
        if (StrUtil.isBlank(plain)) {
            return plain;
        }
        try {
            PublicKey publicKey = parsePublicKey(publicKeyPem);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encrypted = cipher.doFinal(plain.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            // 不回显明文与公钥，仅报加密失败
            throw exception(PayErrorCode.WX_PAY_CALL_FAIL, "银行卡敏感信息加密失败");
        }
    }

    private static PublicKey parsePublicKey(String publicKeyPem) throws Exception {
        String body = publicKeyPem
                .replace(PEM_BEGIN, "")
                .replace(PEM_END, "")
                .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(body);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(der);
        return KeyFactory.getInstance(ALGORITHM).generatePublic(spec);
    }

}
