package com.zhush.agentscope.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

/**
 * 异常事件（错误信息）。
 *
 * @author zhush
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
public class AgentErrorEvent extends AgentEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 错误信息
     */
    private String error;
}
