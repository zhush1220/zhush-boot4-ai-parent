package com.zhush.file.core;

import java.io.InputStream;

/**
 * 文件存储 SPI
 *
 * <p>各存储平台（本地、阿里云 OSS、七牛云、华为云 OBS）实现该接口，
 * 由 {@link FileClient} 按配置统一调度；新增存储平台只需实现本接口并注册为 Bean。
 *
 * @author zhush
 */
public interface FileStorage {

    /**
     * 存储类型标识（见 {@link FileStorageType}）
     */
    String getType();

    /**
     * 上传文件
     *
     * @param inputStream 文件流（调用方负责关闭）
     * @param path        相对存储路径（如 {@code 2026/08/12/xxx.jpg}）
     * @param size        文件大小（字节）
     * @param contentType MIME 类型，可为 {@code null}
     * @return 文件访问 URL
     */
    String upload(InputStream inputStream, String path, long size, String contentType);

    /**
     * 删除文件（删除失败仅记录日志，不阻断业务流程）
     *
     * @param path 相对存储路径
     */
    void delete(String path);

}
