package com.zhush.redis.config;

import com.zhush.redis.core.RedisCache;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * Redis 自动装配
 *
 * <p>装配内容：
 * <ul>
 *     <li>{@code redisTemplate}：String key + JSON value 序列化（含类型信息，
 *     读取时自动还原原始类型），替换 Boot 默认的 JDK 序列化模板，
 *     排序在 {@code DataRedisAutoConfiguration} 之前以保证本模板生效</li>
 *     <li>{@link RedisCache}：常用缓存操作 + Redisson 分布式锁封装</li>
 * </ul>
 *
 * <p>连接配置走 Spring 原生 {@code spring.data.redis.*}（host/port/password/database 等），
 * Redisson 由其自身 starter 自动装配并复用同一份连接配置。
 *
 * @author zhush
 */
@AutoConfiguration(beforeName = "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration")
@ConditionalOnClass(RedisOperations.class)
public class RedisAutoConfiguration {

    /**
     * String key + JSON value 的 RedisTemplate（覆盖 Boot 默认 JDK 序列化）
     */
    @Bean
    @ConditionalOnMissingBean(name = "redisTemplate")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        // key/hashKey 用 String，保证 redis-cli 可读；value/hashValue 用 JSON（带类型信息）
        RedisSerializer<String> keySerializer = RedisSerializer.string();
        RedisSerializer<Object> valueSerializer = RedisSerializer.json();
        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(keySerializer);
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);
        return template;
    }

    /**
     * 缓存操作封装（RedissonClient 缺失时锁能力自动降级为不可用）
     */
    @Bean
    @ConditionalOnMissingBean
    public RedisCache redisCache(RedisTemplate<String, Object> redisTemplate,
                                 ObjectProvider<RedissonClient> redissonClient) {
        return new RedisCache(redisTemplate, redissonClient.getIfAvailable());
    }
}
