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

    /** 供应商标识（dashscope / openai / ollama） */
    private String type;

    /** 默认模型名 */
    private String model;
}
