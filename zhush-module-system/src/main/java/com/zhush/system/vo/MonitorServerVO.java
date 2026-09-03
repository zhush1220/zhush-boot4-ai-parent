package com.zhush.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 服务监控聚合返回
 *
 * <p>CPU / 内存 / JVM / 系统 / 磁盘 由 {@code ServerMonitorService} 基于 JDK 内建 MXBean
 * 与 {@code java.nio.file.FileStore} 采集，零第三方依赖。
 *
 * @author zhush
 */
@Data
@Schema(description = "服务监控聚合返回")
public class MonitorServerVO {

    @Schema(description = "CPU 信息")
    private Cpu cpu;

    @Schema(description = "内存信息")
    private Memory memory;

    @Schema(description = "JVM 信息")
    private Jvm jvm;

    @Schema(description = "系统/服务器信息")
    private Sys sys;

    @Schema(description = "磁盘分区列表")
    private List<Disk> disks;

    @Data
    @Schema(description = "CPU 信息")
    public static class Cpu {

        @Schema(description = "CPU 核数")
        private int cores;

        @Schema(description = "系统负载（1 分钟）")
        private double systemLoad;

        @Schema(description = "CPU 使用率 0~100；首次采样可能为负数表示未知")
        private double usage;
    }

    @Data
    @Schema(description = "内存信息")
    public static class Memory {

        @Schema(description = "物理内存总量（字节）")
        private long total;

        @Schema(description = "已用内存（字节）")
        private long used;

        @Schema(description = "可用内存（字节）")
        private long free;

        @Schema(description = "内存使用率 0~100")
        private double usage;
    }

    @Data
    @Schema(description = "JVM 信息")
    public static class Jvm {

        @Schema(description = "堆内存已用（字节）")
        private long heapUsed;

        @Schema(description = "堆内存最大（字节）")
        private long heapMax;

        @Schema(description = "非堆已用（字节）")
        private long nonHeapUsed;

        @Schema(description = "GC 总次数")
        private long gcCount;

        @Schema(description = "GC 累计耗时（毫秒）")
        private long gcTime;

        @Schema(description = "已加载类数量")
        private int loadedClasses;

        @Schema(description = "当前线程数")
        private int threadCount;

        @Schema(description = "JVM 运行时长（毫秒）")
        private long uptime;
    }

    @Data
    @Schema(description = "系统/服务器信息")
    public static class Sys {

        @Schema(description = "操作系统名称")
        private String osName;

        @Schema(description = "操作系统版本")
        private String osVersion;

        @Schema(description = "系统架构")
        private String osArch;

        @Schema(description = "主机名")
        private String hostname;

        @Schema(description = "IP 地址")
        private String ip;

        @Schema(description = "Java 版本")
        private String javaVersion;

        @Schema(description = "Java 运行目录（user.dir）")
        private String userDir;

        @Schema(description = "JVM 启动时间（ISO-8601 格式）")
        private String startTime;
    }

    @Data
    @Schema(description = "磁盘分区信息")
    public static class Disk {

        @Schema(description = "盘符/挂载点")
        private String name;

        @Schema(description = "文件系统类型")
        private String fsType;

        @Schema(description = "总容量（字节）")
        private long total;

        @Schema(description = "可用容量（字节）")
        private long free;

        @Schema(description = "已用容量（字节）")
        private long used;

        @Schema(description = "使用率 0~100")
        private double usage;
    }
}
