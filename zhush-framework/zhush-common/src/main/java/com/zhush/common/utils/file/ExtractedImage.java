package com.zhush.common.utils.file;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Base64;

/**
 * 文档内嵌图片提取结果
 *
 * <p>由 {@link FileParseUtils#extractImages} 从 docx / pdf 等文档中提取的嵌入图片，
 * 包含文件名、MIME 类型与二进制内容。业务侧可自行上传到文件服务后拼接
 * {@code <img src>} 标签，或直接使用 {@link #toDataUri()} 内嵌展示。
 *
 * @author zhush
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractedImage {

    /**
     * 图片文件名（文档内无名称时回退为 image_N.ext）
     */
    private String fileName;

    /**
     * MIME 类型，如 image/png、image/jpeg
     */
    private String contentType;

    /**
     * 图片二进制内容
     */
    private byte[] data;

    /**
     * 图片大小（字节）
     */
    public int getSize() {
        return data == null ? 0 : data.length;
    }

    /**
     * 转 Base64 编码
     */
    public String toBase64() {
        return data == null ? null : Base64.getEncoder().encodeToString(data);
    }

    /**
     * 生成 Data URI（可直接作为 {@code <img src>} 使用）
     */
    public String toDataUri() {
        return data == null ? null : "data:" + contentType + ";base64," + toBase64();
    }

}
