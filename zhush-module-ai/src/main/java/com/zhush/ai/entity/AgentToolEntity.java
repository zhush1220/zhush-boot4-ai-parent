package com.zhush.ai.entity;

import com.mybatisflex.annotation.Table;
import com.zhush.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 智能体工具注册实体（agent_tool）
 *
 * <p>来源于 {@code @AgentTool} 注解扫描快照，管理端只读展示。
 *
 * @author zhush
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("agent_tool")
public class AgentToolEntity extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 工具名（如 queryHotGoods） */
    private String name;

    /** 工具说明（给 LLM 看） */
    private String description;

    /** 所属 Spring Bean 名 */
    private String beanName;

    /** 所属方法名 */
    private String methodName;

    /** 是否需人工审批：0-否 1-是 */
    private Integer requiresApproval;

    /** 所属业务分组 */
    private String groupCode;

    /** 入参说明（JSON，供前端展示参数结构） */
    private String paramsDoc;

    /** 状态：0-停用 1-启用 */
    private Integer status;

    /** 注册/重新扫描时间 */
    private LocalDateTime reloadTime;
}
