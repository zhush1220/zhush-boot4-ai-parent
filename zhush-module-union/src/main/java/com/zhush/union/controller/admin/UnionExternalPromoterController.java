package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionExternalPromoterParam;
import com.zhush.union.query.UnionExternalPromoterQuery;
import com.zhush.union.service.UnionExternalPromoterService;
import com.zhush.union.vo.UnionExternalPromoterVO;
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
 * @ClassName UnionExternalPromoterController
 * @Description 外部推广者/影子推广者表 控制器
 * @Author zhush
 * @Date 2026/08/08
 **/
@RestController
@Tag(name = "管理端-外部推广者/影子推广者", description = "外部推广者/影子推广者的增删改查")
@RequestMapping("/admin/union/external-promoter")
public class UnionExternalPromoterController {

    private final UnionExternalPromoterService externalPromoterService;

    public UnionExternalPromoterController(UnionExternalPromoterService externalPromoterService) {
        this.externalPromoterService = externalPromoterService;
    }

    /**
     * 新增外部推广者/影子推广者
     */
    @PostMapping
    @OperateLog(module = "外部推广者/影子推广者", type = OperateTypeEnum.CREATE)
    @Operation(summary = "新增外部推广者/影子推广者")
    public Result<Long> create(@RequestBody @Valid UnionExternalPromoterParam param) {
        return Result.success(externalPromoterService.createExternalPromoter(param));
    }

    /**
     * 修改外部推广者/影子推广者
     */
    @PutMapping
    @OperateLog(module = "外部推广者/影子推广者", type = OperateTypeEnum.UPDATE)
    @Operation(summary = "修改外部推广者/影子推广者")
    public Result<Void> update(@RequestBody @Valid UnionExternalPromoterParam param) {
        externalPromoterService.updateExternalPromoter(param);
        return Result.success();
    }

    /**
     * 删除外部推广者/影子推广者
     */
    @DeleteMapping("/{id}")
    @OperateLog(module = "外部推广者/影子推广者", type = OperateTypeEnum.DELETE)
    @Operation(summary = "删除外部推广者/影子推广者")
    public Result<Void> delete(@Parameter(description = "ID") @PathVariable Long id) {
        externalPromoterService.deleteExternalPromoter(id);
        return Result.success();
    }

    /**
     * 外部推广者/影子推广者详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "外部推广者/影子推广者详情")
    public Result<UnionExternalPromoterVO> get(@Parameter(description = "ID") @PathVariable Long id) {
        return Result.success(externalPromoterService.getExternalPromoter(id));
    }

    /**
     * 外部推广者/影子推广者分页
     */
    @GetMapping("/page")
    @Operation(summary = "外部推广者/影子推广者分页")
    public Result<PageResult<UnionExternalPromoterVO>> page(@Valid UnionExternalPromoterQuery query) {
        return Result.success(externalPromoterService.getExternalPromoterPage(query));
    }

}
