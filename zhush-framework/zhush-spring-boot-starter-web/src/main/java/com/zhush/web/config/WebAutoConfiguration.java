package com.zhush.web.config;

import com.zhush.web.core.handler.DataAccessExceptionAdvice;
import com.zhush.web.core.handler.GlobalExceptionHandler;
import com.zhush.web.core.handler.SaTokenExceptionAdvice;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * zhush-spring-boot-starter-web 自动装配入口
 *
 * <p>装配内容：
 * <ul>
 *     <li>{@link GlobalExceptionHandler}：全局异常处理器（始终装配）</li>
 *     <li>{@link SaTokenExceptionAdvice}：Sa-Token 异常处理（引入 Sa-Token 时生效）</li>
 *     <li>{@link DataAccessExceptionAdvice}：数据访问异常处理（引入 spring-tx 时生效）</li>
 * </ul>
 *
 * @author zhush
 */
@AutoConfiguration
public class WebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    /**
     * Sa-Token 存在时才装配对应的异常处理 Advice
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "cn.dev33.satoken.exception.NotLoginException")
    static class SaTokenAdviceConfiguration {
        @Bean
        @ConditionalOnMissingBean
        public SaTokenExceptionAdvice saTokenExceptionAdvice() {
            return new SaTokenExceptionAdvice();
        }
    }

    /**
     * Spring 数据访问 API 存在时才装配对应的异常处理 Advice
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.dao.DuplicateKeyException")
    static class DataAccessAdviceConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public DataAccessExceptionAdvice dataAccessExceptionAdvice() {
            return new DataAccessExceptionAdvice();
        }
    }

}
