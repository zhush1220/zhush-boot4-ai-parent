package com.zhush.union.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zhush.common.constant.CommonConstant;
import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionExternalWithdrawParam;
import com.zhush.union.query.UnionExternalWithdrawQuery;
import com.zhush.union.service.UnionExternalWithdrawService;
import com.zhush.union.vo.UnionExternalWithdrawVO;
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
 * @ClassName UnionExternalWithdrawController
 * @Description 外部推广者提现申请表 控制器
 * @Author zhush
 * @Date 2026/08/08
 **/
@RestController
@Tag(name = "管理端-外部推广者提现申请", description = "外部推广者提现申请的增删改查")
@RequestMapping("/admin/union/external-withdraw")
public class UnionExternalWithdrawController {

    private final UnionExternalWithdrawService externalWithdrawService;

    public UnionExternalWithdrawController(UnionExternalWithdrawService externalWithdrawService) {
        this.externalWithdrawService = externalWithdrawService;
    }

    /**
     * 新增外部推广者提现申请
     */
    @PostMapping
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:external-withdraw:create")
    @OperateLog(module = "外部推广者提现申请", type = OperateTypeEnum.CREATE)
    @Operation(summary = "新增外部推广者提现申请")
    public Result<Long> create(@RequestBody @Valid UnionExternalWithdrawParam param) {
        return Result.success(externalWithdrawService.createExternalWithdraw(param));
    }

    /**
     * 修改外部推广者提现申请
     */
    @PutMapping
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:external-withdraw:update")
    @OperateLog(module = "外部推广者提现申请", type = OperateTypeEnum.UPDATE)
    @Operation(summary = "修改外部推广者提现申请")
    public Result<Void> update(@RequestBody @Valid UnionExternalWithdrawParam param) {
        externalWithdrawService.updateExternalWithdraw(param);
        return Result.success();
    }

    /**
     * 删除外部推广者提现申请
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:external-withdraw:delete")
    @OperateLog(module = "外部推广者提现申请", type = OperateTypeEnum.DELETE)
    @Operation(summary = "删除外部推广者提现申请")
    public Result<Void> delete(@Parameter(description = "ID") @PathVariable Long id) {
        externalWithdrawService.deleteExternalWithdraw(id);
        return Result.success();
    }

    /**
     * 外部推广者提现申请详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:external-withdraw:query")
    @Operation(summary = "外部推广者提现申请详情")
    public Result<UnionExternalWithdrawVO> get(@Parameter(description = "ID") @PathVariable Long id) {
        return Result.success(externalWithdrawService.getExternalWithdraw(id));
    }

    /**
     * 外部推广者提现申请分页
     */
    @GetMapping("/page")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:external-withdraw:query")
    @Operation(summary = "外部推广者提现申请分页")
    public Result<PageResult<UnionExternalWithdrawVO>> page(@Valid UnionExternalWithdrawQuery query) {
        return Result.success(externalWithdrawService.getExternalWithdrawPage(query));
    }

}
