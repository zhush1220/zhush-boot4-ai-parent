package com.zhush.ai.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 会话视图对象。
 *
 * @author zhush
 */
@Data
public class AiSessionVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String sessionId;

    private String agentName;

    private String agentDesc;

    private String model;

    private String title;

    private Long userId;

    private String userType;

    private Integer messageCount;

    private Long tokenCost;

    private Integer status;

    private LocalDateTime startTime;

    private LocalDateTime finishTime;

    private LocalDateTime lastActiveTime;
}
