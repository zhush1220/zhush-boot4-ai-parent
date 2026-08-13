package com.zhush.file.storage;

import cn.hutool.core.util.StrUtil;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.zhush.common.exception.GlobalErrorCode;
import com.zhush.file.config.FileStorageProperties;
import com.zhush.file.core.FileStorage;
import com.zhush.file.core.FileStorageType;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;

import static com.zhush.common.exception.ServiceExceptionUtil.exception;

/**
 * 阿里云 OSS 存储实现
 *
 * <p>基于 aliyun-sdk-oss，客户端为线程安全单例，随 Bean 生命周期持有；
 * 访问 URL 优先取 {@code domain}，未配置时按 {@code https://{bucket}.{endpoint}/} 拼接。
 *
 * @author zhush
 */
@Slf4j
public class AliyunOssFileStorage implements FileStorage {

    private final FileStorageProperties.AliyunOss config;
    private final OSS ossClient;

    public AliyunOssFileStorage(FileStorageProperties.AliyunOss config) {
        this.config = config;
        this.ossClient = new OSSClientBuilder().build(
                config.getEndpoint(), config.getAccessKeyId(), config.getAccessKeySecret());
    }

    @Override
    public String getType() {
        return FileStorageType.ALIYUN_OSS;
    }

    @Override
    public String upload(InputStream inputStream, String path, long size, String contentType) {
        try {
            ossClient.putObject(config.getBucketName(), path, inputStream);
        } catch (Exception e) {
            log.error("[AliyunOssFileStorage] OSS 上传失败, path={}", path, e);
            throw exception(GlobalErrorCode.FILE_UPLOAD_ERROR, e.getMessage());
        }
        return buildUrl(path);
    }

    @Override
    public void delete(String path) {
        try {
            ossClient.deleteObject(config.getBucketName(), path);
        } catch (Exception e) {
            log.warn("[AliyunOssFileStorage] OSS 删除失败, path={}", path, e);
        }
    }

    private String buildUrl(String path) {
        if (StrUtil.isNotBlank(config.getDomain())) {
            return StrUtil.addSuffixIfNot(config.getDomain(), "/") + path;
        }
        return "https://" + config.getBucketName() + "." + config.getEndpoint() + "/" + path;
    }

}
