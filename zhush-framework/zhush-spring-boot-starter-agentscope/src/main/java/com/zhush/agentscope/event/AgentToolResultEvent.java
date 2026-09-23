package com.zhush.agentscope.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

/**
 * 工具执行结果事件：桥接 AgentScope {@code TOOL_RESULT_TEXT_DELTA / TOOL_RESULT_END} 事件，
 * 按 toolCallId 聚合结果文本后一次性下发（超长截断）。
 *
 * <p>与 {@code TOOL_CALLED}（调用开始）配对：字段命名对齐前端工具卡片约定
 * （{@code id / name / result / ok}），产物类工具（如文档生成）据此让前端提取下载链接。
 *
 * @author zhush
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
public class AgentToolResultEvent extends AgentEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 工具调用 ID（与 TOOL_CALLED 事件的 id 配对）
     */
    private String id;

    /**
     * 工具名
     */
    private String name;

    /**
     * 工具返回文本（JSON 字符串，超长截断）
     */
    private String result;

    /**
     * 是否执行成功（AgentScope ToolResultState == SUCCESS）
     */
    private boolean ok;
}
