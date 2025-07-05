package com.na.common.utils;

import org.apache.commons.lang3.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.TimeZone;

public class NaDateTimeUtil {
    public enum DateFormat {
        YYYY_MM_DD_HH_MM_ss("yyyy-MM-dd HH:mm:ss"),
        YYYY_MM_DD_HH_MM_SS_CHINESE("yyyy年MM月dd日 HH:mm:ss"),
        YYYY_X_MM_X_DD_HH_MM_ss("yyyy/MM/dd HH:mm:ss"),

        YYYY_MM_DD_HH_MM("yyyy-MM-dd HH:mm"),
        YYY_MM_DD_HH_MM_CHINESE("yyyy年MM月dd日 HH:mm"),

        YYYY_MM_DD("yyyy-MM-dd"),
        YYYY_MM_DD_CHINESE("yyyy年MM月dd日"),
        YYYY_X_MM_X_DD("yyyy/MM/dd"),
        YYYYMMDD("yyyyMMdd"),

        HH_MM_SS("HH:mm:ss"),
        HH_MM_SS_CHINESE("HH时mm分ss秒"),

        HH_MM("HH:mm"),
        HH_MM_CHINESE("HH时mm分"),

        SS("ss"),
        SS_CHINESE("ss秒"),

        YYYY_MM("yyyy-MM"),
        YYYY_MM_CHINESE("yyyy年MM月"),
        YYYYMM("yyyyMM"),

        MM_DD("MM-dd"),
        MM_DD_CHINESE("MM月dd日"),

        YYYY("yyyy"),
        YYYY_CHINESE("yyyy年"),

        MM("MM"),
        MM_CHINESE("MM月"),

        DD("dd"),
        DD_CHINESE("dd日"),

        ;

        private String value;

        DateFormat(String value) {
            this.value = value;
        }

        public String getValue() {
            return this.value;
        }
    }

    /**
     * 显式使用 Asia/Shanghai 时区
     *
     * @return LocalDateTime
     */
    public static LocalDateTime getCurrentBeijingDateTime() {
        ZonedDateTime beijingTime = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"));
        return beijingTime.toLocalDateTime(); // 去掉时区，只保留 +8 时间
    }

    /**
     * 显式使用 Asia/Shanghai 时区
     *
     * @return Date
     */
    public static Date getCurrentBeijingDate() {
        ZonedDateTime beijingTime = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"));
        return Date.from(beijingTime.toInstant());
    }

    public static long getBeijingTimestampMillis() {
        return ZonedDateTime.now(ZoneId.of("Asia/Shanghai"))
                .toInstant()
                .toEpochMilli();
    }

    public static long getBeijingTimestampAfterDays(int days) {
        return ZonedDateTime.now(ZoneId.of("Asia/Shanghai"))
                .plusDays(days)
                .toInstant()
                .toEpochMilli();
    }

    /**
     * 将 `Date` 对象转换为字符串。默认使用北京时间（Asia/Shanghai）时区，
     * 如果传入的 `zoneId` 不为 null，则按照指定时区进行转换。
     *
     * @param date       需要转换的 `Date` 对象
     * @param timeFormat 日期格式，使用 `DateFormat` 枚举。如果为 null，则默认为 `YYYY_MM_DD_HH_mm_ss`
     * @param zoneId     时区；如果为 null，则使用北京时间（Asia/Shanghai）
     * @return 转换后的日期字符串
     */
    public static String parseToString(Date date, DateFormat timeFormat, ZoneId zoneId) {
        // 如果日期为空，返回空字符串
        if (date == null) {
            return StringUtils.EMPTY;
        }

        // 如果时间格式为 null，默认为 YYYY_MM_DD_HH_mm_ss 格式
        if (timeFormat == null) {
            timeFormat = DateFormat.YYYY_MM_DD_HH_MM_ss;
        }

        // 使用 SimpleDateFormat 来格式化日期
        SimpleDateFormat sdf = new SimpleDateFormat(timeFormat.getValue());

        // 如果时区为 null，默认使用北京时间（Asia/Shanghai）
        if (zoneId == null) {
            zoneId = ZoneId.of("Asia/Shanghai");
        }

        // 将时间设置为指定时区
        sdf.setTimeZone(TimeZone.getTimeZone(zoneId));

        // 返回格式化后的日期字符串
        return sdf.format(date);
    }

