package com.na.common.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.na.common.utils.DateTimeUtil;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@ConditionalOnProperty(
        name = {"na.sqlTime.enable"},
        havingValue = "true",
        matchIfMissing = true
)
public class SqlTimeConfig implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        if (metaObject.hasSetter("createTime")) {
            this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, DateTimeUtil.getCurrentBeijingDateTime());
        }
        if (metaObject.hasSetter("isDel")) {
            Object isDel = getFieldValByName("isDel", metaObject);
            if(isDel == null){
                this.setFieldValByName("isDel", Boolean.FALSE, metaObject);
            }
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        LocalDateTime now = DateTimeUtil.getCurrentBeijingDateTime();
        if (metaObject.hasSetter("updateTime")) {
            this.setFieldValByName("updateTime",  now, metaObject);
        }
        if (metaObject.hasSetter("isDel")) {
            // ⭐ 如果是逻辑删除，同时填 deleteTime
            Object isDel = getFieldValByName("isDel", metaObject);
            if (Boolean.TRUE.equals(isDel)) {
                this.setFieldValByName("isDel", Boolean.TRUE, metaObject);
                if (metaObject.hasSetter("deleteTime")) {
                    this.setFieldValByName("deleteTime", now, metaObject);
                }
            }
        }
    }
}
