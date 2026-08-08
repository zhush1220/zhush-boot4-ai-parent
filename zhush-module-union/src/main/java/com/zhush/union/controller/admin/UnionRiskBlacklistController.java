package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionRiskBlacklistParam;
import com.zhush.union.query.UnionRiskBlacklistQuery;
import com.zhush.union.service.UnionRiskBlacklistService;
import com.zhush.union.vo.UnionRiskBlacklistVO;
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
 * @ClassName UnionRiskBlacklistController
 * @Description 风控黑名单表 控制器
 * @Author zhush
 * @Date 2026/08/08
 **/
@RestController
@Tag(name = "管理端-风控黑名单", description = "风控黑名单的增删改查")
@RequestMapping("/admin/union/risk-blacklist")
public class UnionRiskBlacklistController {

    private final UnionRiskBlacklistService riskBlacklistService;

    public UnionRiskBlacklistController(UnionRiskBlacklistService riskBlacklistService) {
        this.riskBlacklistService = riskBlacklistService;
    }

    /**
     * 新增风控黑名单
     */
    @PostMapping
    @OperateLog(module = "风控黑名单", type = OperateTypeEnum.CREATE)
    @Operation(summary = "新增风控黑名单")
    public Result<Long> create(@RequestBody @Valid UnionRiskBlacklistParam param) {
        return Result.success(riskBlacklistService.createRiskBlacklist(param));
    }

    /**
     * 修改风控黑名单
     */
    @PutMapping
    @OperateLog(module = "风控黑名单", type = OperateTypeEnum.UPDATE)
    @Operation(summary = "修改风控黑名单")
    public Result<Void> update(@RequestBody @Valid UnionRiskBlacklistParam param) {
        riskBlacklistService.updateRiskBlacklist(param);
        return Result.success();
    }

    /**
     * 删除风控黑名单
     */
    @DeleteMapping("/{id}")
    @OperateLog(module = "风控黑名单", type = OperateTypeEnum.DELETE)
    @Operation(summary = "删除风控黑名单")
    public Result<Void> delete(@Parameter(description = "ID") @PathVariable Long id) {
        riskBlacklistService.deleteRiskBlacklist(id);
        return Result.success();
    }

    /**
     * 风控黑名单详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "风控黑名单详情")
    public Result<UnionRiskBlacklistVO> get(@Parameter(description = "ID") @PathVariable Long id) {
        return Result.success(riskBlacklistService.getRiskBlacklist(id));
    }

    /**
     * 风控黑名单分页
     */
    @GetMapping("/page")
    @Operation(summary = "风控黑名单分页")
    public Result<PageResult<UnionRiskBlacklistVO>> page(@Valid UnionRiskBlacklistQuery query) {
        return Result.success(riskBlacklistService.getRiskBlacklistPage(query));
    }

}
