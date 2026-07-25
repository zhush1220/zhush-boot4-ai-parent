package com.zhush.excel.core;

import lombok.Data;
import org.apache.fesod.sheet.write.handler.WriteHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Excel 单个 Sheet 的导出数据描述
 *
 * <p>用于 {@code ExcelUtils#exportMultiSheet} 多 Sheet 复杂导出，
 * 表头支持两种方式（二选一，优先使用 {@link #headClass}）：
 * <ul>
 *     <li>{@link #headClass}：注解模型方式，通过 {@code @ExcelProperty} 定义表头</li>
 *     <li>{@link #head}：动态表头方式，可由 {@link ExcelHeadBuilder} 构建</li>
 * </ul>
 *
 * @author zhush
 */
@Data
public class ExcelSheetData {

    /**
     * Sheet 名称
     */
    private String sheetName;

    /**
     * 注解模型表头（@ExcelProperty 方式）
     */
    private Class<?> headClass;

    /**
     * 动态表头（每列自上而下的各级标题）
     */
    private List<List<String>> head;

    /**
     * 行数据：注解模型方式为实体列表；动态表头方式为 {@code List<List<Object>>}
     */
    private List<?> data;

    /**
     * 该 Sheet 独立的写处理器（合并策略、样式策略等），可选
     */
    private List<WriteHandler> writeHandlers = new ArrayList<>();

    /**
     * 创建注解模型方式的 Sheet 数据
     */
    public static ExcelSheetData of(String sheetName, Class<?> headClass, List<?> data) {
        ExcelSheetData sheetData = new ExcelSheetData();
        sheetData.setSheetName(sheetName);
        sheetData.setHeadClass(headClass);
        sheetData.setData(data);
        return sheetData;
    }

    /**
     * 创建动态表头方式的 Sheet 数据
     */
    public static ExcelSheetData of(String sheetName, List<List<String>> head, List<? extends List<?>> data) {
        ExcelSheetData sheetData = new ExcelSheetData();
        sheetData.setSheetName(sheetName);
        sheetData.setHead(head);
        sheetData.setData(data);
        return sheetData;
    }

    /**
     * 链式追加写处理器（如合并策略、样式策略）
     */
    public ExcelSheetData addWriteHandler(WriteHandler writeHandler) {
        this.writeHandlers.add(writeHandler);
        return this;
    }

}
