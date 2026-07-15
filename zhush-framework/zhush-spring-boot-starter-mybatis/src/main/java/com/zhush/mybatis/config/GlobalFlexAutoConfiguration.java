package com.zhush.mybatis.config;

import cn.dev33.satoken.stp.StpUtil;
import com.mybatisflex.core.FlexGlobalConfig;
import com.zhush.common.entity.BaseEntity;
import com.zhush.mybatis.core.handler.LoginUserContextHolder;
import com.zhush.mybatis.core.listener.GlobalInsertListener;
import com.zhush.mybatis.core.listener.GlobalUpdateListener;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 *
 * @ClassName GlobalFlexAutoConfiguration
 * @Description MyBatis-Flex 全局配置：注册插入/更新监听器，注入 Sa-Token 用户ID获取能力
 * @Author zhush
 * @Date 2026/6/29 星期一 16:29
 *
 **/
@AutoConfiguration(after = ZhushDataSourceAutoConfiguration.class)
public class GlobalFlexAutoConfiguration {

    @Bean
    public GlobalInsertListener globalInsertListener() {
        // 注入 Sa-Token 用户ID获取逻辑到 LoginUserContextHolder
        LoginUserContextHolder.setUserIdSupplier(() -> {
            try {
                return StpUtil.getLoginIdAsLong();
            } catch (Exception e) {
                return null;
            }
        });

        GlobalInsertListener globalInsertListener = new GlobalInsertListener();
        FlexGlobalConfig.getDefaultConfig().registerInsertListener(globalInsertListener, BaseEntity.class);
        return globalInsertListener;
    }

    @Bean
    public GlobalUpdateListener globalUpdateListener() {
        GlobalUpdateListener globalUpdateListener = new GlobalUpdateListener();
        FlexGlobalConfig.getDefaultConfig().registerUpdateListener(globalUpdateListener, BaseEntity.class);
        return globalUpdateListener;
    }

}
