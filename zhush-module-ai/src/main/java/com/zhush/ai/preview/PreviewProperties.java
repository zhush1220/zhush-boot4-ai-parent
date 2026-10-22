package com.zhush.ai.preview;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 文件预览配置（前缀 {@code zhush.preview}）。
 *
 * <p>白名单与上传侧的 {@code zhush.file.allowed-extensions} 是两个独立口径：
 * 前者决定「能不能在线预览」，后者决定「能不能存进系统」。
 *
 * <p>路线甲由 {@code office-converter} 单值切换：{@code none} 为纯前端基线，
 * {@code libreoffice}（甲-1）用内嵌 soffice 转 PDF 后走 {@code server-pdf}，
 * {@code kkfileview}（甲-2）由外部服务渲染并走 {@code server-iframe}。改这一个值即完成升降档，
 * 前端与接口契约不变；两个实现的条件装配互斥，任何时刻只存在一个。
 *
 * <p>甲-2 的三个键保持扁平（{@code zhush.preview.base-url} / {@code source-base-url} / {@code probe-url}），
 * 与《kkFileView在线预览整合方案.md》§4.1 一致，便于那份蓝本照着改配置。
 *
 * @author zhush
 */
@Data
@Component
@ConfigurationProperties(prefix = "zhush.preview")
public class PreviewProperties {

    private static final String DEFAULT_BASE_URL = "/preview";

    /** 预览能力总开关，关闭后所有产物回落「仅下载」 */
    private boolean enabled = true;

    /** 旧格式处理路线：none（路线乙，纯前端）/ libreoffice（甲-1）/ kkfileview（甲-2） */
    private String officeConverter = "none";

    /** 允许在线预览的扩展名（小写、不含点号）；为空表示用内置默认表 */
    private Set<String> allowedExtensions;

    /**
     * 前端本就能渲染、但显式要求改走服务端的格式（保真度不达标时的逃生口）。
     *
     * <p>甲-1 下转成 PDF，甲-2 下交给外部渲染服务，两档共用这一个键——它是「偏好服务端」的开关，
     * 不特指 PDF。只有服务端才渲染得了的格式（{@code doc/xls/ppt/ofd/…}）不需要列在这里。
     */
    private Set<String> preferPdfExtensions;

    /** 服务端转换参数（甲-1，{@code office-converter=libreoffice} 时生效） */
    private Convert convert = new Convert();

    // ================ 甲-2：外部渲染服务（kkFileView） ================

    /** 对外预览前缀：站内相对路径（同源经 nginx {@code /preview}）或独立源的绝对地址 */
    private String baseUrl = DEFAULT_BASE_URL;

    /**
     * 源文件绝对前缀。docker 的 {@code zhush.file.local.domain=/api/files/} 是相对前缀，
     * 外部容器无法解析，必须靠它重建绝对地址；dev 的产物地址本就是绝对地址时留空即可。
     */
    private String sourceBaseUrl;

    /**
     * 可达性探测地址（绝对，如 {@code http://172.17.0.1:8012/}）。
     *
     * <p>配了才探测，探测不通即回落 {@code unsupported}（门禁原则 2）；留空则后端无法自检——
     * {@code base-url} 写成同源相对前缀时，本进程并不知道浏览器解析出来的那个源，只能把不可达留给前端发现。
     */
    private String probeUrl;

    /**
     * yaml 里只写 {@code convert:} 而把子项全注释掉时，binder 会把该属性绑成 null，
     * 而转换器初始化就依赖它——宁可回默认值，也不要让应用起不来。
     */
    public Convert getConvert() {
        if (convert == null) {
            convert = new Convert();
        }
        return convert;
    }

    /** yaml 里写了 {@code base-url:} 空值时也要有前缀可用，否则拼出来的预览地址是残缺的 */
    public String getBaseUrl() {
        return baseUrl == null || baseUrl.isBlank() ? DEFAULT_BASE_URL : baseUrl.trim();
    }

    @Data
    public static class Convert {

        /** soffice 可执行文件；Linux 通常为 libreoffice 或 soffice，Windows 需写完整路径 */
        private String sofficePath = "soffice";

        /** 单文档转换超时（秒），超时即杀进程并降级 */
        private int timeoutSeconds = 60;

        /** office 进程上限，防一份大 PPT 拖垮 zhush-server（方案 §0.1 门禁原则 4） */
        private int maxConcurrent = 1;

        /** 排队上限（不含正在转换的），超出立即降级为「转换繁忙」 */
        private int queueLimit = 20;

        /** 单个请求最长排队时间（秒），超出降级 */
        private int queueTimeoutSeconds = 30;

        /** 转换产物缓存目录；为空则取本地存储根的同级 preview-cache 目录 */
        private String cachePath;

        /** 缓存有效期（小时），超期重新转换；文件本身按 fileId 唯一，不存在同名覆盖 */
        private int cacheTtlHours = 168;

        /** 参与转换的源文件体积上限（字节），默认与 zhush.file.max-size 同为 50MB */
        private long maxSourceBytes = 52_428_800L;
    }

}
