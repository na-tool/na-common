package com.na.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface NaExcelImportColumn {
    int index(); // 列索引
    String name() default ""; // 可选：列名称，仅作参考
}
