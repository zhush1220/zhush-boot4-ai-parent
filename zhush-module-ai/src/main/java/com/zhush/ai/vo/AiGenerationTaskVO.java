package com.zhush.ai.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 生成任务视图对象：前端轮询呈现与后台列表共用。
 *
 * <p>前端凭 {@code taskId} 轮询本对象，{@code status=2} 时用 {@code resultUrl} 渲染图片/视频，
 * {@code 3/4} 时展示 {@code errorMsg}。
 *
 * @author zhush
 */
@Data
public class AiGenerationTaskVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String taskId;

    /** 任务类型：image/video */
    private String taskType;

    /** 状态：0排队 1运行 2成功 3失败 4超时 5待审批 */
    private Integer status;

    private String model;

    private String prompt;

    /** 生成参数快照（JSON） */
    private String options;

    /** 产物访问 URL（成功时为转存后的稳定外链） */
    private String resultUrl;

    private Long fileId;

    private Long fileSize;

    private String errorMsg;

    private String sessionId;

    private LocalDateTime submitTime;

    private LocalDateTime finishTime;
}
