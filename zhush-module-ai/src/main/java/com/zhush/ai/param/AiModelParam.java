package com.zhush.ai.param;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * AI 模型配置保存入参（管理端新增/编辑）。
 *
 * @author zhush
 */
@Data
public class AiModelParam implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（编辑时必填） */
    private Long id;

    /** 模型编码（唯一） */
    @NotBlank(message = "模型编码不能为空")
    private String code;

    /** 展示名 */
    @NotBlank(message = "模型名称不能为空")
    private String name;

    /** 供应商标识：dashscope / openai / ollama */
    @NotBlank(message = "供应商类型不能为空")
    private String type;

    /** API Key（编辑时为空表示保留原值） */
    private String apiKey;

    /** 服务地址 */
    private String baseUrl;

    /** 实际模型名 */
    @NotBlank(message = "实际模型名不能为空")
    private String model;

    /** 能力类型：text / image / video / audio */
    private String capability;

    /** 是否默认模型：0-否 1-是 */
    private Integer isDefault;

    /** 优先级 */
    private Integer priority;

    /** 透传参数 */
    private Map<String, Object> options;

    /** 状态：0-停用 1-启用 */
    private Integer status;

    /** 备注 */
    private String remark;
}
