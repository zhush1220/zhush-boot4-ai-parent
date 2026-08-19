package com.zhush.captcha.config;

import cloud.tianai.captcha.application.ImageCaptchaApplication;
import cloud.tianai.captcha.application.TACBuilder;
import cloud.tianai.captcha.cache.CacheStore;
import cloud.tianai.captcha.generator.impl.MultiImageCaptchaGenerator;
import cloud.tianai.captcha.generator.impl.transform.Base64ImageTransform;
import cloud.tianai.captcha.interceptor.EmptyCaptchaInterceptor;
import cloud.tianai.captcha.resource.ResourceProviders;
import cloud.tianai.captcha.resource.impl.DefaultImageCaptchaResourceManager;
import cloud.tianai.captcha.resource.impl.LocalMemoryResourceStore;
import cloud.tianai.captcha.resource.impl.provider.ClassPathResourceProvider;
import cloud.tianai.captcha.validator.impl.SimpleImageCaptchaValidator;
import com.zhush.captcha.core.CaptchaService;
import com.zhush.captcha.store.RedisCacheStore;
import com.zhush.redis.core.RedisCache;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 行为验证码自动装配
 *
 * <p>依赖 Redis（验证码答案分布式存储）；{@code zhush.captcha.enabled=false} 可整体关闭，
 * 关闭后不装配任何验证码 Bean，登录链路通过 {@code ObjectProvider} 自动跳过校验。
 *
 * @author zhush
 */
@AutoConfiguration
@ConditionalOnClass({ImageCaptchaApplication.class, RedisCache.class})
@ConditionalOnProperty(prefix = "zhush.captcha", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(CaptchaProperties.class)
public class CaptchaAutoConfiguration {

    /**
     * 验证码答案存储（Redis 实现，支持多实例部署）
     */
    @Bean
    @ConditionalOnMissingBean
    public CacheStore captchaCacheStore(RedisCache redisCache) {
        return new RedisCacheStore(redisCache);
    }

    /**
     * tianai-captcha 核心应用（显式装配资源管理 / 生成器 / 校验器，不依赖官方 Boot starter）
     */
    @Bean
    @ConditionalOnMissingBean
    public ImageCaptchaApplication imageCaptchaApplication(CacheStore captchaCacheStore,
                                                           CaptchaProperties properties) {
        // 资源管理：内置模板存本地内存，classpath 资源加载器读取 jar 内置素材
        ResourceProviders providers = new ResourceProviders();
        providers.registerResourceProvider(new ClassPathResourceProvider());
        DefaultImageCaptchaResourceManager resourceManager = new DefaultImageCaptchaResourceManager(
                new LocalMemoryResourceStore(), providers);
        MultiImageCaptchaGenerator generator = new MultiImageCaptchaGenerator(resourceManager,
                new Base64ImageTransform());
        generator.init();
        return TACBuilder.builder()
                .setResourceStore(resourceManager.getResourceStore())
                .setGenerator(generator)
                .setValidator(new SimpleImageCaptchaValidator())
                .setCacheStore(captchaCacheStore)
                .setInterceptor(new EmptyCaptchaInterceptor())
                .addDefaultTemplate()
                .expire(properties.getType(), properties.getExpireSeconds())
                .build();
    }

    /**
     * 验证码统一门面（业务方唯一入口）
     */
    @Bean
    @ConditionalOnMissingBean
    public CaptchaService captchaService(ImageCaptchaApplication imageCaptchaApplication,
                                         CaptchaProperties properties) {
        return new CaptchaService(imageCaptchaApplication, properties);
    }
}
