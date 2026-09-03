package com.na.common.syslog.event;


import com.na.common.syslog.dto.SysLogDto;
import org.springframework.context.ApplicationEvent;

/**
 * 系统日志事件
 *
 */
public class SysLogEvent extends ApplicationEvent {

    public SysLogEvent(SysLogDto sysLogDto) {
        super(sysLogDto);
    }
}
