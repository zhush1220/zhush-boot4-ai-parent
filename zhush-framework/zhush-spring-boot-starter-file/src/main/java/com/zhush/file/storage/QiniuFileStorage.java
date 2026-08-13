package com.zhush.file.storage;

import cn.hutool.core.util.StrUtil;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import com.zhush.common.exception.GlobalErrorCode;
import com.zhush.file.config.FileStorageProperties;
import com.zhush.file.core.FileStorage;
import com.zhush.file.core.FileStorageType;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;

import static com.zhush.common.exception.ServiceExceptionUtil.exception;

/**
 * 七牛云 Kodo 存储实现
 *
 * <p>基于 qiniu-java-sdk，使用自动区域配置；每次上传生成短时上传凭证。
 * 访问 URL = {@code domain} + 相对路径（domain 需配置为七牛测试域名或绑定的 CDN 域名）。
 *
 * <p>七牛删除文件需调用管理凭证接口（BucketManager），此处删除为尽力而为，
 * 失败仅记录日志；如需强一致删除可结合 BucketManager 扩展。
 *
 * @author zhush
 */
@Slf4j
public class QiniuFileStorage implements FileStorage {

    private final FileStorageProperties.Qiniu config;
    private final Auth auth;
    private final UploadManager uploadManager;

    public QiniuFileStorage(FileStorageProperties.Qiniu config) {
        this.config = config;
        this.auth = Auth.create(config.getAccessKey(), config.getSecretKey());
        Configuration cfg = new Configuration(Region.autoRegion());
        cfg.resumableUploadAPIVersion = Configuration.ResumableUploadAPIVersion.V2;
        this.uploadManager = new UploadManager(cfg);
    }

    @Override
    public String getType() {
        return FileStorageType.QINIU;
    }

    @Override
    public String upload(InputStream inputStream, String path, long size, String contentType) {
        String token = auth.uploadToken(config.getBucket());
        try {
            Response response = uploadManager.put(inputStream.readAllBytes(), path, token);
            if (!response.isOK()) {
                throw exception(GlobalErrorCode.FILE_UPLOAD_ERROR, response.error);
            }
        } catch (IOException e) {
            log.error("[QiniuFileStorage] 读取上传流失败, path={}", path, e);
            throw exception(GlobalErrorCode.FILE_UPLOAD_ERROR, e.getMessage());
        }
        return StrUtil.addSuffixIfNot(config.getDomain(), "/") + path;
    }

    @Override
    public void delete(String path) {
        // 七牛删除需 BucketManager 管理凭证，删除失败不阻断业务
        log.debug("[QiniuFileStorage] 七牛文件删除（需 BucketManager 支持），path={}", path);
    }

}
