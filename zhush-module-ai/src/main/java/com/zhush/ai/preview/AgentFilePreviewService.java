package com.zhush.ai.preview;

import com.mybatisflex.core.query.QueryWrapper;
import com.zhush.ai.context.AiSessionContextResolver;
import com.zhush.ai.entity.AiChatMessageEntity;
import com.zhush.ai.entity.AiGenerationTaskEntity;
import com.zhush.ai.generation.RemoteFileFetcher;
import com.zhush.ai.mapper.AiChatMessageMapper;
import com.zhush.ai.mapper.AiGenerationTaskMapper;
import com.zhush.ai.service.AgentChatOwnershipGuard;
import com.zhush.ai.vo.FilePreviewMetaVO;
import com.zhush.agentscope.context.AgentContext;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static com.zhush.ai.enums.AiErrorCode.FILE_CONVERT_FAILED;
import static com.zhush.ai.enums.AiErrorCode.FILE_PREVIEW_DENIED;
import static com.zhush.common.exception.ServiceExceptionUtil.assertTrue;

/**
 * 会话产物预览门面：按 fileId 反查产物、断言归属、裁决 {@link PreviewMode}，并按需触发服务端转换。
 *
 * <p>只读 AI 自有表（{@code ai_chat_message.attachments} / {@code ai_generation_task}），
 * 不跨模块读 {@code infra_file}，保持 module-ai 不依赖 module-system 的分层口径。
 *
 * <p>归属判定（方案 S5）：fileId 必须出现在当前登录人可访问的会话产物里才放行——
 * {@code FileService.checkOwnership} 只约束会员端，管理端没有逐文件归属校验，故此处不可省略。
 *
 * <p>双路线（方案 §0.1）：{@code zhush.preview.office-converter=none} 为纯前端基线；
 * {@code libreoffice}（甲-1）时旧格式转 PDF 走 {@code server-pdf}；{@code kkfileview}（甲-2）时
 * 由外部服务渲染、走 {@code server-iframe}，本系统不再缓存产物、只生成渲染地址。
 * 裁决权在这里，前端只认 {@code previewMode}，因此升档只是改配置重启后端。
 * 转换或渲染任一环节失败都回落 {@code unsupported}，<b>不抛异常、不阻断对话</b>。
 *
 * @author zhush
 */
@Slf4j
@Service
public class AgentFilePreviewService {

    /** 旧版二进制格式：客户端渲染无解，只能由路线甲的转换器转 PDF */
    private static final Set<String> LEGACY_EXTS = Set.of("doc", "xls", "ppt");

    /** 只有服务端才拿得动的格式（LibreOffice 覆盖面比前端渲染器宽） */
    private static final Set<String> SERVER_ONLY_EXTS = Set.of("pptx", "odt", "ods", "odp", "rtf");

    /** 未配置 zhush.preview.allowed-extensions 时的默认可预览白名单（方案 §4.2 路线乙覆盖的九类） */
    private static final Set<String> DEFAULT_ALLOWED = Set.of(
            "png", "jpg", "jpeg", "gif", "webp", "bmp",
            "mp4", "webm", "mov", "m4v",
            "md", "markdown", "txt", "html", "htm",
            "pdf", "docx", "xlsx", "csv");

