package com.na.common.exceptions;


import com.na.common.result.enums.NaStatus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface NaApiException {
    NaStatus value() default NaStatus.DEFAULT;
}
