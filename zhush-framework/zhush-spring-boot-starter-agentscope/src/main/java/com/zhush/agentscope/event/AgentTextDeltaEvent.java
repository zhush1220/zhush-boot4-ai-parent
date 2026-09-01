package com.zhush.agentscope.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

/**
 * 文本增量事件（流式推送的分片）。
 *
 * @author zhush
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
public class AgentTextDeltaEvent extends AgentEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 本次增量文本
     */
    private String delta;
}
