package com.zhush.ai.preview;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link PathMaskUtils} 单测：回答正文里的本机绝对路径必须打码，且打码后仍可被
 * {@link AnswerFileArtifactService#normalize(String)} 解析（预览入口不能因为脱敏而失效）。
 *
 * @author zhush
 */
class PathMaskUtilsTest {

    private static final String ELLIPSIS = "…";

    /** 生产环境的根来自 user.dir/tmpdir，单测里显式注入才能跨机器断言 */
    private static final List<String> ROOTS = List.of(
            "D:\\zhush\\project\\back-boot\\zhush-boot4-parent", "/srv/zhush/app");

    private static String mask(String text) {
        return PathMaskUtils.mask(text, ROOTS);
    }

    @Test
    void masksWorkspacePathsKeepingPartitionRelativeTail() {
        assertThat(mask("D:\\zhush\\project\\back-boot\\zhush-boot4-parent\\.agentscope\\workspace\\1\\brand\\png\\icon-16.png"))
                .isEqualTo(ELLIPSIS + "/brand/png/icon-16.png");
        // 模型自己省写过的前缀里仍带着工作区标记，一样要吞掉——它暴露的是服务器目录结构
        assertThat(mask("…\\agentscope\\workspace\\1\\brand\\png\\zhush-logo.ico"))
                .isEqualTo(ELLIPSIS + "/brand/png/zhush-logo.ico");
        // 容器里的 POSIX 形态
        assertThat(mask("/data/zhush/.agentscope/workspace/42/brand/report.docx"))
                .isEqualTo(ELLIPSIS + "/brand/report.docx");
        // 目录（无文件名）也要处理
        assertThat(mask("完整根路径：D:\\zhush\\.agentscope\\workspace\\1\\brand\\png\\"))
                .isEqualTo("完整根路径：" + ELLIPSIS + "/brand/png/");
    }

    @Test
    void masksPathsUnderKnownRoots() {
        assertThat(mask("见 " + ROOTS.get(0) + "\\sql\\14_notice.sql")).isEqualTo("见 " + ELLIPSIS + "/sql/14_notice.sql");
        assertThat(mask(ROOTS.get(1) + "/logs/app.log")).isEqualTo(ELLIPSIS + "/logs/app.log");
    }

    @Test
    void fallsBackToTailForForeignAbsolutePaths() {
        // 不在我们的根下、也不是工作区：只保留末两段，够用户认文件，不暴露目录结构
        assertThat(mask("C:\\Users\\alice\\Desktop\\季度报表.xlsx")).isEqualTo(ELLIPSIS + "/Desktop/季度报表.xlsx");
        assertThat(mask("\\\\fileserver\\share\\a\\b.pdf")).isEqualTo(ELLIPSIS + "/a/b.pdf");
    }

    @Test
    void leavesRelativeAndWebReferencesAlone() {
        assertThat(mask("brand/png/icon-16.png")).isEqualTo("brand/png/icon-16.png");
        assertThat(mask(ELLIPSIS + "/brand/png/icon-16.png")).isEqualTo(ELLIPSIS + "/brand/png/icon-16.png");
        assertThat(mask("https://files.example.com/a/b.png")).isEqualTo("https://files.example.com/a/b.png");
        // 协议相对 URL 归一后也是 // 开头，不能被当成 UNC 打掉
        assertThat(mask("//cdn.example.com/a/b.png")).isEqualTo("//cdn.example.com/a/b.png");
        assertThat(mask("/files/2026/a.png")).isEqualTo("/files/2026/a.png");
        assertThat(mask("参考 /etc/passwd 与 example.com/brand/x.svg")).isEqualTo("参考 /etc/passwd 与 example.com/brand/x.svg");
        assertThat(mask("README.md 里没有路径")).isEqualTo("README.md 里没有路径");
    }

    @Test
    void keepsSurroundingProseAndPunctuation() {
        assertThat(mask("- 文件：`D:\\w\\.agentscope\\workspace\\1\\brand\\zhush-logo.svg`，尺寸 16×16。"))
                .isEqualTo("- 文件：`" + ELLIPSIS + "/brand/zhush-logo.svg`，尺寸 16×16。");
        assertThat(mask("产物在 D:\\proj\\.agentscope\\workspace\\1\\brand\\a.md."))
                .isEqualTo("产物在 " + ELLIPSIS + "/brand/a.md.");
    }

    @Test
    void maskingIsIdempotentAndNullSafe() {
        String once = mask("D:\\proj\\.agentscope\\workspace\\1\\brand\\png\\icon-16.png");
        assertThat(mask(once)).isEqualTo(once);
        assertThat(mask(null)).isNull();
        assertThat(mask("   ")).isEqualTo("   ");
        assertThat(mask("没有路径的普通回答")).isEqualTo("没有路径的普通回答");
    }

    @Test
    void maskedReferenceStillResolvesThroughNormalize() {
        // 脱敏不能打断「正文点击预览」：打码结果丢给 normalize 仍要还原成分区内相对路径
        String masked = mask("D:\\zhush\\.agentscope\\workspace\\1\\brand\\png\\icon-16.png");
        assertThat(AnswerFileArtifactService.normalize(masked)).isEqualTo("brand/png/icon-16.png");
    }
}
