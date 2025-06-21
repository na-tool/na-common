package com.na.common.utils;

import com.na.common.constant.NaConst;
import org.apache.commons.lang3.StringUtils;

public class NaCommonUtil {
    /**
     * 是否为http(s)://开头
     *
     * @param link 链接
     * @return 结果
     */
    public static boolean ishttp(String link)
    {
        return StringUtils.startsWithAny(link, NaConst.HTTP, NaConst.HTTPS);
    }

    /**
     * 判断是否为windows操作系统
     * @return true 是，false 否
     */
    public static boolean isWindows() {
        String osName = System.getProperty("os.name").toLowerCase();
        return osName.contains("win");
    }

    /**
     * 对字符串进行脱敏处理，保留前3位和后2位，中间部分用星号替代。
     * 示例：
     * {@code mask("张三丰12345") ->} 张三***45
     * {@code mask("abcde") ->} abc***de
     * {@code mask("abc") ->} abc***（长度不足时自动补***）
     *
     * @param input 输入字符串
     * @return 脱敏后的字符串
     */
    public static String mask(String input) {
        if (input == null || input.isEmpty()) {
            return input;  // 空或 null 不处理
        }

        int len = input.length();

        // 长度 <= 5，返回原始 + ***
        if (len <= 5) {
            return input + "***";
        }

        // 正常脱敏逻辑：保留前3位和后2位
        String prefix = input.substring(0, 3);
        String suffix = input.substring(len - 2);
        return prefix + "***" + suffix;
    }


}
