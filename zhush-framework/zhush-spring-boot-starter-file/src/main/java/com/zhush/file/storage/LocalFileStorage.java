package com.zhush.file.storage;

import cn.hutool.core.util.StrUtil;
import com.zhush.common.exception.GlobalErrorCode;
import com.zhush.file.config.FileStorageProperties;
import com.zhush.file.core.FileStorage;
import com.zhush.file.core.FileStorageType;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static com.zhush.common.exception.ServiceExceptionUtil.exception;

/**
 * 本地磁盘存储实现
 *
 * <p>文件写入 {@code zhush.file.local.base-path}，
 * 访问 URL = {@code zhush.file.local.domain} + 相对路径；
 * 本地文件对外提供访问依赖静态资源映射（由 FileAutoConfiguration 按 url-path 自动注册）。
 *
 * @author zhush
 */
@Slf4j
public class LocalFileStorage implements FileStorage {

    private final FileStorageProperties.Local config;

    public LocalFileStorage(FileStorageProperties.Local config) {
        this.config = config;
    }

    @Override
    public String getType() {
        return FileStorageType.LOCAL;
    }

    @Override
    public String upload(InputStream inputStream, String path, long size, String contentType) {
        Path target = resolveTarget(path);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("[LocalFileStorage] 本地文件写入失败, path={}", target, e);
            throw exception(GlobalErrorCode.FILE_UPLOAD_ERROR, e.getMessage());
        }
        return buildUrl(path);
    }

    @Override
    public void delete(String path) {
        try {
            Files.deleteIfExists(resolveTarget(path));
        } catch (IOException e) {
            log.warn("[LocalFileStorage] 本地文件删除失败, path={}", path, e);
        }
    }

    /**
     * 解析目标文件路径（防目录穿越：规范化后必须仍位于 basePath 内）
     */
    private Path resolveTarget(String path) {
        String basePath = StrUtil.addSuffixIfNot(config.getBasePath(), "/");
        Path base = Paths.get(basePath).toAbsolutePath().normalize();
        Path target = base.resolve(path).normalize();
        if (!target.startsWith(base)) {
            throw exception(GlobalErrorCode.FILE_STORAGE_ERROR, "非法存储路径：" + path);
        }
        return target;
    }

    private String buildUrl(String path) {
        return StrUtil.addSuffixIfNot(config.getDomain(), "/") + path;
    }

    /**
     * 本地存储根目录（供静态资源映射使用）
     */
    public File getBaseDir() {
        return new File(config.getBasePath());
    }

    /**
     * 静态资源映射 URL 路径（如 /files/**）
     */
    public String getUrlPath() {
        return config.getUrlPath();
    }

}
