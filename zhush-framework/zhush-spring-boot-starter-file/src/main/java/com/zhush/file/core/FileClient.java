package com.zhush.file.core;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.zhush.common.exception.GlobalErrorCode;
import com.zhush.file.config.FileStorageProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
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
        try (InputStream inputStream = file.getInputStream()) {
            return doUpload(inputStream, StrUtil.blankToDefault(file.getOriginalFilename(), "unnamed"),
                    file.getSize(), file.getContentType(), storageType);
        } catch (com.zhush.common.exception.ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("[FileClient] 文件上传失败, name={}", file.getOriginalFilename(), e);
            throw exception(GlobalErrorCode.FILE_UPLOAD_ERROR, e.getMessage());
        }
    }

    /**
     * 上传字节内容（适用于文档解析提取的图片、程序生成的文件等非 MultipartFile 场景），使用默认存储平台
     *
     * @param content     文件字节内容（不能为空）
     * @param fileName    文件名（用于提取扩展名与记录原始名，如 image1.png）
     * @param contentType MIME 类型，可为 {@code null}
     */
    public FileUploadResult upload(byte[] content, String fileName, String contentType) {
        return upload(content, fileName, contentType, null);
    }

    /**
     * 上传字节内容（storageType 为空时使用默认存储平台）
     */
    public FileUploadResult upload(byte[] content, String fileName, String contentType, String storageType) {
        validate(content, fileName);
        try (InputStream inputStream = new ByteArrayInputStream(content)) {
            return doUpload(inputStream, fileName, content.length, contentType, storageType);
        } catch (com.zhush.common.exception.ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("[FileClient] 文件上传失败, name={}", fileName, e);
            throw exception(GlobalErrorCode.FILE_UPLOAD_ERROR, e.getMessage());
        }
    }

    /**
     * 上传公共逻辑：生成存储路径并委托具体存储实现，屏蔽各平台差异
     */
    private FileUploadResult doUpload(InputStream inputStream, String originalName, long size,
                                      String contentType, String storageType) throws Exception {
        FileStorage storage = getStorage(StrUtil.blankToDefault(storageType, properties.getPrimary()));

        String ext = FileNameUtil.extName(originalName);
        String path = LocalDate.now().format(PATH_FORMATTER) + "/" + IdUtil.fastSimpleUUID()
                + (StrUtil.isBlank(ext) ? "" : "." + ext.toLowerCase());

        String url = storage.upload(inputStream, path, size, contentType);
        return FileUploadResult.builder()
                .storageType(storage.getType())
                .path(path)
                .url(url)
                .originalName(originalName)
                .size(size)
                .contentType(contentType)
                .build();
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
     * 上传前校验：非空、大小上限、扩展名白名单（MultipartFile 版）
     */
    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw exception(GlobalErrorCode.FILE_EMPTY);
        }
        validateSizeAndExtension(file.getSize(), file.getOriginalFilename());
    }

    /**
     * 上传前校验：非空、大小上限、扩展名白名单（字节内容版）
     */
    private void validate(byte[] content, String fileName) {
        if (content == null || content.length == 0) {
            throw exception(GlobalErrorCode.FILE_EMPTY);
        }
        validateSizeAndExtension(content.length, fileName);
    }

    /**
     * 大小上限与扩展名白名单公共校验
     */
    private void validateSizeAndExtension(long size, String fileName) {
        if (size > properties.getMaxSize()) {
            throw exception(GlobalErrorCode.PAYLOAD_TOO_LARGE);
        }
        if (CollUtil.isNotEmpty(properties.getAllowedExtensions())) {
            String ext = FileNameUtil.extName(StrUtil.blankToDefault(fileName, ""));
            if (StrUtil.isBlank(ext) || !properties.getAllowedExtensions().contains(ext.toLowerCase())) {
                throw exception(GlobalErrorCode.FILE_TYPE_UNSUPPORTED, ext);
            }
        }
    }

}
