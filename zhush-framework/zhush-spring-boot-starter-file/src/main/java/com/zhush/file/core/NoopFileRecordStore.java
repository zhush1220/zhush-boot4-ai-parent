package com.zhush.file.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link FileRecordStore} 兜底实现：只上传、不落库。
 *
 * <p>未引入 module-system（{@code infra_file} 表所在模块）的独立应用也能装配成功；
 * 此时产物文件依然可访问，但没有文件管理记录，故打一条 WARN 提示口径缺失。
 *
 * @author zhush
 */
@Slf4j
@RequiredArgsConstructor
public class NoopFileRecordStore implements FileRecordStore {

    private final FileClient fileClient;

    @Override
    public FileUploadResult upload(byte[] content, String fileName, String contentType, String userType, Long userId) {
        FileUploadResult result = fileClient.upload(content, fileName, contentType);
        log.warn("[NoopFileRecordStore] 文件已上传但未落文件记录（无 FileRecordStore 实现）, name={}, url={}",
                fileName, result.getUrl());
        return result;
    }
}
