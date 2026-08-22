package com.zhush.common.utils.file;

import cn.hutool.core.util.StrUtil;
import com.zhush.common.exception.GlobalErrorCode;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.extractor.EmbeddedDocumentExtractor;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import static com.zhush.common.exception.ServiceExceptionUtil.exception;

/**
 * 文档内容解析工具类 —— 基于 Apache Tika 封装，全局可用
 *
 * <h3>能力</h3>
 * <ul>
 *     <li>文本提取：{@link #extractText}，支持 docx / doc / pdf / txt / html / rtf / csv 等
 *         全部 Tika 标准解析器覆盖的格式，按内容自动识别（不依赖文件后缀）</li>
 *     <li>内嵌图片提取：{@link #extractImages}，支持 docx 嵌入图片与 PDF 内联图片</li>
 *     <li>元数据提取：{@link #extractMetadata}（标题、作者、页数、Content-Type 等）</li>
 *     <li>类型探测：{@link #detectContentType}（基于内容的 MIME 探测，可防后缀伪造）</li>
 * </ul>
 *
 * <h3>使用说明</h3>
 * <ul>
 *     <li>所有方法均会消费并关闭传入的输入流，请勿在调用后复用该流</li>
 *     <li>文本提取默认上限 {@link #DEFAULT_MAX_CHARS} 字符，防止超大文件 OOM，可通过重载调整或传 -1 取消限制</li>
 *     <li>图片提取只负责"取出"，上传文件服务与 HTML 拼接属业务行为，由调用方自行组合</li>
 *     <li>运行时需存在 {@code tika-parsers-standard-package}（{@code zhush-common} 中为 optional，
 *         使用方模块需显式引入，{@code zhush-spring-boot-starter-file} 已默认携带）</li>
 * </ul>
 *
 * @author zhush
 */
public class FileParseUtils {

    /**
     * 文本提取默认字符上限（防超大文件 OOM），-1 表示不限
     */
    public static final int DEFAULT_MAX_CHARS = 100_000;

    /**
     * 共享解析器（线程安全，避免重复扫描 SPI 注册表）
     */
    private static final Parser PARSER = new AutoDetectParser();

    /** 私有构造，防止实例化 */
    private FileParseUtils() {
    }

    // ======================================================================
    // ============================== 文本提取 ==============================
    // ======================================================================

    /**
     * 提取文档纯文本（自动识别格式，默认字符上限 {@link #DEFAULT_MAX_CHARS}）
     *
     * @param input 文档输入流（方法内消费并关闭）
     * @return 纯文本内容
     */
    public static String extractText(InputStream input) {
        return extractText(input, DEFAULT_MAX_CHARS);
    }

    /**
     * 提取文档纯文本
     *
     * @param input    文档输入流（方法内消费并关闭）
     * @param maxChars 最大提取字符数，-1 表示不限
     * @return 纯文本内容
     */
    public static String extractText(InputStream input, int maxChars) {
        Objects.requireNonNull(input, "输入流不能为空");
        try (TikaInputStream tikaInput = TikaInputStream.get(input)) {
            BodyContentHandler handler = new BodyContentHandler(maxChars);
            ParseContext context = new ParseContext();
            context.set(Parser.class, PARSER);
            PARSER.parse(tikaInput, handler, new Metadata(), context);
            return handler.toString().trim();
        } catch (SAXException e) {
            // BodyContentHandler 超过字符上限时抛出
            throw exception(GlobalErrorCode.FILE_PARSE_ERROR, "文本内容超过 " + maxChars + " 字符上限");
        } catch (IOException | TikaException e) {
            throw exception(GlobalErrorCode.FILE_PARSE_ERROR, e, e.getMessage());
        }
    }

    /**
     * 提取文档纯文本（字节数组入参，适用于 MultipartFile.getBytes()）
     */
    public static String extractText(byte[] data) {
        Objects.requireNonNull(data, "文件内容不能为空");
        return extractText(new ByteArrayInputStream(data), DEFAULT_MAX_CHARS);
    }

