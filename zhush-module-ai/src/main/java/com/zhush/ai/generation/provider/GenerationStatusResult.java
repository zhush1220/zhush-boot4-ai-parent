package com.zhush.ai.generation.provider;

import lombok.Builder;
import lombok.Data;

/**
 * 生成任务状态查询结果：归一化供应商任务状态为本仓库统一状态机码（对齐 {@code ai_generation_task.status}）。
 *
 * @author zhush
 */
@Data
@Builder
public class GenerationStatusResult {

    /** 排队 */
    public static final int STATUS_QUEUED = 0;
    /** 运行中 */
    public static final int STATUS_RUNNING = 1;
    /** 成功 */
    public static final int STATUS_SUCCEEDED = 2;
    /** 失败 */
    public static final int STATUS_FAILED = 3;
    /** 超时 */
    public static final int STATUS_TIMEOUT = 4;

    /** 归一化状态（见上常量） */
    private int status;

    /** 产物原始 URL（成功时有值，供应商域名，约 24h 过期） */
    private String resultUrl;

    /** 供应商原始状态串（留档/排障） */
    private String rawStatus;

    /** 失败错误码（可空） */
    private String errorCode;

    /** 失败信息（可空） */
    private String errorMsg;

    public boolean isSucceeded() {
        return status == STATUS_SUCCEEDED;
    }

    public boolean isFailed() {
        return status == STATUS_FAILED;
    }

    public boolean isTerminal() {
        return status == STATUS_SUCCEEDED || status == STATUS_FAILED || status == STATUS_TIMEOUT;
    }
}
