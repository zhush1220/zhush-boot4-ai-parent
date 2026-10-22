package com.zhush.ai.preview;

import com.zhush.ai.context.AiSessionContextResolver;
import com.zhush.ai.entity.AiChatMessageEntity;
import com.zhush.ai.generation.FileArtifact;
import com.zhush.ai.mapper.AiChatMessageMapper;
import com.zhush.ai.param.AgentFileResolveParam;
import com.zhush.ai.service.AgentChatOwnershipGuard;
import com.zhush.ai.vo.AnswerFileVO;
import com.zhush.agentscope.context.AgentContext;
import com.zhush.file.core.FileRecordStore;
import com.zhush.file.core.FileUploadResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.zhush.ai.enums.AiErrorCode.ANSWER_FILE_DISABLED;
import static com.zhush.ai.enums.AiErrorCode.ANSWER_FILE_NOT_FOUND;
import static com.zhush.ai.enums.AiErrorCode.ANSWER_FILE_TOO_LARGE;
import static com.zhush.common.exception.ServiceExceptionUtil.assertTrue;
import static com.zhush.common.exception.ServiceExceptionUtil.exception;

/**
 * 回答正文引用文件的按需登记：把智能体工作区里的产物变成有 {@code fileId} 的会话附件，从而能走既有预览链路。
 *
 * <p>为什么需要它：AgentScope harness 的内置文件/记忆能力会把产物直接写进本地工作区
 * （{@code .agentscope/workspace/{userId}/brand/zhush-logo.svg} 这类），回复正文只是把路径当文本写出来。
 * 这类文件不在 {@code infra_file}、没有 URL、不在 {@code attachments}，而预览门面认死 {@code fileId}
 * （方案 S6：只认 fileId 才堵得住预览侧 SSRF）。所以不能给预览接口开一条「按路径预览」的旁路，
 * 只能在此把文件读出来上传一次、拿到真正的 {@code fileId}、补写回该会话消息的 {@code attachments}，
 * 之后 {@code preview-meta}、归属校验、双路线裁决、前端渲染器全部照旧复用，一处特例都不留。
 *
 * <p>安全口径（三条都成立才放行）：
 * <ol>
 *     <li>会话归属先过 {@link AgentChatOwnershipGuard}，且只接受路径、不接受任何带协议的地址；</li>
 *     <li>引用串里的分区段与省略号前的前缀<b>一律丢弃</b>，只按<b>调用者自己的</b> userId 重建路径——harness 按
 *         {@code RuntimeContext.userId} 分目录，别人贴一个 {@code workspace/2/…} 也只会落到自己分区里找不到；</li>
 *     <li>{@code toRealPath()} 全量解析软链后必须仍在 {@code {workspace}/{userId}} 之下，
 *         且末段本身不是符号链接（只看解析后前缀会漏掉「末段是条指向系统目录的软链」这一种）。</li>
 * </ol>
 *
 * <p>按需而非抢跑：只在用户点击正文里的文件引用时登记，没人看的产物不进库、不占存储；
 * 同名引用重复点击直接复用已登记的 {@code fileId}，不产生重复文件记录。
 *
 * @author zhush
 */
@Slf4j
@Service
public class AnswerFileArtifactService {

    /**
     * 允许登记为产物的扩展名：预览默认白名单（{@code AgentFilePreviewService.DEFAULT_ALLOWED}）
     * 加上两档服务端路线才渲染得了的旧格式。登记后至少能出「预览或下载」卡片，
     * 升档（{@code office-converter} 改值）后同一份产物无需重新登记即可预览。
     */
    private static final Set<String> REGISTRABLE_EXTS = Set.of(
            "png", "jpg", "jpeg", "gif", "webp", "bmp", "svg", "ico",
            "mp4", "webm", "mov", "m4v",
            "md", "markdown", "txt", "csv", "html", "htm", "pdf",
            "doc", "docx", "xls", "xlsx", "ppt", "pptx");

