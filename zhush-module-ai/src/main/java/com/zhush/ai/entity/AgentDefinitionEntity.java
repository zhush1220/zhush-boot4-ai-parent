package com.zhush.ai.entity;

import com.mybatisflex.annotation.Table;
import com.zhush.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 智能体定义实体（agent_definition）
 *
 * <p>来源三态：1-代码声明（{@code AgentDefinitionProvider}） 2-管理端配置(落库) 3-运行时动态注册。
 *
 * @author zhush
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("agent_definition")
public class AgentDefinitionEntity extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 智能体唯一标识（如 ai-chat、ai-translator） */
    private String name;

    /** 功能说明 */
    private String description;

    /** 系统提示词 */
    private String systemPrompt;

    /** 模型名（如 qwen-max，空则用默认模型） */
    private String model;

    /** 允许使用的工具清单（JSON 数组字符串） */
    private String toolNames;

    /** 是否支持流式：0-否 1-是 */
    private Integer streaming;

    /** 最大推理步数 */
    private Integer maxSteps;

    /** 技能清单（JSON 数组字符串） */
    private String skills;

    /** 分组：common-公共 biz-业务 */
    private String groupCode;

    /** 来源：1-代码声明 2-管理端配置 3-运行时动态注册 */
    private Integer source;

    /** 版本号 */
    private Integer version;

    /** 状态：0-停用 1-启用 */
    private Integer status;

    /** 备注 */
    private String remark;
}
