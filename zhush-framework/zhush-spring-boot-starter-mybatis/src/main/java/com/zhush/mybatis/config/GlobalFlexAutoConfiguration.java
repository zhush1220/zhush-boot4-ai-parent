package com.zhush.mybatis.config;

import com.mybatisflex.core.FlexGlobalConfig;
import com.zhush.common.entity.BaseEntity;
import com.zhush.mybatis.core.listener.GlobalInsertListener;
import com.zhush.mybatis.core.listener.GlobalUpdateListener;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 *
 * @ClassName GlobalFlexAutoConfiguration
 * @Description TODO
 * @Author zhush
 * @Date 2026/6/29 星期一 16:29
 *
 **/
@AutoConfiguration
public class GlobalFlexAutoConfiguration {

    @Bean
    public GlobalInsertListener globalInsertListener() {
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
