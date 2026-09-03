package com.na.common.utils;

import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * ID 生成工具类
 * <p>
 * 提供时间戳ID、UUID、随机串、订单号、纯数字ID、带业务前缀ID等多种生成方式。
 * 注意事项：
 * 1. 基于时间戳的ID在同一毫秒内并发时存在碰撞可能，随机段越长越安全；
 * 2. UUID/随机串不可按时间排序，需要时间有序时请使用时间戳类ID；
 * 3. 随机段使用 ThreadLocalRandom，线程安全且无锁竞争，但不适用于安全场景（如密钥、令牌）
 */
public class IDUtil {

    /** 随机字符集: 数字(10) + 大写字母(26), 共36位 */
    private static final char[] RANDOM_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    /** 订单号时间部分格式: yyyyMMddHHmmss, 共14位 */
    private static final DateTimeFormatter ORDER_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** 默认随机段长度 */
    private static final int DEFAULT_RANDOM_LENGTH = 8;

    /** 时间戳ID中时间戳占用的位数(毫秒级时间戳为13位) */
    private static final int TIMESTAMP_LENGTH = 13;

    /** 纯静态工具类，禁止实例化 */
    private IDUtil() {
    }

    /**
     * 生成 Long 型ID：当前毫秒级时间戳(13位)。
     * 注意: 同毫秒并发调用会重复，仅适用于单机低频或允许重复的场景
     *
     * @return 当前毫秒级时间戳
     */
    public static Long ID() {
        return DateTimeUtil.getBeijingTimestampMillis();
    }

    /**
     * 生成 32 位小写 UUID(去掉中划线)，全局唯一
     *
     * @return 32位十六进制小写字符串
     */
    public static String getUUID32() {
        return UUID.randomUUID().toString().replace("-", "").toLowerCase();
    }

    /**
     * 生成字符串ID：毫秒时间戳(13位) + 指定长度随机串(数字+大写字母)。
     * 随机段上限 17 位，总长度不超过 30 位，避免超出常见字段/表单长度限制
     *
     * @param length 随机段长度；null 或 <=0 时默认 8 位
     * @return 时间戳 + 随机串组成的ID
     */
    public static String ID(Integer length) {
        int len = (length == null || length <= 0) ? DEFAULT_RANDOM_LENGTH : Math.min(length, 17);
        return DateTimeUtil.getBeijingTimestampMillis() + getRandomNo(len);
    }

    /**
     * 生成指定长度的随机串，字符集为 数字 + 大写字母(共36种)
     *
     * @param length 随机串长度；null 或 <=0 时返回空字符串
     * @return 随机字符串
     */
    public static String getRandomNo(Integer length) {
        if (length == null || length <= 0) {
            return "";
        }
        // ThreadLocalRandom 为线程私有的随机源，高并发下无锁竞争
        ThreadLocalRandom random = ThreadLocalRandom.current();
        StringBuilder buffer = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            buffer.append(RANDOM_CHARS[random.nextInt(RANDOM_CHARS.length)]);
        }
        return buffer.toString();
    }

    /**
     * 生成订单号风格ID：yyyyMMddHHmmss(14位) + 6位随机数字，共20位纯数字。
     * 同一秒内并发由随机段区分，极端高并发下仍有碰撞可能
     *
     * @return 20位纯数字订单号
     */
    public static String getOrderNo() {
        String timestamp = DateTimeUtil.getCurrentBeijingDateTime().format(ORDER_TIME_FORMAT);
        return timestamp + getRandomNumeric(6);
    }

    /**
     * 生成纯数字ID：毫秒时间戳(13位) + 指定长度随机数字，适用于仅支持数字主键的场景
     *
     * @param length 总长度；null 或 <=0 时默认 20 位，最小 13 位(仅时间戳)
     * @return 纯数字ID字符串
     */
    public static String getNumericId(Integer length) {
        int total = (length == null || length <= 0) ? 20 : Math.max(length, TIMESTAMP_LENGTH);
        return DateTimeUtil.getBeijingTimestampMillis() + getRandomNumeric(total - TIMESTAMP_LENGTH);
    }

    /**
     * 生成带业务前缀的ID：前缀 + 时间戳(13位) + 随机串。
     * 常用于区分业务线，如 "ORD"、"PAY"、"REFUND"
     *
     * @param prefix 业务前缀，为 null 时不加前缀
     * @param length 随机段长度，含义同 {@link #ID(Integer)}
     * @return 前缀 + 时间戳 + 随机串组成的ID
     */
    public static String getIDWithPrefix(String prefix, Integer length) {
        return (prefix == null ? "" : prefix) + ID(length);
    }

    /**
     * 生成指定长度的纯数字随机串
     *
     * @param length 随机数字长度；<=0 时返回空字符串
     * @return 纯数字随机字符串
     */
    private static String getRandomNumeric(int length) {
        if (length <= 0) {
            return "";
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        StringBuilder buffer = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            buffer.append(random.nextInt(10));
        }
        return buffer.toString();
    }
}