    /** 扩展名 → MIME（前端按 previewMode 分派，本字段仅用于展示与兜底） */
    private static final Map<String, String> CONTENT_TYPES = Map.ofEntries(
            Map.entry("png", "image/png"), Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"), Map.entry("gif", "image/gif"),
            Map.entry("webp", "image/webp"), Map.entry("bmp", "image/bmp"),
            Map.entry("mp4", "video/mp4"), Map.entry("webm", "video/webm"),
            Map.entry("mov", "video/quicktime"), Map.entry("m4v", "video/mp4"),
            Map.entry("md", "text/markdown"), Map.entry("markdown", "text/markdown"),
            Map.entry("txt", "text/plain"), Map.entry("csv", "text/csv"),
            Map.entry("html", "text/html"), Map.entry("htm", "text/html"),
            Map.entry("pdf", "application/pdf"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("ppt", "application/vnd.ms-powerpoint"),
            Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"));

    private final AiChatMessageMapper messageMapper;
    private final AiGenerationTaskMapper taskMapper;
    private final AgentChatOwnershipGuard ownershipGuard;
    private final AiSessionContextResolver contextResolver;
    private final PreviewProperties properties;
    private final ObjectMapper objectMapper;
    private final RemoteFileFetcher fileFetcher;
    private final PreviewCacheStore cacheStore;
    private final ObjectProvider<OfficeFileConverter> converterProvider;
    private final ObjectProvider<RemotePreviewRenderer> rendererProvider;

    public AgentFilePreviewService(AiChatMessageMapper messageMapper, AiGenerationTaskMapper taskMapper,
                                   AgentChatOwnershipGuard ownershipGuard, AiSessionContextResolver contextResolver,
                                   PreviewProperties properties, ObjectMapper objectMapper,
                                   RemoteFileFetcher fileFetcher, PreviewCacheStore cacheStore,
                                   ObjectProvider<OfficeFileConverter> converterProvider,
                                   ObjectProvider<RemotePreviewRenderer> rendererProvider) {
        this.messageMapper = messageMapper;
        this.taskMapper = taskMapper;
        this.ownershipGuard = ownershipGuard;
        this.contextResolver = contextResolver;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.fileFetcher = fileFetcher;
        this.cacheStore = cacheStore;
        this.converterProvider = converterProvider;
        this.rendererProvider = rendererProvider;
    }

    /** 甲-1 与甲-2 由 {@code office-converter} 单值保证互斥，装配不到实现时按 none 跑并说明代价 */
    @PostConstruct
    void checkConverter() {
        String configured = properties.getOfficeConverter();
        if (configured == null || "none".equalsIgnoreCase(configured.trim())) {
            return;
        }
        OfficeFileConverter converter = converterProvider.getIfAvailable();
        if (converter != null) {
            log.info("[预览] office-converter={} 已装配，doc/xls/ppt 等旧格式转 PDF 后按 server-pdf 预览", converter.name());
            return;
        }
        RemotePreviewRenderer renderer = rendererProvider.getIfAvailable();
        if (renderer != null) {
            log.info("[预览] office-converter={} 已装配，旧格式与 ofd/压缩包等广度格式按 server-iframe 预览", renderer.name());
            return;
        }
        log.warn("[预览] office-converter={} 无对应实现，按 none 运行，旧格式只提供下载", configured);
    }

    /**
     * 取产物预览元信息。
     *
     * @param fileId    文件记录主键（{@code infra_file.id}）
     * @param sessionId 当前会话 ID，可为空；为空时按 fileId 反查所有引用它的会话并逐个核验归属
     * @throws com.zhush.common.exception.ServiceException fileId 不属于当前登录人可访问的会话产物
     */
    public FilePreviewMetaVO previewMeta(Long fileId, String sessionId) {
        return build(locate(fileId, sessionId), fileId);
    }

    // ================ 归属与取数 ================

    /**
     * 定位产物引用并断言归属：先查对话消息附件，未命中再回退生成任务记录。
     */
    private PreviewSource locate(Long fileId, String sessionId) {
        String targetId = String.valueOf(fileId);
        if (sessionId != null && !sessionId.isBlank()) {
            ownershipGuard.assertSessionOwner(sessionId);
            PreviewSource fromSession = findBySession(sessionId, targetId);
            return fromSession != null ? fromSession : fromTask(fileId, sessionId);
        }
        for (AiChatMessageEntity message : messageMapper.selectByAttachmentFileId(fileId)) {
            PreviewSource matched = matchAttachment(message, targetId);
            if (matched == null) {
                continue;
            }
            // 粗筛可能命中他人会话：归属不成立就继续核验下一个引用，全部不成立即拒绝
            if (isSessionOwner(matched.sessionId())) {
                return matched;
            }
        }
        return fromTask(fileId, null);
    }

    /** 会话归属：管理端放行（口径同 AgentChatOwnershipGuard），其余须本人会话 */
    private boolean isSessionOwner(String sessionId) {
        try {
            ownershipGuard.assertSessionOwner(sessionId);
            return true;
        } catch (RuntimeException e) {
            log.debug("[预览] 会话 {} 非当前登录人所有，跳过该引用", sessionId);
            return false;
        }
    }

    private PreviewSource findBySession(String sessionId, String targetId) {
        for (AiChatMessageEntity message : messageMapper.selectBySessionId(sessionId)) {
            PreviewSource matched = matchAttachment(message, targetId);
            if (matched != null) {
                return matched;
            }
        }
        return null;
    }

    /**
     * 解析消息 attachments JSON 精确匹配 fileId。
     *
     * <p>全局 Long→String 序列化使 {@code fileId} 落库为 JSON 字符串，故按文本比较，
     * 不用 {@code asLong}（历史数据两种形态都可能出现）。
     */
    private PreviewSource matchAttachment(AiChatMessageEntity message, String targetId) {
        String json = message.getAttachments();
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isArray()) {
                return null;
            }
            for (JsonNode node : root) {
                if (!targetId.equals(node.path("fileId").asString())) {
                    continue;
                }
                return new PreviewSource(node.path("type").asString(), node.path("name").asString(),
                        node.path("format").asString(), node.path("url").asString(),
                        node.path("size").asLong(), message.getSessionId());
            }
        } catch (RuntimeException e) {
            log.warn("[预览] 消息附件 JSON 解析失败, sessionId={}, seq={}", message.getSessionId(), message.getSeq(), e);
        }
        return null;
    }

