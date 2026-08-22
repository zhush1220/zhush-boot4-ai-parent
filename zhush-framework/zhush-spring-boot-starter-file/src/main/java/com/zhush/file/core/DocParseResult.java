package com.zhush.file.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 文档解析结果（文本 + 已上传图片）
 *
 * <p>{@code html} 字段为可直接回显到富文本编辑器（TinyMCE / wangEditor / Quill 等）
 * 的 HTML 内容：文本按段落包裹 {@code <p>}，文档内嵌图片已上传文件服务并
 * 以 {@code <img src='外链'>} 形式追加。
 *
 * @author zhush
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocParseResult {

    /**
     * 原始文档文件名
     */
    private String fileName;

    /**
     * 文档内容中是否包含图片（自动探测）
     */
    private boolean hasImage;

    /**
     * 纯文本内容（不含任何标签，适用于摘要、全文索引等场景）
     */
    private String text;

    /**
     * 富文本 HTML 内容（图片为上传后的访问路径，可直接放入富文本编辑器）
     */
    private String html;

    /**
     * 图片引用列表（文件名 / 访问 URL / 存储路径），无图片时为空集合
     */
    private List<DocParseImage> images;

}
