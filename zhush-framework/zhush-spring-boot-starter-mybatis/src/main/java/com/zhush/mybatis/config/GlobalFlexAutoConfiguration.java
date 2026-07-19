package com.zhush.mybatis.config;

import com.mybatisflex.core.FlexGlobalConfig;
import com.zhush.common.entity.BaseEntity;
import com.zhush.mybatis.core.handler.LoginUserContextHolder;
import com.zhush.mybatis.core.listener.GlobalInsertListener;
import com.zhush.mybatis.core.listener.GlobalUpdateListener;
import com.zhush.satoken.core.StpAdminUtil;
import com.zhush.satoken.core.StpMemberUtil;
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
        // 注入 Sa-Token 用户ID获取逻辑到 LoginUserContextHolder：管理端优先，其次会员端
        LoginUserContextHolder.setUserIdSupplier(() -> {
            Long adminId = StpAdminUtil.getLoginIdDefaultNull();
            if (adminId != null) {
                return adminId;
            }
            return StpMemberUtil.getLoginIdDefaultNull();
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