    /**
     * 回退到生成任务记录（产物已转存但消息未落附件的场景）。
     *
     * @param sessionId 调用方给定的会话，非空时要求任务确实属于该会话
     */
    private PreviewSource fromTask(Long fileId, String sessionId) {
        AiGenerationTaskEntity task = taskMapper.selectOneByQuery(
                QueryWrapper.create().where(AiGenerationTaskEntity::getFileId).eq(fileId));
        boolean matched = task != null && (sessionId == null || sessionId.isBlank()
                ? isTaskOwner(task) : sessionId.equals(task.getSessionId()));
        assertTrue(matched, FILE_PREVIEW_DENIED, fileId);
        String format = "video".equals(task.getTaskType()) ? "mp4" : "png";
        return new PreviewSource(task.getTaskType(), task.getTaskId(), format, task.getResultUrl(),
                task.getFileSize() == null ? 0L : task.getFileSize(), task.getSessionId());
    }

    /** 任务归属：管理端放行，其余须 userId + userType 双维匹配（防 admin/member 自增 ID 串号） */
    private boolean isTaskOwner(AiGenerationTaskEntity task) {
        AgentContext ctx = contextResolver.resolve();
        if (ctx == null) {
            return false;
        }
        if ("admin".equals(ctx.getUserType())) {
            return true;
        }
        return ctx.getUserId() != null && ctx.getUserId().equals(task.getUserId())
                && task.getUserType() != null && task.getUserType().equals(ctx.getUserType());
    }

    // ================ 模式裁决 ================

    private FilePreviewMetaVO build(PreviewSource source, Long fileId) {
        String ext = resolveExt(source);
        FilePreviewMetaVO vo = new FilePreviewMetaVO();
        vo.setId(fileId);
        vo.setName(source.name());
        vo.setExt(ext);
        vo.setSize(source.size() == null || source.size() <= 0 ? null : source.size());
        vo.setContentType(ext == null ? "application/octet-stream"
                : CONTENT_TYPES.getOrDefault(ext, "application/octet-stream"));
        vo.setAccessUrl(source.url());

        if (!properties.isEnabled()) {
            return markUnsupported(vo, "预览能力已关闭，请下载查看");
        }
        if (ext == null) {
            return markUnsupported(vo, "无法识别文件格式，请下载查看");
        }
        // SVG 可内嵌脚本且与后台同源，两档路线一律不内联（方案 S1）
        if ("svg".equals(ext)) {
            return markUnsupported(vo, "出于安全考虑，该格式不提供在线预览");
        }
        Set<String> allowed = properties.getAllowedExtensions() == null
                ? DEFAULT_ALLOWED : properties.getAllowedExtensions();
        // 转换器/渲染器自身的 supports() 是白名单：即使运维把 html/svg 写进 prefer-pdf-extensions 也不会交给服务端（S1）
        boolean viaConvert = serverPdf(ext) != null;
        boolean viaRender = serverIframe(ext) != null;
        if (!allowed.contains(ext) && !viaConvert && !viaRender) {
            return markUnsupported(vo, legacyHint(ext));
        }
        PreviewMode mode = switch (ext) {
            // 图片/视频/文本/HTML/PDF 两档甲都不接管：前端零成本，交给服务端只会多一次回源与排队（§3.3）
            case "png", "jpg", "jpeg", "gif", "webp", "bmp" -> PreviewMode.INLINE_IMAGE;
            case "mp4", "webm", "mov", "m4v" -> PreviewMode.INLINE_VIDEO;
            case "md", "markdown", "txt" -> PreviewMode.INLINE_TEXT;
            case "html", "htm" -> PreviewMode.IFRAME_SANDBOX;
            case "pdf" -> PreviewMode.CLIENT_PDF;
            case "docx" -> viaConvert ? PreviewMode.SERVER_PDF : viaRender ? PreviewMode.SERVER_IFRAME : PreviewMode.CLIENT_DOCX;
            case "xlsx" -> viaConvert ? PreviewMode.SERVER_PDF : viaRender ? PreviewMode.SERVER_IFRAME : PreviewMode.CLIENT_SHEET;
            case "csv" -> PreviewMode.CLIENT_SHEET;
            default -> viaConvert ? PreviewMode.SERVER_PDF : viaRender ? PreviewMode.SERVER_IFRAME : PreviewMode.UNSUPPORTED;
        };
        if (mode == PreviewMode.UNSUPPORTED) {
            return markUnsupported(vo, legacyHint(ext));
        }
        if (mode == PreviewMode.SERVER_PDF && !prepareConvertedPdf(fileId, source)) {
            // 已在 prepareConvertedPdf 内记日志，这里只给前端一句可展示的降级文案
            return markUnsupported(vo, legacyHint(ext));
        }
        if (mode == PreviewMode.SERVER_IFRAME && !prepareIframeUrl(vo, source, ext)) {
            return markUnsupported(vo, legacyHint(ext));
        }
        if (mode == PreviewMode.SERVER_PDF) {
            // accessUrl 改指鉴权下发接口：缓存目录刻意放在免鉴权的 /api/files/** 之外
            vo.setAccessUrl(contentUrl(fileId, source.sessionId()));
            vo.setContentType("application/pdf");
        }
        vo.setPreviewable(true);
        vo.setPreviewMode(mode.getValue());
        return vo;
    }

