package com.zhush.ai.preview;

/**
 * 外部渲染服务门面（路线甲-2）：本系统只生成一个浏览器可直接打开的渲染地址，产物不落在本系统。
 *
 * <p>与 {@link OfficeFileConverter} 的区别在交付物：后者交回 PDF 字节，由本系统缓存后经带归属校验的
 * {@code preview-content} 接口下发（{@code server-pdf}）；前者交回渲染页地址，由外部服务自己出图
 * （{@code server-iframe}）。两者由 {@code zhush.preview.office-converter} 单值开关保证互斥，
 * 门面对外的 {@code previewMode} 契约不变，前端仍只是按值分派。
 *
 * <p>实现必须<b>自己</b>声明不接哪些格式：可携带脚本的内容（HTML/SVG）交给外部渲染器，
 * 等于在本系统的源里多开一个可执行内容出口（方案 S1）；图片/视频/md/pdf 交给它则纯属倒退
 * （多一次回源与排队，方案 §3.3）。
 *
 * @author zhush
 */
public interface RemotePreviewRenderer {

    /** 对应 {@code zhush.preview.office-converter} 的取值 */
    String name();

    /** 服务是否可达；不可达时门面回落 {@code unsupported}，不给前端一个必然报错的 iframe */
    boolean available();

    /** 该扩展名能否交给本渲染器 */
    boolean supports(String ext);

    /**
     * 前端渲染面完全覆盖不了、只能靠服务端渲染的格式。
     *
     * <p>命中即默认改走本渲染器；反之（如 docx/xlsx）只有运维显式列进
     * {@code zhush.preview.prefer-pdf-extensions} 才让外部服务接手，避免给一份本可本地渲染的文件
     * 凭空加一次回源与转换。
     */
    boolean serverOnly(String ext);

    /**
     * 生成渲染地址。
     *
     * @return 浏览器可直接打开的站内相对路径或绝对地址
     * @throws PreviewConversionException 服务未就绪 / 源地址无法重建成绝对地址
     */
    String renderUrl(RenderTarget target) throws PreviewConversionException;

    /**
     * @param ext       小写、不含点号的扩展名
     * @param name      产物展示名，外部服务据此判定格式
     * @param sourceUrl 产物地址，可能是相对前缀（docker 的 {@code /api/files/…}）也可能是绝对地址
     */
    record RenderTarget(String ext, String name, String sourceUrl) {
    }

}
