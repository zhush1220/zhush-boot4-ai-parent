package com.zhush.ai.preview;

import com.zhush.ai.preview.PreviewProperties.Convert;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 甲-1：LibreOffice headless 内嵌转换（无第二容器）。
 *
 * <p>直接 {@code ProcessBuilder} 起 {@code soffice --headless --convert-to pdf}，
 * 不引 {@code jodconverter-spring-boot-starter}（4.4.11 基于 Boot 2.7、5.x 面向 Boot 3.x，Boot 4 未验证）。
 *
 * <p>三条硬约束：
 * <ol>
 *     <li><b>限流</b>：office 进程数固定为 {@code max-concurrent}，每个进程用独立的
 *         {@code -env:UserInstallation} profile 目录（同一 profile 并发启动会互相抢锁而失败）；
 *         排队人数超过 {@code queue-limit} 立即报忙，不让 HTTP 线程无限堆积。</li>
 *     <li><b>超时</b>：单文档 {@code timeout-seconds}，超时 {@code destroyForcibly}，防一份坏 PPT 卡死进程。</li>
 *     <li><b>不接管 HTML/SVG</b>：这类内容可携带脚本，只允许前端沙箱 iframe 渲染（方案 S1）。</li>
 * </ol>
 *
 * <p>依赖未装时不阻断启动：{@link #available()} 为 false，门面按路线乙降级。
 *
 * @author zhush
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "zhush.preview", name = "office-converter", havingValue = "libreoffice")
public class LibreOfficeFileConverter implements OfficeFileConverter {

