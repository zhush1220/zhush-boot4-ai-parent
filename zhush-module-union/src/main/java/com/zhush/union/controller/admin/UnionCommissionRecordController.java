package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionCommissionRecordParam;
import com.zhush.union.query.UnionCommissionRecordQuery;
import com.zhush.union.service.UnionCommissionRecordService;
import com.zhush.union.vo.UnionCommissionRecordVO;
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
 * @ClassName UnionCommissionRecordController
 * @Description 佣金记录表 控制器
 * @Author zhush
 * @Date 2026/08/08
 **/
@RestController
@Tag(name = "管理端-佣金记录", description = "佣金记录的增删改查")
@RequestMapping("/admin/union/commission-record")
public class UnionCommissionRecordController {

    private final UnionCommissionRecordService commissionRecordService;

    public UnionCommissionRecordController(UnionCommissionRecordService commissionRecordService) {
        this.commissionRecordService = commissionRecordService;
    }

    /**
     * 新增佣金记录
     */
    @PostMapping
    @OperateLog(module = "佣金记录", type = OperateTypeEnum.CREATE)
    @Operation(summary = "新增佣金记录")
    public Result<Long> create(@RequestBody @Valid UnionCommissionRecordParam param) {
        return Result.success(commissionRecordService.createCommissionRecord(param));
    }

    /**
     * 修改佣金记录
     */
    @PutMapping
    @OperateLog(module = "佣金记录", type = OperateTypeEnum.UPDATE)
    @Operation(summary = "修改佣金记录")
    public Result<Void> update(@RequestBody @Valid UnionCommissionRecordParam param) {
        commissionRecordService.updateCommissionRecord(param);
        return Result.success();
    }

    /**
     * 删除佣金记录
     */
    @DeleteMapping("/{id}")
    @OperateLog(module = "佣金记录", type = OperateTypeEnum.DELETE)
    @Operation(summary = "删除佣金记录")
    public Result<Void> delete(@Parameter(description = "ID") @PathVariable Long id) {
        commissionRecordService.deleteCommissionRecord(id);
        return Result.success();
    }

    /**
     * 佣金记录详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "佣金记录详情")
    public Result<UnionCommissionRecordVO> get(@Parameter(description = "ID") @PathVariable Long id) {
        return Result.success(commissionRecordService.getCommissionRecord(id));
    }

    /**
     * 佣金记录分页
     */
    @GetMapping("/page")
    @Operation(summary = "佣金记录分页")
    public Result<PageResult<UnionCommissionRecordVO>> page(@Valid UnionCommissionRecordQuery query) {
        return Result.success(commissionRecordService.getCommissionRecordPage(query));
    }

}
