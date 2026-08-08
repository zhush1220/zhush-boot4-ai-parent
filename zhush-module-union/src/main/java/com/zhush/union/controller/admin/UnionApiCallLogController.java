package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionApiCallLogParam;
import com.zhush.union.query.UnionApiCallLogQuery;
import com.zhush.union.service.UnionApiCallLogService;
import com.zhush.union.vo.UnionApiCallLogVO;
import com.zhush.web.operatelog.annotation.OperateLog;
import com.zhush.web.operatelog.enums.OperateTypeEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @ClassName UnionApiCallLogController
 * @Description 平台API调用日志表 控制器
 * @Author zhush
 * @Date 2026/08/08
 **/
@RestController
@Tag(name = "管理端-平台API调用日志", description = "平台API调用日志的增删改查")
@RequestMapping("/admin/union/api-call-log")
public class UnionApiCallLogController {

    private final UnionApiCallLogService apiCallLogService;

    public UnionApiCallLogController(UnionApiCallLogService apiCallLogService) {
        this.apiCallLogService = apiCallLogService;
    }

    /**
     * 新增平台API调用日志
     */
    @PostMapping
    @OperateLog(module = "平台API调用日志", type = OperateTypeEnum.CREATE)
    @Operation(summary = "新增平台API调用日志")
    public Result<Long> create(@RequestBody @Valid UnionApiCallLogParam param) {
        return Result.success(apiCallLogService.createApiCallLog(param));
    }

    /**
     * 修改平台API调用日志
     */
    @PutMapping
    @OperateLog(module = "平台API调用日志", type = OperateTypeEnum.UPDATE)
    @Operation(summary = "修改平台API调用日志")
    public Result<Void> update(@RequestBody @Valid UnionApiCallLogParam param) {
        apiCallLogService.updateApiCallLog(param);
        return Result.success();
    }

    /**
     * 删除平台API调用日志
     */
    @DeleteMapping("/{id}")
    @OperateLog(module = "平台API调用日志", type = OperateTypeEnum.DELETE)
    @Operation(summary = "删除平台API调用日志")
    public Result<Void> delete(@Parameter(description = "ID") @PathVariable Long id) {
        apiCallLogService.deleteApiCallLog(id);
        return Result.success();
    }

    /**
     * 平台API调用日志详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "平台API调用日志详情")
    public Result<UnionApiCallLogVO> get(@Parameter(description = "ID") @PathVariable Long id) {
        return Result.success(apiCallLogService.getApiCallLog(id));
    }

    /**
     * 平台API调用日志分页
     */
    @GetMapping("/page")
    @Operation(summary = "平台API调用日志分页")
    public Result<PageResult<UnionApiCallLogVO>> page(@Valid UnionApiCallLogQuery query) {
        return Result.success(apiCallLogService.getApiCallLogPage(query));
    }

}
