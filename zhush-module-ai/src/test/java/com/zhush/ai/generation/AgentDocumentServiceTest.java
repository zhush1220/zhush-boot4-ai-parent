package com.zhush.ai.generation;

import com.zhush.common.exception.ServiceException;
import com.zhush.file.core.FileClient;
import com.zhush.file.core.FileUploadResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link AgentDocumentService} 渲染链路测试：
 * docx（PK _zip 签名）/ pdf（%PDF 头）字节有效性、文件名规范与参数校验；
 * 文件服务以 Mock 替代，不依赖真实上传。
 *
 * <p>注：OpenPDF 中文（STSongStd-Light 非嵌入 CJK）在真实阅读器的显示效果需人工验证，
 * 本用例仅断言产物字节头与大小。
 *
 * @author zhush
 */
@ExtendWith(MockitoExtension.class)
class AgentDocumentServiceTest {

    @Mock
    private FileClient fileClient;

    private static final String MARKDOWN = """
            # 测试报告

            段落内容 **加粗** 与链接 [文档](https://a.com/x)

            - 项目一
            - 项目二

            | 列1 | 列2 |
            | --- | --- |
            | 甲 | 乙 |
            """;

    private AgentDocumentService service() {
        return new AgentDocumentService(fileClient);
    }

    private void mockUpload() {
        when(fileClient.upload(any(byte[].class), anyString(), anyString()))
                .thenReturn(FileUploadResult.builder()
                        .url("http://localhost:9166/files/2026/08/test.docx")
                        .size(1024L)
                        .build());
    }

    @Test
    void generateDocxProducesValidBytes() {
        mockUpload();
        FileArtifact artifact = service().generate(MARKDOWN, "docx", "测试报告");

        assertThat(artifact.getFormat()).isEqualTo("docx");
        assertThat(artifact.getName()).isEqualTo("测试报告.docx");
        assertThat(artifact.getUrl()).isNotBlank();
        assertThat(artifact.getType()).isEqualTo("file");

        ArgumentCaptor<byte[]> bytes = ArgumentCaptor.forClass(byte[].class);
        verify(fileClient).upload(bytes.capture(), eq("测试报告.docx"), anyString());
        // docx 为 zip 容器，PK\x03\x04 签名
        byte[] content = bytes.getValue();
        assertThat(content.length).isGreaterThan(1000);
        assertThat(content[0]).isEqualTo((byte) 'P');
        assertThat(content[1]).isEqualTo((byte) 'K');
    }

    @Test
    void generatePdfProducesValidBytes() {
        mockUpload();
        FileArtifact artifact = service().generate(MARKDOWN, "PDF", null);

        assertThat(artifact.getFormat()).isEqualTo("pdf");
        // 未指定文件名时自动生成并带扩展名
        assertThat(artifact.getName()).matches("document-\\d+\\.pdf");

        ArgumentCaptor<byte[]> bytes = ArgumentCaptor.forClass(byte[].class);
        verify(fileClient).upload(bytes.capture(), anyString(), eq("application/pdf"));
        assertThat(new String(bytes.getValue(), 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }

    @Test
    void blankContentRejected() {
        assertThatThrownBy(() -> service().generate("  ", "docx", null))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void unsupportedFormatRejected() {
        assertThatThrownBy(() -> service().generate(MARKDOWN, "exe", null))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("exe");
    }

    @Test
    void fileNameSanitizedAgainstTraversal() {
        mockUpload();
        FileArtifact artifact = service().generate(MARKDOWN, "docx", "../..\\恶意:文件*");

        assertThat(artifact.getName()).doesNotContain("..").doesNotContain("/").doesNotContain("\\");
    }
}
