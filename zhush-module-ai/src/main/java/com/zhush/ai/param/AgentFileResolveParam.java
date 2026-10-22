package com.zhush.ai.param;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 回答正文引用文件登记入参（点击正文里的文件引用时按需登记为可预览产物）。
 *
 * <p>{@code reference} 是正文原文里的那段路径文本，可能是绝对路径、相对工作区的路径或 {@code 目录/文件名}；
 * 只接受路径，带协议的地址会被拒（方案 S6：预览侧不得接受任意 URL）。
 *
 * @author zhush
 */
@Data
public class AgentFileResolveParam implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 会话 ID（必填：登记前要先断言会话归属，同时也是预览门面的归属校验上下文） */
    @NotBlank(message = "会话ID不能为空")
    @Size(max = 64, message = "会话ID长度超出限制")
    private String sessionId;

    /** 正文里的文件引用原文 */
    @NotBlank(message = "文件引用不能为空")
    @Size(max = 512, message = "文件引用长度超出限制")
    private String reference;

    /** 消息序号（可选）：历史消息精确回填用；缺省时登记到会话内最近一条助手消息 */
    private Integer seq;
}
