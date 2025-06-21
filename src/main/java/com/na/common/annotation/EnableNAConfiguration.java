package com.na.common.annotation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class EnableNAConfiguration {
    static {
        System.out.println("EnableNAConfiguration 加载成功!");
        log.info("EnableNAConfiguration 加载成功!");
    }
}
