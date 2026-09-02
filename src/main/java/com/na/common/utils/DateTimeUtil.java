package com.na.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.TimeZone;

/**
 * 日期时间工具类
 * <p>
 * 统一约定：所有带 ZoneId 参数的方法，zoneId 为 null 时默认使用北京时间（Asia/Shanghai）。
 * 时间戳均为毫秒级（绝对时刻，与时区无关）；LocalDateTime / 字符串均为墙钟时间，时区仅影响解释方式。
 */
@Slf4j
public class DateTimeUtil {

    /** 默认时区：北京时间 */
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Shanghai");

    /** 一小时的毫秒数 */
    private static final long ONE_HOUR_MILLIS = 60 * 60 * 1000L;

    /** 纯静态工具类，禁止实例化 */
    private DateTimeUtil() {
    }

    /**
     * 日期格式枚举
     * <p>
     * 命名规则: X 代表斜杠 /，CHINESE 代表中文格式
     */
    public enum DateFormat {
        YYYY_MM_DD_HH_MM_ss("yyyy-MM-dd HH:mm:ss"),
        YYYY_MM_DD_HH_MM_SS_CHINESE("yyyy年MM月dd日 HH:mm:ss"),
        YYYY_X_MM_X_DD_HH_MM_ss("yyyy/MM/dd HH:mm:ss"),

        YYYY_MM_DD_HH_MM("yyyy-MM-dd HH:mm"),
        YYY_MM_DD_HH_MM_CHINESE("yyyy年MM月dd日 HH:mm"),
        YYYY_X_MM_X_DD_HH_MM("yyyy/MM/dd HH:mm"),

        YYYY_MM_DD("yyyy-MM-dd"),
        YYYY_MM_DD_CHINESE("yyyy年MM月dd日"),
        YYYY_X_MM_X_DD("yyyy/MM/dd"),
        YYYYMMDD("yyyyMMdd"),

        HH_MM_SS("HH:mm:ss"),
        HH_MM_SS_CHINESE("HH时mm分ss秒"),

        HH_MM("HH:mm"),
        HH_MM_CHINESE("HH时mm分"),

        MM_SS("mm:ss"),
        MM_SS_CHINESE("mm分ss秒"),

        HH("HH"),
        HH_CHINESE("HH时"),

        MM("mm"),
        MM_CHINESE("mm分"),

        SS("ss"),
        SS_CHINESE("ss秒"),

        YYYY_MM("yyyy-MM"),
        YYYY_MM_CHINESE("yyyy年MM月"),
        YYYYMM("yyyyMM"),

        MM_DD("MM-dd"),
        MM_DD_CHINESE("MM月dd日"),

        YYYY("yyyy"),
        YYYY_CHINESE("yyyy年"),

        MMY("MM"),
        MMY_CHINESE("MM月"),

        DD("dd"),
        DD_CHINESE("dd日"),

        ;

        private final String value;

        DateFormat(String value) {
            this.value = value;
        }

        public String getValue() {
            return this.value;
        }
    }

    /**
     * 获取当前北京时间（墙钟时间，已去掉时区信息）
     *
     * @return 北京时区的当前 LocalDateTime
     */
    public static LocalDateTime getCurrentBeijingDateTime() {
        // ZonedDateTime 取北京时区墙钟，转 LocalDateTime 后即无时区的 +8 时间
        return ZonedDateTime.now(DEFAULT_ZONE).toLocalDateTime();
    }

    /**
     * 获取当前时间。
     * 注意: Date 本身无时区概念，其存储的是绝对时刻，任何时区下 new Date() 结果一致
     *
     * @return 当前时刻的 Date
     */
    public static Date getCurrentBeijingDate() {
        return new Date();
    }

    /**
     * 获取当前毫秒级时间戳。
     * 注意: 时间戳是绝对时刻，与时区无关，"北京时间时间戳"与 UTC 时间戳数值相同
     *
     * @return 当前毫秒级时间戳
     */
    public static long getBeijingTimestampMillis() {
        return System.currentTimeMillis();
    }

    /**
     * 获取当前时间加上指定天数的毫秒级时间戳（常用于过期时间计算）
     *
     * @param days 偏移天数，可为负数表示过去
     * @return 偏移后的毫秒级时间戳
     */
    public static long getBeijingTimestampAfterDays(int days) {
        return ZonedDateTime.now(DEFAULT_ZONE).plusDays(days).toInstant().toEpochMilli();
    }

