package com.zhush.ai.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 对话消息视图对象。
 *
 * @author zhush
 */
@Data
public class AiChatMessageVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String sessionId;

    private Integer seq;

    private String role;

    private String contentType;

    private String content;

    private String thinking;

    private String toolCalls;

    /** 产物附件（JSON 数组：type/format/name/url/size） */
    private String attachments;

    private String tokenUsage;

    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
