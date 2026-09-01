package com.zhush.agentscope.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * 已注册工具：持有 Bean 与方法的元数据快照，供构建 AgentScope Toolkit 时反射调用。
 *
 * @author zhush
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisteredTool implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 工具名（@AgentTool.name 或方法名）
     */
    private String name;

    /**
     * 工具说明（给 LLM 看）
     */
    private String description;

    /**
     * 所属 Spring Bean 名
     */
    private String beanName;

    /**
     * 目标 Bean 实例（桥接 AgentScope Toolkit 时反射调用）
     */
    private transient Object bean;

    /**
     * 所属方法
     */
    private transient Method method;

    /**
     * 是否需人工审批（敏感工具）
     */
    private boolean requiresApproval;

    /**
     * 业务分组（common-公共 biz-业务）
     */
    private String groupCode;
}
