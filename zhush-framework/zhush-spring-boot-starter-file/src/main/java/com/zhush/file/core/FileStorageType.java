package com.zhush.file.core;

/**
 * 文件存储类型常量
 *
 * <p>与 {@code zhush.file.primary} 及 {@code infra_file.storage_type} 取值对应。
 *
 * @author zhush
 */
public final class FileStorageType {

    private FileStorageType() {
    }

    /**
     * 本地磁盘存储
     */
    public static final String LOCAL = "local";

    /**
     * 阿里云 OSS
     */
    public static final String ALIYUN_OSS = "aliyun-oss";

    /**
     * 七牛云 Kodo
     */
    public static final String QINIU = "qiniu";

    /**
     * 华为云 OBS
     */
    public static final String HUAWEI_OBS = "huawei-obs";

}
