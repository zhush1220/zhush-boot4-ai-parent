package com.zhush.excel.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Excel 多级表头构建器
 *
 * <p>用于动态构建 {@code List<List<String>>} 结构的多级表头，每一列的表头自上而下为一个 List，
 * 相邻列的同名上级表头会被自动合并为一个单元格；单级列（如"序号"）会自动向下补齐并合并，
 * 从而实现"一级表头合并两个或以上二级表头"的效果。
 *
 * <p>使用示例（一级表头"基本信息"合并"姓名"、"年龄"两个二级表头）：
 * <pre>{@code
 * List<List<String>> head = ExcelHeadBuilder.create()
 *         .col("序号")
 *         .group("基本信息", "姓名", "年龄")
 *         .group("联系方式", "手机号", "邮箱")
 *         .col("备注")
 *         .build();
 * }</pre>
 * 生成的表头效果：
 * <pre>
 * +------+-----------------+-------------------+------+
 * | 序号 |    基本信息      |     联系方式       | 备注 |
 * |      +--------+--------+---------+---------+      |
 * |      |  姓名  |  年龄  |  手机号  |  邮箱   |      |
 * +------+--------+--------+---------+---------+------+
 * </pre>
 *
 * @author zhush
 */
public class ExcelHeadBuilder {

    /**
     * 所有列的表头，每个元素代表一列自上而下的各级标题
     */
    private final List<List<String>> columns = new ArrayList<>();

    private ExcelHeadBuilder() {
    }

    public static ExcelHeadBuilder create() {
        return new ExcelHeadBuilder();
    }

    /**
     * 添加一列表头，支持任意级数（自上而下传入各级标题）
     *
     * <p>例如 {@code col("序号")} 为单级列；{@code col("成绩", "语文", "平时分")} 为三级列。
     *
     * @param levels 该列自上而下的各级标题，至少一级
     * @return this
     */
    public ExcelHeadBuilder col(String... levels) {
        if (levels == null || levels.length == 0) {
            throw new IllegalArgumentException("表头列的标题不能为空");
        }
        columns.add(new ArrayList<>(List.of(levels)));
        return this;
    }

    /**
     * 添加一组二级表头：一个一级表头合并两个或以上的二级表头
     *
     * <p>例如 {@code group("基本信息", "姓名", "年龄")}，"基本信息"横向合并两个单元格，
     * 分别对应下方的"姓名"、"年龄"两列。
     *
     * @param parent   一级表头标题
     * @param children 其下的二级表头标题，至少一个
     * @return this
     */
    public ExcelHeadBuilder group(String parent, String... children) {
        if (children == null || children.length == 0) {
            throw new IllegalArgumentException("分组表头 [" + parent + "] 下至少需要一个子表头");
        }
        for (String child : children) {
            columns.add(new ArrayList<>(List.of(parent, child)));
        }
        return this;
    }

    /**
     * 在指定的多级父表头下添加一组子表头，用于构建三级及以上表头
     *
     * <p>例如 {@code group(new String[]{"成绩", "理科"}, "物理", "化学")}，
     * 一级"成绩"、二级"理科"共同合并"物理"、"化学"两列。
     *
     * @param parents  自上而下的各级父表头标题
     * @param children 最下级的子表头标题，至少一个
     * @return this
     */
    public ExcelHeadBuilder group(String[] parents, String... children) {
        if (parents == null || parents.length == 0) {
            throw new IllegalArgumentException("分组表头的父级标题不能为空");
        }
        if (children == null || children.length == 0) {
            throw new IllegalArgumentException("分组表头下至少需要一个子表头");
        }
        for (String child : children) {
            List<String> column = new ArrayList<>(List.of(parents));
            column.add(child);
            columns.add(column);
        }
        return this;
    }

    /**
     * 构建最终表头
     *
     * <p>会将级数不足的列用其最后一级标题向下补齐至最大级数，
     * 使单级列（如"序号"）在多级表头中自动纵向合并为一个单元格。
     *
     * @return 可直接用于 {@code FesodSheet.write().head(...)} 的表头结构
     */
    public List<List<String>> build() {
        if (columns.isEmpty()) {
            throw new IllegalStateException("表头不能为空，请先调用 col/group 添加列");
        }
        int maxDepth = columns.stream().mapToInt(List::size).max().orElse(1);
        List<List<String>> head = new ArrayList<>(columns.size());
        for (List<String> column : columns) {
            List<String> padded = new ArrayList<>(column);
            String last = padded.get(padded.size() - 1);
            while (padded.size() < maxDepth) {
                padded.add(last);
            }
            head.add(padded);
        }
        return head;
    }

}
