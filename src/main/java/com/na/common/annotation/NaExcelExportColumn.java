package com.na.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface NaExcelExportColumn {
    String headerName();            // 表头名
    int order() default 0;          // 顺序
    String dateFormat() default ""; // 日期格式，支持 Date 和 LocalDateTime
}