    /**
     * 将日期字符串解析为 {@link Date} 对象（使用 JVM 默认时区解释）。
     * 注意: 与三参重载不同，本方法不追加 8 小时偏移，解析结果即为字符串对应的真实时刻
     *
     * @param date       日期字符串
     * @param timeFormat 日期格式；为 null 时默认 {@link DateFormat#YYYY_MM_DD_HH_MM_ss}
     * @return 解析后的 Date；入参为空返回 null
     * @throws RuntimeException 解析失败时抛出
     */
    public static Date parseToDate(String date, DateTimeUtil.DateFormat timeFormat) {
        if (date == null || date.trim().isEmpty()) {
            return null;
        }

        if (timeFormat == null) {
            timeFormat = DateFormat.YYYY_MM_DD_HH_MM_ss;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat(timeFormat.getValue());
            // 严格解析，拒绝 "2024-13-40" 这类非法日期
            sdf.setLenient(false);
            return sdf.parse(date);
        } catch (ParseException e) {
            log.error("日期解析失败, date:[{}], format:[{}]", date, timeFormat.getValue(), e);
            throw new RuntimeException("日期解析失败：" + e.getMessage(), e);
        }
    }

    /**
     * 将 {@link Date} 对象格式化为字符串，默认使用北京时间
     *
     * @param date       需要转换的 Date 对象
     * @param timeFormat 日期格式；为 null 时默认 {@link DateFormat#YYYY_MM_DD_HH_MM_ss}
     * @param zoneId     时区；为 null 时默认北京时间
     * @return 转换后的日期字符串；date 为 null 时返回空字符串
     */
    public static String parseToString(Date date, DateFormat timeFormat, ZoneId zoneId) {
        if (date == null) {
            return StringUtils.EMPTY;
        }
        if (timeFormat == null) {
            timeFormat = DateFormat.YYYY_MM_DD_HH_MM_ss;
        }
        if (zoneId == null) {
            zoneId = DEFAULT_ZONE;
        }

        SimpleDateFormat sdf = new SimpleDateFormat(timeFormat.getValue());
        sdf.setTimeZone(TimeZone.getTimeZone(zoneId));
        return sdf.format(date);
    }

    /**
     * 将 {@link LocalDateTime} 对象格式化为字符串。
     * 注意: LocalDateTime 本身无时区，格式化输出即其墙钟时间，zoneId 参数不影响结果，仅为兼容保留
     *
     * @param currentDateTime 需要转换的 LocalDateTime 对象
     * @param timeFormat      日期格式；为 null 时默认 {@link DateFormat#YYYY_MM_DD_HH_MM_ss}
     * @param zoneId          兼容参数，实际不影响输出
     * @return 转换后的日期字符串；入参为 null 时返回空字符串
     */
    public static String parseToString(LocalDateTime currentDateTime, DateFormat timeFormat, ZoneId zoneId) {
        if (currentDateTime == null) {
            return StringUtils.EMPTY;
        }
        if (timeFormat == null) {
            timeFormat = DateFormat.YYYY_MM_DD_HH_MM_ss;
        }
        // LocalDateTime 无时区概念，直接按墙钟时间格式化(与原实现等价)
        return currentDateTime.format(DateTimeFormatter.ofPattern(timeFormat.getValue()));
    }

    /**
     * 将毫秒时长格式化为时分秒字符串（非日期时间戳，而是时间段）。
     * 注意: 本方法与其他 parseToString 默认格式不同，为 null 时默认 {@link DateFormat#SS_CHINESE}
     *
     * @param time       毫秒时长
     * @param timeFormat 支持 HH_MM_SS / HH_MM_SS_CHINESE / HH_MM / HH_MM_CHINESE / SS / SS_CHINESE
     * @return 格式化后的时长字符串；入参为 null 或格式不支持时返回空字符串
     */
    public static String parseToString(Long time, DateFormat timeFormat) {
        if (time == null) {
            return StringUtils.EMPTY;
        }
        if (timeFormat == null) {
            timeFormat = DateFormat.SS_CHINESE;
        }

        // 毫秒转为总秒数(1秒=1000毫秒)
        long totalSeconds = time / 1000;
        // 秒: 不足1分钟的部分
        long seconds = totalSeconds % 60;
        // 分: 去掉小时后剩余的部分
        long minutes = (totalSeconds / 60) % 60;
        // 时: 总秒数除以3600(1小时=3600秒)
        long hours = totalSeconds / 3600;

        switch (timeFormat) {
            case HH_MM_SS:
                return String.format("%02d:%02d:%02d", hours, minutes, seconds);
            case HH_MM_SS_CHINESE:
                return String.format("%02d时%02d分%02d秒", hours, minutes, seconds);
            case HH_MM:
                return String.format("%02d:%02d", hours, minutes);
            case HH_MM_CHINESE:
                return String.format("%02d时%02d分", hours, minutes);
            case SS:
                return String.format("%02d", totalSeconds);
            case SS_CHINESE:
                return String.format("%02d秒", totalSeconds);
            default:
                return "";
        }
    }

