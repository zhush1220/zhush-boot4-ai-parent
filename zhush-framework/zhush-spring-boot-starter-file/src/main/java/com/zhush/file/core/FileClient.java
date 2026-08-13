package com.zhush.file.core;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.zhush.common.exception.GlobalErrorCode;
import com.zhush.file.config.FileStorageProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.zhush.common.exception.ServiceExceptionUtil.exception;

/**
 * 文件上传统一门面
 *
 * <p>聚合全部已启用的 {@link FileStorage} 实现，按 {@code zhush.file.primary}
 * 选择默认存储平台；上传前完成空文件、大小、扩展名白名单校验，
 * 并生成 {@code yyyy/MM/dd/uuid.ext} 形式的存储路径，屏蔽各平台差异。
 *
 * @author zhush
 */
@Slf4j
public class FileClient {

    private static final DateTimeFormatter PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final Map<String, FileStorage> storageMap;
    private final FileStorageProperties properties;

    public FileClient(List<FileStorage> storages, FileStorageProperties properties) {
        this.storageMap = storages.stream()
                .collect(Collectors.toMap(FileStorage::getType, Function.identity()));
        this.properties = properties;
        log.info("[FileClient] 已启用文件存储: {}, 默认存储: {}", storageMap.keySet(), properties.getPrimary());
    }

    /**
     * 使用默认存储平台上传
     */
    public FileUploadResult upload(MultipartFile file) {
        return upload(file, null);
    }

    /**
     * 上传文件（storageType 为空时使用默认存储平台）
     */
    public FileUploadResult upload(MultipartFile file, String storageType) {
        validate(file);
        FileStorage storage = getStorage(StrUtil.blankToDefault(storageType, properties.getPrimary()));

        String originalName = StrUtil.blankToDefault(file.getOriginalFilename(), "unnamed");
        String ext = FileNameUtil.extName(originalName);
        String path = LocalDate.now().format(PATH_FORMATTER) + "/" + IdUtil.fastSimpleUUID()
                + (StrUtil.isBlank(ext) ? "" : "." + ext.toLowerCase());

        try (InputStream inputStream = file.getInputStream()) {
            String url = storage.upload(inputStream, path, file.getSize(), file.getContentType());
            return FileUploadResult.builder()
                    .storageType(storage.getType())
                    .path(path)
                    .url(url)
                    .originalName(originalName)
                    .size(file.getSize())
                    .contentType(file.getContentType())
                    .build();
        } catch (com.zhush.common.exception.ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("[FileClient] 文件上传失败, name={}", originalName, e);
            throw exception(GlobalErrorCode.FILE_UPLOAD_ERROR, e.getMessage());
        }
    }

    /**
     * 按存储类型与相对路径删除文件
     */
    public void delete(String storageType, String path) {
        FileStorage storage = storageMap.get(storageType);
        if (storage == null) {
            log.warn("[FileClient] 删除时未找到存储实现（可能已停用）, type={}, path={}", storageType, path);
            return;
        }
        storage.delete(path);
    }

    /**
     * 获取指定存储实现，未启用时抛出配置错误
     */
    public FileStorage getStorage(String storageType) {
        FileStorage storage = storageMap.get(storageType);
        if (storage == null) {
            throw exception(GlobalErrorCode.FILE_STORAGE_ERROR,
                    "存储 [" + storageType + "] 未启用，请检查 zhush.file 配置");
        }
        return storage;
    }

    /**
     * 上传前校验：非空、大小上限、扩展名白名单
     */
    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw exception(GlobalErrorCode.FILE_EMPTY);
        }
        if (file.getSize() > properties.getMaxSize()) {
            throw exception(GlobalErrorCode.PAYLOAD_TOO_LARGE);
        }
        if (CollUtil.isNotEmpty(properties.getAllowedExtensions())) {
            String ext = FileNameUtil.extName(StrUtil.blankToDefault(file.getOriginalFilename(), ""));
            if (StrUtil.isBlank(ext) || !properties.getAllowedExtensions().contains(ext.toLowerCase())) {
                throw exception(GlobalErrorCode.FILE_TYPE_UNSUPPORTED, ext);
            }
        }
    }

}
