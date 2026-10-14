package com.na.common.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 金额工具类
 * <p>
 * 提供人民币金额转中文大写功能，书写规则遵循中国人民银行财务票据规范：
 * <ul>
 *   <li>金额截断到 2 位小数(不四舍五入)，支持到"兆"级(16位整数以内)</li>
 *   <li>有角无分时"角"后不写"整"；无角无分时写"整"</li>
 *   <li>中间连续多个"0"时只写一个"零"；万位/元位为 0 且千位/角位非 0 时，"零"可省略</li>
 *   <li>负数金额以"负"字开头，按绝对值转换</li>
 * </ul>
 * 示例: 1234.56 → 壹仟贰佰叁拾肆元伍角陆分；1000005.00 → 壹佰万零伍元整；0.05 → 伍分
 */
public final class MoneyUtil {

    /** 数字 0~9 对应的中文大写 */
    private static final String[] CN_UPPER_NUMBER = {"零", "壹", "贰", "叁", "肆", "伍", "陆", "柒", "捌", "玖"};

    /** 节内单位: 个、拾、佰、仟 */
    private static final String[] CN_UNIT = {"", "拾", "佰", "仟"};

    /** 分节单位: 每 4 位为一节(个节、万节、亿节、兆节) */
    private static final String[] CN_SECTION = {"", "万", "亿", "兆"};

    /** 小数位单位: 角、分 */
    private static final String[] CN_DECIMAL_UNIT = {"角", "分"};

    private static final String CN_ZERO = "零";
    private static final String CN_DOLLAR = "元";
    private static final String CN_INTEGER = "整";
    private static final String CN_NEGATIVE = "负";

    /** 纯静态工具类，禁止实例化 */
    private MoneyUtil() {
    }

    /**
     * 将人民币金额转换为中文大写字符串
     *
     * @param amount 金额；null 或 0 返回 "零元整"
     * @return 中文大写金额，如 "壹仟贰佰叁拾肆元伍角陆分"
     * @throws IllegalArgumentException 金额超出可转换范围(整数部分超过 16 位，即 9999 兆)
     */
    public static String convertToChineseUpper(BigDecimal amount) {
        if (amount == null) {
            return "零元整";
        }

        // 负数: 记录符号后按绝对值转换
        boolean negative = amount.signum() < 0;
        BigDecimal absAmount = amount.abs();

        // 截断到 2 位小数后放大 100 倍转为"分"，全程整型运算避免浮点误差
        BigDecimal totalFen = absAmount.setScale(2, RoundingMode.DOWN).movePointRight(2);
        // 溢出防护: 超出 long 范围时若不拦截会静默产生错误大写，财务场景必须显式报错
        if (totalFen.compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) > 0) {
            throw new IllegalArgumentException("金额超出可转换范围: " + amount.toPlainString());
        }
        long money = totalFen.longValue();

        if (money == 0) {
            return "零元整";
        }

        long scale = money % 100;   // 小数部分(分)
        long integer = money / 100; // 整数部分(元)

        // ---------- 小数部分: 角、分 ----------
        StringBuilder sb = new StringBuilder();
        if (scale == 0) {
            // 无角无分 → "整"
            sb.append(CN_INTEGER);
        } else {
            long jiao = scale / 10;
            long fen = scale % 10;
            if (jiao > 0) {
                sb.append(CN_UPPER_NUMBER[(int) jiao]).append(CN_DECIMAL_UNIT[0]);
            } else if (fen > 0 && integer > 0) {
                // 有分无角且存在整数部分时，按规范在"分"前补"零"(如 100.05 → 壹佰元零伍分)
                // 整数部分为 0 时不补(如 0.05 → 伍分)
                sb.append(CN_ZERO);
            }
            if (fen > 0) {
                sb.append(CN_UPPER_NUMBER[(int) fen]).append(CN_DECIMAL_UNIT[1]);
            }
        }

        // ---------- 整数部分: 每 4 位一节，从低位到高位逐节处理 ----------
        StringBuilder integerPart = new StringBuilder();
        long tempInt = integer;
        int unitPos = 0;    // 当前节序号: 0=个节, 1=万节, 2=亿节, 3=兆节
        boolean zero = false; // 上一节是否为 0，用于节间补零

        while (tempInt > 0) {
            long section = tempInt % 10000;
            if (section != 0) {
                StringBuilder sectionBuilder = new StringBuilder();

                // 节内逐位构建: 从低位到高位，非零位插入"数字+节内单位"
                int pos = 0;
                boolean sectionZero = true; // 是否仅有前导零(尚未遇到非零数字)
                long s = section;
                while (s > 0) {
                    int digit = (int) (s % 10);
                    if (digit != 0) {
                        sectionBuilder.insert(0, CN_UPPER_NUMBER[digit] + CN_UNIT[pos]);
                        sectionZero = false;
                    } else if (!sectionZero
                            && sectionBuilder.length() > 0
                            && sectionBuilder.charAt(0) != '零') {
                        // 中间遇到 0 且之前有非零数字、首位还不是"零"时，补一个"零"
                        sectionBuilder.insert(0, CN_ZERO);
                    }
                    pos++;
                    s /= 10;
                }

                // 节内值不足 1000(存在前导零)且更高节还有内容时，节前补"零"
                // 如 1000005 → 节[0]=0005 → "壹佰万零伍"；注意必须在节内构建完成后再前插
                if (section < 1000 && tempInt / 10000 > 0) {
                    sectionBuilder.insert(0, CN_ZERO);
                }

                sectionBuilder.append(CN_SECTION[unitPos]);
                integerPart.insert(0, sectionBuilder);
            } else if (!zero && integerPart.length() > 0) {
                // 整节为 0 且前后均有内容时补一个"零"(连续零由最终正则合并)
                integerPart.insert(0, CN_ZERO);
            }
            zero = section == 0;
            unitPos++;
            tempInt /= 10000;
        }

        // 只在整数部分 > 0 时拼接"整数部分+元"(纯小数如 0.05 直接以"伍分"开头)
        if (integer > 0) {
            sb.insert(0, integerPart.append(CN_DOLLAR));
        }

        String result = sb.toString()
                .replaceAll("零+", "零")   // 合并连续"零"为一个
                .replaceAll("零元", "元")   // "元"前多余的"零"去掉
                .replaceAll("零角", "")     // "零角"不合法，去掉
                .replaceAll("零分", "");    // "零分"不合法，去掉

        return negative ? CN_NEGATIVE + result : result;
    }

}
