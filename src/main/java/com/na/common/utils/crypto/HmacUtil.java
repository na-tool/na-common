package com.na.common.utils.crypto;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * HMAC 签名工具类(支持 HmacSHA256 / HmacSHA1 / HmacMD5)
 * <p>
 * HMAC 带密钥的哈希, 适用于接口签名、防篡改验证; 同一密钥+消息签名结果恒定
 * <p>
 * 使用示例:
 * <pre>{@code
 * // 签名
 * String sign = HmacUtil.hmacSha256("data", "secretKey");
 *
 * // Base64 格式签名
 * String signB64 = HmacUtil.hmacSha256Base64("data", "secretKey");
 *
 * * 验签
 * boolean valid = HmacUtil.verify("data", "secretKey", sign);
 * }</pre>
 */
public final class HmacUtil {

    /** 纯静态工具类, 禁止实例化 */
    private HmacUtil() {
    }

    /** HmacSHA256 算法名 */
    private static final String HMAC_SHA256 = "HmacSHA256";

    /** HmacSHA1 算法名 */
    private static final String HMAC_SHA1 = "HmacSHA1";

    /** HmacMD5 算法名 */
    private static final String HMAC_MD5 = "HmacMD5";

    /**
     * HmacSHA256 签名(小写十六进制)
     *
     * @param data 待签名数据
     * @param key  密钥
     * @return 64 位小写十六进制签名; 入参为 null 时返回空串
     */
    public static String hmacSha256(String data, String key) {
        return hmac(data, key, HMAC_SHA256, false);
    }

    /**
     * HmacSHA256 签名(Base64 编码)
     *
     * @param data 待签名数据
     * @param key  密钥
     * @return Base64 编码签名; 入参为 null 时返回空串
     */
    public static String hmacSha256Base64(String data, String key) {
        return hmac(data, key, HMAC_SHA256, true);
    }

    /**
     * HmacSHA1 签名(小写十六进制)
     *
     * @param data 待签名数据
     * @param key  密钥
     * @return 40 位小写十六进制签名; 入参为 null 时返回空串
     */
    public static String hmacSha1(String data, String key) {
        return hmac(data, key, HMAC_SHA1, false);
    }

    /**
     * HmacMD5 签名(小写十六进制)
     *
     * @param data 待签名数据
     * @param key  密钥
     * @return 32 位小写十六进制签名; 入参为 null 时返回空串
     */
    public static String hmacMd5(String data, String key) {
        return hmac(data, key, HMAC_MD5, false);
    }

    /**
     * 验证 HmacSHA256 签名(恒定时间比较, 防时序攻击)
     *
     * @param data       原始数据
     * @param key        密钥
     * @param signature  待验证的十六进制签名
     * @return true=签名一致
     */
    public static boolean verify(String data, String key, String signature) {
        if (data == null || key == null || signature == null) {
            return false;
        }
        String computed = hmacSha256(data, key);
        return CryptoSupport.constantTimeEquals(computed, signature);
    }

    /**
     * HMAC 签名核心方法
     *
     * @param data     待签名数据
     * @param key      密钥
     * @param algorithm 算法名(HmacSHA256 / HmacSHA1 / HmacMD5)
     * @param base64    true=Base64 编码, false=十六进制
     * @return 签名结果; 入参为 null 时返回空串
     */
    private static String hmac(String data, String key, String algorithm, boolean base64) {
        if (data == null || key == null) {
            return "";
        }
        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), algorithm));
            byte[] result = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return base64
                    ? Base64.getEncoder().encodeToString(result)
                    : CryptoSupport.toHex(result);
        } catch (Exception e) {
            throw new RuntimeException("HMAC签名失败[" + algorithm + "]: " + e.getMessage(), e);
        }
    }
}