    /** LibreOffice 能可靠转 PDF 的办公格式；其余一律不接单 */
    private static final Set<String> SUPPORTED = Set.of(
            "doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "odp", "rtf");

    private static final int SLOT_ACQUIRE_GRACE_MS = 5_000;
    private static final int KILL_WAIT_MS = 5_000;

    private final PreviewProperties previewProperties;
    private final Path workingRoot;

    /** 空闲槽位队列：槽位号同时是 profile 目录后缀，保证一个槽位一个 Office 用户配置 */
    private BlockingQueue<Integer> freeSlots;
    private final AtomicInteger waiting = new AtomicInteger();
    private volatile boolean available;

    public LibreOfficeFileConverter(PreviewProperties previewProperties, PreviewCacheStore cacheStore) {
        this.previewProperties = previewProperties;
        this.workingRoot = cacheStore.root().resolve("work");
    }

    @PostConstruct
    void init() {
        Convert convert = previewProperties.getConvert();
        int slots = Math.max(1, convert.getMaxConcurrent());
        freeSlots = new ArrayBlockingQueue<>(slots);
        for (int i = 0; i < slots; i++) {
            freeSlots.offer(i);
        }
        // 清掉上次运行残留：JVM 被 kill 时 soffice 可能还在持有 profile 锁，会让本次首个转换直接失败
        deleteQuietly(workingRoot);
        probe();
    }

    @Override
    public String name() {
        return "libreoffice";
    }

    @Override
    public boolean available() {
        return available;
    }

    @Override
    public boolean supports(String ext) {
        return ext != null && SUPPORTED.contains(ext.toLowerCase());
    }

    @Override
    public byte[] convertToPdf(String ext, byte[] sourceBytes) {
        if (!available) {
            throw new PreviewConversionException(PreviewConversionException.Kind.UNAVAILABLE,
                    "LibreOffice 不可用：" + previewProperties.getConvert().getSofficePath());
        }
        if (sourceBytes == null || sourceBytes.length == 0) {
            throw new PreviewConversionException(PreviewConversionException.Kind.FAILED, "源文件为空");
        }
        Convert convert = previewProperties.getConvert();
        if (sourceBytes.length > convert.getMaxSourceBytes()) {
            throw new PreviewConversionException(PreviewConversionException.Kind.FAILED,
                    "文件超出转换体积上限：" + sourceBytes.length + " 字节");
        }
        int slot = acquireSlot(convert);
        Path jobDir = workingRoot.resolve("job-" + UUID.randomUUID().toString().replace("-", ""));
        try {
            Files.createDirectories(jobDir);
            Path sourceFile = jobDir.resolve("source." + ext.toLowerCase());
            Files.write(sourceFile, sourceBytes);
            Path profileDir = workingRoot.resolve("profile-" + slot);
            Files.createDirectories(profileDir);
            runSoffice(convert, sourceFile, jobDir, profileDir);
            Path pdf = jobDir.resolve("source.pdf");
            if (!Files.isRegularFile(pdf)) {
                throw new PreviewConversionException(PreviewConversionException.Kind.FAILED,
                        "转换结束但未产出 PDF，详见 " + jobDir.getFileName() + "/soffice.log");
            }
            byte[] bytes = Files.readAllBytes(pdf);
            if (bytes.length == 0) {
                throw new PreviewConversionException(PreviewConversionException.Kind.FAILED, "转换产出空 PDF");
            }
            return bytes;
        } catch (PreviewConversionException e) {
            throw e;
        } catch (IOException e) {
            throw new PreviewConversionException(PreviewConversionException.Kind.FAILED,
                    "转换临时文件读写失败：" + e.getMessage(), e);
        } finally {
            freeSlots.offer(slot);
            deleteQuietly(jobDir);
        }
    }

    // ================ 内部 ================

    private int acquireSlot(Convert convert) {
        if (waiting.incrementAndGet() > convert.getMaxConcurrent() + convert.getQueueLimit()) {
            waiting.decrementAndGet();
            throw new PreviewConversionException(PreviewConversionException.Kind.BUSY,
                    "转换排队已达上限 " + convert.getQueueLimit());
        }
        try {
            Integer slot = freeSlots.poll(convert.getQueueTimeoutSeconds(), TimeUnit.SECONDS);
            if (slot == null) {
                throw new PreviewConversionException(PreviewConversionException.Kind.BUSY,
                        "等待转换槽位超时 " + convert.getQueueTimeoutSeconds() + "s");
            }
            return slot;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PreviewConversionException(PreviewConversionException.Kind.BUSY, "等待转换槽位被中断");
        } finally {
            waiting.decrementAndGet();
        }
    }

    private void runSoffice(Convert convert, Path sourceFile, Path outDir, Path profileDir) {
        List<String> command = List.of(
                convert.getSofficePath(),
                "--headless", "--invisible", "--norestore", "--nolockcheck",
                "--nodefault", "--nofirststartwizard",
                "-env:UserInstallation=" + profileDir.toUri(),
                "--convert-to", "pdf",
                "--outdir", outDir.toString(),
                sourceFile.toString());
        Path logFile = outDir.resolve("soffice.log");
        Process process = null;
        try {
            ProcessBuilder builder = new ProcessBuilder(command)
                    .directory(outDir.toFile())
                    // 输出重定向到文件：走管道需边读边清缓冲区，否则 soffice 写满管道后会阻塞
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.to(logFile.toFile()));
            process = builder.start();
            if (!process.waitFor(convert.getTimeoutSeconds(), TimeUnit.SECONDS)) {
                process.destroyForcibly();
                awaitExit(process);
                throw new PreviewConversionException(PreviewConversionException.Kind.TIMEOUT,
                        "转换超时 " + convert.getTimeoutSeconds() + "s");
            }
            if (process.exitValue() != 0) {
                throw new PreviewConversionException(PreviewConversionException.Kind.FAILED,
                        "soffice 退出码 " + process.exitValue() + "：" + tail(logFile));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (process != null) {
                process.destroyForcibly();
            }
            throw new PreviewConversionException(PreviewConversionException.Kind.BUSY, "转换被中断");
        } catch (IOException e) {
            // 二进制不存在或不可执行：依赖缺失属部署问题，标记为不可用，后续请求不再排队
            available = false;
            throw new PreviewConversionException(PreviewConversionException.Kind.UNAVAILABLE,
                    "无法启动 soffice：" + e.getMessage(), e);
        }
    }

    private void awaitExit(Process process) {
        try {
            process.waitFor(KILL_WAIT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** 启动即探测一次：缺依赖时给出可执行提示，而不是等用户点预览才在日志里冒一条 IO 异常 */
    private void probe() {
        Convert convert = previewProperties.getConvert();
        try {
            Files.createDirectories(workingRoot);
            Process process = new ProcessBuilder(convert.getSofficePath(), "--version")
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();
            if (!process.waitFor(15, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                markUnavailable("探测超时 15s");
                return;
            }
            available = process.exitValue() == 0;
            if (!available) {
                markUnavailable("退出码 " + process.exitValue());
                return;
            }
            log.info("[预览] LibreOffice 已就绪: {}，并发 {}，单文档超时 {}s",
                    convert.getSofficePath(), convert.getMaxConcurrent(), convert.getTimeoutSeconds());
        } catch (IOException e) {
            markUnavailable(e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            markUnavailable("探测被中断");
        }
    }

    private void markUnavailable(String detail) {
        available = false;
        log.warn("[预览] LibreOffice 不可用（{}），旧格式回落「仅下载」。容器内需安装 libreoffice 与中文字体，"
                + "或将 zhush.preview.convert.soffice-path 指向实际可执行文件；缺中文字体时转出的 PDF 会中文乱码", detail);
    }

    private String tail(Path logFile) {
        try {
            if (!Files.isRegularFile(logFile)) {
                return "无输出";
            }
            String text = Files.readString(logFile);
            int len = text.length();
            return len <= 400 ? text.trim() : text.substring(len - 400).trim();
        } catch (IOException e) {
            return "日志不可读";
        }
    }

    private void deleteQuietly(Path dir) {
        if (dir == null || !Files.exists(dir)) {
            return;
        }
        // 超时强杀的 soffice 可能仍占着临时文件，删除失败只留日志，不影响降级结果
        try (var paths = Files.walk(dir)) {
            paths.sorted((a, b) -> b.getNameCount() - a.getNameCount()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    log.debug("[预览] 临时文件删除失败: {}", path);
                }
            });
        } catch (IOException e) {
            log.debug("[预览] 临时目录清理失败: {}", dir, e);
        }
    }

    @PreDestroy
    void shutdown() {
        // 转换进程随请求逐个销毁；这里只回收工作目录（含 profile），下次启动会重建
        deleteQuietly(workingRoot);
    }

}
