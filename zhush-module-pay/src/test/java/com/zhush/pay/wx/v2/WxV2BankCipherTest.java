package com.zhush.pay.wx.v2;

import com.zhush.common.exception.ServiceException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.util.Base64;
import javax.crypto.Cipher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 微信 V2 银行卡敏感字段 RSA-OAEP 加密测试
 *
 * <p>用自生成的 RSA 公钥加密，再用配套私钥解密验证往返正确（口径与微信一致：OAEP-SHA1/MGF1-SHA1）。
 */
class WxV2BankCipherTest {

    private PrivateKey privateKey;
    private String publicKeyPem;

    private void genKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        this.privateKey = pair.getPrivate();
        this.publicKeyPem = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
    }

    private String decrypt(String cipherTextB64) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] plain = cipher.doFinal(Base64.getDecoder().decode(cipherTextB64));
        return new String(plain, java.nio.charset.StandardCharsets.UTF_8);
    }

    @Test
    void encrypt_isOaepDecryptable() throws Exception {
        genKey();
        String plain = "6222021234567890123";
        String enc = WxV2BankCipher.encrypt(publicKeyPem, plain);

        assertNotEquals(plain, enc);
        assertTrue(enc.matches("[A-Za-z0-9+/=]+"), "密文应为 Base64");
        assertEquals(plain, decrypt(enc));
    }

    @Test
    void encrypt_blankPassThrough() {
        assertEquals("", WxV2BankCipher.encrypt("whatever", ""));
        assertEquals(null, WxV2BankCipher.encrypt("whatever", null));
    }

    @Test
    void encrypt_invalidKey_throwsServiceException() {
        assertThrows(ServiceException.class, () -> WxV2BankCipher.encrypt("not-a-valid-key", "6222021234"));
    }

    @Test
    void encrypt_acceptsPemWithHeaders() throws Exception {
        genKey();
        // 同一公钥 DER，包成带 PEM 头的格式，验证解析容错
        String pem = "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(
                        Base64.getDecoder().decode(publicKeyPem))
                + "\n-----END PUBLIC KEY-----";
        String enc = WxV2BankCipher.encrypt(pem, "张三");
        assertEquals("张三", decrypt(enc));
    }
}
