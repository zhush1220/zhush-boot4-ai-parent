package com.zhush.ai.generation;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 对话产物附件（生成文件回链）。
 *
 * <p>序列化为 JSON 后：① 作为 {@code @AgentTool} 结果回给 LLM（含下载链接提示）；
 * ② 由 {@code AgentChatService} 从工具结果事件提取，落 {@code ai_chat_message.attachments}；
 * ③ 前端消息气泡渲染下载卡片。
 *
 * @author zhush
 */
@Data
@Builder
public class FileArtifact implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 附件类型：file 文件 / image 图片 / video 视频 */
    private String type;

    /** 产物格式：docx / pdf / png / mp4 等 */
    private String format;

    /** 展示文件名（含扩展名） */
    private String name;

    /** 访问/下载 URL（文件服务外链） */
    private String url;

    /** 字节大小 */
    private Long size;

    /**
     * 文件记录主键（{@code infra_file.id}）；产物已落库时有值，
     * 供文件管理/清理与生成任务关联，旧数据可为 {@code null}
     */
    private Long fileId;
}
