package com.zhush.common.utils;

import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.SM2;
import cn.hutool.crypto.symmetric.SM4;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Map;

/**
 * 国密算法工具类 —— 基于 Hutool 对 SM2 / SM3 / SM4 的二次增强封装
 *
 * <h3>SM2（非对称加密 / 签名验签）</h3>
 * <ul>
 *     <li>固定密钥模式：使用系统预置的默认公私钥对，密钥通过环境变量或系统属性配置</li>
 *     <li>动态密钥模式：调用时传入公私钥参数，灵活适配多租户、多场景</li>
 * </ul>
 *
 * <h3>SM3（摘要 / 消息认证码）</h3>
 * <ul>
 *     <li>普通摘要：无密钥的 SM3 哈希</li>
 *     <li>HMAC-SM3（固定密钥）：使用系统预置默认密钥</li>
 *     <li>HMAC-SM3（动态密钥）：调用时传入密钥</li>
 * </ul>
 *
 * <h3>SM4（对称加密）</h3>
 * <ul>
 *     <li>固定密钥模式（ECB/CBC）：使用系统预置默认密钥</li>
 *     <li>动态密钥模式（ECB/CBC）：调用时传入密钥和可选 IV</li>
 * </ul>
 *
 * <h3>默认密钥配置方式（优先级从高到低）</h3>
 * <ol>
 *     <li>系统属性：{@code -Dzhush.crypto.sm2.privateKey=xxx}</li>
 *     <li>环境变量：{@code ZHUSH_CRYPTO_SM2_PRIVATE_KEY=xxx}</li>
 *     <li>代码内硬编码默认值（仅用于开发测试，生产环境务必替换）</li>
 * </ol>
 *
 * @author zhush
 * @since 2026/6/29
 */
public class SmUtils {

    // ======================== 默认密钥配置 ========================

    /**
     * SM2 默认私钥（Base64 编码，PKCS#8 格式）
     */
    private static final String DEFAULT_SM2_PRIVATE_KEY;

    /**
     * SM2 默认公钥（Base64 编码，X509 格式）
     */
    private static final String DEFAULT_SM2_PUBLIC_KEY;

    /**
     * SM3 默认 HMAC 密钥
     */
    private static final String DEFAULT_SM3_KEY;

    /**
     * SM4 默认密钥（16 字节 / 128 位）
     */
    private static final String DEFAULT_SM4_KEY;

    /**
     * SM4 密钥覆盖值（由 Spring 配置注入，优先级高于 DEFAULT_SM4_KEY）
     */
    private static volatile String sm4KeyOverride;

    static {
        // SM2 密钥对 —— 优先读取系统属性，其次环境变量，最后使用内置默认值
        DEFAULT_SM2_PRIVATE_KEY = getConfig("zhush.crypto.sm2.privateKey",
                "ZHUSH_CRYPTO_SM2_PRIVATE_KEY", "");
        DEFAULT_SM2_PUBLIC_KEY = getConfig("zhush.crypto.sm2.publicKey",
                "ZHUSH_CRYPTO_SM2_PUBLIC_KEY", "");
        DEFAULT_SM3_KEY = getConfig("zhush.crypto.sm3.key",
                "ZHUSH_CRYPTO_SM3_KEY",
                "zhush_sm3_default_key");
        DEFAULT_SM4_KEY = getConfig("zhush.crypto.sm4.key",
                "ZHUSH_CRYPTO_SM4_KEY",
                "zhush123456789012");
    }

    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    /** 私有构造，防止实例化 */
    private SmUtils() {
    }

    // ======================================================================
    // =========================== SM2 非对称加密 ===========================
    // ======================================================================

    /**
     * 生成 SM2 密钥对
     *
     * @return Map 包含 "publicKey"（Base64）和 "privateKey"（Base64）
     */
    public static Map<String, String> generateSm2KeyPair() {
        KeyPair keyPair = SecureUtil.generateKeyPair("SM2");
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();
        return Map.of(
                "privateKey", Base64.getEncoder().encodeToString(privateKey.getEncoded()),
                "publicKey", Base64.getEncoder().encodeToString(publicKey.getEncoded())
        );
    }

    // ---------- 固定密钥（使用默认公私钥对） ----------

