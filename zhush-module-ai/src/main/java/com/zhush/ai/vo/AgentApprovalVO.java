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

    private String rejectReason;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
