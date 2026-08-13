package com.zhush.union.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zhush.common.constant.CommonConstant;
import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionAccountTransactionParam;
import com.zhush.union.query.UnionAccountTransactionQuery;
import com.zhush.union.service.UnionAccountTransactionService;
import com.zhush.union.vo.UnionAccountTransactionVO;
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
 * @ClassName UnionAccountTransactionController
 * @Description 账户资金流水表 控制器
 * @Author zhush
 * @Date 2026/08/08
 **/
@RestController
@Tag(name = "管理端-账户资金流水", description = "账户资金流水的增删改查")
@RequestMapping("/admin/union/account-transaction")
public class UnionAccountTransactionController {

    private final UnionAccountTransactionService accountTransactionService;

    public UnionAccountTransactionController(UnionAccountTransactionService accountTransactionService) {
        this.accountTransactionService = accountTransactionService;
    }

    /**
     * 新增账户资金流水
     */
    @PostMapping
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:account-transaction:create")
    @OperateLog(module = "账户资金流水", type = OperateTypeEnum.CREATE)
    @Operation(summary = "新增账户资金流水")
    public Result<Long> create(@RequestBody @Valid UnionAccountTransactionParam param) {
        return Result.success(accountTransactionService.createAccountTransaction(param));
    }

    /**
     * 修改账户资金流水
     */
    @PutMapping
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:account-transaction:update")
    @OperateLog(module = "账户资金流水", type = OperateTypeEnum.UPDATE)
    @Operation(summary = "修改账户资金流水")
    public Result<Void> update(@RequestBody @Valid UnionAccountTransactionParam param) {
        accountTransactionService.updateAccountTransaction(param);
        return Result.success();
    }

    /**
     * 删除账户资金流水
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:account-transaction:delete")
    @OperateLog(module = "账户资金流水", type = OperateTypeEnum.DELETE)
    @Operation(summary = "删除账户资金流水")
    public Result<Void> delete(@Parameter(description = "ID") @PathVariable Long id) {
        accountTransactionService.deleteAccountTransaction(id);
        return Result.success();
    }

    /**
     * 账户资金流水详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:account-transaction:query")
    @Operation(summary = "账户资金流水详情")
    public Result<UnionAccountTransactionVO> get(@Parameter(description = "ID") @PathVariable Long id) {
        return Result.success(accountTransactionService.getAccountTransaction(id));
    }

    /**
     * 账户资金流水分页
     */
    @GetMapping("/page")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:account-transaction:query")
    @Operation(summary = "账户资金流水分页")
    public Result<PageResult<UnionAccountTransactionVO>> page(@Valid UnionAccountTransactionQuery query) {
        return Result.success(accountTransactionService.getAccountTransactionPage(query));
    }

}
