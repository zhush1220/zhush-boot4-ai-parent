package com.zhush.captcha.config;

import cloud.tianai.captcha.application.ImageCaptchaApplication;
import cloud.tianai.captcha.application.TACBuilder;
import cloud.tianai.captcha.cache.CacheStore;
import cloud.tianai.captcha.generator.impl.MultiImageCaptchaGenerator;
import cloud.tianai.captcha.generator.impl.transform.Base64ImageTransform;
import cloud.tianai.captcha.interceptor.EmptyCaptchaInterceptor;
import cloud.tianai.captcha.resource.ResourceProviders;
import cloud.tianai.captcha.resource.common.model.dto.Resource;
import cloud.tianai.captcha.resource.impl.DefaultImageCaptchaResourceManager;
import cloud.tianai.captcha.resource.impl.LocalMemoryResourceStore;
import cloud.tianai.captcha.resource.impl.provider.ClassPathResourceProvider;
import cloud.tianai.captcha.validator.impl.SimpleImageCaptchaValidator;
import com.zhush.captcha.core.CaptchaService;
import com.zhush.captcha.store.RedisCacheStore;
import com.zhush.redis.core.RedisCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

    private static final Logger log = LoggerFactory.getLogger(CaptchaAutoConfiguration.class);

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
        LocalMemoryResourceStore resourceStore = new LocalMemoryResourceStore();
        DefaultImageCaptchaResourceManager resourceManager = new DefaultImageCaptchaResourceManager(
                resourceStore, providers);
        MultiImageCaptchaGenerator generator = new MultiImageCaptchaGenerator(resourceManager,
                new Base64ImageTransform());
        generator.init();
        TACBuilder builder = TACBuilder.builder()
                // 注意：必须传原始 store（CrudResourceStore）而非 resourceManager.getResourceStore()：
                // DefaultImageCaptchaResourceManager 构造时会用 FontCache 包装 store（仅实现 ResourceStore），
                // addDefaultTemplate() 内部有 instanceof CrudResourceStore 判断，传包装实例会导致默认模板注册被静默跳过
                .setResourceStore(resourceStore)
                .setGenerator(generator)
                .setValidator(new SimpleImageCaptchaValidator())
                .setCacheStore(captchaCacheStore)
                .setInterceptor(new EmptyCaptchaInterceptor())
                // addDefaultTemplate() 只注册滑块/旋转模板与字体，不注册背景图；
                // 背景图需通过 addResource() 显式注册（build() 时 flush 到 store），否则生成时随机取资源为空
                .addDefaultTemplate();
        // 背景图：优先使用 classpath 下自定义目录（多张随机），为空时回退 jar 内置图片
        for (Resource background : loadBackgroundResources(properties.getResourcePath())) {
            builder.addResource(properties.getType(), background);
        }
        return builder
                .expire(properties.getType(), properties.getExpireSeconds())
                .build();
    }

    /**
     * 加载验证码背景图：扫描 classpath 指定目录下的图片（jpg/jpeg/png/webp），
     * 目录不存在或无匹配图片时回退 jar 内置背景图，保证资源注册不落空
     */
    private List<Resource> loadBackgroundResources(String resourcePath) {
        List<Resource> backgrounds = new ArrayList<>(4);
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            org.springframework.core.io.Resource[] found = resolver.getResources(
                    PathMatchingResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX
                            + resourcePath + "/*.{jpg,jpeg,png,webp}");
            for (org.springframework.core.io.Resource res : found) {
                backgrounds.add(new Resource("classpath", resourcePath + "/" + res.getFilename()));
            }
        } catch (IOException e) {
            log.warn("扫描自定义验证码背景图失败，将使用内置背景图: {}", e.getMessage());
        }
        if (backgrounds.isEmpty()) {
            log.info("未在 classpath:{} 下找到自定义背景图，使用 jar 内置背景图", resourcePath);
            backgrounds.add(new Resource("classpath", "META-INF/cut-image/resource/1.jpg"));
        }
        return backgrounds;
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
