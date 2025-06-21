package com.na.common.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/**
 * AES 加密工具类
 * 提供基于 AES 对称加密算法的加解密方法，支持：
 * {@code LinkedList<String>} 格式数据加解密（以 ":" 分隔）
 * {@code Map<String, Object>} 格式数据加解密（以 {@code key=value&key=value} 拼接）
 * 加密后使用 Base64 编码
 */
public class NaAesUtils {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES";

    /**
     * 使用 accessKey 对 {@code LinkedList<String>} 数据加密（以 ":" 拼接）
     *
     * @param accessKey 密钥字符串
     * @param data      数据列表
     * @return 加密后的 Base64 编码字符串
     * @throws Exception 异常抛出
     */
    public static String encrypt(String accessKey, LinkedList<String> data) throws Exception {
        SecretKey key = generateKeyFromStr(accessKey);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, key);

        String combinedStr = String.join(":", data);
        byte[] encryptedBytes = cipher.doFinal(combinedStr.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    /**
     * 解密 Base64 字符串为原始 {@code LinkedList<String>} 数据
     *
     * @param accessKey    密钥字符串
     * @param encryptedStr 加密的 Base64 字符串
     * @return 解密后的字符串数组
     * @throws Exception 异常抛出
     */
    public static String[] decrypt(String accessKey, String encryptedStr) throws Exception {
        SecretKey key = generateKeyFromStr(accessKey);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, key);

        byte[] decodedBytes = Base64.getDecoder().decode(encryptedStr);
        byte[] decryptedBytes = cipher.doFinal(decodedBytes);
        String decryptedStr = new String(decryptedBytes, StandardCharsets.UTF_8);

        return decryptedStr.split(":");
    }

    /**
     * 使用 accessKey 对 {@code Map<String, Object>} 数据加密（以 {@code key=value&key=value} 拼接）
     *
     * @param accessKey 密钥字符串
     * @param data      数据 Map
     * @return 加密后的 Base64 编码字符串
     * @throws Exception 异常抛出
     */
    public static String encrypt(String accessKey, Map<String, Object> data) throws Exception {
        SecretKey key = generateKeyFromStr(accessKey);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, key);

        StringBuilder combinedStr = new StringBuilder();
        Iterator<Map.Entry<String, Object>> iterator = data.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, Object> entry = iterator.next();
            combinedStr.append(entry.getKey()).append("=").append(entry.getValue());
            if (iterator.hasNext()) {
                combinedStr.append("&");
            }
        }

        byte[] encryptedBytes = cipher.doFinal(combinedStr.toString().getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    /**
     * 解密 Map 加密数据为 {@code Map<String, Object>}
     *
     * @param accessKey     密钥字符串
     * @param encryptedData 加密的 Base64 字符串
     * @return 解密后的 Map
     * @throws Exception 异常抛出
     */
    public static Map<String, Object> decryptMap(String accessKey, String encryptedData) throws Exception {
        SecretKey key = generateKeyFromStr(accessKey);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, key);

        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        String decryptedStr = new String(decryptedBytes, StandardCharsets.UTF_8);

        Map<String, Object> resultMap = new HashMap<>();
        for (String pair : decryptedStr.split("&")) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                resultMap.put(keyValue[0], keyValue[1]);
            }
        }
        return resultMap;
    }

    /**
     * 将字符串转换为 SecretKey，用于 AES 加密
     *
     * @param keyStr 密钥字符串
     * @return SecretKey 对象
     * @throws Exception 异常抛出
     */
    private static SecretKey generateKeyFromStr(String keyStr) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = sha.digest(keyStr.getBytes(StandardCharsets.UTF_8));
        // 兼容性考虑：只使用前 16 字节（128 位）确保兼容 JDK 默认 AES 支持
        keyBytes = Arrays.copyOf(keyBytes, 16);
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }

//    public static void main(String[] args) throws Exception {
//        LinkedList<String> list = new LinkedList<>();
//        list.add("1");
//        list.add("QINIU");
//        list.add("2025-05-23 18:30:00");
//
//        String key = "mySecretKey123";
//
//        String encrypted = NaAesUtils.encrypt(key, list);
//        System.out.println("加密后：" + encrypted);
//
//        String[] decrypted = NaAesUtils.decrypt(key, encrypted);
//        System.out.println("解密后：" + Arrays.toString(decrypted));
//
//        Map<String,Object> map = new HashMap<>();
//        map.put("key1", "value1");
//        map.put("key2", "value2");
//
//        encrypted = NaAesUtils.encrypt(key, map);
//        System.out.println("加密后：" + encrypted);
//
//        Map<String, Object> decryptMap = NaAesUtils.decryptMap(key, encrypted);
//        System.out.println("解密后：" + decryptMap);
//    }

}
