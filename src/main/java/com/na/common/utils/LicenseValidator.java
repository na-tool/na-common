package com.na.common.utils;

import com.na.common.constant.INaGlobalConst;
import com.na.common.crypto.NaAesUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedList;

public class LicenseValidator {
    /**
     * 校验 license key 是否有效
     *
     * @param key license 字符串，格式为前缀-加密时间戳
     * @return 是否有效
     */
    public static boolean isValidLicense(String key) {
        if (StringUtils.isEmpty(key)) {
            return false;
        }

        try {
            String[] decrypted  = NaAesUtils.decrypt(INaGlobalConst.SECURITY.STRING_KEY,key.trim());
            if (decrypted == null || decrypted.length != 2) {
                return false;
            }
            long expireTime = Long.parseLong(decrypted[1]);
            long currentTime = NaDateTimeUtil.getBeijingTimestampMillis();
            return currentTime <= expireTime;

        }catch (Exception e){
            System.out.println(e);
            return false;
        }

    }

    public static String generateLicenseKey(String toolConst,long expireTime)  {
        if (StringUtils.isEmpty(toolConst)) {
            throw new IllegalArgumentException("toolConst must not be empty");
        }

        String value = String.valueOf(expireTime);
        if (value.length() != 13 && value.length() != 10) {
            throw new IllegalArgumentException("expireTime must be a 10 or 13 digit timestamp");
        }
        if (value.length() == 10) {
            value += "000"; // 秒级转毫秒
        }

        LinkedList<String> list = new LinkedList<>();
        list.add(toolConst.trim());
        list.add(value);

        try {
            return NaAesUtils.encrypt(INaGlobalConst.SECURITY.STRING_KEY, list);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public static void main(String[] args) {
        System.out.println(generateLicenseKey("CTSS",4904450251L));
        System.out.println(isValidLicense("y5RShlNTJiwl8p6YzuO1e9iuMB75fBIwqBJqzWyrzFE="));
    }
}