    /**
     * 将 `LocalDateTime` 对象转换为字符串。默认使用北京时间（Asia/Shanghai）时区，
     * 如果传入的 `zoneId` 不为 null，则按照指定时区进行转换。
     *
     * @param currentDateTime 需要转换的 `LocalDateTime` 对象
     * @param timeFormat      日期格式，使用 `DateFormat` 枚举。如果为 null，则默认为 `YYYY_MM_DD_HH_mm_ss`
     * @param zoneId          时区；如果为 null，则使用北京时间（Asia/Shanghai）
     * @return 转换后的日期字符串
     */
    public static String parseToString(LocalDateTime currentDateTime, DateFormat timeFormat, ZoneId zoneId) {
        // 如果当前日期时间为空，返回空字符串
        if (currentDateTime == null) {
            return StringUtils.EMPTY;
        }

        // 如果时间格式为 null，默认为 YYYY_MM_DD_HH_mm_ss 格式
        if (timeFormat == null) {
            timeFormat = DateFormat.YYYY_MM_DD_HH_MM_ss;
        }

        // 如果时区为 null，默认使用北京时间（Asia/Shanghai）
        if (zoneId == null) {
            zoneId = ZoneId.of("Asia/Shanghai");
        }

        // 使用指定时区的 DateTimeFormatter 来格式化 LocalDateTime 对象
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(timeFormat.getValue()).withZone(zoneId);

        // 将 LocalDateTime 转换为指定时区的字符串并返回
        return currentDateTime.atZone(zoneId).format(dtf);
    }

    public static String parseToString(Long time, DateFormat timeFormat) {
        // 如果当前日期时间为空，返回空字符串
        if (time == null) {
            return StringUtils.EMPTY;
        }

        if (timeFormat == null) {
            timeFormat = DateFormat.SS_CHINESE;
        }

        // 将毫秒转为总秒数（1秒 = 1000毫秒）
        long totalSeconds = time / 1000;

        // 计算出秒（不足1分钟的部分）
        long seconds = totalSeconds % 60;

        // 计算出分钟（去掉小时后剩下的部分）
        long minutes = (totalSeconds / 60) % 60;

        // 计算出小时（总秒数除以3600，即1小时 = 3600秒）
        long hours = totalSeconds / 3600;

        if (timeFormat.equals(DateFormat.HH_MM_SS)) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else if (timeFormat.equals(DateFormat.HH_MM_SS_CHINESE)) {
            return String.format("%02d时%02d分%02d秒", hours, minutes, seconds);
        } else if (timeFormat.equals(DateFormat.HH_MM)) {
            return String.format("%02d:%02d", hours, minutes);
        } else if (timeFormat.equals(DateFormat.HH_MM_CHINESE)) {
            return String.format("%02d时%02d分", hours, minutes);
        } else if (timeFormat.equals(DateFormat.SS)) {
            return String.format("%02d", totalSeconds);
        } else if (timeFormat.equals(DateFormat.SS_CHINESE)) {
            return String.format("%02d秒", totalSeconds);
        }
        return "";
    }

