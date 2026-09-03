package com.zhush.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zhush.common.constant.CommonConstant;
import com.zhush.common.response.Result;
import com.zhush.system.service.ServerMonitorService;
import com.zhush.system.vo.MonitorServerVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务监控接口（CPU/内存/服务器/JVM/磁盘）
 *
 * @author zhush
 */
@RestController
@Tag(name = "管理端-服务监控", description = "CPU/内存/服务器信息/JVM/磁盘状态采集")
@RequestMapping("/admin/system/monitor/server")
public class AdminServerMonitorController {

    private final ServerMonitorService serverMonitorService;

    public AdminServerMonitorController(ServerMonitorService serverMonitorService) {
        this.serverMonitorService = serverMonitorService;
    }

    /**
     * 聚合服务监控信息
     */
    @GetMapping("/info")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:monitor:server:query")
    @Operation(summary = "服务监控聚合信息")
    public Result<MonitorServerVO> info() {
        return Result.success(serverMonitorService.getInfo());
    }
}
