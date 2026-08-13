package com.zhush.union.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zhush.common.constant.CommonConstant;
import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionWithdrawRiskRecordParam;
import com.zhush.union.query.UnionWithdrawRiskRecordQuery;
import com.zhush.union.service.UnionWithdrawRiskRecordService;
import com.zhush.union.vo.UnionWithdrawRiskRecordVO;
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
 * @ClassName UnionWithdrawRiskRecordController
 * @Description 提现风控记录表 控制器
 * @Author zhush
 * @Date 2026/08/08
 **/
@RestController
@Tag(name = "管理端-提现风控记录", description = "提现风控记录的增删改查")
@RequestMapping("/admin/union/withdraw-risk-record")
public class UnionWithdrawRiskRecordController {

    private final UnionWithdrawRiskRecordService withdrawRiskRecordService;

    public UnionWithdrawRiskRecordController(UnionWithdrawRiskRecordService withdrawRiskRecordService) {
        this.withdrawRiskRecordService = withdrawRiskRecordService;
    }

    /**
     * 新增提现风控记录
     */
    @PostMapping
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:withdraw-risk-record:create")
    @OperateLog(module = "提现风控记录", type = OperateTypeEnum.CREATE)
    @Operation(summary = "新增提现风控记录")
    public Result<Long> create(@RequestBody @Valid UnionWithdrawRiskRecordParam param) {
        return Result.success(withdrawRiskRecordService.createWithdrawRiskRecord(param));
    }

    /**
     * 修改提现风控记录
     */
    @PutMapping
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:withdraw-risk-record:update")
    @OperateLog(module = "提现风控记录", type = OperateTypeEnum.UPDATE)
    @Operation(summary = "修改提现风控记录")
    public Result<Void> update(@RequestBody @Valid UnionWithdrawRiskRecordParam param) {
        withdrawRiskRecordService.updateWithdrawRiskRecord(param);
        return Result.success();
    }

    /**
     * 删除提现风控记录
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:withdraw-risk-record:delete")
    @OperateLog(module = "提现风控记录", type = OperateTypeEnum.DELETE)
    @Operation(summary = "删除提现风控记录")
    public Result<Void> delete(@Parameter(description = "ID") @PathVariable Long id) {
        withdrawRiskRecordService.deleteWithdrawRiskRecord(id);
        return Result.success();
    }

    /**
     * 提现风控记录详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:withdraw-risk-record:query")
    @Operation(summary = "提现风控记录详情")
    public Result<UnionWithdrawRiskRecordVO> get(@Parameter(description = "ID") @PathVariable Long id) {
        return Result.success(withdrawRiskRecordService.getWithdrawRiskRecord(id));
    }

    /**
     * 提现风控记录分页
     */
    @GetMapping("/page")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:withdraw-risk-record:query")
    @Operation(summary = "提现风控记录分页")
    public Result<PageResult<UnionWithdrawRiskRecordVO>> page(@Valid UnionWithdrawRiskRecordQuery query) {
        return Result.success(withdrawRiskRecordService.getWithdrawRiskRecordPage(query));
    }

}
