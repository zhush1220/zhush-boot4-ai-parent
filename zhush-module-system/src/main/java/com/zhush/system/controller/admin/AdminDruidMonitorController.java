package com.zhush.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zhush.common.constant.CommonConstant;
import com.zhush.common.response.Result;
import com.zhush.protection.idempotent.annotation.Idempotent;
import com.zhush.system.service.DruidMonitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 数据监控接口（Druid）
 *
 * <p>Druid Web（/druid/*）已关闭，统一通过本接口读取授权后的统计信息。
 *
 * @author zhush
 */
@RestController
@Tag(name = "管理端-数据监控", description = "Druid 数据源/SQL/URI/Wall 统计读取与重置")
@RequestMapping("/admin/system/monitor/druid")
public class AdminDruidMonitorController {

    private final DruidMonitorService druidMonitorService;

    public AdminDruidMonitorController(DruidMonitorService druidMonitorService) {
        this.druidMonitorService = druidMonitorService;
    }

    /**
     * 数据源监控列表
     */
    @GetMapping("/datasource")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:monitor:druid:query")
    @Operation(summary = "数据源监控列表")
    public Result<List<Map<String, Object>>> datasource() {
        return Result.success(druidMonitorService.getDataSourceList());
    }

    /**
     * SQL 监控列表（可按 SQL 文本过滤）
     */
    @GetMapping("/sql")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:monitor:druid:query")
    @Operation(summary = "SQL 监控列表")
    public Result<List<Map<String, Object>>> sql(
            @Parameter(description = "SQL 文本关键词") @RequestParam(required = false) String sql) {
        return Result.success(druidMonitorService.getSqlList(sql));
    }

    /**
     * URI 访问监控列表
     */
    @GetMapping("/uri")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:monitor:druid:query")
    @Operation(summary = "URI 访问监控列表")
    public Result<List<Map<String, Object>>> uri() {
        return Result.success(druidMonitorService.getUriList());
    }

    /**
     * Wall 防火墙统计列表
     */
    @GetMapping("/wall")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:monitor:druid:query")
    @Operation(summary = "Wall 防火墙统计列表")
    public Result<List<Map<String, Object>>> wall() {
        return Result.success(druidMonitorService.getWallList());
    }

    /**
     * 重置全部监控统计
     */
    @PostMapping("/reset")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:monitor:druid:reset")
    @Idempotent
    @Operation(summary = "重置全部监控统计")
    public Result<Void> reset() {
        druidMonitorService.reset();
        return Result.success();
    }
}