    /**
     * 将字符串日期解析为 {@link Date} 对象，并追加 8 小时偏移。
     * <p>
     * 【重要-历史行为】解析后刻意 +8 小时，使返回 Date 的 epoch 毫秒值在"数值上"等于北京墙钟时间
     * （即 {@code getTime()} 并不代表真实时刻，而是"伪 UTC"毫秒），与三参 {@link #dateToLong(String, DateFormat, ZoneId)}
     * 及常规时间戳体系相差 8 小时。该行为为历史兼容保留，新代码请使用 {@link #parseToLocalDateTime} 或
     * {@link #parseToDate(String, DateTimeUtil.DateFormat)}（后者无偏移）。
     *
     * @param date       日期字符串
     * @param timeFormat 日期格式；为 null 时默认 {@link DateFormat#YYYY_MM_DD_HH_MM_ss}
     * @param zoneId     解析时使用的时区；为 null 时默认北京时间
     * @return 解析后追加 8 小时偏移的 Date；入参为 null 返回 null
     * @throws RuntimeException 解析失败时抛出（注意: 不返回 null）
     */
    public static Date parseToDate(String date, DateFormat timeFormat, ZoneId zoneId) {
        if (date == null) {
            return null;
        }
        if (timeFormat == null) {
            timeFormat = DateFormat.YYYY_MM_DD_HH_MM_ss;
        }
        if (zoneId == null) {
            zoneId = DEFAULT_ZONE;
        }

        SimpleDateFormat sdf = new SimpleDateFormat(timeFormat.getValue());
        sdf.setTimeZone(TimeZone.getTimeZone(zoneId));

        try {
            // 先按指定时区解析出该墙钟时间对应的绝对时刻
            Date parsedDate = sdf.parse(date);
            // 历史行为: +8h 使 epoch 毫秒值在数值上等于北京墙钟时间(伪UTC)，勿用于真实时刻计算
            return new Date(parsedDate.getTime() + 8 * ONE_HOUR_MILLIS);
        } catch (ParseException e) {
            log.error("日期解析失败, date:[{}], format:[{}]", date, timeFormat.getValue(), e);
            throw new RuntimeException("日期解析失败：" + e.getMessage(), e);
        }
    }

    /**
     * 将日期字符串解析为 {@link LocalDateTime} 对象（按墙钟时间解析）。
     *
     * @param date       日期字符串
     * @param timeFormat 日期格式；为 null 时默认 {@link DateFormat#YYYY_MM_DD_HH_MM_ss}
     * @param zoneId     兼容参数；LocalDateTime 本身无时区，实际不影响结果
     * @return 解析后的 LocalDateTime；入参为 null 或解析失败时返回 null
     */
    public static LocalDateTime parseToLocalDateTime(String date, DateFormat timeFormat, ZoneId zoneId) {
        if (date == null) {
            return null;
        }
        if (timeFormat == null) {
            timeFormat = DateFormat.YYYY_MM_DD_HH_MM_ss;
        }
        // zoneId 兼容保留；LocalDateTime 按墙钟时间解析，与时区无关

        try {
            return LocalDateTime.parse(date, DateTimeFormatter.ofPattern(timeFormat.getValue()));
        } catch (DateTimeParseException e) {
            log.error("日期解析失败, date:[{}], format:[{}]", date, timeFormat.getValue(), e);
            return null;
        }
    }

    /**
     * 将毫秒级时间戳转换为 {@link Date} 对象。
     * 注意: 时间戳是绝对时刻，zoneId 不影响转换结果，仅为兼容保留
     *
     * @param date   时间戳，支持毫秒(13位)或秒(10位，自动乘1000)
     * @param zoneId 兼容参数，实际不影响结果
     * @return 转换后的 Date；入参为 null 返回 null
     */
    public static Date parseLongToDateAllowNull(Long date, ZoneId zoneId) {
        if (date == null) {
            return null;
        }
        // 10位视为秒级时间戳，转为毫秒级
        if (String.valueOf(date).length() == 10) {
            date *= 1000;
        }
        // 时间戳为绝对时刻，直接构造 Date(与原时区往返转换结果等价)
        return new Date(date);
    }

