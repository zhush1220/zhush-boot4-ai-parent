package com.zhush.ai.generation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link MarkdownLiteParser} 块解析与行内清洗测试：
 * 覆盖标题/段落/围栏代码/引用/列表/表格识别、连续块合并与链接等行内标记清洗。
 *
 * @author zhush
 */
class MarkdownLiteParserTest {

    @Test
    void parseMixedBlocks() {
        String markdown = """
                # 一级标题

                普通段落第一行
                普通段落第二行

                ```java
                System.out.println("hi");
                ```

                > 引用一行
                > 引用二行

                - 无序A
                - 无序B

                1. 有序一
                2. 有序二

                | 列1 | 列2 |
                | --- | :-- |
                | a | b |
                """;
        List<MarkdownBlock> blocks = MarkdownLiteParser.parse(markdown);

        assertThat(blocks).extracting(MarkdownBlock::type).containsExactly(
                MarkdownBlock.BlockType.HEADING,
                MarkdownBlock.BlockType.PARAGRAPH,
                MarkdownBlock.BlockType.CODE,
                MarkdownBlock.BlockType.QUOTE,
                MarkdownBlock.BlockType.LIST,
                MarkdownBlock.BlockType.LIST,
                MarkdownBlock.BlockType.TABLE);

        MarkdownBlock heading = blocks.get(0);
        assertThat(heading.level()).isEqualTo(1);
        assertThat(heading.text()).isEqualTo("一级标题");

        // 段落收集连续文本行
        assertThat(blocks.get(1).text()).isEqualTo("普通段落第一行\n普通段落第二行");

        // 代码块保留原样（含字符串内的围栏不误判）
        assertThat(blocks.get(2).text()).contains("System.out.println(\"hi\");");

        // 引用合并多行
        assertThat(blocks.get(3).text()).contains("引用一行").contains("引用二行");

        // 无序/有序列表：同类合并，items 每项单元素
        MarkdownBlock ul = blocks.get(4);
        assertThat(ul.ordered()).isFalse();
        assertThat(ul.items()).hasSize(2);
        MarkdownBlock ol = blocks.get(5);
        assertThat(ol.ordered()).isTrue();
        assertThat(ol.items()).hasSize(2);

        // 表格：首行为表头，分隔行被跳过
        MarkdownBlock table = blocks.get(6);
        assertThat(table.items()).hasSize(2);
        assertThat(table.items().get(0)).containsExactly("列1", "列2");
        assertThat(table.items().get(1)).containsExactly("a", "b");
    }

    @Test
    void cleanInlineStripsMarkers() {
        assertThat(MarkdownLiteParser.cleanInline("**加粗** 与 *斜体* 与 `代码` 与 ~~删除~~"))
                .isEqualTo("加粗 与 斜体 与 代码 与 删除");
        assertThat(MarkdownLiteParser.cleanInline("[文档](https://a.com/b.docx)"))
                .isEqualTo("文档（https://a.com/b.docx）");
        assertThat(MarkdownLiteParser.cleanInline("![图](https://img.png)"))
                .isEqualTo("图（https://img.png）");
    }

    @Test
    void blankInputReturnsEmpty() {
        assertThat(MarkdownLiteParser.parse(null)).isEmpty();
        assertThat(MarkdownLiteParser.parse("   ")).isEmpty();
    }
}
