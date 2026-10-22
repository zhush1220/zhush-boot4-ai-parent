package com.zhush.ai.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 回答正文引用文件的登记结果：一条可直接当作会话附件使用的产物描述。
 *
 * <p>字段口径与 {@code FileArtifact} 一致（前端拿到即可塞进附件列表并凭 {@code fileId} 查 preview-meta）；
 * {@code fileId} 经全局 Long→String 序列化后为字符串，旧口径同源。
 *
 * @author zhush
 */
@Data
public class AnswerFileVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 文件记录主键（infra_file.id 的字符串形态；未落库实现时为空，前端退回按扩展名尽力渲染） */
    private String fileId;

    /** 附件类型：file / image / video */
    private String type;

    /** 产物格式（小写、不含点号） */
    private String format;

    /** 展示文件名（含扩展名） */
    private String name;

    /** 访问/下载 URL（文件服务外链） */
    private String url;

    /** 字节大小 */
    private Long size;
}
