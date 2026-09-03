package com.na.common.utils.crypto;

import org.bouncycastle.asn1.gm.GMNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.engines.SM2Engine;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.crypto.signers.SM2Signer;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.BigIntegers;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * SM2 国密非对称加密/签名工具
 * <p>
 * SM2 是中国国家标准非对称加密算法, 基于 ECC 椭圆曲线, 256 位密钥安全强度等同 RSA 3072 位
 * <p>
 * <b>使用场景:</b>
 * <ul>
 *   <li>电子合同/电子发票: 公钥加密敏感数据, 私钥解密</li>
 *   <li>身份认证/数字签名: 私钥签名, 公钥验签(不可抵赖)</li>
 *   <li>金融/政务系统: 符合国密合规要求的密钥交换与数据保护</li>
 *   <li>替代 RSA: 密钥更短(256 位 vs 2048 位), 性能更高</li>
 * </ul>
 * <p>
 * 使用示例:
 * <pre>{@code
 * // 生成密钥对
 * SM2Util.KeyPair keyPair = SM2Util.generateKeyPair();
 *
 * // 加密(公钥)
 * String encrypted = SM2Util.encrypt("敏感数据", keyPair.getPublicKey());
 *
 * // 解密(私钥)
 * String decrypted = SM2Util.decrypt(encrypted, keyPair.getPrivateKey());
 *
 * // 签名(私钥)
 * String signature = SM2Util.sign("待签名数据", keyPair.getPrivateKey());
 *
 * // 验签(公钥)
 * boolean valid = SM2Util.verify("待签名数据", signature, keyPair.getPublicKey());
 * }</pre>
 */
public final class SM2Util {

    /** 纯静态工具类, 禁止实例化 */
    private SM2Util() {
    }

    /** SM2 曲线参数(sm2p256v1) */
    private static final X9ECParameters CURVE_PARAMS = GMNamedCurves.getByName("sm2p256v1");

    /** EC 域参数 */
    private static final ECDomainParameters DOMAIN = new ECDomainParameters(
            CURVE_PARAMS.getCurve(), CURVE_PARAMS.getG(), CURVE_PARAMS.getN(), CURVE_PARAMS.getH());

    static {
        // 注册 BouncyCastle Provider
        java.security.Security.addProvider(new BouncyCastleProvider());
    }

    /** Base64 编码器 */
    private static final Base64.Encoder ENCODER = Base64.getEncoder();

    /** Base64 解码器 */
    private static final Base64.Decoder DECODER = Base64.getDecoder();

    /**
     * 密钥对(公钥 + 私钥, Base64 编码)
     */
    public static class KeyPair {
        private final String publicKey;
        private final String privateKey;

        public KeyPair(String publicKey, String privateKey) {
            this.publicKey = publicKey;
            this.privateKey = privateKey;
        }

        /**
         * @return Base64 编码的公钥(04 + X + Y, 共 65 字节)
         */
        public String getPublicKey() {
            return publicKey;
        }

        /**
         * @return Base64 编码的私钥(32 字节)
         */
        public String getPrivateKey() {
            return privateKey;
        }
    }

    /**
     * 生成 SM2 密钥对
     *
     * @return 密钥对(公钥 + 私钥, Base64 编码)
     */
    public static KeyPair generateKeyPair() {
        SecureRandom random = new SecureRandom();
        BigInteger n = DOMAIN.getN();
        BigInteger d;
        do {
            d = new BigInteger(n.bitLength(), random);
        } while (d.equals(BigInteger.ZERO) || d.compareTo(n) >= 0);

        ECPoint q = DOMAIN.getG().multiply(d).normalize();

        String publicKey = ENCODER.encodeToString(q.getEncoded(false));
        String privateKey = ENCODER.encodeToString(BigIntegers.asUnsignedByteArray(d));

        return new KeyPair(publicKey, privateKey);
    }

    /**
     * SM2 加密(公钥加密)
     *
     * @param plainText  明文
     * @param publicKey  Base64 编码的公钥
     * @return Base64 编码的密文(C1+C3+C2 模式)
     */
    public static String encrypt(String plainText, String publicKey) {
        try {
            byte[] pubKeyBytes = DECODER.decode(publicKey);
            ECPoint q = DOMAIN.getCurve().decodePoint(pubKeyBytes);
            ECPublicKeyParameters pubKeyParams = new ECPublicKeyParameters(q, DOMAIN);

            SM2Engine engine = new SM2Engine(SM2Engine.Mode.C1C3C2);
            engine.init(true, new ParametersWithRandom(pubKeyParams, new SecureRandom()));

            byte[] encrypted = engine.processBlock(plainText.getBytes(StandardCharsets.UTF_8), 0,
                    plainText.getBytes(StandardCharsets.UTF_8).length);
            return ENCODER.encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("SM2加密失败: " + e.getMessage(), e);
        }
    }

    /**
     * SM2 解密(私钥解密)
     *
     * @param cipherText Base64 编码的密文
     * @param privateKey Base64 编码的私钥
     * @return 明文
     */
    public static String decrypt(String cipherText, String privateKey) {
        try {
            byte[] cipherBytes = DECODER.decode(cipherText);
            byte[] priKeyBytes = DECODER.decode(privateKey);
            BigInteger d = new BigInteger(1, priKeyBytes);
            ECPrivateKeyParameters priKeyParams = new ECPrivateKeyParameters(d, DOMAIN);

            SM2Engine engine = new SM2Engine(SM2Engine.Mode.C1C3C2);
            engine.init(false, priKeyParams);

            byte[] decrypted = engine.processBlock(cipherBytes, 0, cipherBytes.length);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("SM2解密失败: " + e.getMessage(), e);
        }
    }

    /**
     * SM2 数字签名(私钥签名)
     *
     * @param data       待签名数据
     * @param privateKey Base64 编码的私钥
     * @return Base64 编码的签名
     */
    public static String sign(String data, String privateKey) {
        try {
            byte[] priKeyBytes = DECODER.decode(privateKey);
            BigInteger d = new BigInteger(1, priKeyBytes);
            ECPrivateKeyParameters priKeyParams = new ECPrivateKeyParameters(d, DOMAIN);

            SM2Signer signer = new SM2Signer();
            signer.init(true, new ParametersWithRandom(priKeyParams, new SecureRandom()));

            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            signer.update(dataBytes, 0, dataBytes.length);
            byte[] signature = signer.generateSignature();

            return ENCODER.encodeToString(signature);
        } catch (Exception e) {
            throw new RuntimeException("SM2签名失败: " + e.getMessage(), e);
        }
    }

    /**
     * SM2 验签(公钥验签)
     *
     * @param data       原始数据
     * @param signature  Base64 编码的签名
     * @param publicKey  Base64 编码的公钥
     * @return true=签名有效
     */
    public static boolean verify(String data, String signature, String publicKey) {
        try {
            byte[] pubKeyBytes = DECODER.decode(publicKey);
            ECPoint q = DOMAIN.getCurve().decodePoint(pubKeyBytes);
            ECPublicKeyParameters pubKeyParams = new ECPublicKeyParameters(q, DOMAIN);

            SM2Signer signer = new SM2Signer();
            signer.init(false, pubKeyParams);

            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            signer.update(dataBytes, 0, dataBytes.length);

            return signer.verifySignature(DECODER.decode(signature));
        } catch (Exception e) {
            throw new RuntimeException("SM2验签失败: " + e.getMessage(), e);
        }
    }
}
