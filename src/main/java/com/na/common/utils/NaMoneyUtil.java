package com.na.common.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class NaMoneyUtil {
    public static String convertToChineseUpper(BigDecimal amount) {
        if (amount == null) {
            return "零元整";
        }

        final String[] CN_UPPER_NUMBER = {"零", "壹", "贰", "叁", "肆", "伍", "陆", "柒", "捌", "玖"};
        final String[] CN_UNIT = {"", "拾", "佰", "仟"};
        final String[] CN_SECTION = {"", "万", "亿", "兆"};
        final String CN_DOLLAR = "元";
        final String CN_INTEGER = "整";
        final String[] CN_DECIMAL_UNIT = {"角", "分"};

        long money = amount.setScale(2, RoundingMode.DOWN).multiply(BigDecimal.valueOf(100)).longValue();

        if (money == 0) return "零元整";

        StringBuilder sb = new StringBuilder();
        int scale = (int) (money % 100); // 小数部分
        int integer = (int) (money / 100); // 整数部分

        // 小数部分
        if (scale == 0) {
            sb.append(CN_INTEGER);
        } else {
            int jiao = scale / 10;
            int fen = scale % 10;
            if (jiao > 0) sb.append(CN_UPPER_NUMBER[jiao]).append(CN_DECIMAL_UNIT[0]);
            if (fen > 0) sb.append(CN_UPPER_NUMBER[fen]).append(CN_DECIMAL_UNIT[1]);
        }

        // 整数部分（按4位分节处理）
        int unitPos = 0;
        boolean zero = false;
        StringBuilder integerPart = new StringBuilder();
        int tempInt = integer;
        while (tempInt > 0) {
            int section = tempInt % 10000;
            if (section != 0) {
                StringBuilder sectionBuilder = new StringBuilder();
                int pos = 0;
                boolean sectionZero = true;
                while (section > 0) {
                    int digit = section % 10;
                    if (digit != 0) {
                        sectionBuilder.insert(0, CN_UPPER_NUMBER[digit] + CN_UNIT[pos]);
                        sectionZero = false;
                    } else if (!sectionZero && sectionBuilder.length() > 0 && sectionBuilder.charAt(0) != '零') {
                        sectionBuilder.insert(0, "零");
                    }
                    pos++;
                    section /= 10;
                }
                sectionBuilder.append(CN_SECTION[unitPos]);
                integerPart.insert(0, sectionBuilder);
            } else if (!zero && integerPart.length() > 0) {
                integerPart.insert(0, "零");
            }
            zero = section == 0;
            unitPos++;
            tempInt /= 10000;
        }

        // ✅ 只在整数部分 > 0 时才加“元”和整数部分
        if (integer > 0) {
            sb.insert(0, integerPart.append(CN_DOLLAR));
        }

        return sb.toString()
                .replaceAll("零+", "零")
                .replaceAll("零元", "元")
                .replaceAll("零角", "")
                .replaceAll("零分", "");
    }
}
