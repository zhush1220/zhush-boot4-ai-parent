package com.zhush.ai.preview;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 正文引用路径解析测试：工作区绝对路径不得把分区段拼两次，且越界形态一律判死。
 *
 * <p>只覆盖路径解析，故其余协作者传 null——{@code normalize} 是纯函数，{@code locate} 只碰文件系统与配置。
 *
 * @author zhush
 */
class AnswerFileArtifactServiceTest {

    /** 省略号 U+2026：模型为了排版常把工作区之前的绝对前缀省成它，正文里因此出现「看不全的路径」 */
    private static final String ELLIPSIS = "…";

    private static AnswerFileArtifactService serviceWithRoot(Path workspaceRoot) {
        PreviewProperties properties = new PreviewProperties();
        properties.setAnswerFileWorkspaceRoot(workspaceRoot.toString());
        return new AnswerFileArtifactService(properties, null, null, null, null, new ObjectMapper());
    }

    @Test
    void normalizeDropsPartitionSegmentOfWorkspaceAbsolutePath() {
        String reference = "D:\\zhush\\project\\back-boot\\zhush-boot4-parent"
                + "\\.agentscope\\workspace\\1\\brand\\png\\zhush-logo.ico";

        assertThat(AnswerFileArtifactService.normalize(reference))
                .isEqualTo("brand/png/zhush-logo.ico");
    }

    @Test
    void normalizeKeepsReferenceAlreadyRelativeToPartition() {
        assertThat(AnswerFileArtifactService.normalize("brand/zhush-logo.svg"))
                .isEqualTo("brand/zhush-logo.svg");
        assertThat(AnswerFileArtifactService.normalize("`brand/zhush-logo.svg`"))
                .isEqualTo("brand/zhush-logo.svg");
    }

    @Test
    void normalizeRejectsExternalAndEscapingForms() {
        assertThat(AnswerFileArtifactService.normalize("http://127.0.0.1:8080/a.png")).isNull();
        assertThat(AnswerFileArtifactService.normalize("/etc/passwd")).isNull();
        assertThat(AnswerFileArtifactService.normalize("../../etc/passwd")).isNull();
        assertThat(AnswerFileArtifactService.normalize("brand/../../windows/win.ini")).isNull();
        // 省略号只吞前缀，不给跳出分区开后门：省略段之后的 .. 仍然判死
        assertThat(AnswerFileArtifactService.normalize(ELLIPSIS + "/../../etc/passwd")).isNull();
        assertThat(AnswerFileArtifactService.normalize("")).isNull();
    }

    @Test
    void normalizeTreatsEllipsisSegmentAsElidedPrefix() {
        // 模型排版时常见写法：把工作区之前的绝对前缀省成省略号
        assertThat(AnswerFileArtifactService.normalize(ELLIPSIS + "/brand/png/icon-16.png"))
                .isEqualTo("brand/png/icon-16.png");
        assertThat(AnswerFileArtifactService.normalize(".../brand/png/icon-16.png"))
                .isEqualTo("brand/png/icon-16.png");
        assertThat(AnswerFileArtifactService.normalize("D:\\zhush\\project\\" + ELLIPSIS + "\\brand\\png\\icon-16.png"))
                .isEqualTo("brand/png/icon-16.png");
        // 省略号与 /workspace/ 标记同时出现时，标记优先：省略的仍是前缀，分区段照旧丢弃
        assertThat(AnswerFileArtifactService.normalize(ELLIPSIS + "\\.agentscope\\workspace\\1\\brand\\zhush-logo.svg"))
                .isEqualTo("brand/zhush-logo.svg");
        // 省略段把整条路径吞光就是非法引用，不能退化成「分区根目录」
        assertThat(AnswerFileArtifactService.normalize("brand/png/" + ELLIPSIS)).isNull();
    }

    @Test
    void workspaceAbsolutePathResolvesWithoutDuplicatingPartition(@TempDir Path root) throws IOException {
        Path workspaceRoot = root.resolve(".agentscope").resolve("workspace");
        Path assetDir = workspaceRoot.resolve("1").resolve("brand").resolve("png");
        Files.createDirectories(assetDir);
        Files.writeString(assetDir.resolve("zhush-logo.ico"), "icon");

        String reference = assetDir.resolve("zhush-logo.ico").toString();
        String relative = AnswerFileArtifactService.normalize(reference);

        assertThat(relative).isEqualTo("brand/png/zhush-logo.ico");
        assertThat(serviceWithRoot(workspaceRoot).locate(relative, "1")).isNotNull();
    }

    @Test
    void elidedPrefixResolvesInsideCallerPartition(@TempDir Path root) throws IOException {
        Path workspaceRoot = root.resolve(".agentscope").resolve("workspace");
        Path assetDir = workspaceRoot.resolve("1").resolve("brand").resolve("png");
        Files.createDirectories(assetDir);
        Files.writeString(assetDir.resolve("icon-16.png"), "icon");

        AnswerFileArtifactService service = serviceWithRoot(workspaceRoot);
        // 正文里被省掉前缀的引用必须点得开：省略段之前的部分本就不该参与定位
        assertThat(service.locate(AnswerFileArtifactService.normalize(ELLIPSIS + "/brand/png/icon-16.png"), "1"))
                .isNotNull();
        // 但省前缀不等于给分区：别人的分区段不会因为一次省略就可读
        assertThat(service.locate(AnswerFileArtifactService.normalize(ELLIPSIS + "/2/brand/png/icon-16.png"), "1"))
                .isNull();
    }

    @Test
    void locateFallsBackWhenReferenceCarriesOwnPartition(@TempDir Path root) throws IOException {
        Path workspaceRoot = root.resolve(".agentscope").resolve("workspace");
        Path assetDir = workspaceRoot.resolve("1").resolve("brand").resolve("png");
        Files.createDirectories(assetDir);
        Files.writeString(assetDir.resolve("zhush-logo.ico"), "icon");
        Files.createDirectories(workspaceRoot.resolve("2").resolve("brand"));

        AnswerFileArtifactService service = serviceWithRoot(workspaceRoot);
        // 正文里直接写「分区/相对路径」时也要能定位：先按分区内原样找，找不到再剥掉重复的自有分区段
        assertThat(service.locate("1/brand/png/zhush-logo.ico", "1")).isNotNull();
        // 但只有调用者自己的分区段可以这么剥，别人的前缀不得被接受
        assertThat(service.locate("2/brand/zhush-logo.ico", "1")).isNull();
    }

    @Test
    void locateStaysInsideCallerPartition(@TempDir Path root) throws IOException {
        Path workspaceRoot = root.resolve(".agentscope").resolve("workspace");
        Path other = workspaceRoot.resolve("2").resolve("brand");
        Files.createDirectories(other);
        Files.writeString(other.resolve("secret.png"), "x");

        AnswerFileArtifactService service = serviceWithRoot(workspaceRoot);
        Files.createDirectories(workspaceRoot.resolve("1"));

        assertThat(service.locate("../2/brand/secret.png", "1")).isNull();
        assertThat(service.locate("brand/secret.png", "1")).isNull();
        assertThat(service.locate("brand/secret.png", "2")).isNotNull();
    }
}