    /**
     * SM2 公钥加密（固定密钥）
     *
     * @param data 明文数据
     * @return Hex 编码的密文
     */
    public static String sm2Encrypt(String data) {
        checkNotBlank(DEFAULT_SM2_PUBLIC_KEY, "SM2 默认公钥未配置，请通过系统属性或环境变量设置");
        SM2 sm2 = new SM2(null, DEFAULT_SM2_PUBLIC_KEY);
        return sm2.encryptHex(StrUtil.utf8Bytes(data), KeyType.PublicKey);
    }

    /**
     * SM2 私钥解密（固定密钥）
     *
     * @param encryptedHex Hex 编码的密文
     * @return 解密后的明文
     */
    public static String sm2Decrypt(String encryptedHex) {
        checkNotBlank(DEFAULT_SM2_PRIVATE_KEY, "SM2 默认私钥未配置，请通过系统属性或环境变量设置");
        SM2 sm2 = new SM2(DEFAULT_SM2_PRIVATE_KEY, null);
        return StrUtil.utf8Str(sm2.decrypt(HexUtil.decodeHex(encryptedHex), KeyType.PrivateKey));
    }

    /**
     * SM2 私钥签名（固定密钥）
     *
     * @param data 待签名数据
     * @return Hex 编码的签名值
     */
    public static String sm2Sign(String data) {
        checkNotBlank(DEFAULT_SM2_PRIVATE_KEY, "SM2 默认私钥未配置，请通过系统属性或环境变量设置");
        SM2 sm2 = new SM2(DEFAULT_SM2_PRIVATE_KEY, null);
        return sm2.signHex(HexUtil.encodeHexStr(StrUtil.utf8Bytes(data)));
    }

    /**
     * SM2 公钥验签（固定密钥）
     *
     * @param data         原始数据
     * @param signatureHex Hex 编码的签名值
     * @return 验签是否通过
     */
    public static boolean sm2Verify(String data, String signatureHex) {
        checkNotBlank(DEFAULT_SM2_PUBLIC_KEY, "SM2 默认公钥未配置，请通过系统属性或环境变量设置");
        SM2 sm2 = new SM2(null, DEFAULT_SM2_PUBLIC_KEY);
        return sm2.verifyHex(HexUtil.encodeHexStr(StrUtil.utf8Bytes(data)), signatureHex);
    }

    // ---------- 动态密钥 ----------

    /**
     * SM2 公钥加密（动态密钥）
     *
     * @param data         明文数据
     * @param publicKeyHex 公钥 Hex 或 Base64 编码
     * @return Hex 编码的密文
     */
    public static String sm2Encrypt(String data, String publicKeyHex) {
        checkNotBlank(publicKeyHex, "SM2 公钥不能为空");
        SM2 sm2 = new SM2(null, publicKeyHex);
        return sm2.encryptHex(StrUtil.utf8Bytes(data), KeyType.PublicKey);
    }

    /**
     * SM2 私钥解密（动态密钥）
     *
     * @param encryptedHex  Hex 编码的密文
     * @param privateKeyHex 私钥 Hex 或 Base64 编码
     * @return 解密后的明文
     */
    public static String sm2Decrypt(String encryptedHex, String privateKeyHex) {
        checkNotBlank(privateKeyHex, "SM2 私钥不能为空");
        SM2 sm2 = new SM2(privateKeyHex, null);
        return StrUtil.utf8Str(sm2.decrypt(HexUtil.decodeHex(encryptedHex), KeyType.PrivateKey));
    }

    /**
     * SM2 私钥签名（动态密钥）
     *
     * @param data          待签名数据
     * @param privateKeyHex 私钥 Hex 或 Base64 编码
     * @return Hex 编码的签名值
     */
    public static String sm2Sign(String data, String privateKeyHex) {
        checkNotBlank(privateKeyHex, "SM2 私钥不能为空");
        SM2 sm2 = new SM2(privateKeyHex, null);
        return sm2.signHex(HexUtil.encodeHexStr(StrUtil.utf8Bytes(data)));
    }

    /**
     * SM2 公钥验签（动态密钥）
     *
     * @param data         原始数据
     * @param signatureHex Hex 编码的签名值
     * @param publicKeyHex 公钥 Hex 或 Base64 编码
     * @return 验签是否通过
     */
    public static boolean sm2Verify(String data, String signatureHex, String publicKeyHex) {
        checkNotBlank(publicKeyHex, "SM2 公钥不能为空");
        SM2 sm2 = new SM2(null, publicKeyHex);
        return sm2.verifyHex(HexUtil.encodeHexStr(StrUtil.utf8Bytes(data)), signatureHex);
    }

