package com.zhush.common.utils.file;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
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
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * FileParseUtils 文档解析单测
 *
 * <p>测试文档均在内存中构造（POI 生成 docx、PDFBox 生成 pdf），无需外部资源文件。
 *
 * @author zhush
 */
class FileParseUtilsTest {

    // ============================== 文本提取 ==============================

    @Test
    void extractTextFromPlainText() {
        byte[] data = "Hello Tika 文本提取".getBytes(StandardCharsets.UTF_8);
        assertThat(FileParseUtils.extractText(data)).contains("Hello Tika 文本提取");
    }

    @Test
    void extractTextFromDocx() throws Exception {
        byte[] docx = createDocx("Hello Word Document", true);
        assertThat(FileParseUtils.extractText(docx)).contains("Hello Word Document");
    }

    @Test
    void extractTextFromPdf() throws Exception {
        byte[] pdf = createPdf("Hello PDF Document", true);
        assertThat(FileParseUtils.extractText(pdf)).contains("Hello PDF Document");
    }

    @Test
    void extractTextExceedMaxCharsThrows() {
        byte[] data = "abcdefghij".getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> FileParseUtils.extractText(new ByteArrayInputStream(data), 5))
                .hasMessageContaining("字符上限");
    }

    // ============================== 图片提取 ==============================

    @Test
    void extractImagesFromDocx() throws Exception {
        byte[] docx = createDocx("with image", true);
        List<ExtractedImage> images = FileParseUtils.extractImages(docx);
        assertThat(images).hasSize(1);
        ExtractedImage image = images.get(0);
        // OOXML 嵌入图片的资源名为 word/media/imageN.png 内部命名
        assertThat(image.getFileName()).endsWith(".png");
        assertThat(image.getContentType()).isEqualTo("image/png");
        assertThat(image.getSize()).isGreaterThan(0);
        assertThat(image.toDataUri()).startsWith("data:image/png;base64,");
    }

    @Test
    void extractImagesFromDocxWithoutImageReturnsEmpty() throws Exception {
        byte[] docx = createDocx("text only", false);
        assertThat(FileParseUtils.extractImages(docx)).isEmpty();
    }

    @Test
    void extractImagesFromPdf() throws Exception {
        byte[] pdf = createPdf("with image", true);
        List<ExtractedImage> images = FileParseUtils.extractImages(pdf);
        assertThat(images).isNotEmpty();
        assertThat(images.get(0).getContentType()).startsWith("image/");
        assertThat(images.get(0).getSize()).isGreaterThan(0);
    }

    // ============================ 元数据与类型探测 ============================

    @Test
    void extractMetadataFromDocx() throws Exception {
        byte[] docx = createDocx("metadata", false);
        Map<String, String> metadata = FileParseUtils.extractMetadata(docx);
        assertThat(metadata)
                .containsKey("Content-Type")
                .containsValue("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    }

    @Test
    void detectContentTypeByContent() throws Exception {
        assertThat(FileParseUtils.detectContentType(createPng())).isEqualTo("image/png");
        assertThat(FileParseUtils.detectContentType(createDocx("detect", false)))
                .isEqualTo("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        assertThat(FileParseUtils.detectContentType(createPdf("detect", false)))
                .isEqualTo("application/pdf");
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

    /** 内存生成 pdf（可选内嵌一张图片，文本使用 ASCII 避免字体编码问题） */
    private static byte[] createPdf(String text, boolean withImage) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(50, 700);
                cs.showText(text);
                cs.endText();
                if (withImage) {
                    BufferedImage buffered = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
                    PDImageXObject image = LosslessFactory.createFromImage(doc, buffered);
                    cs.drawImage(image, 50, 500, 100, 100);
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

}
