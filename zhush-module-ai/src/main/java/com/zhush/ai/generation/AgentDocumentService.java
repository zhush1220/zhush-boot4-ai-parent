package com.zhush.ai.generation;

import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.zhush.file.core.FileClient;
import com.zhush.file.core.FileUploadResult;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static com.zhush.ai.enums.AiErrorCode.DOCUMENT_CONTENT_EMPTY;
import static com.zhush.ai.enums.AiErrorCode.DOCUMENT_FORMAT_UNSUPPORTED;
import static com.zhush.ai.enums.AiErrorCode.DOCUMENT_RENDER_FAIL;
import static com.zhush.common.exception.ServiceExceptionUtil.exception;
import static com.zhush.common.exception.ServiceExceptionUtil.assertTrue;

/**
 * 文档生成服务：Markdown → Word(docx) / PDF 确定性渲染（不消耗大模型），产物经
 * {@link FileClient} 上传文件服务后回下载链接。
 *
 * <p>定位：对话多能力「文案/文档导出」的执行层，供 {@code generateDocument} 工具
 * 与管理端导出接口共用。渲染基于轻量块模型 {@link MarkdownLiteParser}，
 * 保证标题/段落/列表/代码/引用/表格结构，不做像素级还原。
 *
 * <p>PDF 中文依赖 OpenPDF 内置 CJK 字体（STSongStd-Light + UniGB-UCS2-H，非嵌入），
 * 阅读器需自带宋体渲染；如目标环境异常可改为嵌入字体文件方案。
 *
 * @author zhush
 */
@Service
public class AgentDocumentService {

    private static final Logger log = LoggerFactory.getLogger(AgentDocumentService.class);

    /** 支持格式 */
    public static final String FORMAT_DOCX = "docx";
    public static final String FORMAT_PDF = "pdf";

    /** 输入内容上限（字符），超出截断，防御超大文档拖垮渲染与内存 */
    private static final int MAX_CONTENT_CHARS = 200_000;

    private static final String DOCX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final String PDF_CONTENT_TYPE = "application/pdf";

    private final FileClient fileClient;

    public AgentDocumentService(FileClient fileClient) {
        this.fileClient = fileClient;
    }

    /**
     * 生成文档并上传文件服务。
     *
     * @param content  Markdown 内容（LLM 产出的结构化文本）
     * @param format   输出格式：docx / pdf（空默认 docx）
     * @param fileName 文件名（可不含扩展名，空则自动生成）
     * @return 产物附件（含下载 URL）
     */
    public FileArtifact generate(String content, String format, String fileName) {
        assertTrue(content != null && !content.isBlank(), DOCUMENT_CONTENT_EMPTY);
        String fmt = (format == null || format.isBlank()) ? FORMAT_DOCX : format.trim().toLowerCase();
        if (!FORMAT_DOCX.equals(fmt) && !FORMAT_PDF.equals(fmt)) {
            throw exception(DOCUMENT_FORMAT_UNSUPPORTED, fmt);
        }
        String markdown = content.length() > MAX_CONTENT_CHARS
                ? content.substring(0, MAX_CONTENT_CHARS) + "\n\n（内容超长，已截断）"
                : content;
        List<MarkdownBlock> blocks = MarkdownLiteParser.parse(markdown);
        byte[] bytes = FORMAT_PDF.equals(fmt) ? renderPdf(blocks) : renderDocx(blocks);
        String name = resolveName(fileName, fmt);
        FileUploadResult uploaded = fileClient.upload(bytes, name,
                FORMAT_PDF.equals(fmt) ? PDF_CONTENT_TYPE : DOCX_CONTENT_TYPE);
        log.info("[AgentDocumentService] 文档生成完成, format={}, name={}, size={}", fmt, name, bytes.length);
        return FileArtifact.builder()
                .type("file")
                .format(fmt)
                .name(name)
                .url(uploaded.getUrl())
                .size(uploaded.getSize())
                .build();
    }

    // ================ Word（POI XWPF） ================

