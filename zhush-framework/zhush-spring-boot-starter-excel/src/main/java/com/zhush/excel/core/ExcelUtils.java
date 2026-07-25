package com.zhush.excel.core;

import com.zhush.common.exception.GlobalErrorCode;
import com.zhush.common.exception.ServiceException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.fesod.sheet.ExcelWriter;
import org.apache.fesod.sheet.FesodSheet;
import org.apache.fesod.sheet.write.builder.ExcelWriterBuilder;
import org.apache.fesod.sheet.write.builder.ExcelWriterSheetBuilder;
import org.apache.fesod.sheet.write.handler.WriteHandler;
import org.apache.fesod.sheet.write.metadata.WriteSheet;
import org.apache.fesod.sheet.write.style.column.LongestMatchColumnWidthStyleStrategy;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Excel 导出工具类（基于 Apache Fesod，原 EasyExcel）
 *
 * <p>支持能力：
 * <ul>
 *     <li>简单导出：注解模型方式，实体类通过 {@code @ExcelProperty} 定义表头，
 *         多级表头写法 {@code @ExcelProperty({"基本信息", "姓名"})}，相邻列同名一级表头自动合并</li>
 *     <li>复杂导出：动态多级表头方式，配合 {@link ExcelHeadBuilder} 构建
 *         "一级表头合并两个或以上二级表头"的合并表头，支持任意级数</li>
 *     <li>多 Sheet 导出：配合 {@link ExcelSheetData} 一次导出多个 Sheet，
 *         各 Sheet 可独立指定表头与合并/样式策略</li>
 *     <li>图片导出：注解模型中 {@code byte[]}、{@code File}、{@code InputStream}、{@code URL}
 *         类型字段自动写出为图片（{@code String} 图片路径字段需指定
 *         {@code @ExcelProperty(converter = StringImageConverter.class)}）；
 *         需要多图、边距、跨行列等精细控制时配合 {@link ExcelImageCell} 构建图片单元格</li>
 *     <li>所有方法均可追加 {@link WriteHandler}（如内容合并、自定义样式），默认注册列宽自适应策略；
 *         图片行建议追加 {@code SimpleRowHeightStyleStrategy} 调大内容行高</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * // 1. 简单导出（注解模型）
 * ExcelUtils.export(response, "用户列表", "用户", UserExcelVO.class, list);
 *
 * // 2. 复杂导出（动态合并表头）
 * List<List<String>> head = ExcelHeadBuilder.create()
 *         .col("序号")
 *         .group("基本信息", "姓名", "年龄")
 *         .build();
 * ExcelUtils.export(response, "用户列表", "用户", head, dataList);
 *
 * // 3. 图片导出（行数据中放入图片单元格，并调大行高）
 * List<Object> row = List.of("张三", ExcelImageCell.of(avatarBytes));
 * ExcelUtils.export(response, "用户列表", "用户", head, List.of(row),
 *         new SimpleRowHeightStyleStrategy(null, (short) 60));
 * }</pre>
 *
 * @author zhush
 */
@Slf4j
public class ExcelUtils {

    /**
     * Excel 文件后缀
     */
    private static final String XLSX_SUFFIX = ".xlsx";

    private ExcelUtils() {
    }

    // ==================== 简单导出（注解模型） ====================

    /**
     * 简单导出：注解模型方式写出到响应流
     *
     * <p>实体类通过 {@code @ExcelProperty} 定义表头，支持多级合并表头，
     * 如 {@code @ExcelProperty({"基本信息", "姓名"})} 与 {@code @ExcelProperty({"基本信息", "年龄"})}
     * 的一级表头"基本信息"会自动合并两个单元格。
     *
     * @param response      响应对象
     * @param filename      文件名（可不带 .xlsx 后缀）
     * @param sheetName     Sheet 名称
     * @param headClass     表头模型类
     * @param data          行数据
     * @param writeHandlers 附加的写处理器（合并策略、样式策略等），可选
     */
    public static <T> void export(HttpServletResponse response, String filename, String sheetName,
                                  Class<T> headClass, List<T> data, WriteHandler... writeHandlers) {
        try {
            prepareResponse(response, filename);
            write(response.getOutputStream(), sheetName, headClass, data, writeHandlers);
        } catch (Exception e) {
            log.error("[export][导出 Excel({}) 失败]", filename, e);
            throw new ServiceException(GlobalErrorCode.INTERNAL_ERROR, e);
        }
    }

    /**
     * 简单导出：注解模型方式写出到任意输出流（保存文件、上传对象存储等场景）
     */
    public static <T> void write(OutputStream outputStream, String sheetName,
                                 Class<T> headClass, List<T> data, WriteHandler... writeHandlers) {
        ExcelWriterSheetBuilder sheetBuilder = FesodSheet.write(outputStream, headClass)
                .autoCloseStream(false)
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .sheet(sheetName);
        registerHandlers(sheetBuilder, writeHandlers);
        sheetBuilder.doWrite(data);
    }

