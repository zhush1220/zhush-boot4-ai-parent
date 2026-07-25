package com.zhush.excel.core;

import com.zhush.common.exception.GlobalErrorCode;
import com.zhush.common.exception.ServiceException;
import org.apache.fesod.sheet.enums.CellDataTypeEnum;
import org.apache.fesod.sheet.metadata.data.ClientAnchorData;
import org.apache.fesod.sheet.metadata.data.ImageData;
import org.apache.fesod.sheet.metadata.data.WriteCellData;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Excel 图片单元格构建器（基于 Apache Fesod，原 EasyExcel）
 *
 * <p>将图片包装为 {@link WriteCellData}，可直接作为行数据的一个字段值使用：
 * <ul>
 *     <li>注解模型导出：实体类字段声明为 {@code WriteCellData<Void>} 类型</li>
 *     <li>动态表头导出：行数据 {@code List<Object>} 中对应列直接放入构建结果</li>
 * </ul>
 *
 * <p>支持能力：
 * <ul>
 *     <li>图片来源：字节数组、本地文件、输入流、URL（网络图片）</li>
 *     <li>一个单元格内放置多张图片</li>
 *     <li>图片边距（上右下左，单位像素）、跨行/跨列展示、锚定方式</li>
 *     <li>图片单元格附带文字内容</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * // 1. 最简单：单元格放一张图片
 * WriteCellData<Void> cell = ExcelImageCell.of(imageBytes);
 *
 * // 2. 复杂：多图 + 边距 + 跨列
 * WriteCellData<Void> cell = ExcelImageCell.create()
 *         .image(imageBytes).margin(5, 5, 5, 5)
 *         .image(new File("logo.png")).span(1, 2)
 *         .build();
 *
 * // 3. 行数据中使用（动态表头导出）
 * List<Object> row = new ArrayList<>();
 * row.add("张三");
 * row.add(ExcelImageCell.of(avatarBytes));
 * }</pre>
 *
 * <p>注解模型直接用图片字段类型也可（无需本构建器）：{@code byte[]}、{@link File}、
 * {@link InputStream}、{@link URL} 类型字段开箱即用；{@code String} 图片路径字段
 * 需显式指定 {@code @ExcelProperty(converter = StringImageConverter.class)}。
 *
 * @author zhush
 */
public class ExcelImageCell {

    /**
     * 单元格内的图片列表
     */
    private final List<ImageData> imageDataList = new ArrayList<>();

    /**
     * 单元格文字内容（可选，与图片同时展示）
     */
    private String text;

    private ExcelImageCell() {
    }

    /**
     * 创建构建器
     */
    public static ExcelImageCell create() {
        return new ExcelImageCell();
    }

    // ==================== 静态快捷方法（单图场景） ====================

    /**
     * 快捷构建：单张图片的单元格
     *
     * @param image 图片字节数组
     */
    public static WriteCellData<Void> of(byte[] image) {
        return create().image(image).build();
    }

    /**
     * 快捷构建：单张本地图片文件的单元格
     */
    public static WriteCellData<Void> of(File imageFile) {
        return create().image(imageFile).build();
    }

    /**
     * 快捷构建：单张网络图片的单元格
     */
    public static WriteCellData<Void> of(URL imageUrl) {
        return create().image(imageUrl).build();
    }

    // ==================== 添加图片 ====================

    /**
     * 添加一张图片（字节数组）
     *
     * <p>图片格式（PNG/JPEG 等）由 Fesod 自动识别，也可调用 {@link #imageType} 显式指定。
     */
    public ExcelImageCell image(byte[] image) {
        if (image == null || image.length == 0) {
            throw new IllegalArgumentException("图片内容不能为空");
        }
        ImageData imageData = new ImageData();
        imageData.setImage(image);
        imageDataList.add(imageData);
        return this;
    }

    /**
     * 添加一张图片（本地文件）
     */
    public ExcelImageCell image(File imageFile) {
        try {
            return image(Files.readAllBytes(imageFile.toPath()));
        } catch (Exception e) {
            throw new ServiceException(GlobalErrorCode.INTERNAL_ERROR, e);
        }
    }

    /**
     * 添加一张图片（输入流，读取完毕后关闭）
     */
    public ExcelImageCell image(InputStream inputStream) {
        try (InputStream in = inputStream) {
            return image(in.readAllBytes());
        } catch (Exception e) {
            throw new ServiceException(GlobalErrorCode.INTERNAL_ERROR, e);
        }
    }

    /**
     * 添加一张图片（URL，如网络图片地址）
     */
    public ExcelImageCell image(URL imageUrl) {
        try {
            return image(imageUrl.openStream());
        } catch (Exception e) {
            throw new ServiceException(GlobalErrorCode.INTERNAL_ERROR, e);
        }
    }

    // ==================== 定制最近添加的图片 ====================

    /**
     * 显式指定图片格式（作用于最近一次添加的图片）
     */
    public ExcelImageCell imageType(ImageData.ImageType imageType) {
        lastImage().setImageType(imageType);
        return this;
    }

    /**
     * 设置图片边距（作用于最近一次添加的图片）
     *
     * @param top    上边距（像素）
     * @param right  右边距（像素）
     * @param bottom 下边距（像素）
     * @param left   左边距（像素）
     */
    public ExcelImageCell margin(Integer top, Integer right, Integer bottom, Integer left) {
        ImageData imageData = lastImage();
        imageData.setTop(top);
        imageData.setRight(right);
        imageData.setBottom(bottom);
        imageData.setLeft(left);
        return this;
    }

    /**
     * 设置图片跨行/跨列展示（作用于最近一次添加的图片）
     *
     * <p>如 {@code span(2, 3)} 表示图片从当前单元格起，向下覆盖 2 行、向右覆盖 3 列。
     *
     * @param rows 覆盖的行数（≥1）
     * @param cols 覆盖的列数（≥1）
     */
    public ExcelImageCell span(int rows, int cols) {
        if (rows < 1 || cols < 1) {
            throw new IllegalArgumentException("跨行/跨列数必须大于等于 1");
        }
        ImageData imageData = lastImage();
        imageData.setRelativeLastRowIndex(rows - 1);
        imageData.setRelativeLastColumnIndex(cols - 1);
        return this;
    }

    /**
     * 设置图片锚定方式（作用于最近一次添加的图片），默认随单元格移动并缩放
     */
    public ExcelImageCell anchorType(ClientAnchorData.AnchorType anchorType) {
        lastImage().setAnchorType(anchorType);
        return this;
    }

    // ==================== 其它 ====================

    /**
     * 设置单元格文字内容（与图片同时展示）
     */
    public ExcelImageCell text(String text) {
        this.text = text;
        return this;
    }

    /**
     * 构建单元格数据
     */
    public WriteCellData<Void> build() {
        if (imageDataList.isEmpty()) {
            throw new IllegalArgumentException("图片单元格至少需要一张图片");
        }
        WriteCellData<Void> cellData;
        if (text != null) {
            cellData = new WriteCellData<>(text);
        } else {
            cellData = new WriteCellData<>(CellDataTypeEnum.EMPTY);
        }
        cellData.setImageDataList(imageDataList);
        return cellData;
    }

    private ImageData lastImage() {
        if (imageDataList.isEmpty()) {
            throw new IllegalStateException("请先调用 image(...) 添加图片");
        }
        return imageDataList.get(imageDataList.size() - 1);
    }

}
