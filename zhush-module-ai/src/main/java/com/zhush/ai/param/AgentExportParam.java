package com.zhush.ai.param;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 文档导出入参（管理端导出接口，与 generateDocument 工具同一渲染链路）。
 *
 * @author zhush
 */
@Data
public class AgentExportParam implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 文档正文（Markdown） */
    @NotBlank(message = "文档内容不能为空")
    private String content;

    /** 输出格式：docx / pdf，缺省 docx */
    private String format;

    /** 文件名（可不含扩展名） */
    private String fileName;
}