    // ==================== 复杂导出（动态多级表头） ====================

    /**
     * 复杂导出：动态多级表头方式写出到响应流
     *
     * <p>表头建议通过 {@link ExcelHeadBuilder} 构建，支持一级表头合并多个二级表头、
     * 任意级数嵌套等合并表头场景。
     *
     * @param response      响应对象
     * @param filename      文件名（可不带 .xlsx 后缀）
     * @param sheetName     Sheet 名称
     * @param head          动态表头（每列自上而下的各级标题）
     * @param data          行数据，每行为一个 List，与表头列一一对应
     * @param writeHandlers 附加的写处理器（合并策略、样式策略等），可选
     */
    public static void export(HttpServletResponse response, String filename, String sheetName,
                              List<List<String>> head, List<? extends List<?>> data,
                              WriteHandler... writeHandlers) {
        try {
            prepareResponse(response, filename);
            write(response.getOutputStream(), sheetName, head, data, writeHandlers);
        } catch (Exception e) {
            log.error("[export][导出 Excel({}) 失败]", filename, e);
            throw new ServiceException(GlobalErrorCode.INTERNAL_ERROR, e);
        }
    }

    /**
     * 复杂导出：动态多级表头方式写出到任意输出流
     */
    public static void write(OutputStream outputStream, String sheetName,
                             List<List<String>> head, List<? extends List<?>> data,
                             WriteHandler... writeHandlers) {
        ExcelWriterSheetBuilder sheetBuilder = FesodSheet.write(outputStream)
                .autoCloseStream(false)
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .head(head)
                .sheet(sheetName);
        registerHandlers(sheetBuilder, writeHandlers);
        sheetBuilder.doWrite(data);
    }

    // ==================== 复杂导出（多 Sheet） ====================

    /**
     * 复杂导出：多 Sheet 写出到响应流
     *
     * <p>每个 Sheet 通过 {@link ExcelSheetData} 描述，可独立使用注解模型或动态表头，
     * 并可独立追加合并/样式策略。
     *
     * @param response 响应对象
     * @param filename 文件名（可不带 .xlsx 后缀）
     * @param sheets   各 Sheet 的导出数据
     */
    public static void exportMultiSheet(HttpServletResponse response, String filename, List<ExcelSheetData> sheets) {
        try {
            prepareResponse(response, filename);
            writeMultiSheet(response.getOutputStream(), sheets);
        } catch (Exception e) {
            log.error("[exportMultiSheet][导出 Excel({}) 失败]", filename, e);
            throw new ServiceException(GlobalErrorCode.INTERNAL_ERROR, e);
        }
    }

    /**
     * 复杂导出：多 Sheet 写出到任意输出流
     */
    public static void writeMultiSheet(OutputStream outputStream, List<ExcelSheetData> sheets) {
        if (sheets == null || sheets.isEmpty()) {
            throw new IllegalArgumentException("导出的 Sheet 数据不能为空");
        }
        ExcelWriterBuilder writerBuilder = FesodSheet.write(outputStream)
                .autoCloseStream(false)
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy());
        try (ExcelWriter excelWriter = writerBuilder.build()) {
            for (int i = 0; i < sheets.size(); i++) {
                ExcelSheetData sheetData = sheets.get(i);
                ExcelWriterSheetBuilder sheetBuilder = FesodSheet.writerSheet(i, sheetData.getSheetName());
                // 表头二选一：优先注解模型，其次动态表头
                if (sheetData.getHeadClass() != null) {
                    sheetBuilder.head(sheetData.getHeadClass());
                } else if (sheetData.getHead() != null) {
                    sheetBuilder.head(sheetData.getHead());
                }
                for (WriteHandler writeHandler : sheetData.getWriteHandlers()) {
                    sheetBuilder.registerWriteHandler(writeHandler);
                }
                WriteSheet writeSheet = sheetBuilder.build();
                excelWriter.write(sheetData.getData(), writeSheet);
            }
        }
    }

    // ==================== 内部方法 ====================

    /**
     * 设置文件下载的响应头（Content-Type、Content-Disposition）
     */
    private static void prepareResponse(HttpServletResponse response, String filename) {
        String finalName = filename.endsWith(XLSX_SUFFIX) ? filename : filename + XLSX_SUFFIX;
        // RFC 5987：filename* 支持中文文件名，URLEncoder 将空格编码为 + 需替换为 %20
        String encodedName = URLEncoder.encode(finalName, StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + encodedName);
    }

    private static void registerHandlers(ExcelWriterSheetBuilder sheetBuilder, WriteHandler... writeHandlers) {
        if (writeHandlers == null) {
            return;
        }
        for (WriteHandler writeHandler : writeHandlers) {
            sheetBuilder.registerWriteHandler(writeHandler);
        }
    }

}