    /**
     * 将毫秒级时间戳转换为指定时区的 {@link LocalDateTime}（墙钟时间）。
     *
     * @param date   时间戳，支持毫秒(13位)或秒(10位，自动乘1000)
     * @param zoneId 目标时区；为 null 时默认北京时间
     * @return 转换后的 LocalDateTime；入参为 null 返回 null
     */
    public static LocalDateTime parseLongToLocalDateTimeAllowNull(Long date, ZoneId zoneId) {
        if (date == null) {
            return null;
        }
        // 10位视为秒级时间戳，转为毫秒级
        if (String.valueOf(date).length() == 10) {
            date *= 1000;
        }
        if (zoneId == null) {
            zoneId = DEFAULT_ZONE;
        }
        // 按指定时区取墙钟时间
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(date), zoneId);
    }

    /**
     * 将 {@link Date} 转换为毫秒级时间戳。
     * 注意: Date 存储的是绝对时刻，转换与时区无关，zoneId 仅为兼容保留
     *
     * @param date   要转换的日期对象
     * @param zoneId 兼容参数，实际不影响结果
     * @return 毫秒级时间戳；入参为 null 返回 null
     */
    public static Long dateToLong(Date date, ZoneId zoneId) {
        if (date == null) {
            return null;
        }
        return date.getTime();
    }

    /**
     * 将 {@link LocalDateTime} 转换为指定时区的毫秒级时间戳。
     *
     * @param date   要转换的 LocalDateTime 对象
     * @param zoneId 解释墙钟时间所用时区；为 null 时默认北京时间
     * @return 毫秒级时间戳；入参为 null 返回 null
     */
    public static Long dateToLong(LocalDateTime date, ZoneId zoneId) {
        if (date == null) {
            return null;
        }
        if (zoneId == null) {
            zoneId = DEFAULT_ZONE;
        }
        return date.atZone(zoneId).toInstant().toEpochMilli();
    }

    /**
     * 将日期字符串按指定格式与时区解析为毫秒级时间戳。
     *
     * @param dateString 需要转换的日期字符串
     * @param timeFormat 日期格式；为 null 时默认 {@link DateFormat#YYYY_MM_DD_HH_MM_ss}
     * @param zoneId     解释墙钟时间所用时区；为 null 时默认北京时间
     * @return 毫秒级时间戳；入参为空返回 null
     * @throws DateTimeParseException 字符串与格式不匹配时抛出（未捕获，由调用方处理）
     */
    public static Long dateToLong(String dateString, DateFormat timeFormat, ZoneId zoneId) {
        if (StringUtils.isEmpty(dateString)) {
            return null;
        }
        if (timeFormat == null) {
            timeFormat = DateFormat.YYYY_MM_DD_HH_MM_ss;
        }
        if (zoneId == null) {
            zoneId = DEFAULT_ZONE;
        }
        return LocalDateTime.parse(dateString, DateTimeFormatter.ofPattern(timeFormat.getValue()))
                .atZone(zoneId).toInstant().toEpochMilli();
    }

    /**
     * 计算两个 LocalDateTime 之间的时间差
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 时间差（毫秒，end - start 可为负）；任一入参为 null 返回 null
     */
    public static Long calculateTimeDifference(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return null;
        }
        return Duration.between(start, end).toMillis();
    }

    /**
     * 判断当前时间是否超过传入时间加上指定小时数（常用于超时校验）
     *
     * @param date   起始时间；为 null 时返回 false
     * @param hour   增加的小时数；为 null 时返回 false
     * @param zoneId 时区；为 null 时默认北京时间
     * @return 当前时间大于 date + hour 小时返回 true，否则 false
     */
    public static Boolean isOutTime(Date date, Integer hour, ZoneId zoneId) {
        if (date == null || hour == null) {
            return false;
        }
        if (zoneId == null) {
            zoneId = DEFAULT_ZONE;
        }
        ZonedDateTime end = date.toInstant().atZone(zoneId).plusHours(hour);
        return ZonedDateTime.now(zoneId).isAfter(end);
    }

    /**
     * 判断当前时间是否超过传入时间加上指定小时数（常用于超时校验）
     *
     * @param date   起始时间；为 null 时返回 false
     * @param hour   增加的小时数；为 null 时返回 false
     * @param zoneId 时区；为 null 时默认北京时间
     * @return 当前时间大于 date + hour 小时返回 true，否则 false
     */
    public static Boolean isOutTime(LocalDateTime date, Integer hour, ZoneId zoneId) {
        if (date == null || hour == null) {
            return false;
        }
        if (zoneId == null) {
            zoneId = DEFAULT_ZONE;
        }
        ZonedDateTime end = date.atZone(zoneId).plusHours(hour);
        return ZonedDateTime.now(zoneId).isAfter(end);
    }

    /**
     * 主方法用于测试时间戳获取
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        System.out.println(getBeijingTimestampMillis());
        System.out.println(getBeijingTimestampAfterDays(1));
        System.out.println(getBeijingTimestampAfterDays(-1));
    }
}
