package com.zhush.mybatis.core.listener;

import com.mybatisflex.annotation.InsertListener;
import com.zhush.common.entity.BaseEntity;
import com.zhush.mybatis.core.handler.LoginUserContextHolder;

import java.time.LocalDateTime;

/**
 *
 * @ClassName GlobalInsertListener
 * @Description 自动填充创建人、创建时间
 * @Author zhush
 * @Date 2026/6/27 星期六 11:02
 *
 **/
public class GlobalInsertListener implements InsertListener {
    @Override
    public void onInsert(Object entity) {
        if (!(entity instanceof BaseEntity baseEntity)) {
            return;
        }

        baseEntity.setCreateBy(LoginUserContextHolder.getUserId());
        baseEntity.setCreateTime(LocalDateTime.now());
    }
}
