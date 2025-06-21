package com.na.common.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.na.common.utils.NaDateTimeUtil;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@ConditionalOnProperty(
        name = {"na.sqlTime"}, matchIfMissing = false
)
public class NaMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        // 插入时自动填充 createTime
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, NaDateTimeUtil.getCurrentBeijingDateTime());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // 更新时自动填充 updateTime
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, NaDateTimeUtil.getCurrentBeijingDateTime());
    }
}

