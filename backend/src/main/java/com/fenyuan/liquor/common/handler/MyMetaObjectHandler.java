package com.fenyuan.liquor.common.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.fenyuan.liquor.common.utils.SecurityUtils;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        Long userId = SecurityUtils.getUserId();
        if (userId != null) {
            strictInsertFill(metaObject, "createBy", Long.class, userId);
            strictInsertFill(metaObject, "updateBy", Long.class, userId);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        Long userId = SecurityUtils.getUserId();
        if (userId != null) {
            strictUpdateFill(metaObject, "updateBy", Long.class, userId);
        }
    }
}
