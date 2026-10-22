package com.zhush.ai.preview;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 预览渲染模式（前后端唯一契约，见《AI智能助手文件预览功能方案》§4.1）。
 *
 * <p>裁决权在后端：前端只按本枚举分派渲染器，新增格式或从「纯前端渲染」升到「服务端转换」
 * 只改 {@code zhush.preview.office-converter} 与本表的映射，前端不发版。
 *
 * <p>{@link #SERVER_PDF} / {@link #SERVER_IFRAME} 属于路线甲（LibreOffice / kkFileView），
 * 当前无转换器实现时不会返回，保留取值以稳定契约。
 *
 * @author zhush
 */
@Getter
@AllArgsConstructor
public enum PreviewMode {

    /** 图片：直链 + 灯箱 */
    INLINE_IMAGE("inline-image"),
    /** 视频：直链 + 原生播放器 */
    INLINE_VIDEO("inline-video"),
    /** 文本类（md / txt / csv 原文）：前端取文本渲染 */
    INLINE_TEXT("inline-text"),
    /** HTML：沙箱 iframe（永不与 allow-same-origin 并存，见方案 S1） */
    IFRAME_SANDBOX("iframe-sandbox"),
    /** PDF：pdf.js 前端渲染（需配 cmaps，见方案 5.5） */
    CLIENT_PDF("client-pdf"),
    /** Word（docx）：docx-preview 前端渲染 */
    CLIENT_DOCX("client-docx"),
    /** 表格（xlsx）：exceljs 前端渲染 */
    CLIENT_SHEET("client-sheet"),
    /** 服务端转 PDF 后交前端同一个 pdf.js 组件渲染（路线甲） */
    SERVER_PDF("server-pdf"),
    /** 服务端自带渲染页，iframe 嵌入（路线甲-2 kkFileView） */
    SERVER_IFRAME("server-iframe"),
    /** 不可预览：前端显示原因并引导下载 */
    UNSUPPORTED("unsupported");

    /** 下发前端的取值（kebab-case） */
    private final String value;

}
