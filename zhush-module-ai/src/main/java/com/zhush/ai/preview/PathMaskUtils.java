package com.zhush.ai.preview;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 回答正文的本机绝对路径打码：把 {@code D:\...\brand\png\icon-16.png} 收成
 * {@code …/brand/png/icon-16.png}，只留「用户认得出文件、服务器目录结构不外露」的那一截。
 *
 * <p><b>为什么要打码</b>：harness 内置文件工具（{@code io.agentscope.core.tool.file.WriteFileTool}）
 * 走 {@code toAbsolutePath()} 并把绝对路径写进工具结果文本，模型照抄进正文，
 * 于是驱动器号、部署目录、运行账号都进了回答与历史表。第三方库的返回值改不了，只能在出口上收。
 *
 * <p><b>打码不能打断预览</b>：工作区路径保留分区段之后的<b>完整</b>相对尾段（{@code brand/png/x.png}），
 * 前端 {@code FILE_REF_PATTERN} 仍把它认成可点击引用，后端
 * {@link AnswerFileArtifactService#normalize(String)} 的省略号规则又能把它还原成分区内相对路径。
 * 只留末两级的兜底形态（非本机的绝对路径）本就定位不到，不存在回退损失。
 *
 * <p>口径与前端 {@code views/ai/assistant/modules/preview/body-files.ts} 的 {@code maskLocalPaths} 对齐，
 * 两边各自实现：服务端知道部署根目录所以能精确命中，浏览器不知道，只能按形态兜底。
 *
 * @author zhush
 */
public final class PathMaskUtils {

    /** 省略号 U+2026，与 {@link AnswerFileArtifactService} 的省写路径口径同一个字符 */
    private static final String ELLIPSIS = "…";

    /** 路径分隔符断点：与前端识别正文文件引用的字符集一致，另加中文句读，避免把整句话吞进 token */
    private static final String SEGMENT_CHAR = "[^\\s<>\"'`|(),;：，。、；！？]";

    /** 路径形态 token：至少一个分隔符，允许 {@code …} 这类省写前缀整体入串 */
    private static final Pattern PATH_TOKEN = Pattern.compile(
            "(?:[A-Za-z]:)?" + SEGMENT_CHAR + "*(?:[/\\\\]" + SEGMENT_CHAR + "*)+");

    /** 盘符段：{@code D:} */
    private static final Pattern DRIVE_SEGMENT = Pattern.compile("[A-Za-z]:");

    /** harness 工作区目录名（隐藏目录写成 {@code .agentscope}，被省略号吃掉前导点后是 {@code agentscope}） */
    private static final String AGENTSCOPE = "agentscope";

    /** 工作区根目录名，其后第一段是 userId 分区 */
    private static final String WORKSPACE = "workspace";

    /** 非本机根路径的兜底：只保留末两段，够认文件 */
    private static final int TAIL_SEGMENTS = 2;

    /** 默认参与匹配的服务器根：进程工作目录与临时目录（容器里即 /tmp、/data/... 之类） */
    private static final List<String> DEFAULT_ROOTS = defaultRoots();

    private PathMaskUtils() {
    }

    /**
     * 用服务器自身的根目录打码。
     */
    public static String mask(String text) {
        return mask(text, DEFAULT_ROOTS);
    }

    /**
     * 打码正文里所有指向本机文件系统的绝对路径，其余文本原样保留。
     *
     * <p>相对路径（{@code brand/png/x.png}）、URL、域名与不属于这些根的 POSIX 路径都不动：
     * 讲技术时写的 {@code /etc/passwd} 是真内容，不是泄漏。
     *
     * @param roots 需要抹掉的服务器根目录，命中即保留根之后的完整尾段
     */
    public static String mask(String text, Collection<String> roots) {
        if (text == null || text.isEmpty() || (text.indexOf('/') < 0 && text.indexOf('\\') < 0)) {
            return text;
        }
        Matcher matcher = PATH_TOKEN.matcher(text);
        StringBuilder out = null;
        int cursor = 0;
        while (matcher.find()) {
            String masked = maskToken(matcher.group(), roots);
            if (masked == null) {
                continue;
            }
            if (out == null) {
                out = new StringBuilder(text.length());
            }
            out.append(text, cursor, matcher.start()).append(masked);
            cursor = matcher.end();
        }
        if (out == null) {
            return text;
        }
        return out.append(text.substring(cursor)).toString();
    }

    /**
     * @return 打码后的文本；无需打码时返回 {@code null}（区分于「打码结果恰好等于原串」）
     */
    private static String maskToken(String token, Collection<String> roots) {
        // 句末点号会被字符类当成路径的一部分，剥下来等处理完再贴回去
        String body = token;
        String trailing = "";
        while (body.endsWith(".")) {
            body = body.substring(0, body.length() - 1);
            trailing = trailing + ".";
        }
        boolean directory = body.endsWith("/") || body.endsWith("\\");
        // Windows 形态必须在归一化之前判：归一后 UNC 与「协议相对 URL」都长成 // 开头，只有反斜杠分得开
        boolean windowsAbsolute = body.matches("(?i)^[A-Za-z]:[/\\\\].*") || body.startsWith("\\\\");
        String value = body.replace('\\', '/');
        if (value.contains("://")) {
            return null;
        }
        List<String> segments = segments(value);
        List<String> tail = tailOf(value, segments, roots, windowsAbsolute);
        if (tail == null) {
            return null;
        }
        return ELLIPSIS + "/" + String.join("/", tail) + (directory && !tail.isEmpty() ? "/" : "") + trailing;
    }

    /**
     * 该保留哪一截：工作区标记 &gt; 已知根 &gt; 末两段兜底；{@code null} 表示不打码。
     */
    private static List<String> tailOf(String value, List<String> segments, Collection<String> roots,
                                       boolean windowsAbsolute) {
        int marker = workspaceMarkerIndex(segments);
        if (marker >= 0) {
            // 标记之后第一段是 userId 分区，跟着前缀一起丢——定位时按调用者自己的分区重建
            return slice(segments, marker + 3);
        }
        for (String root : roots) {
            String prefix = normalizeRoot(root);
            if (!prefix.isEmpty() && value.regionMatches(true, 0, prefix + "/", 0, prefix.length() + 1)) {
                return List.of(value.substring(prefix.length() + 1).split("/"));
            }
        }
        if (windowsAbsolute) {
            // 认不出归属的 Windows 路径（别人的机器、共享目录）：只留末两级，仍然不给驱动器号与目录层级。
            // POSIX 绝对路径不在此列——/etc/passwd 与 /files/2026/a.png 都是真内容不是泄漏，误伤一条回答比漏一条更糟。
            return slice(segments, Math.max(0, segments.size() - TAIL_SEGMENTS));
        }
        return null;
    }

    /** {@code agentscope} 紧跟 {@code workspace} 的位置，即 harness 工作区；找不到返回 -1 */
    private static int workspaceMarkerIndex(List<String> segments) {
        for (int i = 0; i + 1 < segments.size(); i++) {
            if (isAgentscope(segments.get(i)) && WORKSPACE.equalsIgnoreCase(segments.get(i + 1))) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isAgentscope(String segment) {
        String cleaned = segment.replace(".", "").replace(ELLIPSIS, "");
        return AGENTSCOPE.equalsIgnoreCase(cleaned);
    }

    /** 拆段：丢空段与盘符段，POSIX/UNC/盘符三种前缀的差异在此抹平 */
    private static List<String> segments(String value) {
        List<String> segments = new ArrayList<>();
        for (String segment : value.split("/")) {
            if (!segment.isEmpty() && !DRIVE_SEGMENT.matcher(segment).matches()) {
                segments.add(segment);
            }
        }
        return segments;
    }

    private static List<String> slice(List<String> segments, int from) {
        return from >= segments.size() ? List.of() : List.copyOf(segments.subList(from, segments.size()));
    }

    private static String normalizeRoot(String root) {
        String value = root == null ? "" : root.replace('\\', '/');
        while (value.length() > 1 && (value.endsWith("/") || value.endsWith("\\"))) {
            value = value.substring(0, value.length() - 1);
        }
        return value.endsWith("/") ? "" : value;
    }

    private static List<String> defaultRoots() {
        List<String> roots = new ArrayList<>(2);
        addIfPresent(roots, System.getProperty("user.dir"));
        addIfPresent(roots, System.getProperty("java.io.tmpdir"));
        return List.copyOf(roots);
    }

    private static void addIfPresent(List<String> roots, String value) {
        if (value != null && !value.isBlank()) {
            roots.add(value);
        }
    }
}
