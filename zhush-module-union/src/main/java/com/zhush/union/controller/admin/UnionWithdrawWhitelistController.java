package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionWithdrawWhitelistParam;
import com.zhush.union.query.UnionWithdrawWhitelistQuery;
import com.zhush.union.service.UnionWithdrawWhitelistService;
import com.zhush.union.vo.UnionWithdrawWhitelistVO;
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
 * @ClassName UnionWithdrawWhitelistController
 * @Description 提现白名单表 控制器
 * @Author zhush
 * @Date 2026/08/08
 **/
@RestController
@Tag(name = "管理端-提现白名单", description = "提现白名单的增删改查")
@RequestMapping("/admin/union/withdraw-whitelist")
public class UnionWithdrawWhitelistController {

    private final UnionWithdrawWhitelistService withdrawWhitelistService;

    public UnionWithdrawWhitelistController(UnionWithdrawWhitelistService withdrawWhitelistService) {
        this.withdrawWhitelistService = withdrawWhitelistService;
    }

    /**
     * 新增提现白名单
     */
    @PostMapping
    @OperateLog(module = "提现白名单", type = OperateTypeEnum.CREATE)
    @Operation(summary = "新增提现白名单")
    public Result<Long> create(@RequestBody @Valid UnionWithdrawWhitelistParam param) {
        return Result.success(withdrawWhitelistService.createWithdrawWhitelist(param));
    }

    /**
     * 修改提现白名单
     */
    @PutMapping
    @OperateLog(module = "提现白名单", type = OperateTypeEnum.UPDATE)
    @Operation(summary = "修改提现白名单")
    public Result<Void> update(@RequestBody @Valid UnionWithdrawWhitelistParam param) {
        withdrawWhitelistService.updateWithdrawWhitelist(param);
        return Result.success();
    }

    /**
     * 删除提现白名单
     */
    @DeleteMapping("/{id}")
    @OperateLog(module = "提现白名单", type = OperateTypeEnum.DELETE)
    @Operation(summary = "删除提现白名单")
    public Result<Void> delete(@Parameter(description = "ID") @PathVariable Long id) {
        withdrawWhitelistService.deleteWithdrawWhitelist(id);
        return Result.success();
    }

    /**
     * 提现白名单详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "提现白名单详情")
    public Result<UnionWithdrawWhitelistVO> get(@Parameter(description = "ID") @PathVariable Long id) {
        return Result.success(withdrawWhitelistService.getWithdrawWhitelist(id));
    }

    /**
     * 提现白名单分页
     */
    @GetMapping("/page")
    @Operation(summary = "提现白名单分页")
    public Result<PageResult<UnionWithdrawWhitelistVO>> page(@Valid UnionWithdrawWhitelistQuery query) {
        return Result.success(withdrawWhitelistService.getWithdrawWhitelistPage(query));
    }

}
