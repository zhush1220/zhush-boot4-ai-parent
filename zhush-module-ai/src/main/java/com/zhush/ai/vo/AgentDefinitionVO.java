package com.zhush.ai.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 智能体定义视图对象（管理端 CRUD）。
 *
 * @author zhush
 */
@Data
public class AgentDefinitionVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    private String description;

    private String systemPrompt;

    private String model;

    private List<String> toolNames;

    private Integer streaming;

    private Integer maxSteps;

    private List<String> skills;

    private String groupCode;

    private Integer source;

    private Integer version;

    private Integer status;

    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
