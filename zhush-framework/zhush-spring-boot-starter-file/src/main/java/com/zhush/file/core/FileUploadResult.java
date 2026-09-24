package com.zhush.file.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件上传结果
 *
 * @author zhush
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResult {

    /**
     * 文件记录主键（infra_file.id）；仅经 {@link FileRecordStore} 上传时有值，
     * 直连 {@link FileClient} 上传不写库则为 {@code null}
     */
    private Long id;

    /**
     * 存储类型（见 {@link FileStorageType}）
     */
    private String storageType;

    /**
     * 相对存储路径（用于删除定位）
     */
    private String path;

    /**
     * 文件访问 URL
     */
    private String url;

    /**
     * 原始文件名
     */
    private String originalName;

    /**
     * 文件大小（字节）
     */
    private Long size;

    /**
     * MIME 类型
     */
    private String contentType;

}
