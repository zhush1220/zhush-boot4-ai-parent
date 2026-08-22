package com.zhush.file.core;

import com.zhush.common.exception.ServiceException;
import com.zhush.file.config.FileStorageProperties;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * FileClient 上传门面单测（覆盖字节内容上传：文档解析提取图片等场景）
 *
 * @author zhush
 */
class FileClientTest {

    /** 内存假存储：记录上传路径并返回固定 URL */
    static class FakeStorage implements FileStorage {

        String uploadedPath;
        long uploadedSize;

        @Override
        public String getType() {
            return "local";
        }

        @Override
        public String upload(InputStream inputStream, String path, long size, String contentType) {
            this.uploadedPath = path;
            this.uploadedSize = size;
            return "http://localhost:9166/files/" + path;
        }

        @Override
        public void delete(String path) {
        }
    }

    private FileClient client(FileStorageProperties properties, FakeStorage storage) {
        return new FileClient(List.of(storage), properties);
    }

    private FileStorageProperties defaultProperties() {
        FileStorageProperties properties = new FileStorageProperties();
        properties.setPrimary("local");
        return properties;
    }

    @Test
    void uploadBytesReturnsResult() {
        FakeStorage storage = new FakeStorage();
        FileClient client = client(defaultProperties(), storage);

        byte[] content = "image-bytes".getBytes(StandardCharsets.UTF_8);
        FileUploadResult result = client.upload(content, "image1.png", "image/png");

        assertThat(result.getStorageType()).isEqualTo("local");
        assertThat(result.getOriginalName()).isEqualTo("image1.png");
        assertThat(result.getSize()).isEqualTo(content.length);
        assertThat(result.getContentType()).isEqualTo("image/png");
        // 路径形如 yyyy/MM/dd/uuid.png
        assertThat(result.getPath()).matches("\\d{4}/\\d{2}/\\d{2}/[0-9a-f]{32}\\.png");
        assertThat(result.getUrl()).startsWith("http://localhost:9166/files/");
        assertThat(storage.uploadedPath).isEqualTo(result.getPath());
        assertThat(storage.uploadedSize).isEqualTo(content.length);
    }

    @Test
    void uploadEmptyBytesThrowsFileEmpty() {
        FileClient client = client(defaultProperties(), new FakeStorage());
        assertThatThrownBy(() -> client.upload(new byte[0], "a.png", "image/png"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("不能为空");
    }

    @Test
    void uploadExtensionNotInWhitelistThrows() {
        FileStorageProperties properties = defaultProperties();
        properties.setAllowedExtensions(Set.of("png", "jpg"));
        FileClient client = client(properties, new FakeStorage());

        byte[] content = "image-bytes".getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> client.upload(content, "a.exe", "application/octet-stream"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("exe");
    }

    @Test
    void uploadExceedMaxSizeThrows() {
        FileStorageProperties properties = defaultProperties();
        properties.setMaxSize(10);
        FileClient client = client(properties, new FakeStorage());

        byte[] content = "0123456789ABCDEF".getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> client.upload(content, "a.png", "image/png"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("过大");
    }

}