    /**
     * 提取文档纯文本（文件入参）
     */
    public static String extractText(File file) {
        try (InputStream in = openFile(file)) {
            return extractText(in, DEFAULT_MAX_CHARS);
        } catch (IOException e) {
            throw exception(GlobalErrorCode.FILE_PARSE_ERROR, e, e.getMessage());
        }
    }

    // ======================================================================
    // ============================ 内嵌图片提取 ============================
    // ======================================================================

    /**
     * 提取文档中的内嵌图片（支持 docx 嵌入图片、PDF 内联图片）
     *
     * @param input 文档输入流（方法内消费并关闭）
     * @return 图片列表，无图片时返回空集合
     */
    public static List<ExtractedImage> extractImages(InputStream input) {
        Objects.requireNonNull(input, "输入流不能为空");
        try (TikaInputStream tikaInput = TikaInputStream.get(input)) {
            ParseContext context = new ParseContext();
            context.set(Parser.class, PARSER);
            // PDF 内联图片默认不提取，需显式开启（反射设置，缺失 parsers 包时静默跳过）
            enablePdfInlineImageExtraction(context);
            ImageCollector collector = new ImageCollector();
            context.set(EmbeddedDocumentExtractor.class, collector);
            // 空 handler 丢弃正文输出，只收集嵌入资源，降低内存占用
            PARSER.parse(tikaInput, new DefaultHandler(), new Metadata(), context);
            return collector.getImages();
        } catch (IOException | SAXException | TikaException e) {
            throw exception(GlobalErrorCode.FILE_PARSE_ERROR, e, e.getMessage());
        }
    }

    /**
     * 提取文档中的内嵌图片（字节数组入参）
     */
    public static List<ExtractedImage> extractImages(byte[] data) {
        Objects.requireNonNull(data, "文件内容不能为空");
        return extractImages(new ByteArrayInputStream(data));
    }

    /**
     * 提取文档中的内嵌图片（文件入参）
     */
    public static List<ExtractedImage> extractImages(File file) {
        try (InputStream in = openFile(file)) {
            return extractImages(in);
        } catch (IOException e) {
            throw exception(GlobalErrorCode.FILE_PARSE_ERROR, e, e.getMessage());
        }
    }

    // ======================================================================
    // ============================== 元数据提取 ============================
    // ======================================================================

    /**
     * 提取文档元数据（标题、作者、创建时间、页数、Content-Type 等，随格式而异）
     *
     * @param input 文档输入流（方法内消费并关闭）
     * @return 元数据键值对（保持解析顺序）
     */
    public static Map<String, String> extractMetadata(InputStream input) {
        Objects.requireNonNull(input, "输入流不能为空");
        try (TikaInputStream tikaInput = TikaInputStream.get(input)) {
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();
            context.set(Parser.class, PARSER);
            PARSER.parse(tikaInput, new DefaultHandler(), metadata, context);
            Map<String, String> result = new LinkedHashMap<>();
            for (String name : metadata.names()) {
                result.put(name, metadata.get(name));
            }
            return result;
        } catch (IOException | SAXException | TikaException e) {
            throw exception(GlobalErrorCode.FILE_PARSE_ERROR, e, e.getMessage());
        }
    }

    /**
     * 提取文档元数据（字节数组入参）
     */
    public static Map<String, String> extractMetadata(byte[] data) {
        Objects.requireNonNull(data, "文件内容不能为空");
        return extractMetadata(new ByteArrayInputStream(data));
    }

    // ======================================================================
    // ============================== 类型探测 ==============================
    // ======================================================================

    /**
     * 基于文件内容探测 MIME 类型（不信任文件后缀，可防伪造）
     *
     * @param input 输入流（方法内消费并关闭）
     * @return MIME 类型，如 application/pdf、
     *         application/vnd.openxmlformats-officedocument.wordprocessingml.document
     */
    public static String detectContentType(InputStream input) {
        Objects.requireNonNull(input, "输入流不能为空");
        try (TikaInputStream tikaInput = TikaInputStream.get(input)) {
            return TikaConfig.getDefaultConfig().getDetector()
                    .detect(tikaInput, new Metadata()).toString();
        } catch (IOException e) {
            throw exception(GlobalErrorCode.FILE_PARSE_ERROR, e, e.getMessage());
        }
    }

