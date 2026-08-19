package com.zhush.captcha.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 行为验证码响应（屏蔽 tianai-captcha 内部类型，业务方只依赖本 starter）
 *
 * <p>前端根据 {@code type} 渲染对应交互组件，用户完成行为后携带
 * {@code id} 与行为数据（如滑块位移量）调用登录等接口校验。
 *
 * @author zhush
 */
@Data
public class CaptchaVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 验证码唯一标识（校验时回传）
     */
    private String id;

    /**
     * 验证码类型：SLIDER-滑块 ROTATE-旋转 CONCAT-组合拼图
     */
    private String type;

    /**
     * 背景图（Base64）
     */
    private String backgroundImage;

    /**
     * 模板图 / 滑块图（Base64）
     */
    private String templateImage;

    /**
     * 背景图宽
     */
    private Integer backgroundImageWidth;

    /**
     * 背景图高
     */
    private Integer backgroundImageHeight;

    /**
     * 模板图宽
     */
    private Integer templateImageWidth;

    /**
     * 模板图高
     */
    private Integer templateImageHeight;

    /**
     * 扩展数据（部分验证码类型携带的附加信息）
     */
    private Object data;
}