    /** 图片类扩展名：登记成 image 型附件，气泡里直接出缩略图（与 generateImage 的口径一致） */
    private static final Set<String> IMAGE_EXTS = Set.of("png", "jpg", "jpeg", "gif", "webp", "bmp", "svg", "ico");

    private static final Set<String> VIDEO_EXTS = Set.of("mp4", "webm", "mov", "m4v");

    /** harness 工作区分区标记：绝对路径里出现它时，只取它之后的相对段重建 */
    private static final String WORKSPACE_MARKER = "/workspace/";

    /** 省略号 U+2026：模型省写路径前缀时用它，两个句点 {@code ..} 不在此列 */
    private static final char ELLIPSIS = '…';

    /** 引用串长度上限：正常路径不会更长，超长即视为整段贴进来的文本 */
    private static final int MAX_REFERENCE_LENGTH = 512;

    /** markdown/中文标点包裹，登记前需剥掉——正文里的引用常被写成 {@code `path`}、（path）、path、 */
    private static final String LEADING_JUNK = "`\"'([{<";
    private static final String TRAILING_JUNK = "`\"')]}>,.;:!?、。，；：！？”";

    private final PreviewProperties properties;
    private final AiChatMessageMapper messageMapper;
    private final AgentChatOwnershipGuard ownershipGuard;
    private final AiSessionContextResolver contextResolver;
    private final FileRecordStore fileRecordStore;
    private final ObjectMapper objectMapper;

    public AnswerFileArtifactService(PreviewProperties properties, AiChatMessageMapper messageMapper,
                                     AgentChatOwnershipGuard ownershipGuard,
                                     AiSessionContextResolver contextResolver,
                                     FileRecordStore fileRecordStore, ObjectMapper objectMapper) {
        this.properties = properties;
        this.messageMapper = messageMapper;
        this.ownershipGuard = ownershipGuard;
        this.contextResolver = contextResolver;
        this.fileRecordStore = fileRecordStore;
        this.objectMapper = objectMapper;
    }

    /**
     * 把正文引用登记为会话产物附件，返回前端可直接塞进附件列表的描述。
     *
     * @throws com.zhush.common.exception.ServiceException 开关关闭、会话越权、路径不可读、体积超限
     */
    public AnswerFileVO resolve(AgentFileResolveParam param) {
        assertTrue(properties.isAnswerFileEnabled(), ANSWER_FILE_DISABLED);
        // 归属断言放在解析之前：非本人会话连「这个工作区文件存不存在」都不该回答
        ownershipGuard.assertSessionOwner(param.getSessionId());
        AgentContext ctx = contextResolver.resolve();
        Long currentUserId = ctx == null ? null : ctx.getUserId();
        String reference = normalize(param.getReference());
        assertTrue(reference != null && currentUserId != null, ANSWER_FILE_NOT_FOUND, param.getReference());

        Path file = locate(reference, String.valueOf(currentUserId));
        assertTrue(file != null, ANSWER_FILE_NOT_FOUND, reference);
        long size = sizeOf(file);
        long cap = properties.getAnswerFileMaxSize();
        assertTrue(size > 0, ANSWER_FILE_NOT_FOUND, reference);
        assertTrue(cap <= 0 || size <= cap, ANSWER_FILE_TOO_LARGE, reference);

        String name = file.getFileName().toString();
        String ext = extensionOf(name);
        assertTrue(ext != null && REGISTRABLE_EXTS.contains(ext), ANSWER_FILE_NOT_FOUND, reference);

        List<AiChatMessageEntity> messages = messageMapper.selectBySessionId(param.getSessionId());
        // 幂等：同名引用在本会话登记过就复用，重复点击不重复入库
        AnswerFileVO registered = findRegistered(messages, name);
        if (registered != null) {
            return registered;
        }
        FileArtifact artifact = upload(file, name, ext, ctx, param.getSessionId());
        appendAttachment(messages, param.getSeq(), artifact);
        return toVO(artifact);
    }

