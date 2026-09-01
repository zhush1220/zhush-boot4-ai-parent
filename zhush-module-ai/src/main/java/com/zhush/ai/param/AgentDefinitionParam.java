package com.zhush.ai.param;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 智能体定义保存入参（管理端新增/编辑）。
 *
 * @author zhush
 */
@Data
public class AgentDefinitionParam implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（编辑时必填） */
    private Long id;

    /** 智能体唯一标识 */
    @NotBlank(message = "智能体名称不能为空")
    private String name;

    /** 功能说明 */
    private String description;

    /** 系统提示词 */
    private String systemPrompt;

    /** 模型名 */
    private String model;

    /** 允许使用的工具名清单 */
    private List<String> toolNames;

    /** 是否支持流式：0-否 1-是 */
    private Integer streaming;

    /** 最大推理步数 */
    private Integer maxSteps;

    /** 技能清单 */
    private List<String> skills;

    /** 分组：common-公共 biz-业务 */
    private String groupCode;

    /** 状态：0-停用 1-启用 */
    private Integer status;

    /** 备注 */
    private String remark;
}
