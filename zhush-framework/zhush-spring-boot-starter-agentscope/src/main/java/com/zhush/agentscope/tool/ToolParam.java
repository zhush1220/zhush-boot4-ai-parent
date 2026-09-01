package com.zhush.agentscope.tool;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 工具入参说明注解：配合 {@link AgentTool} 在方法参数上补充参数名与说明，供 LLM 理解。
 *
 * @author zhush
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface ToolParam {

    /**
     * 参数名（缺省取形参名）。
     *
     * @return 参数名
     */
    String name() default "";

    /**
     * 参数说明（给 LLM 看）。
     *
     * @return 说明
     */
    String description() default "";
}
