package com.zhush.mybatis.config;

import cn.hutool.core.util.StrUtil;
import com.zhush.common.utils.SmUtils;
import com.zhush.mybatis.core.handler.EncryptContextHolder;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 字段加密自动配置
 * <p>
 * 启动时根据 {@code zhush.encrypt.*} 配置完成以下初始化：
 * <ol>
 *     <li>将 SM4 密钥注入到 SmUtils（如果配置了 {@code sm4-key}）</li>
 *     <li>将全局开关同步到 {@link EncryptContextHolder}，供 TypeHandler 读取</li>
 *     <li>注册 {@link EncryptProperties} Bean</li>
 * </ol>
 *
 * @author zhush
 * @since 2026/7/11
 */
@AutoConfiguration
@EnableConfigurationProperties(EncryptProperties.class)
public class EncryptAutoConfiguration {

    @Bean
    public EncryptContextInitializer encryptContextInitializer(EncryptProperties properties) {
        // 如果 Spring 配置了 SM4 密钥，覆盖 SmUtils 的默认密钥
        if (StrUtil.isNotBlank(properties.getSm4Key())) {
            SmUtils.overrideSm4Key(properties.getSm4Key());
        }

        // 将全局开关同步到静态上下文，供 EncryptTypeHandler 使用
        EncryptContextHolder.setEnabled(properties.isEnabled());

        return new EncryptContextInitializer();
    }

    /**
     * 上下文初始化标记 Bean（仅用于确保初始化逻辑在 Spring 容器启动时执行）
     */
    public static class EncryptContextInitializer {

    }
}
