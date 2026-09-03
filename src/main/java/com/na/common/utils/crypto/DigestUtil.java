package com.na.common.utils.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 通用摘要算法工具类(SHA-1 / SHA-256 / SHA-512)
 * <p>
 * SHA-256/512 比 MD5 更安全, 适用于数字签名、文件完整性校验
 * <p>
 * 使用示例:
 * <pre>{@code
 * String hash = DigestUtil.sha256("hello");
 * String hash512 = DigestUtil.sha512("hello");
 * }</pre>
 */
public final class DigestUtil {

    /** 纯静态工具类, 禁止实例化 */
    private DigestUtil() {
    }

    /** SHA-1 算法名 */
    private static final String SHA_1 = "SHA-1";

    /** SHA-256 算法名 */
    private static final String SHA_256 = "SHA-256";

    /** SHA-512 算法名 */
    private static final String SHA_512 = "SHA-512";

    /**
     * SHA-1 哈希(40 位小写十六进制)
     *
     * @param input 原文
     * @return 40 位小写十六进制哈希; input 为 null 时返回空串
     */
    public static String sha1(String input) {
        return digest(input, SHA_1);
    }

    /**
     * SHA-256 哈希(64 位小写十六进制)
     *
     * @param input 原文
     * @return 64 位小写十六进制哈希; input 为 null 时返回空串
     */
    public static String sha256(String input) {
        return digest(input, SHA_256);
    }

    /**
     * SHA-512 哈希(128 位小写十六进制)
     *
     * @param input 原文
     * @return 128 位小写十六进制哈希; input 为 null 时返回空串
     */
    public static String sha512(String input) {
        return digest(input, SHA_512);
    }

    /**
     * 计算摘要(内部通用方法)
     *
     * @param input    原文
     * @param algorithm 算法名
     * @return 小写十六进制哈希; input 为 null 时返回空串
     */
    private static String digest(String input, String algorithm) {
        if (input == null) {
            return "";
        }
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return toHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(algorithm + "算法不可用", e);
        }
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
}
