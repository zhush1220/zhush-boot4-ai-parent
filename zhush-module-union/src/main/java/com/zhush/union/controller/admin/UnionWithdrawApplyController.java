package com.zhush.union.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zhush.common.constant.CommonConstant;
import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionWithdrawApplyParam;
import com.zhush.union.query.UnionWithdrawApplyQuery;
import com.zhush.union.service.UnionWithdrawApplyService;
import com.zhush.union.vo.UnionWithdrawApplyVO;
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
 * @ClassName UnionWithdrawApplyController
 * @Description 提现申请表 控制器
 * @Author zhush
 * @Date 2026/08/08
 **/
@RestController
@Tag(name = "管理端-提现申请", description = "提现申请的增删改查")
@RequestMapping("/admin/union/withdraw-apply")
public class UnionWithdrawApplyController {

    private final UnionWithdrawApplyService withdrawApplyService;

    public UnionWithdrawApplyController(UnionWithdrawApplyService withdrawApplyService) {
        this.withdrawApplyService = withdrawApplyService;
    }

    /**
     * 新增提现申请
     */
    @PostMapping
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:withdraw-apply:create")
    @OperateLog(module = "提现申请", type = OperateTypeEnum.CREATE)
    @Operation(summary = "新增提现申请")
    public Result<Long> create(@RequestBody @Valid UnionWithdrawApplyParam param) {
        return Result.success(withdrawApplyService.createWithdrawApply(param));
    }

    /**
     * 修改提现申请
     */
    @PutMapping
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:withdraw-apply:update")
    @OperateLog(module = "提现申请", type = OperateTypeEnum.UPDATE)
    @Operation(summary = "修改提现申请")
    public Result<Void> update(@RequestBody @Valid UnionWithdrawApplyParam param) {
        withdrawApplyService.updateWithdrawApply(param);
        return Result.success();
    }

    /**
     * 删除提现申请
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:withdraw-apply:delete")
    @OperateLog(module = "提现申请", type = OperateTypeEnum.DELETE)
    @Operation(summary = "删除提现申请")
    public Result<Void> delete(@Parameter(description = "ID") @PathVariable Long id) {
        withdrawApplyService.deleteWithdrawApply(id);
        return Result.success();
    }

    /**
     * 提现申请详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:withdraw-apply:query")
    @Operation(summary = "提现申请详情")
    public Result<UnionWithdrawApplyVO> get(@Parameter(description = "ID") @PathVariable Long id) {
        return Result.success(withdrawApplyService.getWithdrawApply(id));
    }

    /**
     * 提现申请分页
     */
    @GetMapping("/page")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:withdraw-apply:query")
    @Operation(summary = "提现申请分页")
    public Result<PageResult<UnionWithdrawApplyVO>> page(@Valid UnionWithdrawApplyQuery query) {
        return Result.success(withdrawApplyService.getWithdrawApplyPage(query));
    }

}
