package com.zhush.ai.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 智能体定义视图对象。
 *
 * @author zhush
 */
@Data
public class AgentVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    private String description;

    private String systemPrompt;

    private String model;

    private String toolNames;

    private Integer streaming;

    private Integer maxSteps;

    private String groupCode;

    private Integer source;

    private Integer status;

    private LocalDateTime createTime;
}
