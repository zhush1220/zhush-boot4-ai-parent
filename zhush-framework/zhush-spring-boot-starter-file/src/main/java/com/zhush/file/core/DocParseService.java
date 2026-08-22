package com.zhush.file.core;

import cn.hutool.core.util.StrUtil;
import com.zhush.common.utils.file.ExtractedImage;
import com.zhush.common.utils.file.FileParseUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static com.zhush.common.exception.ServiceExceptionUtil.exception;

/**
 * 文档解析上传服务 —— 面向富文本编辑器场景的一站式封装
 *
 * <p>完整流程：
 * <ol>
 *     <li>基于 Tika 提取文档纯文本（{@link FileParseUtils#extractText}）</li>
 *     <li>自动探测文档内容中是否包含内嵌图片（{@link FileParseUtils#extractImages}）</li>
 *     <li>存在图片时逐张上传文件服务（{@link FileClient}），获取访问 URL</li>
 *     <li>组装富文本 HTML：文本按段落包裹 {@code <p>}，图片以 {@code <img src='外链'>} 追加</li>
 * </ol>
 *
 * <p>返回的 {@link DocParseResult#getHtml()} 可直接回显到富文本编辑器；
 * 纯文本与图片引用列表也一并返回，供摘要、索引、落库等场景使用。
 *
 * <p>注意：图片上传受 {@code zhush.file.allowed-extensions} 白名单约束，
 * 若配置了白名单请确保包含 png / jpg / gif 等图片扩展名。
 *
 * @author zhush
 */
@Slf4j
@RequiredArgsConstructor
public class DocParseService {

    private final FileClient fileClient;

    /**
     * 解析上传的文档（MultipartFile 入参，适用于 Controller 直传场景）
     *
     * @param file 文档文件（docx / doc / pdf / txt / html 等 Tika 支持的格式）
     */
    public DocParseResult parse(MultipartFile file) {
        try {
            return parse(file.getBytes(), file.getOriginalFilename());
        } catch (IOException e) {
            throw exception(com.zhush.common.exception.GlobalErrorCode.FILE_PARSE_ERROR, e, e.getMessage());
        }
    }

    /**
     * 解析文档输入流
     *
     * @param input    文档输入流（方法内消费并关闭）
     * @param fileName 文档文件名（仅用于结果记录，格式识别基于内容）
     */
    public DocParseResult parse(InputStream input, String fileName) {
        try {
            return parse(input.readAllBytes(), fileName);
        } catch (IOException e) {
            throw exception(com.zhush.common.exception.GlobalErrorCode.FILE_PARSE_ERROR, e, e.getMessage());
        }
    }

    /**
     * 解析文档字节内容（核心实现）
     *
     * @param data     文档字节内容
     * @param fileName 文档文件名（仅用于结果记录，格式识别基于内容）
     * @return 解析结果（文本 + 富文本 HTML + 已上传图片引用）
     */
    public DocParseResult parse(byte[] data, String fileName) {
        // 1. 提取纯文本
        String text = FileParseUtils.extractText(data);
        // 2. 自动探测并提取内嵌图片
        List<ExtractedImage> extractedImages = FileParseUtils.extractImages(data);
        // 3. 存在图片则逐张上传文件服务，收集访问路径
        List<DocParseImage> imageRefs = new ArrayList<>(extractedImages.size());
        for (ExtractedImage image : extractedImages) {
            FileUploadResult uploadResult = fileClient.upload(
                    image.getData(), image.getFileName(), image.getContentType());
            imageRefs.add(DocParseImage.builder()
                    .fileName(image.getFileName())
                    .url(uploadResult.getUrl())
                    .path(uploadResult.getPath())
                    .storageType(uploadResult.getStorageType())
                    .contentType(image.getContentType())
                    .size((long) image.getSize())
                    .build());
        }
        // 4. 组装富文本 HTML（文本段落 + 图片外链）
        String html = buildHtml(text, imageRefs);
        log.info("[DocParseService] 文档解析完成, name={}, 文本长度={}, 图片数={}",
                fileName, text.length(), imageRefs.size());
        return DocParseResult.builder()
                .fileName(fileName)
                .hasImage(!imageRefs.isEmpty())
                .text(text)
                .html(html)
                .images(imageRefs)
                .build();
    }

    /**
     * 组装富文本 HTML：文本按空行分段包裹 {@code <p>}，图片以 {@code <img>} 追加
     */
    private String buildHtml(String text, List<DocParseImage> imageRefs) {
        StringBuilder html = new StringBuilder();
        if (StrUtil.isNotBlank(text)) {
            for (String paragraph : text.split("\\r?\\n")) {
                if (StrUtil.isNotBlank(paragraph)) {
                    html.append("<p>").append(paragraph.trim()).append("</p>");
                }
            }
        }
        for (DocParseImage image : imageRefs) {
            html.append("<p><img src='").append(image.getUrl())
                    .append("' alt='").append(image.getFileName()).append("' /></p>");
        }
        return html.toString();
    }

}
