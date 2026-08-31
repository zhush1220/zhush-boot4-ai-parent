package com.zhush.web.config;

import com.zhush.web.core.handler.DataAccessExceptionAdvice;
import com.zhush.web.core.handler.GlobalExceptionHandler;
import com.zhush.web.core.handler.SaTokenExceptionAdvice;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

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

    /**
     * Jackson 3 长整型序列化配置：统一把 Long/long 序列化为字符串。
     *
     * <p>系统主键采用雪花算法（Long），数值超出 JS Number 安全整数范围（2^53-1），
     * 若按数字序列化，前端读取时会丢失精度（如 {@code 452014217814351872} 被舍入为
     * {@code 452014217814351900}），导致修改用户时传回错误的主键。统一序列化为字符串可避免该问题。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "tools.jackson.databind.ObjectMapper")
    static class JacksonLongToStringConfiguration {

        @Bean
        public JsonMapperBuilderCustomizer jacksonLongToStringCustomizer() {
            return (builder) -> {
                SimpleModule module = new SimpleModule("zhushLongToStringModule");
                module.addSerializer(Long.class, ToStringSerializer.instance);
                module.addSerializer(Long.TYPE, ToStringSerializer.instance);
                builder.addModule(module);
            };
        }
    }

}