    // ================ 引用串解析与定位 ================

    /**
     * 清洗正文引用文本：统一分隔符、剥包裹符、丢弃工作区绝对前缀与分区段，非法形态判死。
     *
     * <p>模型省写的路径前缀（{@code …/brand/png/x.png}）一并丢弃：省略号吞掉的正是绝对前缀，
     * 尾段仍是分区内的相对路径。
     *
     * @return 相对调用者分区的路径；不可用时 {@code null}
     */
    static String normalize(String raw) {
        if (raw == null || raw.isBlank() || raw.length() > MAX_REFERENCE_LENGTH) {
            return null;
        }
        String value = raw.trim().replace('\\', '/');
        // 带协议的一律拒绝：本接口只读本机工作区，不接受任何外部地址，否则就成了 SSRF 出口
        if (value.contains("://") || value.indexOf('\0') >= 0) {
            return null;
        }
        value = trimJunk(value);
        int marker = value.lastIndexOf(WORKSPACE_MARKER);
        if (marker >= 0) {
            value = value.substring(marker + WORKSPACE_MARKER.length());
            // 标记之后的第一段就是 harness 的 userId 分区，必须连着丢掉：locate() 会按调用者自己的 id 重建，
            // 留着它等于把分区拼两次（workspace/1/1/brand/...），既定位不到，又让引用方有权指定分区
            int partitionEnd = value.indexOf('/');
            value = partitionEnd < 0 ? "" : value.substring(partitionEnd + 1);
        }
        while (value.startsWith("./")) {
            value = value.substring(2);
        }
        value = trimJunk(dropElidedPrefix(value));
        // 绝对残留（/ 或 D:/ 开头）与父目录跳出在这里就拦掉，后面的包含校验只是第二层
        if (value.isEmpty() || value.startsWith("/") || value.indexOf(':') >= 0) {
            return null;
        }
        for (String segment : value.split("/")) {
            if (segment.isEmpty() || "..".equals(segment)) {
                return null;
            }
        }
        return value;
    }

    /**
     * 剥掉被省略号吞掉的前缀：模型为排版常把工作区之前的绝对路径省成 {@code …/brand/png/icon-16.png}
     * 或 {@code D:\zhush\…\brand\png\icon-16.png}，省略的恰好是「不该参与定位的那一段」，
     * 尾段仍是分区内的真实相对路径，取最后一个省略段之后即可还原。
     *
     * <p>只丢前缀、不猜前缀：剩余段仍要过绝对路径 / {@code ..} / {@code toRealPath} 包含校验，
     * 也绝不在工作区里按后缀搜索——那会引入歧义命中，等于给引用方一个探测目录结构的机会。
     */
    private static String dropElidedPrefix(String value) {
        String[] segments = value.split("/");
        int elided = -1;
        for (int i = 0; i < segments.length; i++) {
            if (isElision(segments[i])) {
                elided = i;
            }
        }
        if (elided < 0) {
            return value;
        }
        return String.join("/", Arrays.copyOfRange(segments, elided + 1, segments.length));
    }

    /** 省略段：整段是三个以上句点，或整段是省略号。单个 {@code .} 是当前目录、{@code ..} 是父目录跳出，都不算 */
    private static boolean isElision(String segment) {
        if (segment.isEmpty()) {
            return false;
        }
        if (segment.chars().allMatch(c -> c == '.')) {
            return segment.length() >= 3;
        }
        return segment.chars().allMatch(c -> c == ELLIPSIS);
    }

    private static String trimJunk(String value) {
        int from = 0;
        int to = value.length();
        while (from < to && LEADING_JUNK.indexOf(value.charAt(from)) >= 0) {
            from++;
        }
        while (to > from && TRAILING_JUNK.indexOf(value.charAt(to - 1)) >= 0) {
            to--;
        }
        return value.substring(from, to);
    }

