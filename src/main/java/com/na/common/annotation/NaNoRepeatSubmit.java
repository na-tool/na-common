package com.na.common.annotation;

import com.na.common.utils.NaDateTimeUtil;

import java.lang.annotation.*;

/**
 * 防止重复提交
 * <code>@NaNoRepeatSubmit</code>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NaNoRepeatSubmit {
    long value() default 50; // 毫秒
    String msg() default "重复请求，请稍后重试"; // 提示信息
    int num() default 1; // 提交次数
    boolean showRemainingTime() default false; // 是否显示剩余时间

    NaDateTimeUtil.DateFormat dateFormat() default NaDateTimeUtil.DateFormat.SS_CHINESE; // 时间格式
    String zoneId() default "Asia/Shanghai"; // 时区
    boolean requestParams() default false; // 是否使用请求参数构建key
}
