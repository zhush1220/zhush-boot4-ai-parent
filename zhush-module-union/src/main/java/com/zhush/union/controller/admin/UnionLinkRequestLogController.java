package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionLinkRequestLogParam;
import com.zhush.union.query.UnionLinkRequestLogQuery;
import com.zhush.union.service.UnionLinkRequestLogService;
import com.zhush.union.vo.UnionLinkRequestLogVO;
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
 * @ClassName UnionLinkRequestLogController
 * @Description 转链请求日志表 控制器
 * @Author zhush
 * @Date 2026/08/08
 **/
@RestController
@Tag(name = "管理端-转链请求日志", description = "转链请求日志的增删改查")
@RequestMapping("/admin/union/link-request-log")
public class UnionLinkRequestLogController {

    private final UnionLinkRequestLogService linkRequestLogService;

    public UnionLinkRequestLogController(UnionLinkRequestLogService linkRequestLogService) {
        this.linkRequestLogService = linkRequestLogService;
    }

    /**
     * 新增转链请求日志
     */
    @PostMapping
    @OperateLog(module = "转链请求日志", type = OperateTypeEnum.CREATE)
    @Operation(summary = "新增转链请求日志")
    public Result<Long> create(@RequestBody @Valid UnionLinkRequestLogParam param) {
        return Result.success(linkRequestLogService.createLinkRequestLog(param));
    }

    /**
     * 修改转链请求日志
     */
    @PutMapping
    @OperateLog(module = "转链请求日志", type = OperateTypeEnum.UPDATE)
    @Operation(summary = "修改转链请求日志")
    public Result<Void> update(@RequestBody @Valid UnionLinkRequestLogParam param) {
        linkRequestLogService.updateLinkRequestLog(param);
        return Result.success();
    }

    /**
     * 删除转链请求日志
     */
    @DeleteMapping("/{id}")
    @OperateLog(module = "转链请求日志", type = OperateTypeEnum.DELETE)
    @Operation(summary = "删除转链请求日志")
    public Result<Void> delete(@Parameter(description = "ID") @PathVariable Long id) {
        linkRequestLogService.deleteLinkRequestLog(id);
        return Result.success();
    }

    /**
     * 转链请求日志详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "转链请求日志详情")
    public Result<UnionLinkRequestLogVO> get(@Parameter(description = "ID") @PathVariable Long id) {
        return Result.success(linkRequestLogService.getLinkRequestLog(id));
    }

    /**
     * 转链请求日志分页
     */
    @GetMapping("/page")
    @Operation(summary = "转链请求日志分页")
    public Result<PageResult<UnionLinkRequestLogVO>> page(@Valid UnionLinkRequestLogQuery query) {
        return Result.success(linkRequestLogService.getLinkRequestLogPage(query));
    }

}
