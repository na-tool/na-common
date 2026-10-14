package com.na.common.utils.crypto;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * SM4 国密对称加密/解密工具
 * <p>
 * SM4 是中国国家标准分组对称加密算法, 128 位分组/128 位密钥, 安全强度等同 AES-128
 * <p>
 * <b>使用场景:</b>
 * <ul>
 *   <li>数据加密存储: 数据库字段加密、配置文件敏感信息加密</li>
 *   <li>接口数据传输: 服务间通信加密, 替代 AES 用于国密合规场景</li>
 *   <li>文件加密: 电子文档、证照数据加密存储</li>
 *   <li>国密合规: 金融、政务、医疗等行业要求数据使用国密算法</li>
 * </ul>
 * <p>
 * 使用示例:
 * <pre>{@code
 * // 生成密钥(128 位)
 * String key = SM4Util.generateKey();
 *
 * // 加密
 * String encrypted = SM4Util.encrypt("敏感数据", key);
 *
 * // 解密
 * String decrypted = SM4Util.decrypt(encrypted, key);
 * }</pre>
 */
public final class SM4Util {

    /** 纯静态工具类, 禁止实例化 */
    private SM4Util() {
    }

    /** 算法: SM4/GCM/NoPadding, GCM 模式自带认证标签, 无需手动 Padding */
    private static final String ALGORITHM = "SM4/GCM/NoPadding";

    /** 密钥算法名 */
    private static final String KEY_ALGORITHM = "SM4";

    /** GCM 认证标签长度(位) */
    private static final int TAG_LENGTH_BIT = 128;

    /** GCM IV 长度(字节), GCM 推荐 12 字节 */
    private static final int IV_LENGTH_BYTE = 12;

    /** Base64 编码器 */
    private static final Base64.Encoder ENCODER = Base64.getEncoder();

    /** Base64 解码器 */
    private static final Base64.Decoder DECODER = Base64.getDecoder();

    static {
        // 注册 BouncyCastle Provider
        CryptoSupport.ensureBouncyCastleProvider();
    }

    /**
     * 生成 SM4 密钥(Base64 编码, 128 位 = 16 字节)
     *
     * @return Base64 编码的密钥
     */
    public static String generateKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(KEY_ALGORITHM, BouncyCastleProvider.PROVIDER_NAME);
            keyGen.init(128, new SecureRandom());
            return ENCODER.encodeToString(keyGen.generateKey().getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("生成SM4密钥失败: " + e.getMessage(), e);
        }
    }

    /**
     * SM4-GCM 加密
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

            Cipher cipher = Cipher.getInstance(ALGORITHM, BouncyCastleProvider.PROVIDER_NAME);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, KEY_ALGORITHM),
                    new GCMParameterSpec(TAG_LENGTH_BIT, iv));

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // 拼接 IV + 密文
            byte[] output = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, output, 0, iv.length);
            System.arraycopy(encrypted, 0, output, iv.length, encrypted.length);

            return ENCODER.encodeToString(output);
        } catch (Exception e) {
            throw new RuntimeException("SM4加密失败: " + e.getMessage(), e);
        }
    }

    /**
     * SM4-GCM 解密
     *
     * @param cipherText Base64 编码的密文(含 IV 前缀)
     * @param key        Base64 编码的密钥
     * @return 明文
     */
    public static String decrypt(String cipherText, String key) {
        byte[] decoded = DECODER.decode(cipherText);
        // 长度不足时先行拒绝: 否则 new byte[负数] 抛 NegativeArraySizeException,
        // 被下面的 catch 包成"SM4解密失败", 掩盖了真正原因(密文格式非法)
        if (decoded.length <= IV_LENGTH_BYTE + TAG_LENGTH_BIT / 8) {
            throw new IllegalArgumentException("SM4密文格式非法: 长度不足以容纳 IV 与认证标签");
        }
        try {
            byte[] keyBytes = DECODER.decode(key);

            // 提取 IV(前 12 字节)和密文(剩余)
            byte[] iv = new byte[IV_LENGTH_BYTE];
            byte[] encrypted = new byte[decoded.length - IV_LENGTH_BYTE];
            System.arraycopy(decoded, 0, iv, 0, IV_LENGTH_BYTE);
            System.arraycopy(decoded, IV_LENGTH_BYTE, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM, BouncyCastleProvider.PROVIDER_NAME);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, KEY_ALGORITHM),
                    new GCMParameterSpec(TAG_LENGTH_BIT, iv));

            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("SM4解密失败: " + e.getMessage(), e);
        }
    }
}
