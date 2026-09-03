package com.na.common.annotation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class EnableConfig {
    static {
        System.out.println("EnableConfig 加载成功!");
        log.info("EnableConfig 加载成功!");
    }
}
