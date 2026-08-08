package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionWithdrawConfigParam;
import com.zhush.union.query.UnionWithdrawConfigQuery;
import com.zhush.union.service.UnionWithdrawConfigService;
import com.zhush.union.vo.UnionWithdrawConfigVO;
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
 * @ClassName UnionWithdrawConfigController
 * @Description 提现配置表 控制器
 * @Author zhush
 * @Date 2026/08/08
 **/
@RestController
@Tag(name = "管理端-提现配置", description = "提现配置的增删改查")
@RequestMapping("/admin/union/withdraw-config")
public class UnionWithdrawConfigController {

    private final UnionWithdrawConfigService withdrawConfigService;

    public UnionWithdrawConfigController(UnionWithdrawConfigService withdrawConfigService) {
        this.withdrawConfigService = withdrawConfigService;
    }

    /**
     * 新增提现配置
     */
    @PostMapping
    @OperateLog(module = "提现配置", type = OperateTypeEnum.CREATE)
    @Operation(summary = "新增提现配置")
    public Result<Long> create(@RequestBody @Valid UnionWithdrawConfigParam param) {
        return Result.success(withdrawConfigService.createWithdrawConfig(param));
    }

    /**
     * 修改提现配置
     */
    @PutMapping
    @OperateLog(module = "提现配置", type = OperateTypeEnum.UPDATE)
    @Operation(summary = "修改提现配置")
    public Result<Void> update(@RequestBody @Valid UnionWithdrawConfigParam param) {
        withdrawConfigService.updateWithdrawConfig(param);
        return Result.success();
    }

    /**
     * 删除提现配置
     */
    @DeleteMapping("/{id}")
    @OperateLog(module = "提现配置", type = OperateTypeEnum.DELETE)
    @Operation(summary = "删除提现配置")
    public Result<Void> delete(@Parameter(description = "ID") @PathVariable Long id) {
        withdrawConfigService.deleteWithdrawConfig(id);
        return Result.success();
    }

    /**
     * 提现配置详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "提现配置详情")
    public Result<UnionWithdrawConfigVO> get(@Parameter(description = "ID") @PathVariable Long id) {
        return Result.success(withdrawConfigService.getWithdrawConfig(id));
    }

    /**
     * 提现配置分页
     */
    @GetMapping("/page")
    @Operation(summary = "提现配置分页")
    public Result<PageResult<UnionWithdrawConfigVO>> page(@Valid UnionWithdrawConfigQuery query) {
        return Result.success(withdrawConfigService.getWithdrawConfigPage(query));
    }

}