    private byte[] renderDocx(List<MarkdownBlock> blocks) {
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (MarkdownBlock block : blocks) {
                switch (block.type()) {
                    case HEADING -> headingDocx(doc, block);
                    case PARAGRAPH -> {
                        XWPFParagraph p = doc.createParagraph();
                        runDocx(p, block.text(), 11, false);
                    }
                    case CODE -> codeDocx(doc, block.text());
                    case QUOTE -> {
                        XWPFParagraph p = doc.createParagraph();
                        p.setIndentationLeft(400);
                        XWPFRun r = runDocx(p, block.text(), 11, false);
                        r.setItalic(true);
                    }
                    case LIST -> listDocx(doc, block);
                    case TABLE -> tableDocx(doc, block);
                }
            }
            doc.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("[AgentDocumentService] Word 渲染失败", e);
            throw exception(DOCUMENT_RENDER_FAIL, e, e.getMessage());
        }
    }

    private void headingDocx(XWPFDocument doc, MarkdownBlock block) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(240);
        p.setSpacingAfter(120);
        int size = Math.max(22 - block.level() * 2, 12);
        runDocx(p, block.text(), size, true);
    }

    private void codeDocx(XWPFDocument doc, String code) {
        XWPFParagraph p = doc.createParagraph();
        p.setIndentationLeft(300);
        String[] lines = code.split("\n", -1);
        XWPFRun run = p.createRun();
        run.setFontFamily("Courier New");
        run.setFontSize(9);
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                run.addBreak();
            }
            run.setText(lines[i]);
        }
    }

    private void listDocx(XWPFDocument doc, MarkdownBlock block) {
        List<List<String>> items = block.items();
        for (int i = 0; i < items.size(); i++) {
            XWPFParagraph p = doc.createParagraph();
            p.setIndentationLeft(360);
            String prefix = block.ordered() ? (i + 1) + ". " : "• ";
            runDocx(p, prefix + cell(items.get(i), 0), 11, false);
        }
    }

    private void tableDocx(XWPFDocument doc, MarkdownBlock block) {
        List<List<String>> rows = block.items();
        int columns = rows.stream().mapToInt(List::size).max().orElse(0);
        if (columns == 0) {
            return;
        }
        XWPFTable table = doc.createTable(rows.size(), columns);
        for (int r = 0; r < rows.size(); r++) {
            XWPFTableRow row = table.getRow(r);
            List<String> cells = rows.get(r);
            for (int c = 0; c < columns; c++) {
                XWPFTableCell cell = row.getCell(c);
                XWPFParagraph p = cell.getParagraphs().isEmpty()
                        ? cell.addParagraph() : cell.getParagraphs().get(0);
                runDocx(p, cell(cells, c), 10, r == 0);
            }
        }
    }

    private XWPFRun runDocx(XWPFParagraph paragraph, String text, int size, boolean bold) {
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setFontSize(size);
        if (bold) {
            run.setBold(true);
        }
        // 中文字体兜底：ascii/hAnsi/eastAsia 统一宋体，避免部分阅读器显示异常
        run.setFontFamily("宋体", XWPFRun.FontCharRange.ascii);
        run.setFontFamily("宋体", XWPFRun.FontCharRange.hAnsi);
        run.setFontFamily("宋体", XWPFRun.FontCharRange.eastAsia);
        return run;
    }

    // ================ PDF（OpenPDF） ================

    private byte[] renderPdf(List<MarkdownBlock> blocks) {
        try {
            BaseFont cnBase = BaseFont.createFont("STSongStd-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            Font body = new Font(cnBase, 11, Font.NORMAL);
            Font quote = new Font(cnBase, 11, Font.ITALIC);
            Font code = new Font(cnBase, 9, Font.NORMAL);
            com.lowagie.text.Document pdf = new com.lowagie.text.Document(
                    com.lowagie.text.PageSize.A4, 48, 48, 48, 48);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(pdf, out);
            pdf.open();
            for (MarkdownBlock block : blocks) {
                switch (block.type()) {
                    case HEADING -> {
                        Font h = new Font(cnBase, Math.max(22 - block.level() * 2, 12), Font.BOLD);
                        pdf.add(new com.lowagie.text.Paragraph(block.text(), h));
                    }
                    case PARAGRAPH -> pdf.add(new com.lowagie.text.Paragraph(block.text(), body));
                    case CODE -> {
                        com.lowagie.text.Paragraph p = new com.lowagie.text.Paragraph();
                        p.setIndentationLeft(18);
                        for (String line : block.text().split("\n", -1)) {
                            p.add(new com.lowagie.text.Chunk(line + "\n", code));
                        }
                        pdf.add(p);
                    }
                    case QUOTE -> {
                        com.lowagie.text.Paragraph p = new com.lowagie.text.Paragraph(block.text(), quote);
                        p.setIndentationLeft(18);
                        pdf.add(p);
                    }
                    case LIST -> {
                        List<List<String>> items = block.items();
                        for (int i = 0; i < items.size(); i++) {
                            String prefix = block.ordered() ? (i + 1) + ". " : "• ";
                            com.lowagie.text.Paragraph p =
                                    new com.lowagie.text.Paragraph(prefix + cell(items.get(i), 0), body);
                            p.setIndentationLeft(18);
                            pdf.add(p);
                        }
                    }
                    case TABLE -> pdf.add(tablePdf(block, cnBase, body));
                }
            }
            pdf.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("[AgentDocumentService] PDF 渲染失败", e);
            throw exception(DOCUMENT_RENDER_FAIL, e, e.getMessage());
        }
    }

    private PdfPTable tablePdf(MarkdownBlock block, BaseFont cnBase, Font body) throws Exception {
        List<List<String>> rows = block.items();
        int columns = rows.stream().mapToInt(List::size).max().orElse(0);
        PdfPTable table = new PdfPTable(columns);
        table.setWidthPercentage(100);
        Font header = new Font(cnBase, 10, Font.BOLD);
        for (int r = 0; r < rows.size(); r++) {
            List<String> cells = rows.get(r);
            for (int c = 0; c < columns; c++) {
                PdfPCell cell = new PdfPCell(new Phrase(cell(cells, c), r == 0 ? header : body));
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                table.addCell(cell);
            }
        }
        return table;
    }

    // ================ 内部 ================

    private static String cell(List<String> cells, int index) {
        return index < cells.size() ? cells.get(index) : "";
    }

    /**
     * 规范文件名：去路径分隔符与非法字符、补扩展名、防目录穿越。
     */
    private String resolveName(String fileName, String format) {
        String name = fileName == null ? "" : fileName.trim();
        name = name.replaceAll("[\\\\/:*?\"<>|]", "").replace("..", "");
        if (name.isEmpty()) {
            name = "document-" + System.currentTimeMillis();
        }
        if (!name.toLowerCase().endsWith("." + format)) {
            name = name + "." + format;
        }
        return name;
    }
}
