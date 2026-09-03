package com.zhush.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zhush.common.constant.CommonConstant;
import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.protection.idempotent.annotation.Idempotent;
import com.zhush.system.service.CacheMonitorService;
import com.zhush.system.vo.MonitorCacheDetailVO;
import com.zhush.system.vo.MonitorCacheKeyVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 缓存监控接口（Redis）
 *
 * @author zhush
 */
@RestController
@Tag(name = "管理端-缓存监控", description = "Redis 实例信息、键列表/详情/删除")
@RequestMapping("/admin/system/monitor/cache")
public class AdminCacheMonitorController {

    private final CacheMonitorService cacheMonitorService;

    public AdminCacheMonitorController(CacheMonitorService cacheMonitorService) {
        this.cacheMonitorService = cacheMonitorService;
    }

    /**
     * Redis 实例信息
     */
    @GetMapping("/info")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:monitor:cache:query")
    @Operation(summary = "Redis 实例信息")
    public Result<Map<String, Object>> info() {
        return Result.success(cacheMonitorService.getInfo());
    }

    /**
     * 缓存键分页列表
     */
    @GetMapping("/page")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:monitor:cache:query")
    @Operation(summary = "缓存键分页列表")
    public Result<PageResult<MonitorCacheKeyVO>> page(
            @Parameter(description = "键模糊匹配，默认 *") @RequestParam(required = false) String pattern,
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(cacheMonitorService.getKeyPage(pattern, pageNum, pageSize));
    }

    /**
     * 缓存键详情
     */
    @GetMapping("/detail")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:monitor:cache:query")
    @Operation(summary = "缓存键详情")
    public Result<MonitorCacheDetailVO> detail(
            @Parameter(description = "缓存键") @RequestParam String key) {
        return Result.success(cacheMonitorService.getDetail(key));
    }

    /**
     * 删除缓存键
     */
    @DeleteMapping("/delete")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:monitor:cache:delete")
    @Idempotent
    @Operation(summary = "删除缓存键")
    public Result<Void> delete(
            @Parameter(description = "缓存键") @RequestParam String key) {
        cacheMonitorService.deleteKey(key);
        return Result.success();
    }
}
