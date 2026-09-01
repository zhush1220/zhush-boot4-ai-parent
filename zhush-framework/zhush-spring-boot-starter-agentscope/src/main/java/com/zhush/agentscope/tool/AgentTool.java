package com.zhush.agentscope.tool;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 业务工具注解：标注在 Spring Bean 方法上，starter 启动时自动收集到 {@link ToolkitRegistry}。
 *
 * <p>被标注的方法即成为智能体可调用的工具；入参可用 {@link ToolParam} 补充参数名与说明。
 *
 * @author zhush
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AgentTool {

    /**
     * 工具名（缺省取方法名）。
     *
     * @return 工具名
     */
    String name() default "";

    /**
     * 工具说明（给 LLM 看，应描述用途与入参）。
     *
     * @return 说明
     */
    String description();

    /**
     * 是否敏感工具（命中后需人工审批，配合 {@code zhush.agentscope.safety.approval-enabled}）。
     *
     * @return true 为敏感工具
     */
    boolean sensitive() default false;
}
