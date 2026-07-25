package com.zhush.web.operatelog.config;

import com.zhush.web.operatelog.aspect.OperateLogAspect;
import com.zhush.web.operatelog.core.OperateLogHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 操作日志自动配置
 *
 * <p>装配 {@link OperateLogAspect} 切面；可通过 {@code zhush.operate-log.enabled=false} 关闭。
 * 持久化由业务模块提供 {@link OperateLogHandler} 实现（可选）。
 *
 * @author zhush
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "zhush.operate-log", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OperateLogAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OperateLogAspect operateLogAspect(ObjectProvider<OperateLogHandler> handlerProvider) {
        return new OperateLogAspect(handlerProvider);
    }

}