    /**
     * 将字符串日期转换为 `Date` 对象。默认使用北京时间（Asia/Shanghai）时区，
     * 如果传入的 `zoneId` 不为 null，则按照指定时区进行转换。
     *
     * @param date       日期字符串
     * @param timeFormat 日期格式，使用 `DateFormat` 枚举。如果为 null，则默认为 `YYYY_MM_DD_HH_mm_ss`
     * @param zoneId     时区；如果为 null，则使用北京时间（Asia/Shanghai）
     * @return 转换后的 `Date` 对象，转换失败返回 null
     */
    public static Date parseToDate(String date, DateFormat timeFormat, ZoneId zoneId) {
        // 如果日期字符串为空，返回 null
        if (date == null) {
            return null;
        }

        // 如果时间格式为 null，默认为 YYYY_MM_DD_HH_mm_ss 格式
        if (timeFormat == null) {
            timeFormat = DateFormat.YYYY_MM_DD_HH_MM_ss;
        }

        // 如果时区为 null，默认使用北京时间（Asia/Shanghai）
        if (zoneId == null) {
            zoneId = ZoneId.of("Asia/Shanghai");
        }

        // 使用指定的日期格式进行解析
        SimpleDateFormat sdf = new SimpleDateFormat(timeFormat.getValue());

        // 将时间格式设置为指定时区
        sdf.setTimeZone(TimeZone.getTimeZone(zoneId));

        try {
            // 解析日期字符串
            Date parsedDate = sdf.parse(date);

            // 加上 8 小时（单位：毫秒）
            long eightHoursInMillis = 8 * 60 * 60 * 1000L;
            return new Date(parsedDate.getTime() + eightHoursInMillis);
        } catch (ParseException e) {
            System.out.println("日期解析失败：" + e.getMessage());
            throw new RuntimeException("日期解析失败：" + e.getMessage());
        }
    }

    /**
     * 将日期字符串解析为 LocalDateTime 对象，并根据传入的时区进行转换。
     * 如果传入的时区为 null，则默认使用北京时间（Asia/Shanghai）。
     *
     * @param date       日期字符串
     * @param timeFormat 日期字符串的格式
     * @param zoneId     解析时使用的时区，如果为 null，则使用北京时间（Asia/Shanghai）
     * @return 解析后的 LocalDateTime 对象，如果解析失败则返回 null
     */
    public static LocalDateTime parseToLocalDateTime(String date, DateFormat timeFormat, ZoneId zoneId) {
        // 如果日期字符串为空，返回 null
        if (date == null) {
            return null;
        }

        // 如果时间格式为 null，默认为 YYYY_MM_DD_HH_mm_ss 格式
        if (timeFormat == null) {
            timeFormat = DateFormat.YYYY_MM_DD_HH_MM_ss;
        }

        // 如果时区为 null，默认使用北京时间（Asia/Shanghai）
        if (zoneId == null) {
            zoneId = ZoneId.of("Asia/Shanghai");
        }

        // 创建 DateTimeFormatter，使用指定的日期格式进行解析
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(timeFormat.getValue());

        // 将日期字符串解析为 LocalDateTime（不带时区）
        LocalDateTime localDateTime = null;
        try {
            localDateTime = LocalDateTime.parse(date, formatter);
        } catch (DateTimeParseException e) {
            System.out.println("日期解析失败：" + e.getMessage());
            return null;  // 解析失败返回 null
        }

        // 将 LocalDateTime 转换为指定时区的 ZonedDateTime
        ZonedDateTime zonedDateTime = localDateTime.atZone(zoneId);

        // 返回转换后的 LocalDateTime
        return zonedDateTime.toLocalDateTime();
    }

    /**
     * 将时间戳（毫秒级）转换为 `Date` 对象。支持秒级时间戳（10位）自动转换为毫秒级（13位）。
     * 如果传入的 `zoneId` 为 `null`，则默认使用北京时间（Asia/Shanghai）。
     *
     * @param date   时间戳（毫秒级或秒级）
     * @param zoneId 时区，如果为 null，则使用北京时间（Asia/Shanghai）
     * @return 转换后的 `Date` 对象，若 `date` 为 null，则返回 null
     */
    public static Date parseLongToDateAllowNull(Long date, ZoneId zoneId) {
        // 如果时间戳为 null，直接返回 null
        if (date == null) {
            return null;
        }

        // 如果是10位时间戳（秒级），将其转换为13位时间戳（毫秒级）
        if (String.valueOf(date).length() == 10) {
            date *= 1000;  // 转换为毫秒级
        }

        // 如果 zoneId 为 null，使用北京时间（Asia/Shanghai）作为默认时区
        if (zoneId == null) {
            zoneId = ZoneId.of("Asia/Shanghai");
        }

        // 将时间戳转换为 Instant
        Instant instant = Instant.ofEpochMilli(date);

        // 根据传入的时区进行转换
        ZonedDateTime zonedDateTime = instant.atZone(zoneId);

        // 转换为 Date 对象
        return Date.from(zonedDateTime.toInstant());
    }

