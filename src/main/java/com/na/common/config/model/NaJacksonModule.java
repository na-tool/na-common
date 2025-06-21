package com.na.common.config.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import com.na.common.config.custom.NaCustomBigDecimalSerializer;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 自定义 Jackson 模块，用于统一序列化/反序列化格式。
 * 包括对 Java 8 日期时间、Long、BigDecimal 的处理，防止精度丢失、格式不一致等问题。
 */
public class NaJacksonModule extends SimpleModule {

    // 全局默认格式：仅日期
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    // 全局默认格式：日期+时间
    public static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    // 全局默认格式：仅时间
    public static final String DEFAULT_TIME_FORMAT = "HH:mm:ss";

    public NaJacksonModule() {
        // 注册日期/时间序列化器（Java -> JSON）
        this.addSerializer(LocalDateTime.class,
                new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_FORMAT)));
        this.addSerializer(LocalDate.class,
                new LocalDateSerializer(DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)));
        this.addSerializer(LocalTime.class,
                new LocalTimeSerializer(DateTimeFormatter.ofPattern(DEFAULT_TIME_FORMAT)));

        // 注册日期/时间反序列化器（JSON -> Java）
        this.addDeserializer(LocalDateTime.class,
                new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_FORMAT)));
        this.addDeserializer(LocalDate.class,
                new LocalDateDeserializer(DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)));
        this.addDeserializer(LocalTime.class,
                new LocalTimeDeserializer(DateTimeFormatter.ofPattern(DEFAULT_TIME_FORMAT)));

        // Long -> String：防止前端 JS 精度丢失（Long 类型精度超过 JS 最大安全整数）
        this.addSerializer(Long.class, ToStringSerializer.instance);
//        this.addSerializer(Long.TYPE, ToStringSerializer.instance);

        // BigInteger -> String：防止精度丢失
        this.addSerializer(BigInteger.class, ToStringSerializer.instance);

        // BigDecimal 自定义序列化逻辑（可保留几位小数、去除末尾 0 等）
        this.addSerializer(BigDecimal.class, new NaCustomBigDecimalSerializer());
    }
}