    /**
     * 在调用者自己的分区内定位文件；越界、缺失、非普通文件、符号链接、点文件都返回 {@code null}。
     *
     * <p>点文件（{@code .consolidation_state} 之类）是 harness 的运行态而非产物，不该进文件管理。
     */
    Path locate(String relative, String partition) {
        Path root = workspaceRoot();
        if (root == null) {
            return null;
        }
        Path base = root.resolve(partition);
        Path found = resolveInside(base, relative);
        if (found == null && relative.startsWith(partition + "/")) {
            // 正文里也可能直接写「分区/相对路径」（不带 /workspace/ 标记，normalize 无从识别该丢哪一段）：
            // 先按分区内原样找，找不到再剥一次——且只剥调用者自己的分区段，别人的前缀一律不认
            found = resolveInside(base, relative.substring(partition.length() + 1));
        }
        return found;
    }

    /** 在给定分区目录内定位普通可读文件；越界、缺失、软链、点文件都返回 {@code null} */
    private Path resolveInside(Path base, String relative) {
        try {
            Path target = base.resolve(relative).normalize();
            if (Files.isSymbolicLink(target) || target.getFileName() == null
                    || target.getFileName().toString().startsWith(".")) {
                return null;
            }
            // toRealPath 会把路径里的软链全部解开，再比前缀才拦得住「跳出分区」；base 自身也必须存在
            Path real = target.toRealPath();
            if (!real.startsWith(base.toRealPath()) || !Files.isRegularFile(real) || !Files.isReadable(real)) {
                return null;
            }
            return real;
        } catch (IOException e) {
            log.debug("[正文文件] 定位失败, relative={}, msg={}", relative, e.getMessage());
            return null;
        }
    }

    /** 工作区根：相对值按进程工作目录解析；容器部署需显式配置绝对路径并把该目录挂载进来 */
    private Path workspaceRoot() {
        String configured = properties.getAnswerFileWorkspaceRoot();
        if (configured == null || configured.isBlank()) {
            return null;
        }
        Path root = Paths.get(configured.trim());
        if (!root.isAbsolute()) {
            root = Paths.get(System.getProperty("user.dir")).resolve(root);
        }
        try {
            return root.toRealPath();
        } catch (IOException e) {
            log.warn("[正文文件] 工作区根不可用, root={}, msg={}；容器部署需挂载 harness 工作区并配置 "
                    + "zhush.preview.answer-file-workspace-root", root, e.getMessage());
            return null;
        }
    }

    private long sizeOf(Path file) {
        try {
            return Files.size(file);
        } catch (IOException e) {
            log.warn("[正文文件] 体积读取失败, name={}, msg={}", file.getFileName(), e.getMessage());
            return -1L;
        }
    }

    // ================ 登记与回填 ================

