package com.zhush.ai.preview;

import com.zhush.file.config.FileStorageProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;

/**
 * 转换产物缓存：{@code {cache-root}/{fileId}.pdf}。
 *
 * <p>键只用 {@code fileId}（{@code infra_file.id}），上传即新建记录、不存在同 ID 覆盖，
 * 因此缓存天然幂等；换档或清了目录也只是重新转换，不影响正确性。
 *
 * <p>默认落在本地存储根的<b>同级</b>目录而非其内部：存储根经 {@code /api/files/**} 免鉴权直出（方案 S2），
 * 缓存放进去等于把「转换后的 PDF」也公开了，而 {@code preview-content} 接口是要校验会话归属的。
 *
 * @author zhush
 */
@Slf4j
@Component
public class PreviewCacheStore {

    private final PreviewProperties previewProperties;
    private final FileStorageProperties storageProperties;
    private Path root;

    public PreviewCacheStore(PreviewProperties previewProperties, FileStorageProperties storageProperties) {
        this.previewProperties = previewProperties;
        this.storageProperties = storageProperties;
    }

    @PostConstruct
    void init() {
        root = resolveRoot();
        try {
            Files.createDirectories(root);
            warnIfInsideStorageRoot();
            log.info("[预览] 转换缓存目录: {}", root);
        } catch (IOException e) {
            log.warn("[预览] 转换缓存目录不可用: {}（{}），旧格式回落「仅下载」", root, e.getMessage());
        }
    }

    /** 缓存根目录，同时充当 LibreOffice 的工作目录（临时文件与 profile 都放这里） */
    public Path root() {
        return root;
    }

    public Path pdfPath(long fileId) {
        return root.resolve(fileId + ".pdf");
    }

    /** 命中且未过 TTL 才复用；过期文件按「不存在」处理，由调用方重新转换并覆盖 */
    public Path findUsable(long fileId) {
        Path pdf = pdfPath(fileId);
        if (!Files.isRegularFile(pdf)) {
            return null;
        }
        try {
            if (Files.size(pdf) == 0) {
                return null;
            }
            int ttlHours = previewProperties.getConvert().getCacheTtlHours();
            if (ttlHours > 0) {
                Instant lastModified = Files.getLastModifiedTime(pdf).toInstant();
                if (Duration.between(lastModified, Instant.now()).toHours() >= ttlHours) {
                    return null;
                }
            }
            return pdf;
        } catch (IOException e) {
            log.debug("[预览] 缓存文件状态不可读: {}", pdf);
            return null;
        }
    }

    /** 先写临时文件再原子改名，避免并发转换时读到半截 PDF */
    public Path write(long fileId, byte[] pdfBytes) throws IOException {
        Files.createDirectories(root);
        Path target = pdfPath(fileId);
        Path temp = Files.createTempFile(root, fileId + "-", ".pdf.part");
        try {
            Files.write(temp, pdfBytes);
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(temp);
        }
        return target;
    }

    // ================ 内部 ================

    private Path resolveRoot() {
        String configured = previewProperties.getConvert().getCachePath();
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured.trim()).toAbsolutePath().normalize();
        }
        String basePath = storageProperties.getLocal().getBasePath();
        if (basePath != null && !basePath.isBlank()) {
            Path base = Path.of(basePath).toAbsolutePath().normalize();
            Path parent = base.getParent();
            if (parent != null) {
                return parent.resolve("preview-cache");
            }
        }
        return Path.of(System.getProperty("java.io.tmpdir"), "zhush-preview-cache").toAbsolutePath().normalize();
    }

    private void warnIfInsideStorageRoot() {
        String basePath = storageProperties.getLocal().getBasePath();
        if (basePath == null || basePath.isBlank()) {
            return;
        }
        Path base = Path.of(basePath).toAbsolutePath().normalize();
        if (root.startsWith(base)) {
            log.warn("[预览] 转换缓存目录位于文件存储根之下（{} ⊂ {}），产物可经免鉴权的 /api/files/** 直接访问；"
                    + "建议移到存储根之外", root, base);
        }
    }

}
