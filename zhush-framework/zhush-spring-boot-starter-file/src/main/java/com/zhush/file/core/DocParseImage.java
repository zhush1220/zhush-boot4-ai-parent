package com.zhush.file.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档内嵌图片上传后的引用信息
 *
 * <p>由 {@link DocParseService} 解析文档提取图片并上传文件服务后产生，
 * {@code url} 可直接用于富文本编辑器的 {@code <img src>}。
 *
 * @author zhush
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocParseImage {

    /**
     * 图片文件名（文档内嵌资源名，如 image1.png）
     */
    private String fileName;

    /**
     * 文件访问 URL（上传后的外链地址）
     */
    private String url;

    /**
     * 相对存储路径（用于删除定位）
     */
    private String path;

    /**
     * 存储类型（见 {@link FileStorageType}）
     */
    private String storageType;

    /**
     * MIME 类型（如 image/png）
     */
    private String contentType;

    /**
     * 图片大小（字节）
     */
    private Long size;

}
