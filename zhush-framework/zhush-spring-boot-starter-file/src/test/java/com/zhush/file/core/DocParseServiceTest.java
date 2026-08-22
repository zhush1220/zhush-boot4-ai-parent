package com.zhush.file.core;

import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.Document;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DocParseService 单测：验证"提取文本 + 自动判图 + 上传 + 富文本 HTML 组装"完整链路
 *
 * @author zhush
 */
class DocParseServiceTest {

    /** 内存假存储：记录全部上传路径并返回固定 URL */
    static class RecordingStorage implements FileStorage {

        final List<String> uploadedPaths = new ArrayList<>();

        @Override
        public String getType() {
            return "local";
        }

        @Override
        public String upload(InputStream inputStream, String path, long size, String contentType) {
            uploadedPaths.add(path);
            return "http://localhost:9166/files/" + path;
        }

        @Override
        public void delete(String path) {
        }
    }

    private DocParseService service(RecordingStorage storage) {
        com.zhush.file.config.FileStorageProperties properties = new com.zhush.file.config.FileStorageProperties();
        properties.setPrimary("local");
        return new DocParseService(new FileClient(List.of(storage), properties));
    }

    @Test
    void parseDocxWithImageUploadsAndBuildsHtml() throws Exception {
        RecordingStorage storage = new RecordingStorage();
        DocParseService service = service(storage);

        DocParseResult result = service.parse(createDocx("产品介绍第一行", true), "产品说明.docx");

        // 文本提取
        assertThat(result.getText()).contains("产品介绍第一行");
        // 自动判图：存在内嵌图片并逐张上传
        assertThat(result.isHasImage()).isTrue();
        assertThat(result.getImages()).hasSize(1);
        assertThat(storage.uploadedPaths).hasSize(1);
        // 图片引用信息完整（URL 可直接用于 <img src>）
        DocParseImage image = result.getImages().get(0);
        assertThat(image.getFileName()).endsWith(".png");
        assertThat(image.getContentType()).isEqualTo("image/png");
        assertThat(image.getUrl()).startsWith("http://localhost:9166/files/");
        assertThat(image.getStorageType()).isEqualTo("local");
        assertThat(image.getSize()).isPositive();
        // 富文本 HTML：文本包 <p>，图片以 <img> 外链追加
        assertThat(result.getHtml()).contains("<p>产品介绍第一行</p>")
                .contains("<img src='" + image.getUrl() + "'");
    }

    @Test
    void parseDocxWithoutImageReturnsNoImage() throws Exception {
        RecordingStorage storage = new RecordingStorage();
        DocParseService service = service(storage);

        DocParseResult result = service.parse(createDocx("纯文本文档", false), "说明.docx");

        assertThat(result.getText()).contains("纯文本文档");
        assertThat(result.isHasImage()).isFalse();
        assertThat(result.getImages()).isEmpty();
        assertThat(storage.uploadedPaths).isEmpty();
        assertThat(result.getHtml()).contains("<p>纯文本文档</p>").doesNotContain("<img");
    }

    // ============================== 测试数据构造 ==============================

    /** 内存生成一张 10x10 的 PNG 图片 */
    private static byte[] createPng() throws IOException {
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    /** 内存生成 docx（可选嵌入一张 PNG） */
    private static byte[] createDocx(String text, boolean withImage) throws Exception {
        try (XWPFDocument doc = new XWPFDocument()) {
            XWPFParagraph paragraph = doc.createParagraph();
            XWPFRun run = paragraph.createRun();
            run.setText(text);
            if (withImage) {
                try (InputStream in = new ByteArrayInputStream(createPng())) {
                    run.addPicture(in, Document.PICTURE_TYPE_PNG, "test.png",
                            Units.toEMU(50), Units.toEMU(50));
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.write(out);
            return out.toByteArray();
        }
    }

}