    /**
     * 将时间戳转换为指定时区的 LocalDateTime。
     * <p>
     * 如果传入的 zoneId 为 null，默认按北京时间（Asia/Shanghai）处理，
     * 即假设传入的时间戳代表的是中国标准时间（UTC+8）。
     *
     * @param date   时间戳，单位可以是秒（10位）或毫秒（13位）
     * @param zoneId 目标时区；为 null 时默认使用 Asia/Shanghai（北京时间）
     * @return 转换后的 LocalDateTime
     */
    public static LocalDateTime parseLongToLocalDateTimeAllowNull(Long date, ZoneId zoneId) {
        if (date == null) {
            return null;
        }

        // 如果是10位时间戳（秒级），将其转换为13位时间戳（毫秒级）
        if (String.valueOf(date).length() == 10) {
            date *= 1000;  // 转换为毫秒级
        }

        // 创建 Instant 对象
        Instant instant = Instant.ofEpochMilli(date);

        // 如果传入时区为空，则默认认为是北京时间（UTC+8），返回其本地时间
        if (zoneId == null) {
            zoneId = ZoneId.of("Asia/Shanghai");  // 假设为中国时间
        }

        return LocalDateTime.ofInstant(instant, zoneId);
    }

    /**
     * 将指定日期转换为时间戳（毫秒）。
     * 如果提供的时区为 null，则默认为北京时间（Asia/Shanghai）。
     *
     * @param date   要转换的日期对象
     * @param zoneId 要使用的时区，如果为 null，则默认为北京时间（Asia/Shanghai）
     * @return 转换后的时间戳（毫秒）
     */
    public static Long dateToLong(Date date, ZoneId zoneId) {
        if (date == null) {
            return null; // 如果日期为 null，则返回 null
        }

        // 如果 zoneId 为 null，则使用北京时间（Asia/Shanghai）
        if (zoneId == null) {
            zoneId = ZoneId.of("Asia/Shanghai");
        }

        // 获取指定时区的 ZonedDateTime
        ZonedDateTime zonedDateTime = date.toInstant().atZone(zoneId);

        // 返回该时区的时间戳（毫秒）
        return zonedDateTime.toInstant().toEpochMilli();
    }

    /**
     * 将 LocalDateTime 转换为指定时区的时间戳（毫秒）。
     * 如果传入的时区为 null，则默认为北京时间（Asia/Shanghai）。
     *
     * @param date   要转换的 LocalDateTime 对象
     * @param zoneId 要使用的时区，如果为 null，则使用 Asia/Shanghai（北京时间）
     * @return 转换后的时间戳（毫秒）
     */
    public static Long dateToLong(LocalDateTime date, ZoneId zoneId) {
        if (date == null) {
            return null; // 日期为 null 直接返回 null
        }

        // 如果时区为 null，默认使用北京时间
        if (zoneId == null) {
            zoneId = ZoneId.of("Asia/Shanghai");
        }

        // 将 LocalDateTime 转换为指定时区的 ZonedDateTime
        ZonedDateTime zonedDateTime = date.atZone(zoneId);

        // 转换为 Instant 获取时间戳（毫秒）
        return zonedDateTime.toInstant().toEpochMilli();
    }

