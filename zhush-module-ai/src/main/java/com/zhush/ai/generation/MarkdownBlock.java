package com.zhush.ai.generation;

import java.util.List;

/**
 * Markdown 轻量块模型：{@link MarkdownLiteParser} 的解析产物，
 * 供 docx / pdf 渲染器按块输出（不做像素级还原，保证标题/段落/列表/代码/引用/表格结构即可）。
 *
 * @param type    块类型
 * @param level   标题级别（仅 HEADING 有意义，1-6）
 * @param text    块文本（HEADING/PARAGRAPH/QUOTE 为已清洗行内标记的纯文本；CODE 为原始代码内容）
 * @param ordered 列表是否有序（仅 LIST 有意义）
 * @param items   列表项 / 表格行（TABLE 时每个元素为一行单元格列表）
 * @author zhush
 */
public record MarkdownBlock(BlockType type, int level, String text,
                            boolean ordered, List<List<String>> items) {

    /** 块类型 */
    public enum BlockType {
        /** 标题 */
        HEADING,
        /** 段落 */
        PARAGRAPH,
        /** 围栏代码块 */
        CODE,
        /** 引用块 */
        QUOTE,
        /** 无序/有序列表 */
        LIST,
        /** 表格（首行为表头） */
        TABLE
    }

    public static MarkdownBlock heading(int level, String text) {
        return new MarkdownBlock(BlockType.HEADING, level, text, false, List.of());
    }

    public static MarkdownBlock paragraph(String text) {
        return new MarkdownBlock(BlockType.PARAGRAPH, 0, text, false, List.of());
    }

    public static MarkdownBlock code(String text) {
        return new MarkdownBlock(BlockType.CODE, 0, text, false, List.of());
    }

    public static MarkdownBlock quote(String text) {
        return new MarkdownBlock(BlockType.QUOTE, 0, text, false, List.of());
    }

    public static MarkdownBlock list(boolean ordered, List<List<String>> items) {
        return new MarkdownBlock(BlockType.LIST, 0, "", ordered, items);
    }

    public static MarkdownBlock table(List<List<String>> rows) {
        return new MarkdownBlock(BlockType.TABLE, 0, "", false, rows);
    }
}