    // ======================================================================
    // =========================== SM3 摘要算法 =============================
    // ======================================================================

    /**
     * SM3 普通摘要（无密钥）
     *
     * @param data 原始数据
     * @return Hex 编码的摘要值
     */
    public static String sm3(String data) {
        return SmUtil.sm3().digestHex(data);
    }

    /**
     * SM3 普通摘要（无密钥），直接返回字节数组
     *
     * @param data 原始数据
     * @return 摘要字节数组
     */
    public static byte[] sm3Bytes(String data) {
        return SmUtil.sm3().digest(StrUtil.utf8Bytes(data));
    }

    /**
     * HMAC-SM3 消息认证码（固定密钥）
     *
     * @param data 原始数据
     * @return Hex 编码的 MAC 值
     */
    public static String sm3Hmac(String data) {
        return sm3Hmac(data, DEFAULT_SM3_KEY);
    }

    /**
     * HMAC-SM3 消息认证码（动态密钥）
     *
     * @param data 原始数据
     * @param key  HMAC 密钥
     * @return Hex 编码的 MAC 值
     */
    public static String sm3Hmac(String data, String key) {
        checkNotBlank(key, "SM3 HMAC 密钥不能为空");
        return SmUtil.hmacSm3(StrUtil.bytes(key, DEFAULT_CHARSET)).digestHex(data);
    }


    /**
     * 校验 SM3 摘要是否一致
     *
     * @param data      原始数据
     * @param hexDigest 待校验的 Hex 摘要
     * @return 是否匹配
     */
    public static boolean sm3Verify(String data, String hexDigest) {
        return sm3(data).equalsIgnoreCase(hexDigest);
    }

    /**
     * 校验 HMAC-SM3 是否一致（动态密钥）
     *
     * @param data   原始数据
     * @param key    HMAC 密钥
     * @param hexMac 待校验的 Hex MAC 值
     * @return 是否匹配
     */
    public static boolean sm3HmacVerify(String data, String key, String hexMac) {
        return sm3Hmac(data, key).equalsIgnoreCase(hexMac);
    }

    // ======================================================================
    // =========================== SM4 对称加密 =============================
    // ======================================================================

    // ---------- 固定密钥（ECB 模式） ----------

    /**
     * SM4/ECB 加密（固定密钥）
     *
     * @param data 明文
     * @return Hex 编码的密文
     */
    public static String sm4EncryptEcb(String data) {
        return sm4EncryptEcb(data, getEffectiveSm4Key());
    }

    /**
     * SM4/ECB 解密（固定密钥）
     *
     * @param encryptedHex Hex 编码的密文
     * @return 解密后的明文
     */
    public static String sm4DecryptEcb(String encryptedHex) {
        return sm4DecryptEcb(encryptedHex, getEffectiveSm4Key());
    }

    // ---------- 动态密钥（ECB 模式） ----------

    /**
     * SM4/ECB 加密（动态密钥）
     *
     * @param data 明文
     * @param key  密钥（16 字节）
     * @return Hex 编码的密文
     */
    public static String sm4EncryptEcb(String data, String key) {
        checkNotBlank(key, "SM4 密钥不能为空");
        SM4 sm4 = new SM4(StrUtil.bytes(key, DEFAULT_CHARSET));
        return sm4.encryptHex(data, DEFAULT_CHARSET);
    }

    /**
     * SM4/ECB 解密（动态密钥）
     *
     * @param encryptedHex Hex 编码的密文
     * @param key          密钥（16 字节）
     * @return 解密后的明文
     */
    public static String sm4DecryptEcb(String encryptedHex, String key) {
        checkNotBlank(key, "SM4 密钥不能为空");
        SM4 sm4 = new SM4(StrUtil.bytes(key, DEFAULT_CHARSET));
        return sm4.decryptStr(encryptedHex, DEFAULT_CHARSET);
    }

    // ---------- 固定密钥（CBC 模式） ----------

    /**
     * SM4/CBC 加密（固定密钥，默认 IV）
     *
     * @param data 明文
     * @return Hex 编码的密文
     */
    public static String sm4EncryptCbc(String data) {
        return sm4EncryptCbc(data, getEffectiveSm4Key(), getEffectiveSm4Key());
    }