    /**
     * 判定该扩展名是否应由服务端转 PDF，返回命中的转换器；不需要/不能转时返回 null。
     *
     * <p>docx/xlsx 前端本就能渲染，只有显式列进 {@code prefer-pdf-extensions}（V3 保真度不达标时的逃生口）
     * 才改走转换，避免给一份本可本地渲染的文件凭空加一次排队与转换耗时。
     */
    private OfficeFileConverter serverPdf(String ext) {
        if (ext == null) {
            return null;
        }
        OfficeFileConverter converter = converterProvider.getIfAvailable();
        if (converter == null || !converter.available() || !converter.supports(ext)) {
            return null;
        }
        return LEGACY_EXTS.contains(ext) || SERVER_ONLY_EXTS.contains(ext) || optedIntoServer(ext) ? converter : null;
    }

    /**
     * 判定该扩展名是否应交给外部渲染服务（甲-2），返回命中的渲染器；不需要/不能渲染时返回 null。
     *
     * <p>只接「前端渲染不了」的格式与运维显式偏好的格式，理由同 {@link #serverPdf}；
     * 「前端渲染不了」这张表由渲染器自己声明（ofd/压缩包等广度格式随路线变，不该写死在门面里）。
     * 渲染器不可达时返回 null，于是这些格式回到 {@code unsupported} + 下载，而不是给前端一个必然报错的 iframe。
     */
    private RemotePreviewRenderer serverIframe(String ext) {
        if (ext == null) {
            return null;
        }
        RemotePreviewRenderer renderer = rendererProvider.getIfAvailable();
        if (renderer == null || !renderer.available() || !renderer.supports(ext)) {
            return null;
        }
        return renderer.serverOnly(ext) || optedIntoServer(ext) ? renderer : null;
    }

    /** 运维显式要求「本可本地渲染的格式也改走服务端」——甲-1 转 PDF、甲-2 交外部服务，共用这一个偏好键 */
    private boolean optedIntoServer(String ext) {
        Set<String> preferred = properties.getPreferPdfExtensions();
        return preferred != null && preferred.contains(ext);
    }

    /**
     * 生成外部渲染地址写入 {@code accessUrl}。失败即返回 false 由调用方降级，绝不抛异常（门禁原则 2）。
     */
    private boolean prepareIframeUrl(FilePreviewMetaVO vo, PreviewSource source, String ext) {
        RemotePreviewRenderer renderer = serverIframe(ext);
        if (renderer == null) {
            return false;
        }
        try {
            vo.setAccessUrl(renderer.renderUrl(new RemotePreviewRenderer.RenderTarget(ext, source.name(), source.url())));
        } catch (PreviewConversionException e) {
            log.warn("[预览] 外部渲染地址生成失败, fileId={}, kind={}, msg={}", vo.getId(), e.getKind(), e.getMessage());
            return false;
        }
        // accessUrl 指向的是渲染页，与原始产物格式无关；下载仍走前端手里的 attachment.url
        vo.setContentType("text/html");
        return true;
    }

