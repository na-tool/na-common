package com.na.common.utils.crypto;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES 加密/解密工具(GCM 模式, 带认证标签)
 * <p>
 * GCM 模式提供机密性 + 完整性认证, 无需额外 MAC; 每次加密使用随机 IV, 结果格式: Base64(IV + 密文+Tag)
 * <p>
 * 使用示例:
 * <pre>{@code
 * // 生成密钥(128/192/256 位)
 * String key = AESUtil.generateKey(256);
 *
 * // 加密
 * String encrypted = AESUtil.encrypt("敏感数据", key);
 *
 * // 解密
 * String decrypted = AESUtil.decrypt(encrypted, key);
 * }</pre>
 */
public final class AESUtil {

    /** 纯静态工具类, 禁止实例化 */
    private AESUtil() {
    }

    /** 算法: AES/GCM/NoPadding, GCM 模式自带认证标签, 无需手动 Padding */
    private static final String ALGORITHM = "AES/GCM/NoPadding";

    /** 密钥算法 */
    private static final String KEY_ALGORITHM = "AES";

    /** GCM 认证标签长度(位) */
    private static final int TAG_LENGTH_BIT = 128;

    /** GCM IV 长度(字节), GCM 推荐 12 字节 */
    private static final int IV_LENGTH_BYTE = 12;

    /** Base64 编码器 */
    private static final Base64.Encoder ENCODER = Base64.getEncoder();

    /** Base64 解码器 */
    private static final Base64.Decoder DECODER = Base64.getDecoder();

    /**
     * 生成 AES 密钥(Base64 编码)
     *
     * @param keySize 密钥位数, 支持 128/192/256
     * @return Base64 编码的密钥
     */
    public static String generateKey(int keySize) {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(KEY_ALGORITHM);
            keyGen.init(keySize, new SecureRandom());
            SecretKey secretKey = keyGen.generateKey();
            return ENCODER.encodeToString(secretKey.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("生成AES密钥失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从原始字节数组构建 SecretKey
     *
     * @param keyBytes 密钥字节(16/24/32 字节对应 128/192/256 位)
     * @return SecretKey
     */
    private static SecretKey toKey(byte[] keyBytes) {
        return new SecretKeySpec(keyBytes, KEY_ALGORITHM);
    }

    /**
     * AES-GCM 加密
     * <p>
     * 结果格式: Base64(IV[12] + 密文+Tag[N])
     *
     * @param plainText 明文
     * @param key       Base64 编码的密钥
     * @return Base64 编码的密文(含 IV 前缀)
     */
    public static String encrypt(String plainText, String key) {
        try {
            byte[] keyBytes = DECODER.decode(key);
            byte[] iv = new byte[IV_LENGTH_BYTE];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, toKey(keyBytes), new GCMParameterSpec(TAG_LENGTH_BIT, iv));

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // 拼接 IV + 密文
            byte[] output = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, output, 0, iv.length);
            System.arraycopy(encrypted, 0, output, iv.length, encrypted.length);

            return ENCODER.encodeToString(output);
        } catch (Exception e) {
            throw new RuntimeException("AES加密失败: " + e.getMessage(), e);
        }
    }

    /**
     * AES-GCM 解密
     *
     * @param cipherText Base64 编码的密文(含 IV 前缀)
     * @param key        Base64 编码的密钥
     * @return 明文
     */
    public static String decrypt(String cipherText, String key) {
        byte[] decoded = DECODER.decode(cipherText);
        // 长度不足时先行拒绝: 否则 new byte[负数] 抛 NegativeArraySizeException,
        // 被下面的 catch 包成"AES解密失败", 掩盖了真正原因(密文格式非法)
        if (decoded.length <= IV_LENGTH_BYTE + TAG_LENGTH_BIT / 8) {
            throw new IllegalArgumentException("AES密文格式非法: 长度不足以容纳 IV 与认证标签");
        }
        try {
            byte[] keyBytes = DECODER.decode(key);

            // 提取 IV(前 12 字节)和密文(剩余)
            byte[] iv = new byte[IV_LENGTH_BYTE];
            byte[] encrypted = new byte[decoded.length - IV_LENGTH_BYTE];
            System.arraycopy(decoded, 0, iv, 0, IV_LENGTH_BYTE);
            System.arraycopy(decoded, IV_LENGTH_BYTE, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, toKey(keyBytes), new GCMParameterSpec(TAG_LENGTH_BIT, iv));

            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES解密失败: " + e.getMessage(), e);
        }
    }
}
