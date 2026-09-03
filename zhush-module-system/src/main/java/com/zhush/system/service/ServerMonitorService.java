package com.zhush.system.service;

import com.sun.management.OperatingSystemMXBean;
import com.zhush.system.vo.MonitorServerVO;
import com.zhush.system.vo.MonitorServerVO.Cpu;
import com.zhush.system.vo.MonitorServerVO.Disk;
import com.zhush.system.vo.MonitorServerVO.Jvm;
import com.zhush.system.vo.MonitorServerVO.Memory;
import com.zhush.system.vo.MonitorServerVO.Sys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.net.InetAddress;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 服务监控服务
 *
 * <p>基于 JDK 内建 MXBean 与 {@code java.nio.file.FileStore} 采集 CPU（com.sun.management）、
 * 物理内存、JVM（堆/非堆/GC/线程/类加载）、服务器系统信息与磁盘分区，零第三方依赖。
 *
 * @author zhush
 */
@Slf4j
@Service
public class ServerMonitorService {

    /**
     * 采集聚合监控信息
     */
    public MonitorServerVO getInfo() {
        MonitorServerVO vo = new MonitorServerVO();
        vo.setCpu(buildCpu());
        vo.setMemory(buildMemory());
        vo.setJvm(buildJvm());
        vo.setSys(buildSys());
        vo.setDisks(buildDisks());
        return vo;
    }

    private Cpu buildCpu() {
        OperatingSystemMXBean osBean = requireOsBean();
        Cpu cpu = new Cpu();
        cpu.setCores(osBean.getAvailableProcessors());
        double loadAverage = osBean.getSystemLoadAverage();
        cpu.setSystemLoad(loadAverage < 0 ? 0 : Math.round(loadAverage * 100) / 100.0);
        // getCpuLoad() 返回 0~1；-1 表示首次采样未知
        double cpuLoad = osBean.getCpuLoad();
        cpu.setUsage(cpuLoad < 0 ? -1 : Math.round(cpuLoad * 10000) / 100.0);
        return cpu;
    }

    private Memory buildMemory() {
        OperatingSystemMXBean osBean = requireOsBean();
        long total = osBean.getTotalMemorySize();
        long free = osBean.getFreeMemorySize();
        long used = Math.max(0, total - free);
        Memory memory = new Memory();
        memory.setTotal(total);
        memory.setFree(free);
        memory.setUsed(used);
        memory.setUsage(total == 0 ? 0 : Math.round((double) used / total * 10000) / 100.0);
        return memory;
    }

    private Jvm buildJvm() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long gcCount = 0;
        long gcTime = 0;
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            gcCount += gc.getCollectionCount();
            gcTime += gc.getCollectionTime();
        }
        ClassLoadingMXBean classLoading = ManagementFactory.getClassLoadingMXBean();
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();

        Jvm jvm = new Jvm();
        jvm.setHeapUsed(memoryBean.getHeapMemoryUsage().getUsed());
        jvm.setHeapMax(memoryBean.getHeapMemoryUsage().getMax());
        jvm.setNonHeapUsed(memoryBean.getNonHeapMemoryUsage().getUsed());
        jvm.setGcCount(gcCount);
        jvm.setGcTime(gcTime);
        jvm.setLoadedClasses(classLoading.getLoadedClassCount());
        jvm.setThreadCount(threadBean.getThreadCount());
        jvm.setUptime(runtimeBean.getUptime());
        return jvm;
    }

    private Sys buildSys() {
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        Sys sys = new Sys();
        sys.setOsName(System.getProperty("os.name"));
        sys.setOsVersion(System.getProperty("os.version"));
        sys.setOsArch(System.getProperty("os.arch"));
        sys.setJavaVersion(System.getProperty("java.version"));
        sys.setUserDir(System.getProperty("user.dir"));
        sys.setStartTime(Instant.ofEpochMilli(runtimeBean.getStartTime()).toString());
        try {
            InetAddress addr = InetAddress.getLocalHost();
            sys.setHostname(addr.getHostName());
            sys.setIp(addr.getHostAddress());
        } catch (Exception e) {
            log.warn("[ServerMonitor] 获取主机名/IP 失败", e);
            sys.setHostname("unknown");
            sys.setIp("unknown");
        }
        return sys;
    }

    private List<Disk> buildDisks() {
        List<Disk> disks = new ArrayList<>();
        File[] roots = File.listRoots();
        if (roots == null) {
            return disks;
        }
        for (File root : roots) {
            try {
                FileStore store = Files.getFileStore(root.toPath());
                long total = store.getTotalSpace();
                long free = store.getUsableSpace();
                long used = Math.max(0, total - free);
                Disk disk = new Disk();
                disk.setName(root.getPath());
                disk.setFsType(store.type());
                disk.setTotal(total);
                disk.setFree(free);
                disk.setUsed(used);
                disk.setUsage(total == 0 ? 0 : Math.round((double) used / total * 10000) / 100.0);
                disks.add(disk);
            } catch (Exception e) {
                log.warn("[ServerMonitor] 读取磁盘分区失败: {}", root.getPath(), e);
            }
        }
        return disks;
    }

    private OperatingSystemMXBean requireOsBean() {
        // HotSpot/OpenJ9 等主流 JVM 均返回 com.sun.management 子接口实现，可直接强转
        return (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    }
}
