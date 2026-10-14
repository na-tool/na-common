package com.na.common.utils.crypto;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;

/**
 * crypto 包内部的公共实现。
 *
 * <p>包私有，不对外暴露。这些方法是各算法类共用的纯函数与一次性初始化，
 * 集中在这里是为了避免同一段逻辑在多个算法类里各写一份而逐渐漂移：
 * 抽出前 {@code toHex} 有 4 份逐字相同的副本、{@code constantTimeEquals} 有 2 份、
 * BouncyCastle Provider 的注册有 3 份。
 */
final class CryptoSupport {

    private CryptoSupport() {
    }

    /**
     * 字节数组转小写十六进制字符串
     *
     * @param bytes 字节数组
     * @return 小写十六进制字符串
     */
    static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    /**
     * 恒定时间字符串比较，防止时序攻击
     * <p>
     * 长度不同直接返回 false（长度本身不是秘密）；长度相同时逐字符异或累加，
     * 不提前返回，避免比较耗时泄露匹配前缀的长度
     *
     * @param a 字符串 a
     * @param b 字符串 b
     * @return true=内容相同
     */
    static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    /**
     * 幂等地注册 BouncyCastle Provider。
     * <p>
     * 国密算法分散在三个类中，各自都可能在首次使用时触发注册；
     * 这里先查再注册，避免重复 {@code addProvider} 把同一个 Provider 反复塞进列表。
     * 该方法由各算法类的静态块调用，注册失败不抛出（后续算法初始化会给
     * 出更准确的错误信息）。
     */
    static void ensureBouncyCastleProvider() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }
}