    /**
     * 基于文件内容探测 MIME 类型（字节数组入参）
     */
    public static String detectContentType(byte[] data) {
        Objects.requireNonNull(data, "文件内容不能为空");
        return detectContentType(new ByteArrayInputStream(data));
    }

    /**
     * 基于文件内容探测 MIME 类型（文件入参）
     */
    public static String detectContentType(File file) {
        try (InputStream in = openFile(file)) {
            return detectContentType(in);
        } catch (IOException e) {
            throw exception(GlobalErrorCode.FILE_PARSE_ERROR, e, e.getMessage());
        }
    }

    // ======================================================================
    // ============================== 内部实现 ==============================
    // ======================================================================

    /**
     * 打开并校验本地文件
     */
    private static InputStream openFile(File file) throws IOException {
        Objects.requireNonNull(file, "文件不能为空");
        if (!file.exists() || !file.isFile()) {
            throw exception(GlobalErrorCode.FILE_NOT_FOUND, file.getPath());
        }
        return new BufferedInputStream(new FileInputStream(file));
    }

    /**
     * 反射开启 PDF 内联图片提取（PDFParserConfig 位于 tika parsers 包，
     * 反射调用避免 zhush-common 对其产生编译期硬依赖；未引入时静默跳过）
     */
    private static void enablePdfInlineImageExtraction(ParseContext context) {
        try {
            Class<?> configClass = Class.forName("org.apache.tika.parser.pdf.PDFParserConfig");
            Object config = configClass.getDeclaredConstructor().newInstance();
            configClass.getMethod("setExtractInlineImages", boolean.class).invoke(config, true);
            ParseContext.class.getMethod("set", Class.class, Object.class)
                    .invoke(context, configClass, config);
        } catch (Throwable ignored) {
            // tika parsers 包（pdf 模块）未引入时，跳过 PDF 内联图片提取配置
        }
    }

    /**
     * 嵌入资源收集器：仅保留 image/* 类型资源
     */
    private static class ImageCollector implements EmbeddedDocumentExtractor {

        private final List<ExtractedImage> images = new ArrayList<>();

        @Override
        public boolean shouldParseEmbedded(Metadata metadata) {
            return true;
        }

        @Override
        public void parseEmbedded(InputStream stream, ContentHandler handler,
                                  Metadata metadata, boolean recurse) throws IOException {
            String contentType = metadata.get(Metadata.CONTENT_TYPE);
            if (StrUtil.isBlank(contentType)) {
                return;
            }
            // 去掉可能的参数部分（如 "; name=xxx"）
            int semicolon = contentType.indexOf(';');
            if (semicolon > 0) {
                contentType = contentType.substring(0, semicolon).trim();
            }
            if (!contentType.startsWith("image/")) {
                return; // 跳过嵌入文档、字体等非图片资源
            }
            byte[] data = stream.readAllBytes();
            if (data.length == 0) {
                return;
            }
            String fileName = metadata.get(TikaCoreProperties.RESOURCE_NAME_KEY);
            if (StrUtil.isBlank(fileName)) {
                fileName = "image_" + (images.size() + 1) + extensionOf(contentType);
            }
            images.add(new ExtractedImage(fileName, contentType, data));
        }

        List<ExtractedImage> getImages() {
            return Collections.unmodifiableList(images);
        }
    }

    /**
     * 由 MIME 类型推导图片扩展名
     */
    private static String extensionOf(String contentType) {
        String ext = contentType.substring(contentType.indexOf('/') + 1).toLowerCase(Locale.ROOT);
        return switch (ext) {
            case "jpeg" -> ".jpg";
            case "svg+xml" -> ".svg";
            case "x-ms-bmp" -> ".bmp";
            default -> "." + ext;
        };
    }

}
