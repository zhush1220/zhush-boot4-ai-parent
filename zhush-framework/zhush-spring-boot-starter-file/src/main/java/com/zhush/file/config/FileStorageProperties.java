package com.zhush.file.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;

/**
 * 文件存储配置
 *
 * <p>配置前缀 {@code zhush.file}，通过 {@link #primary} 指定默认存储平台，
 * 各平台通过自身 {@code enabled} 开关启停；敏感配置（AK/SK）建议使用
 * jasypt 加密或环境变量注入，yaml 中保留占位符。
 *
 * @author zhush
 */
@Data
@ConfigurationProperties(prefix = "zhush.file")
public class FileStorageProperties {

    /**
     * 文件能力总开关，默认开启
     */
    private boolean enabled = true;

    /**
     * 默认存储平台（local / aliyun-oss / qiniu / huawei-obs），切换存储只需修改本项
     */
    private String primary = "local";

    /**
     * 允许上传的文件扩展名白名单（小写、不含点号）；为空表示不限制
     */
    private Set<String> allowedExtensions;

    /**
     * 单文件大小上限（字节），默认 50MB
     */
    private long maxSize = 50 * 1024 * 1024L;

    /**
     * 本地存储配置
     */
    private Local local = new Local();

    /**
     * 阿里云 OSS 配置
     */
    private AliyunOss aliyunOss = new AliyunOss();

    /**
     * 七牛云配置
     */
    private Qiniu qiniu = new Qiniu();

    /**
     * 华为云 OBS 配置
     */
    private HuaweiObs huaweiObs = new HuaweiObs();

    @Data
    public static class Local {
        /**
         * 是否启用本地存储
         */
        private boolean enabled = true;
        /**
         * 本地存储根目录（建议绝对路径，如 D:/zhush/upload/）
         */
        private String basePath = "D:/zhush/upload/";
        /**
         * 访问域名前缀（拼接相对路径得到 URL，需与静态资源映射一致）
         */
        private String domain = "http://localhost:9166/files/";
        /**
         * 静态资源映射的 URL 路径（本地文件通过该路径对外提供访问）
         */
        private String urlPath = "/files/**";
    }

    @Data
    public static class AliyunOss {
        /**
         * 是否启用阿里云 OSS
         */
        private boolean enabled = false;
        /**
         * Endpoint（如 oss-cn-hangzhou.aliyuncs.com）
         */
        private String endpoint = "your-endpoint";
        /**
         * AccessKeyId（占位符，建议加密或走环境变量）
         */
        private String accessKeyId = "your-access-key-id";
        /**
         * AccessKeySecret（占位符，建议加密或走环境变量）
         */
        private String accessKeySecret = "your-access-key-secret";
        /**
         * Bucket 名称
         */
        private String bucketName = "your-bucket-name";
        /**
         * 访问域名（可为空，为空时按 https://{bucket}.{endpoint}/ 拼接）
         */
        private String domain = "";
    }

    @Data
    public static class Qiniu {
        /**
         * 是否启用七牛云
         */
        private boolean enabled = false;
        /**
         * AccessKey（占位符）
         */
        private String accessKey = "your-access-key";
        /**
         * SecretKey（占位符）
         */
        private String secretKey = "your-secret-key";
        /**
         * 存储空间名称
         */
        private String bucket = "your-bucket";
        /**
         * 访问域名（七牛测试域名或绑定 CDN 域名，如 https://xxx.bkt.clouddn.com/）
         */
        private String domain = "https://your-domain/";
    }

    @Data
    public static class HuaweiObs {
        /**
         * 是否启用华为云 OBS
         */
        private boolean enabled = false;
        /**
         * Endpoint（如 obs.cn-north-4.myhuaweicloud.com）
         */
        private String endpoint = "your-endpoint";
        /**
         * AccessKey（占位符）
         */
        private String accessKey = "your-access-key";
        /**
         * SecretKey（占位符）
         */
        private String secretKey = "your-secret-key";
        /**
         * 桶名称
         */
        private String bucketName = "your-bucket-name";
        /**
         * 访问域名（可为空，为空时按 https://{bucket}.{endpoint}/ 拼接）
         */
        private String domain = "";
    }

}
