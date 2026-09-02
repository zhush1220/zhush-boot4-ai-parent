package com.zhush.ai.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 可用模型视图对象（模型下拉值域）。
 *
 * @author zhush
 */
@Data
public class AgentModelVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 逻辑名（如 dashscope / deepseek / local） */
    private String code;

    /** 展示名 */
    private String name;

    /** 供应商标识（dashscope / openai / ollama） */
    private String type;

    /** 默认模型名 */
    private String model;

    /** 能力类型：text / image / video / audio */
    private String capability;

    /** 是否默认模型：0-否 1-是 */
    private Integer isDefault;
}
