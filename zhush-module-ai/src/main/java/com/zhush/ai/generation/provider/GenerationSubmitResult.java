package com.zhush.ai.generation.provider;

import lombok.Builder;
import lombok.Data;

/**
 * 生成任务提交结果：成功返回供应商任务号（{@code remoteTaskId}），失败携带错误码/信息。
 *
 * @author zhush
 */
@Data
@Builder
public class GenerationSubmitResult {

    /** 是否提交成功 */
    private boolean success;

    /** 供应商任务号（提交成功时有值，后续 fetch 用） */
    private String remoteTaskId;

    /** 失败错误码（供应商返回，可空） */
    private String errorCode;

    /** 失败信息（可空） */
    private String errorMsg;

    public static GenerationSubmitResult ok(String remoteTaskId) {
        return GenerationSubmitResult.builder().success(true).remoteTaskId(remoteTaskId).build();
    }

    public static GenerationSubmitResult fail(String errorCode, String errorMsg) {
        return GenerationSubmitResult.builder()
                .success(false).errorCode(errorCode).errorMsg(errorMsg).build();
    }
}
