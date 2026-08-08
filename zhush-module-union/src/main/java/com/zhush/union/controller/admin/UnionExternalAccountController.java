package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionExternalAccountParam;
import com.zhush.union.query.UnionExternalAccountQuery;
import com.zhush.union.service.UnionExternalAccountService;
import com.zhush.union.vo.UnionExternalAccountVO;
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
 * @ClassName UnionExternalAccountController
 * @Description 外部推广者账户表 控制器
 * @Author zhush
 * @Date 2026/08/08
 **/
@RestController
@Tag(name = "管理端-外部推广者账户", description = "外部推广者账户的增删改查")
@RequestMapping("/admin/union/external-account")
public class UnionExternalAccountController {

    private final UnionExternalAccountService externalAccountService;

    public UnionExternalAccountController(UnionExternalAccountService externalAccountService) {
        this.externalAccountService = externalAccountService;
    }

    /**
     * 新增外部推广者账户
     */
    @PostMapping
    @OperateLog(module = "外部推广者账户", type = OperateTypeEnum.CREATE)
    @Operation(summary = "新增外部推广者账户")
    public Result<Long> create(@RequestBody @Valid UnionExternalAccountParam param) {
        return Result.success(externalAccountService.createExternalAccount(param));
    }

    /**
     * 修改外部推广者账户
     */
    @PutMapping
    @OperateLog(module = "外部推广者账户", type = OperateTypeEnum.UPDATE)
    @Operation(summary = "修改外部推广者账户")
    public Result<Void> update(@RequestBody @Valid UnionExternalAccountParam param) {
        externalAccountService.updateExternalAccount(param);
        return Result.success();
    }

    /**
     * 删除外部推广者账户
     */
    @DeleteMapping("/{id}")
    @OperateLog(module = "外部推广者账户", type = OperateTypeEnum.DELETE)
    @Operation(summary = "删除外部推广者账户")
    public Result<Void> delete(@Parameter(description = "ID") @PathVariable Long id) {
        externalAccountService.deleteExternalAccount(id);
        return Result.success();
    }

    /**
     * 外部推广者账户详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "外部推广者账户详情")
    public Result<UnionExternalAccountVO> get(@Parameter(description = "ID") @PathVariable Long id) {
        return Result.success(externalAccountService.getExternalAccount(id));
    }

    /**
     * 外部推广者账户分页
     */
    @GetMapping("/page")
    @Operation(summary = "外部推广者账户分页")
    public Result<PageResult<UnionExternalAccountVO>> page(@Valid UnionExternalAccountQuery query) {
        return Result.success(externalAccountService.getExternalAccountPage(query));
    }

}
