package com.na.common.utils.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5 哈希工具类
 * <p>
 * MD5 不可逆, 适用于校验码生成、数字指纹; 不推荐用于密码存储(请用 BCrypt/Argon2)
 * <p>
 * 使用示例:
 * <pre>{@code
 * String hash = MD5Util.md5("hello");
 * String hash16 = MD5Util.md5Hex16("hello"); // 16 位中段
 * }</pre>
 */
public final class MD5Util {

    /** 纯静态工具类, 禁止实例化 */
    private MD5Util() {
    }

    /** MD5 摘要字节数(128 位 = 16 字节) */
    private static final int MD5_LENGTH = 16;

    /**
     * 计算 MD5 哈希(32 位小写十六进制)
     *
     * @param input 原文字符串
     * @return 32 位小写十六进制哈希; input 为 null 时返回空串
     */
    public static String md5(String input) {
        if (input == null) {
            return "";
        }
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return toHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5算法不可用", e);
        }
    }

    /**
     * 计算 MD5 哈希(16 位小写十六进制, 取中段 8~24 位)
     * <p>
     * 32 位 MD5 截取中间 16 位, 常用于短校验码
     *
     * @param input 原文字符串
     * @return 16 位小写十六进制哈希; input 为 null 时返回空串
     */
    public static String md5Hex16(String input) {
        String full = md5(input);
        return full.isEmpty() ? "" : full.substring(8, 24);
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