    /**
     * 将字符串日期转换为时间戳（毫秒）。如果传入的 `zoneId` 为 null，则使用北京时间（Asia/Shanghai）进行转换。
     *
     * @param dateString 需要转换的日期字符串
     * @param timeFormat 日期格式，使用 `DateFormat` 枚举。如果为 null，则默认为 `YYYY_MM_DD_HH_mm_ss`
     * @param zoneId     时区；如果为 null，则使用北京时间（Asia/Shanghai）
     * @return 转换后的时间戳（毫秒）
     */
    public static Long dateToLong(String dateString, DateFormat timeFormat, ZoneId zoneId) {
        // 如果输入的日期字符串为空，直接返回 null
        if (StringUtils.isEmpty(dateString)) {
            return null;
        }

        // 如果时间格式为 null，默认为 YYYY_MM_DD_HH_mm_ss 格式
        if (timeFormat == null) {
            timeFormat = DateFormat.YYYY_MM_DD_HH_MM_ss;
        }

        // 使用传入的时间格式化器来解析字符串为 LocalDateTime 对象
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(timeFormat.getValue());
        LocalDateTime dateTime = LocalDateTime.parse(dateString, formatter);

        // 如果时区为 null，则默认使用北京时间（Asia/Shanghai）
        if (zoneId == null) {
            zoneId = ZoneId.of("Asia/Shanghai");
        }

        // 将 LocalDateTime 转换为指定时区的 Instant 对象，再转换为毫秒级时间戳
        return dateTime.atZone(zoneId).toInstant().toEpochMilli();
    }

    /**
     * 计算两个 LocalDateTime 之间的时间差
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 时间差（毫秒）
     */
    public static Long calculateTimeDifference(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return null;
        }
        // 计算时间差
        Duration duration = Duration.between(start, end);
        // 返回时间差（毫秒）
        return duration.toMillis();
    }

    /**
     * 判断当前时间是否大于传入的时间加上指定小时数（支持自定义时区）
     *
     * @param date   起始时间，不能为 null
     * @param hour   增加的小时数，不能为 null
     * @param zoneId 时区，如果为 null 则默认为北京时间（Asia/Shanghai）
     * @return 如果当前时间大于 date 加上 hour 小时，则返回 true；否则返回 false
     */
    public static Boolean isOutTime(Date date, Integer hour, ZoneId zoneId) {
        if (date == null || hour == null) {
            return false;
        }

        // 如果 zoneId 为 null，默认为北京时间
        if (zoneId == null) {
            zoneId = ZoneId.of("Asia/Shanghai");
        }

        // 将传入的时间转换为指定时区的 ZonedDateTime
        ZonedDateTime zonedStart = date.toInstant().atZone(zoneId);

        // 加上指定的小时数
        ZonedDateTime zonedEnd = zonedStart.plusHours(hour);

        // 获取当前时间的 ZonedDateTime（同样在指定时区）
        ZonedDateTime now = ZonedDateTime.now(zoneId);

        // 判断当前时间是否超过结束时间
        return now.isAfter(zonedEnd);
    }

    /**
     * 判断当前时间是否大于传入的时间加上指定小时数（支持自定义时区）
     *
     * @param date   起始时间，不能为 null
     * @param hour   增加的小时数，不能为 null
     * @param zoneId 时区，如果为 null 则默认为北京时间（Asia/Shanghai）
     * @return 如果当前时间大于 date 加上 hour 小时，则返回 true；否则返回 false
     */
    public static Boolean isOutTime(LocalDateTime date, Integer hour, ZoneId zoneId) {
        if (date == null || hour == null) {
            return false;
        }

        // 如果 zoneId 为 null，默认为北京时间
        if (zoneId == null) {
            zoneId = ZoneId.of("Asia/Shanghai");
        }

        // 将传入的时间转换为指定时区的 ZonedDateTime
        ZonedDateTime zonedStart = date.atZone(zoneId);

        // 加上指定的小时数
        ZonedDateTime zonedEnd = zonedStart.plusHours(hour);

        // 获取当前时间的 ZonedDateTime（同样在指定时区）
        ZonedDateTime now = ZonedDateTime.now(zoneId);

        // 判断当前时间是否超过结束时间
        return now.isAfter(zonedEnd);
    }


    public static void main(String[] args) {
        System.out.println(getBeijingTimestampMillis());
        System.out.println(getBeijingTimestampAfterDays(1));
        System.out.println(getBeijingTimestampAfterDays(-1));
    }

}
