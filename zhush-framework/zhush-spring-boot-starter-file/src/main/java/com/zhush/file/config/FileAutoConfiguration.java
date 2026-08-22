package com.zhush.file.config;

import com.zhush.file.core.DocParseService;
import com.zhush.file.core.FileClient;
import com.zhush.file.core.FileStorage;
import com.zhush.file.storage.AliyunOssFileStorage;
import com.zhush.file.storage.HuaweiObsFileStorage;
import com.zhush.file.storage.LocalFileStorage;
import com.zhush.file.storage.QiniuFileStorage;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * 文件存储自动配置
 *
 * <p>按 {@code zhush.file.*} 配置条件装配各存储实现（本地 / 阿里云 OSS / 七牛云 / 华为云 OBS），
 * 并聚合为 {@link FileClient} 统一门面；切换存储只需修改 {@code zhush.file.primary}
 * 与对应平台的 {@code enabled} 开关。本地存储额外注册静态资源映射，
 * 使上传的文件可通过 {@code zhush.file.local.url-path} 直接访问。
 *
 * @author zhush
 */
@AutoConfiguration
@EnableConfigurationProperties(FileStorageProperties.class)
@ConditionalOnProperty(prefix = "zhush.file", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FileAutoConfiguration {

    // ================ 存储实现（按需装配） ================

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "zhush.file.local", name = "enabled", havingValue = "true", matchIfMissing = true)
    public LocalFileStorage localFileStorage(FileStorageProperties properties) {
        return new LocalFileStorage(properties.getLocal());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "zhush.file.aliyun-oss", name = "enabled", havingValue = "true")
    public AliyunOssFileStorage aliyunOssFileStorage(FileStorageProperties properties) {
        return new AliyunOssFileStorage(properties.getAliyunOss());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "zhush.file.qiniu", name = "enabled", havingValue = "true")
    public QiniuFileStorage qiniuFileStorage(FileStorageProperties properties) {
        return new QiniuFileStorage(properties.getQiniu());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "zhush.file.huawei-obs", name = "enabled", havingValue = "true")
    public HuaweiObsFileStorage huaweiObsFileStorage(FileStorageProperties properties) {
        return new HuaweiObsFileStorage(properties.getHuaweiObs());
    }

    // ================ 统一门面 ================

    @Bean
    @ConditionalOnMissingBean
    public FileClient fileClient(List<FileStorage> storages, FileStorageProperties properties) {
        return new FileClient(storages, properties);
    }

    /**
     * 文档解析上传服务：提取文本 + 自动探测图片并上传文件服务，输出富文本 HTML
     */
    @Bean
    @ConditionalOnMissingBean
    public DocParseService docParseService(FileClient fileClient) {
        return new DocParseService(fileClient);
    }

    // ================ 本地存储静态资源映射 ================

    /**
     * 将本地存储目录映射为可访问的静态资源（url-path → file:basePath）
     */
    @Bean
    @ConditionalOnProperty(prefix = "zhush.file.local", name = "enabled", havingValue = "true", matchIfMissing = true)
    public WebMvcConfigurer fileResourceWebMvcConfigurer(LocalFileStorage localFileStorage) {
        return new WebMvcConfigurer() {
            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                registry.addResourceHandler(localFileStorage.getUrlPath())
                        .addResourceLocations("file:" + localFileStorage.getBaseDir().getAbsolutePath() + "/");
            }
        };
    }

}
