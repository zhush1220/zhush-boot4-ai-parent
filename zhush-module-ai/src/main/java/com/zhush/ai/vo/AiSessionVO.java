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

    /** 会话内出现过的任务类型（逗号分隔，如 chat,docx,image），列表展示用 */
    private String taskTypes;

    /** 产物消息数（attachments 非空的消息条数） */
    private Long attachmentCount;

    private Integer status;

    private LocalDateTime startTime;

    private LocalDateTime finishTime;

    private LocalDateTime lastActiveTime;
}