    /**
     * 确保转换产物就绪。失败即返回 false 由调用方降级，绝不抛异常（门禁原则 2）。
     */
    private boolean prepareConvertedPdf(Long fileId, PreviewSource source) {
        try {
            return doConvert(fileId, source) != null;
        } catch (PreviewConversionException e) {
            log.warn("[预览] 服务端转换失败, fileId={}, kind={}, msg={}", fileId, e.getKind(), e.getMessage());
            return false;
        } catch (IOException e) {
            log.warn("[预览] 转换产物读写失败, fileId={}", fileId, e);
            return false;
        }
    }

    /**
     * 取（必要时生成）转换后的 PDF 路径。
     *
     * @return 缓存命中或新写入的 PDF；{@code source.url()} 为空时返回 null
     */
    private Path doConvert(Long fileId, PreviewSource source) throws IOException {
        Path cached = cacheStore.findUsable(fileId);
        if (cached != null) {
            return cached;
        }
        if (source.url() == null || source.url().isBlank()) {
            return null;
        }
        OfficeFileConverter converter = serverPdf(resolveExt(source));
        if (converter == null) {
            throw new PreviewConversionException(PreviewConversionException.Kind.UNAVAILABLE, "转换器未就绪");
        }
        long start = System.currentTimeMillis();
        byte[] pdf = converter.convertToPdf(resolveExt(source), fileFetcher.fetchFromStorage(source.url()));
        Path written = cacheStore.write(fileId, pdf);
        log.info("[预览] fileId={} 经 {} 转出 PDF {} 字节，耗时 {}ms",
                fileId, converter.name(), pdf.length, System.currentTimeMillis() - start);
        return written;
    }

    // ================ 转换产物下发 ================

    /**
     * 读取（必要时先转换）服务端渲染产物，供 {@code preview-content} 接口下发。
     *
     * <p>与 {@code preview-meta} 同一套归属断言：直接拼 URL 访问也必须是自己的会话产物。
     *
     * @return PDF 文件与其展示名
     */
    public ConvertedPdf convertedPdf(Long fileId, String sessionId) {
        PreviewSource source = locate(fileId, sessionId);
        Path pdf = null;
        try {
            pdf = doConvert(fileId, source);
        } catch (PreviewConversionException | IOException e) {
            log.warn("[预览] 产物下发前转换失败, fileId={}, msg={}", fileId, e.getMessage());
        }
        assertTrue(pdf != null, FILE_CONVERT_FAILED, fileId);
        return new ConvertedPdf(pdf, pdfName(source.name()));
    }

    /** 下发地址：前端拿到的是站内相对路径，由 requestClient 带 token 请求 */
    private String contentUrl(Long fileId, String sessionId) {
        String base = "/admin/ai/agent/file/" + fileId + "/preview-content";
        return sessionId == null || sessionId.isBlank()
                ? base
                : base + "?sessionId=" + URLEncoder.encode(sessionId, StandardCharsets.UTF_8);
    }

    /** 转换产物按原文件名换后缀，原名字缺失时退回 fileId */
    private String pdfName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "preview.pdf";
        }
        int dot = originalName.lastIndexOf('.');
        String stem = dot <= 0 ? originalName : originalName.substring(0, dot);
        return stem + ".pdf";
    }

    /** 扩展名优先取产物 format，缺失时退回文件名后缀（历史数据 format 可能为空） */
    private String resolveExt(PreviewSource source) {
        if (source.format() != null && !source.format().isBlank()) {
            return source.format().toLowerCase();
        }
        String name = source.name();
        if (name == null) {
            return null;
        }
        int dot = name.lastIndexOf('.');
        return dot < 0 || dot == name.length() - 1 ? null : name.substring(dot + 1).toLowerCase();
    }

    /** 不可预览文案：旧格式单独给一句「用 Office 打开」，避免与「格式不支持」混淆 */
    private String legacyHint(String ext) {
        return LEGACY_EXTS.contains(ext) || SERVER_ONLY_EXTS.contains(ext)
                ? "旧版格式（." + ext + "）需下载后用 Office/WPS 打开"
                : "该格式不支持在线预览，请下载查看";
    }

    private FilePreviewMetaVO markUnsupported(FilePreviewMetaVO vo, String reason) {
        vo.setPreviewable(false);
        vo.setPreviewMode(PreviewMode.UNSUPPORTED.getValue());
        vo.setReason(reason);
        return vo;
    }

    /**
     * 产物引用：来源可能是消息附件 JSON，也可能是生成任务记录。
     */
    private record PreviewSource(String type, String name, String format, String url, Long size, String sessionId) {
    }

    /**
     * 服务端转换产物：文件 + 展示名。
     */
    public record ConvertedPdf(Path file, String name) {
    }

}
