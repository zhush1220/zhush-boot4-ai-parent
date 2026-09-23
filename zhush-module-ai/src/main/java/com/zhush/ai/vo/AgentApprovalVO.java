package com.zhush.ai.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审批任务视图对象。
 *
 * @author zhush
 */
@Data
public class AgentApprovalVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String approvalId;

    private String sessionId;

    private String agentName;

    private String toolName;

    private String toolArgs;

    private String modifiedArgs;

    private Long proposerId;

    private String proposerType;

    private String proposerName;

    private Integer status;

    /** AgentScope require-confirm replyId：非空表示路线 B 原生会话，前端据此订阅 resume-stream */
    private String replyId;

    /** 续跑幂等闸门：0-未续跑 1-已续跑 */
    private Integer resumed;

    private String rejectReason;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
