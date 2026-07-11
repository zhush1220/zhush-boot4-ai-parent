package com.zhush.mybatis.core.listener;

import com.mybatisflex.annotation.UpdateListener;
import com.zhush.common.entity.BaseEntity;

import java.time.LocalDateTime;

/**
 *
 * @ClassName GlobalUpdateListener
 * @Description 自动填充更新人、更新时间
 * @Author zhush
 * @Date 2026/6/27 星期六 11:05
 *
 **/
public class GlobalUpdateListener implements UpdateListener {
    @Override
    public void onUpdate(Object entity) {
        if (!(entity instanceof BaseEntity baseEntity)) {
            return;
        }

        baseEntity.setUpdateBy(getUserId());
        baseEntity.setUpdateTime(LocalDateTime.now());
    }

    /**
     * 缓存中获取当前用户ID
     */
    private Long getUserId() {
        return 1L;
    }
}
