package com.na.common.utils.crypto;

import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * SM3 国密密码哈希工具
 * <p>
 * SM3 是中国国家标准密码哈希算法, 256 位摘要, 安全强度等同 SHA-256
 * <p>
 * <b>使用场景:</b>
 * <ul>
 *   <li>数据完整性校验: 文件/消息摘要, 检测篡改</li>
 *   <li>口令存储: 加盐哈希存储用户密码(配合随机盐)</li>
 *   <li>数字签名: SM2 签名前对原文做 SM3 摘要, 缩短签名数据量</li>
 *   <li>HMAC-SM3: 带密钥的消息认证码, 用于接口签名防篡改</li>
 *   <li>国密合规: 替代 MD5/SHA-256, 满足金融政务合规要求</li>
 * </ul>
 * <p>
 * 使用示例:
 * <pre>{@code
 * // 基础哈希
 * String hash = SM3Util.hash("hello");
 *
 * // 加盐哈希(防彩虹表)
 * String salted = SM3Util.hashWithSalt("password", "randomSalt");
 *
 * * HMAC-SM3(带密钥的签名)
 * String hmac = SM3Util.hmacSm3("data", "secretKey");
 * }</pre>
 */
public final class SM3Util {

    /** 纯静态工具类, 禁止实例化 */
    private SM3Util() {
    }

    /** Base64 编码器 */
    private static final Base64.Encoder ENCODER = Base64.getEncoder();

    static {
        // 注册 BouncyCastle Provider
        java.security.Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * 计算 SM3 哈希(64 位小写十六进制)
     *
     * @param input 原文
     * @return 64 位小写十六进制哈希; input 为 null 时返回空串
     */
    public static String hash(String input) {
        if (input == null) {
            return "";
        }
        byte[] digest = doHash(input.getBytes(StandardCharsets.UTF_8));
        return toHex(digest);
    }

    /**
     * 计算 SM3 哈希(Base64 编码)
     *
     * @param input 原文
     * @return Base64 编码哈希; input 为 null 时返回空串
     */
    public static String hashBase64(String input) {
        if (input == null) {
            return "";
        }
        byte[] digest = doHash(input.getBytes(StandardCharsets.UTF_8));
        return ENCODER.encodeToString(digest);
    }

    /**
     * 加盐 SM3 哈希(防彩虹表攻击)
     * <p>
     * 格式: SM3(salt + input), salt 为 16 字节随机数
     *
     * @param input 原文
     * @param salt  盐值(随机字符串)
     * @return 64 位小写十六进制哈希; 入参为 null 时返回空串
     */
    public static String hashWithSalt(String input, String salt) {
        if (input == null || salt == null) {
            return "";
        }
        byte[] data = (salt + input).getBytes(StandardCharsets.UTF_8);
        return toHex(doHash(data));
    }

    /**
     * 生成随机盐值(16 字节, Base64 编码)
     * <p>
     * 用于口令存储: hash = SM3(salt + password), 数据库存 salt + hash
     *
     * @return Base64 编码的 16 字节盐值
     */
    public static String generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return ENCODER.encodeToString(salt);
    }

    /**
     * HMAC-SM3 签名(带密钥的哈希, 64 位小写十六进制)
     * <p>
     * 类似 HMAC-SHA256, 用于接口签名防篡改
     *
     * @param data 待签名数据
     * @param key  密钥
     * @return 64 位小写十六进制签名; 入参为 null 时返回空串
     */
    public static String hmacSm3(String data, String key) {
        if (data == null || key == null) {
            return "";
        }
        // HMAC 构造: SM3(key XOR opad || SM3(key XOR ipad || data))
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);

        // ipad / opad
        byte[] ipad = new byte[64];
        byte[] opad = new byte[64];
        for (int i = 0; i < 64; i++) {
            byte k = i < keyBytes.length ? keyBytes[i] : 0;
            ipad[i] = (byte) (k ^ 0x36);
            opad[i] = (byte) (k ^ 0x5c);
        }

        // inner = SM3(ipad || data)
        byte[] innerInput = new byte[64 + dataBytes.length];
        System.arraycopy(ipad, 0, innerInput, 0, 64);
        System.arraycopy(dataBytes, 0, innerInput, 64, dataBytes.length);
        byte[] inner = doHash(innerInput);

        // outer = SM3(opad || inner)
        byte[] outerInput = new byte[64 + inner.length];
        System.arraycopy(opad, 0, outerInput, 0, 64);
        System.arraycopy(inner, 0, outerInput, 64, inner.length);
        return toHex(doHash(outerInput));
    }

    /**
     * 验证 HMAC-SM3 签名(恒定时间比较, 防时序攻击)
     *
     * @param data      原始数据
     * @param key       密钥
     * @param signature 待验证的十六进制签名
     * @return true=签名一致
     */
    public static boolean verifyHmac(String data, String key, String signature) {
        if (data == null || key == null || signature == null) {
            return false;
        }
        String computed = hmacSm3(data, key);
        return constantTimeEquals(computed, signature);
    }

    // === 内部辅助方法 ===

    /**
     * 执行 SM3 摘要计算
     *
     * @param data 原始字节
     * @return 32 字节(256 位)摘要
     */
    private static byte[] doHash(byte[] data) {
        SM3Digest digest = new SM3Digest();
        digest.update(data, 0, data.length);
        byte[] output = new byte[digest.getDigestSize()];
        digest.doFinal(output, 0);
        return output;
    }

    /**
     * 字节数组转小写十六进制字符串
     *
     * @param bytes 字节数组
     * @return 小写十六进制字符串
     */
    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    /**
     * 恒定时间字符串比较, 防止时序攻击
     *
     * @param a 字符串 a
     * @param b 字符串 b
     * @return true=相等
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
