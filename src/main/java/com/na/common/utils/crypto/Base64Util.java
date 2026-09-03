package com.na.common.utils.crypto;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Base64 编解码工具类
 * <p>
 * 支持标准 Base64 和 URL 安全 Base64(无换行、- 替 +、_ 替 /)
 * <p>
 * 使用示例:
 * <pre>{@code
 * String encoded = Base64Util.encode("hello");
 * String decoded = Base64Util.decode(encoded);
 * String urlSafe = Base64Util.encodeUrlSafe("data?key=val");
 * }</pre>
 */
public final class Base64Util {

    /** 纯静态工具类, 禁止实例化 */
    private Base64Util() {
    }

    /** 标准 Base64 编码器 */
    private static final Base64.Encoder ENCODER = Base64.getEncoder();

    /** URL 安全 Base64 编码器 */
    private static final Base64.Encoder URL_SAFE_ENCODER = Base64.getUrlEncoder();

    /** 标准 Base64 解码器 */
    private static final Base64.Decoder DECODER = Base64.getDecoder();

    /** URL 安全 Base64 解码器 */
    private static final Base64.Decoder URL_SAFE_DECODER = Base64.getUrlDecoder();

    /**
     * 标准 Base64 编码
     *
     * @param input 原文字符串
     * @return Base64 编码字符串; input 为 null 时返回空串
     */
    public static String encode(String input) {
        if (input == null) {
            return "";
        }
        return ENCODER.encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 标准 Base64 编码(字节数组)
     *
     * @param bytes 原始字节
     * @return Base64 编码字符串; bytes 为 null 时返回空串
     */
    public static String encode(byte[] bytes) {
        if (bytes == null) {
            return "";
        }
        return ENCODER.encodeToString(bytes);
    }

    /**
     * URL 安全 Base64 编码(+ 替为 -, / 替为 _, 无 = 填充)
     * <p>
     * 适用于 URL 参数、JWT 等
     *
     * @param input 原文字符串
     * @return URL 安全的 Base64 编码; input 为 null 时返回空串
     */
    public static String encodeUrlSafe(String input) {
        if (input == null) {
            return "";
        }
        return URL_SAFE_ENCODER.withoutPadding()
                .encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 标准 Base64 解码
     *
     * @param base64 Base64 编码字符串
     * @return 解码后的字符串; base64 为 null 时返回空串
     */
    public static String decode(String base64) {
        if (base64 == null || base64.isEmpty()) {
            return "";
        }
        byte[] decoded = DECODER.decode(base64);
        return new String(decoded, StandardCharsets.UTF_8);
    }

    /**
     * 标准 Base64 解码(返回字节数组)
     *
     * @param base64 Base64 编码字符串
     * @return 解码后的字节数组; base64 为 null 时返回空数组
     */
    public static byte[] decodeToBytes(String base64) {
        if (base64 == null || base64.isEmpty()) {
            return new byte[0];
        }
        return DECODER.decode(base64);
    }

    /**
     * URL 安全 Base64 解码
     *
     * @param base64 URL 安全的 Base64 编码字符串
     * @return 解码后的字符串; base64 为 null 时返回空串
     */
    public static String decodeUrlSafe(String base64) {
        if (base64 == null || base64.isEmpty()) {
            return "";
        }
        byte[] decoded = URL_SAFE_DECODER.decode(base64);
        return new String(decoded, StandardCharsets.UTF_8);
    }
}
