package com.zhush.ai.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 会话任务统计（聚合查询载体，非表映射）：供会话记录列表展示任务类型与产物数。
 *
 * @author zhush
 */
@Data
public class SessionTaskStatVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 业务会话ID */
    private String sessionId;

    /** 该会话出现过的任务类型（GROUP_CONCAT DISTINCT，逗号分隔，如 chat,docx,image） */
    private String taskTypes;

    /** 产物消息数（attachments 非空的消息条数） */
    private Long attachmentCount;
}