    /**
     * SM4/CBC 解密（固定密钥，默认 IV）
     *
     * @param encryptedHex Hex 编码的密文
     * @return 解密后的明文
     */
    public static String sm4DecryptCbc(String encryptedHex) {
        return sm4DecryptCbc(encryptedHex, getEffectiveSm4Key(), getEffectiveSm4Key());
    }

    // ---------- 动态密钥（CBC 模式） ----------

    /**
     * SM4/CBC 加密（动态密钥 + 动态 IV）
     *
     * @param data 明文
     * @param key  密钥（16 字节）
     * @param iv   偏移向量（16 字节）
     * @return Hex 编码的密文
     */
    public static String sm4EncryptCbc(String data, String key, String iv) {
        checkNotBlank(key, "SM4 密钥不能为空");
        checkNotBlank(iv, "SM4 IV 不能为空");
        SM4 sm4 = new SM4(Mode.CBC, Padding.PKCS5Padding,
                StrUtil.bytes(key, DEFAULT_CHARSET), StrUtil.bytes(iv, DEFAULT_CHARSET));
        return sm4.encryptHex(data, DEFAULT_CHARSET);
    }

    /**
     * SM4/CBC 解密（动态密钥 + 动态 IV）
     *
     * @param encryptedHex Hex 编码的密文
     * @param key          密钥（16 字节）
     * @param iv           偏移向量（16 字节）
     * @return 解密后的明文
     */
    public static String sm4DecryptCbc(String encryptedHex, String key, String iv) {
        checkNotBlank(key, "SM4 密钥不能为空");
        checkNotBlank(iv, "SM4 IV 不能为空");
        SM4 sm4 = new SM4(Mode.CBC, Padding.PKCS5Padding,
                StrUtil.bytes(key, DEFAULT_CHARSET), StrUtil.bytes(iv, DEFAULT_CHARSET));
        return sm4.decryptStr(encryptedHex, DEFAULT_CHARSET);
    }

    // ======================================================================
    // =========================== 工具方法 =================================
    // ======================================================================

    /**
     * 生成 SM4 随机密钥（16 字节，Hex 编码）
     *
     * @return Hex 编码的 16 字节密钥
     */
    public static String generateSm4Key() {
        return HexUtil.encodeHexStr(SecureUtil.generateKey("SM4", 128).getEncoded());
    }

    /**
     * 获取默认 SM2 公钥
     *
     * @return 默认公钥
     */
    public static String getDefaultSm2PublicKey() {
        return DEFAULT_SM2_PUBLIC_KEY;
    }

    /**
     * 获取默认 SM2 私钥
     *
     * @return 默认私钥
     */
    public static String getDefaultSm2PrivateKey() {
        return DEFAULT_SM2_PRIVATE_KEY;
    }

    /**
     * 获取默认 SM4 密钥
     *
     * @return 默认密钥字符串
     */
    public static String getDefaultSm4Key() {
        return DEFAULT_SM4_KEY;
    }

    /**
     * 覆盖 SM4 密钥（由 Spring 配置注入调用）
     * <p>
     * 调用后，所有使用默认密钥的 SM4 加解密方法将使用此密钥，
     * 优先级：override 值 > 系统属性/环境变量 > 硬编码默认值。
     *
     * @param key 新的 SM4 密钥（16 字节）
     */
    public static void overrideSm4Key(String key) {
        sm4KeyOverride = key;
    }

    /**
     * 获取当前生效的 SM4 密钥
     * <p>
     * 优先级：Spring 配置覆盖值 > 系统属性/环境变量默认值
     *
     * @return 当前生效的密钥
     */
    private static String getEffectiveSm4Key() {
        return StrUtil.isNotBlank(sm4KeyOverride) ? sm4KeyOverride : DEFAULT_SM4_KEY;
    }

    // ======================================================================
    // ========================= 内部辅助方法 ===============================
    // ======================================================================

    /**
     * 读取配置值，优先级：系统属性 > 环境变量 > 默认值
     */
    private static String getConfig(String propertyKey, String envKey, String defaultValue) {
        String value = System.getProperty(propertyKey);
        if (StrUtil.isNotBlank(value)) {
            return value;
        }
        value = System.getenv(envKey);
        if (StrUtil.isNotBlank(value)) {
            return value;
        }
        return defaultValue;
    }

    /**
     * 校验字符串不为空
     */
    private static void checkNotBlank(String str, String message) {
        if (StrUtil.isBlank(str)) {
            throw new IllegalArgumentException(message);
        }
    }
}
