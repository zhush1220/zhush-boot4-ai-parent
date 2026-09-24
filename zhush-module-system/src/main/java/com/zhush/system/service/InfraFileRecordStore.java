package com.zhush.system.service;

import com.zhush.file.core.FileRecordStore;
import com.zhush.file.core.FileUploadResult;
import com.zhush.system.entity.InfraFile;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * {@link FileRecordStore} 落库实现：上传 + 写 {@code infra_file}。
 *
 * <p>覆盖 starter 的 {@code NoopFileRecordStore}，使 AI 对话产物（文档 / 生成图像 / 生成视频）
 * 等程序生成的文件也进入统一文件管理，可列表、可删除、可追溯；字段口径与
 * {@link FileService#uploadFile} 完全一致（共用同一 insert 路径）。
 *
 * @author zhush
 */
@Primary
@Service
@RequiredArgsConstructor
public class InfraFileRecordStore implements FileRecordStore {

    private final FileService fileService;

    @Override
    public FileUploadResult upload(byte[] content, String fileName, String contentType,
                                  String userType, Long userId) {
        InfraFile entity = fileService.uploadForRecord(content, fileName, contentType, userType, userId);
        return FileUploadResult.builder()
                .id(entity.getId())
                .storageType(entity.getStorageType())
                .path(entity.getPath())
                .url(entity.getUrl())
                .originalName(entity.getName())
                .size(entity.getSize())
                .contentType(entity.getContentType())
                .build();
    }
}
