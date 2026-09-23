package com.zhush.ai.generation;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 轻量 Markdown 块解析器（零依赖）：面向「LLM 产出内容 → Word/PDF 确定性渲染」管线，
 * 支持标题 / 段落 / 围栏代码块 / 引用 / 无序有序列表 / 管道表格，并清洗行内标记
 * （加粗、斜体、删除线、行内代码、链接、图片）。
 *
 * <p>不做像素级还原，也不是完整 CommonMark 实现：块级结构完整即可，
 * 未识别语法按普通段落降级，保证内容不丢失。
 *
 * @author zhush
 */
public final class MarkdownLiteParser {

    /** 标题：# ~ ###### */
    private static final Pattern HEADING = Pattern.compile("^(#{1,6})\\s+(.*)$");
    /** 无序列表项 */
    private static final Pattern UL_ITEM = Pattern.compile("^[-*+]\\s+(.*)$");
    /** 有序列表项 */
    private static final Pattern OL_ITEM = Pattern.compile("^(\\d+)[.)]\\s+(.*)$");
    /** 分隔线 */
    private static final Pattern HR = Pattern.compile("^(-{3,}|\\*{3,}|_{3,})$");
    /** 行内图片 ![alt](url) */
    private static final Pattern IMG_INLINE = Pattern.compile("!\\[([^\\]]*)]\\(([^)\\s]+)[^)]*\\)");
    /** 行内链接 [text](url) */
    private static final Pattern LINK_INLINE = Pattern.compile("\\[([^\\]]+)]\\(([^)\\s]+)[^)]*\\)");
    /** 加粗/斜体/删除线/行内代码标记 */
    private static final Pattern EMPHASIS = Pattern.compile("(\\*{1,3}|_{1,3}|~~|`)");

    private MarkdownLiteParser() {
    }

    /**
     * 解析 Markdown 文本为块列表（null/空文本返回空列表）。
     */
    public static List<MarkdownBlock> parse(String markdown) {
        List<MarkdownBlock> blocks = new ArrayList<>();
        if (markdown == null || markdown.isBlank()) {
            return blocks;
        }
        String[] lines = markdown.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        int i = 0;
        while (i < lines.length) {
            String line = lines[i];
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                i++;
                continue;
            }
            // 围栏代码块：原样收集到闭合围栏
            if (trimmed.startsWith("```") || trimmed.startsWith("~~~")) {
                String fence = trimmed.substring(0, 3);
                StringBuilder code = new StringBuilder();
                i++;
                while (i < lines.length && !lines[i].trim().startsWith(fence)) {
                    code.append(lines[i]).append('\n');
                    i++;
                }
                if (i < lines.length) {
                    i++;
                }
                blocks.add(MarkdownBlock.code(code.toString()));
                continue;
            }
            // 标题
            Matcher heading = HEADING.matcher(trimmed);
            if (heading.matches()) {
                blocks.add(MarkdownBlock.heading(heading.group(1).length(),
                        cleanInline(heading.group(2).trim())));
                i++;
                continue;
            }
            // 分隔线：降级为空段落占位（保持版面节奏）
            if (HR.matcher(trimmed).matches()) {
                blocks.add(MarkdownBlock.paragraph(""));
                i++;
                continue;
            }
            // 引用块：连续 > 行合并
            if (trimmed.startsWith(">")) {
                StringBuilder quote = new StringBuilder();
                while (i < lines.length && lines[i].trim().startsWith(">")) {
                    if (!quote.isEmpty()) {
                        quote.append('\n');
                    }
                    quote.append(lines[i].trim().replaceFirst("^>\\s?", ""));
                    i++;
                }
                blocks.add(MarkdownBlock.quote(cleanInline(quote.toString())));
                continue;
            }
            // 列表：连续同类项合并（每项以单元素行存储于 items）
            Matcher ul = UL_ITEM.matcher(trimmed);
            Matcher ol = OL_ITEM.matcher(trimmed);
            if (ul.matches() || ol.matches()) {
                boolean ordered = ol.matches();
                List<List<String>> items = new ArrayList<>();
                while (i < lines.length) {
                    String t = lines[i].trim();
                    Matcher m = ordered ? OL_ITEM.matcher(t) : UL_ITEM.matcher(t);
                    if (!m.matches()) {
                        break;
                    }
                    items.add(List.of(cleanInline(ordered ? m.group(2) : m.group(1))));
                    i++;
                }
                blocks.add(MarkdownBlock.list(ordered, items));
                continue;
            }
            // 表格：连续 | 行，跳过 |---| 分隔行
            if (trimmed.startsWith("|")) {
                List<List<String>> rows = new ArrayList<>();
                while (i < lines.length && lines[i].trim().startsWith("|")) {
                    String row = lines[i].trim();
                    if (!isTableSeparator(row)) {
                        rows.add(splitTableCells(row));
                    }
                    i++;
                }
                if (!rows.isEmpty()) {
                    blocks.add(MarkdownBlock.table(rows));
                }
                continue;
            }
            // 段落：收集到下一个块起始/空行为止
            StringBuilder paragraph = new StringBuilder();
            while (i < lines.length) {
                String t = lines[i].trim();
                if (t.isEmpty() || isBlockStart(t)) {
                    break;
                }
                if (!paragraph.isEmpty()) {
                    paragraph.append('\n');
                }
                paragraph.append(t);
                i++;
            }
            blocks.add(MarkdownBlock.paragraph(cleanInline(paragraph.toString())));
        }
        return blocks;
    }

    /**
     * 清洗行内标记：图片 → alt 文本；链接 → 文本（URL）；加粗/斜体/删除线/行内代码去标记。
     */
    static String cleanInline(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String result = IMG_INLINE.matcher(text).replaceAll("$1（$2）");
        result = LINK_INLINE.matcher(result).replaceAll("$1（$2）");
        // 逐段去掉强调标记符号（保留文字内容）
        result = result.replaceAll("\\*{1,3}([^*]+)\\*{1,3}", "$1");
        result = result.replaceAll("(?<![\\w])_{1,3}([^_]+)_{1,3}(?![\\w])", "$1");
        result = result.replaceAll("~~([^~]+)~~", "$1");
        result = result.replaceAll("`([^`]+)`", "$1");
        result = EMPHASIS.matcher(result).replaceAll("");
        return result.strip();
    }

    /**
     * 是否为新块的起始行（用于段落终止判断）。
     */
    private static boolean isBlockStart(String trimmed) {
        return HEADING.matcher(trimmed).matches()
                || UL_ITEM.matcher(trimmed).matches()
                || OL_ITEM.matcher(trimmed).matches()
                || trimmed.startsWith(">")
                || trimmed.startsWith("|")
                || trimmed.startsWith("```")
                || trimmed.startsWith("~~~")
                || HR.matcher(trimmed).matches();
    }

    private static boolean isTableSeparator(String row) {
        String body = row.replace("|", "").replace(" ", "");
        return !body.isEmpty() && body.chars().allMatch(c -> c == '-' || c == ':');
    }

    private static List<String> splitTableCells(String row) {
        String body = row.strip();
        if (body.startsWith("|")) {
            body = body.substring(1);
        }
        if (body.endsWith("|")) {
            body = body.substring(0, body.length() - 1);
        }
        String[] cells = body.split("(?<!\\\\)\\|", -1);
        List<String> result = new ArrayList<>(cells.length);
        for (String cell : cells) {
            result.add(cleanInline(cell.replace("\\|", "|").trim()));
        }
        return result;
    }
}
