package com.zhush.captcha.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 行为验证码配置
 *
 * <p>配置前缀 {@code zhush.captcha}，示例：
 * <pre>
 * zhush:
 *   captcha:
 *     enabled: true          # 总开关（false 时不装配任何验证码 Bean）
 *     login-required: true   # 登录接口是否强制校验验证码
 *     type: SLIDER           # 验证码玩法：SLIDER-滑块 ROTATE-旋转 CONCAT-拼图
 *     image-format: WEBP     # 图片输出格式：WEBP / JPEG_PNG
 *     expire-seconds: 120    # 验证码有效期（秒）
 * </pre>
 *
 * @author zhush
 */
@Data
@ConfigurationProperties(prefix = "zhush.captcha")
public class CaptchaProperties {

    /**
     * 验证码总开关（false 时不装配验证码相关 Bean）
     */
    private boolean enabled = true;

    /**
     * 登录接口是否强制校验验证码（false 时登录不校验，验证码接口仍可用）
     */
    private boolean loginRequired = true;

    /**
     * 验证码类型：SLIDER-滑块拼图 ROTATE-旋转 CONCAT-组合拼图
     */
    private String type = "SLIDER";

    /**
     * 图片输出格式：WEBP / JPEG_PNG
     */
    private String imageFormat = "WEBP";

    /**
     * 验证码有效期（秒），超时后校验失败
     */
    private Long expireSeconds = 120L;
}
