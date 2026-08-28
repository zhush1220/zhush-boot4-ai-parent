package com.zhush.captcha.core;

import cloud.tianai.captcha.application.CaptchaImageType;
import cloud.tianai.captcha.application.ImageCaptchaApplication;
import cloud.tianai.captcha.application.vo.ImageCaptchaVO;
import cloud.tianai.captcha.common.response.ApiResponse;
import cn.hutool.core.util.StrUtil;
import com.zhush.captcha.config.CaptchaProperties;
import com.zhush.captcha.vo.CaptchaVO;
import lombok.extern.slf4j.Slf4j;

/**
 * 行为验证码统一门面
 *
 * <p>管理端 / 会员端统一通过本类生成与校验验证码，屏蔽 tianai-captcha 细节：
 * <ul>
 *     <li>{@link #generate()} —— 生成验证码（类型 / 图片格式 / 有效期由配置决定）</li>
 *     <li>{@link #validate(String, String)} —— 校验验证码（一次性，校验后即失效）</li>
 *     <li>{@link #isLoginRequired()} —— 登录是否强制验证码（供登录链路判断）</li>
 * </ul>
 *
 * @author zhush
 */
@Slf4j
public class CaptchaService {

    private final ImageCaptchaApplication captchaApplication;
    private final CaptchaProperties properties;

    public CaptchaService(ImageCaptchaApplication captchaApplication, CaptchaProperties properties) {
        this.captchaApplication = captchaApplication;
        this.properties = properties;
    }

    /**
     * 生成验证码
     */
    public CaptchaVO generate() {
        CaptchaImageType imageType = CaptchaImageType.valueOf(properties.getImageFormat());
        ApiResponse<ImageCaptchaVO> response = captchaApplication.generateCaptcha(properties.getType(), imageType);
        if (!response.isSuccess() || response.getData() == null) {
            log.error("[验证码] 生成失败：{}", response.getMsg());
            throw new IllegalStateException("验证码生成失败：" + response.getMsg());
        }
        return toVO(response.getData());
    }

    /**
     * 校验验证码（一次性使用：无论成功与否，该校验码随即失效）
     *
     * @param captchaId   验证码ID（生成时返回）
     * @param captchaData 用户行为数据（滑块拼图为缺口百分比 0~1，前端回传）
     * @return true-校验通过
     */
    public boolean validate(String captchaId, String captchaData) {
        if (StrUtil.isBlank(captchaId) || StrUtil.isBlank(captchaData)) {
            return false;
        }
        float value;
        try {
            value = Float.parseFloat(captchaData);
        } catch (NumberFormatException e) {
            return false;
        }
        try {
            return captchaApplication.matching(captchaId, value);
        } catch (Exception e) {
            log.warn("[验证码] 校验异常 id={}", captchaId, e);
            return false;
        }
    }

    /**
     * 登录是否强制校验验证码
     */
    public boolean isLoginRequired() {
        return properties.isLoginRequired();
    }

    // ================ 内部方法 ================

    private CaptchaVO toVO(ImageCaptchaVO data) {
        CaptchaVO vo = new CaptchaVO();
        vo.setId(data.getId());
        vo.setType(data.getType());
        vo.setBackgroundImage(data.getBackgroundImage());
        vo.setTemplateImage(data.getTemplateImage());
        vo.setBackgroundImageWidth(data.getBackgroundImageWidth());
        vo.setBackgroundImageHeight(data.getBackgroundImageHeight());
        vo.setTemplateImageWidth(data.getTemplateImageWidth());
        vo.setTemplateImageHeight(data.getTemplateImageHeight());
        vo.setData(data.getData());
        return vo;
    }
}
