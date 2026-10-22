package com.zhush.ai.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 会话产物预览元信息：前端唯一需要的分派依据。
 *
 * <p>取数口径来自 AI 自有表（{@code ai_chat_message.attachments} / {@code ai_generation_task}），
 * 不跨模块读 {@code infra_file}，故 module-ai 无需依赖 module-system。
 *
 * @author zhush
 */
@Data
public class FilePreviewMetaVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 文件记录主键（infra_file.id，旧数据可为空） */
    private Long id;

    /** 展示文件名 */
    private String name;

    /** 扩展名（小写、不含点号） */
    private String ext;

    /** 字节大小 */
    private Long size;

    /** MIME 类型（按扩展名推断，非存储元数据） */
    private String contentType;

    /** 是否可在线预览 */
    private boolean previewable;

    /** 渲染模式，取值见 {@code PreviewMode.value} */
    private String previewMode;

    /** 不可预览原因（前端直接展示） */
    private String reason;

    /** 带鉴权语义的访问地址（产物外链，前端取 Blob 用） */
    private String accessUrl;

}
