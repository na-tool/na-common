package com.na.common.annotation;

import java.lang.annotation.*;

/**
 * 服务无token调用声明注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NoToken {

}
