package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionClickTrackingParam;
import com.zhush.union.query.UnionClickTrackingQuery;
import com.zhush.union.service.UnionClickTrackingService;
import com.zhush.union.vo.UnionClickTrackingVO;
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
 * @ClassName UnionClickTrackingController
 * @Description 推广链接点击明细表 控制器
 * @Author zhush
 * @Date 2026/08/08
 **/
@RestController
@Tag(name = "管理端-推广链接点击明细", description = "推广链接点击明细的增删改查")
@RequestMapping("/admin/union/click-tracking")
public class UnionClickTrackingController {

    private final UnionClickTrackingService clickTrackingService;

    public UnionClickTrackingController(UnionClickTrackingService clickTrackingService) {
        this.clickTrackingService = clickTrackingService;
    }

    /**
     * 新增推广链接点击明细
     */
    @PostMapping
    @OperateLog(module = "推广链接点击明细", type = OperateTypeEnum.CREATE)
    @Operation(summary = "新增推广链接点击明细")
    public Result<Long> create(@RequestBody @Valid UnionClickTrackingParam param) {
        return Result.success(clickTrackingService.createClickTracking(param));
    }

    /**
     * 修改推广链接点击明细
     */
    @PutMapping
    @OperateLog(module = "推广链接点击明细", type = OperateTypeEnum.UPDATE)
    @Operation(summary = "修改推广链接点击明细")
    public Result<Void> update(@RequestBody @Valid UnionClickTrackingParam param) {
        clickTrackingService.updateClickTracking(param);
        return Result.success();
    }

    /**
     * 删除推广链接点击明细
     */
    @DeleteMapping("/{id}")
    @OperateLog(module = "推广链接点击明细", type = OperateTypeEnum.DELETE)
    @Operation(summary = "删除推广链接点击明细")
    public Result<Void> delete(@Parameter(description = "ID") @PathVariable Long id) {
        clickTrackingService.deleteClickTracking(id);
        return Result.success();
    }

    /**
     * 推广链接点击明细详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "推广链接点击明细详情")
    public Result<UnionClickTrackingVO> get(@Parameter(description = "ID") @PathVariable Long id) {
        return Result.success(clickTrackingService.getClickTracking(id));
    }

    /**
     * 推广链接点击明细分页
     */
    @GetMapping("/page")
    @Operation(summary = "推广链接点击明细分页")
    public Result<PageResult<UnionClickTrackingVO>> page(@Valid UnionClickTrackingQuery query) {
        return Result.success(clickTrackingService.getClickTrackingPage(query));
    }

}
