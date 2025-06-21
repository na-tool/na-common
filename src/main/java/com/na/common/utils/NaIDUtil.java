package com.na.common.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.UUID;

public class NaIDUtil {
    public static Long ID(){
        return NaDateTimeUtil.getBeijingTimestampMillis();
    }

    public static String getUUID32() {
        String uuid = UUID.randomUUID().toString().replace("-", "").toLowerCase();
        return uuid;
    }

    public static String ID(Integer length){
        long beijingTimestampMillis = NaDateTimeUtil.getBeijingTimestampMillis();// 13位
        // 确保TABLE_ID不超过30
        length = Math.min(length, 17); // 保证40位

        return beijingTimestampMillis + getRandomNo(length);
    }

    public static String getRandomNo(Integer length) { // 生成随机字符串
        char[] chr = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I',
                'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z' };
        Random random = new Random();
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < length; i++) {
            buffer.append(chr[random.nextInt(36)]);
        }
        return buffer.toString();
    }
}
