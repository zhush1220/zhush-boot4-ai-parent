package com.zhush.ai.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 审计日志视图对象（只读）。
 *
 * @author zhush
 */
@Data
public class AgentAuditLogVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String traceId;

    private String sessionId;

    private String agentName;

    private String eventType;

    private String toolName;

    private String toolArgs;

    private String toolResult;

    private Integer duration;

    private Integer tokenUsage;

    private Integer stepCount;

    private Long userId;

    private String userType;

    private Long tenantId;

    private Integer resultCode;

    private String resultMsg;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
