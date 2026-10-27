package com.zhush.ai.preview;

/**
 * 旧格式 → PDF 的服务端转换门面（路线甲）。
 *
 * <p>门面只负责「能不能转、转出字节」，缓存与下发由 {@code AgentFilePreviewService} 统一处理，
 * 因此甲-1（LibreOffice 内嵌）与未来的甲-2（kkFileView 独立服务）可共用同一套 {@code previewMode=server-pdf}
 * 契约，前端零改动。
 *
 * @author zhush
 */
public interface OfficeFileConverter {

    /** 对应 {@code zhush.preview.office-converter} 的取值 */
    String name();

    /** 依赖是否就绪；未就绪时门面直接按路线乙降级，不做无谓的排队 */
    boolean available();

    /** 该扩展名是否交给服务端转换 */
    boolean supports(String ext);

    /**
     * 转换一份文档为 PDF。
     *
     * @param ext         源文件扩展名（小写、不含点号）
     * @param sourceBytes 源文件字节
     * @return PDF 字节
     * @throws PreviewConversionException 未就绪 / 排队超限 / 超时 / 转换失败
     */
    byte[] convertToPdf(String ext, byte[] sourceBytes) throws PreviewConversionException;

}