    private FileArtifact upload(Path file, String name, String ext, AgentContext ctx, String sessionId) {
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(file);
        } catch (IOException e) {
            log.warn("[正文文件] 读取失败, sessionId={}, name={}, msg={}", sessionId, name, e.getMessage());
            throw exception(ANSWER_FILE_NOT_FOUND, name);
        }
        FileUploadResult uploaded = fileRecordStore.upload(bytes, name,
                AgentFilePreviewService.contentTypeOf(ext), ctx.getUserType(), ctx.getUserId());
        log.info("[正文文件] 登记完成, sessionId={}, name={}, fileId={}, size={}",
                sessionId, name, uploaded.getId(), bytes.length);
        return FileArtifact.builder()
                .type(typeOf(ext))
                .format(ext)
                .name(name)
                .url(uploaded.getUrl())
                .size(uploaded.getSize() == null ? (long) bytes.length : uploaded.getSize())
                .fileId(uploaded.getId())
                .build();
    }

    /**
     * 追加进消息的 {@code attachments} 列——预览门面的 {@code locate()} 只认「出现在会话产物里的 fileId」，
     * 少这一步，刚登记的 fileId 会被判越权。
     *
     * <p>定位不到目标消息或写入异常都只记日志：文件已入库、URL 可用，前端尽力本地渲染的路径仍然通，
     * 不该因为回填失败而报错（门禁原则 2）。
     */
    private void appendAttachment(List<AiChatMessageEntity> messages, Integer seq, FileArtifact artifact) {
        AiChatMessageEntity target = pickTarget(messages, seq);
        if (target == null) {
            log.warn("[正文文件] 未定位到归属消息，跳过附件回填, seq={}, name={}", seq, artifact.getName());
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(artifact);
            String existing = target.getAttachments();
            String merged = (existing == null || existing.isBlank() || "[]".equals(existing.trim()))
                    ? "[" + json + "]"
                    : existing.trim().substring(0, existing.trim().length() - 1) + "," + json + "]";
            AiChatMessageEntity patch = new AiChatMessageEntity();
            patch.setId(target.getId());
            patch.setAttachments(merged);
            messageMapper.update(patch);
        } catch (Exception e) {
            log.warn("[正文文件] 附件回填失败, messageId={}, name={}, msg={}",
                    target.getId(), artifact.getName(), e.getMessage());
        }
    }

    /** 指定 seq 时精确定位该条助手消息；缺省取会话内最后一条助手消息（前端流式态拿不到 seq） */
    private AiChatMessageEntity pickTarget(List<AiChatMessageEntity> messages, Integer seq) {
        AiChatMessageEntity last = null;
        for (AiChatMessageEntity message : messages) {
            if (!"assistant".equals(message.getRole())) {
                continue;
            }
            if (seq != null && seq.equals(message.getSeq())) {
                return message;
            }
            last = message;
        }
        return last;
    }

    /** 本会话是否已登记过同名产物（凭消息附件 JSON 判断，命中即复用其 fileId 与 URL） */
    private AnswerFileVO findRegistered(List<AiChatMessageEntity> messages, String name) {
        for (AiChatMessageEntity message : messages) {
            String json = message.getAttachments();
            if (json == null || json.isBlank()) {
                continue;
            }
            try {
                JsonNode root = objectMapper.readTree(json);
                if (!root.isArray()) {
                    continue;
                }
                for (JsonNode node : root) {
                    if (name.equals(node.path("name").asString())) {
                        return toVO(node, name);
                    }
                }
            } catch (RuntimeException e) {
                log.debug("[正文文件] 附件 JSON 解析失败, sessionId={}, msg={}",
                        message.getSessionId(), e.getMessage());
            }
        }
        return null;
    }

    private AnswerFileVO toVO(JsonNode node, String name) {
        AnswerFileVO vo = new AnswerFileVO();
        vo.setFileId(node.path("fileId").asString());
        vo.setType(node.path("type").asString());
        vo.setFormat(node.path("format").asString());
        vo.setName(name);
        vo.setUrl(node.path("url").asString());
        vo.setSize(node.path("size").isNumber() ? node.path("size").asLong() : null);
        return vo;
    }

    private AnswerFileVO toVO(FileArtifact artifact) {
        AnswerFileVO vo = new AnswerFileVO();
        // fileId 走全局 Long→String 序列化口径，前端凭这个字符串再拼 preview-meta 路径
        vo.setFileId(artifact.getFileId() == null ? null : String.valueOf(artifact.getFileId()));
        vo.setType(artifact.getType());
        vo.setFormat(artifact.getFormat());
        vo.setName(artifact.getName());
        vo.setUrl(artifact.getUrl());
        vo.setSize(artifact.getSize());
        return vo;
    }

    /** 附件类型语义与 {@code FileArtifact.type} 对齐：image 在气泡里内联出图，video 走播放器，其余是文件卡片 */
    private String typeOf(String ext) {
        if (IMAGE_EXTS.contains(ext)) {
            return "image";
        }
        return VIDEO_EXTS.contains(ext) ? "video" : "file";
    }

    private String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 || dot == name.length() - 1 ? null : name.substring(dot + 1).toLowerCase();
    }
}
