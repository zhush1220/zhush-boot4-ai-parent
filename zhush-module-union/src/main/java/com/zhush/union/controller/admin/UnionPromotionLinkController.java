package com.zhush.union.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zhush.common.constant.CommonConstant;
import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionPromotionLinkParam;
import com.zhush.union.query.UnionPromotionLinkQuery;
import com.zhush.union.service.UnionPromotionLinkService;
import com.zhush.union.vo.UnionPromotionLinkVO;
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
 * @ClassName UnionPromotionLinkController
 * @Description 推广链接表 控制器
 * @Author zhush
 * @Date 2026/08/08
 **/
@RestController
@Tag(name = "管理端-推广链接", description = "推广链接的增删改查")
@RequestMapping("/admin/union/promotion-link")
public class UnionPromotionLinkController {

    private final UnionPromotionLinkService promotionLinkService;

    public UnionPromotionLinkController(UnionPromotionLinkService promotionLinkService) {
        this.promotionLinkService = promotionLinkService;
    }

    /**
     * 新增推广链接
     */
    @PostMapping
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:promotion-link:create")
    @OperateLog(module = "推广链接", type = OperateTypeEnum.CREATE)
    @Operation(summary = "新增推广链接")
    public Result<Long> create(@RequestBody @Valid UnionPromotionLinkParam param) {
        return Result.success(promotionLinkService.createPromotionLink(param));
    }

    /**
     * 修改推广链接
     */
    @PutMapping
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:promotion-link:update")
    @OperateLog(module = "推广链接", type = OperateTypeEnum.UPDATE)
    @Operation(summary = "修改推广链接")
    public Result<Void> update(@RequestBody @Valid UnionPromotionLinkParam param) {
        promotionLinkService.updatePromotionLink(param);
        return Result.success();
    }

    /**
     * 删除推广链接
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:promotion-link:delete")
    @OperateLog(module = "推广链接", type = OperateTypeEnum.DELETE)
    @Operation(summary = "删除推广链接")
    public Result<Void> delete(@Parameter(description = "ID") @PathVariable Long id) {
        promotionLinkService.deletePromotionLink(id);
        return Result.success();
    }

    /**
     * 推广链接详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:promotion-link:query")
    @Operation(summary = "推广链接详情")
    public Result<UnionPromotionLinkVO> get(@Parameter(description = "ID") @PathVariable Long id) {
        return Result.success(promotionLinkService.getPromotionLink(id));
    }

    /**
     * 推广链接分页
     */
    @GetMapping("/page")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:promotion-link:query")
    @Operation(summary = "推广链接分页")
    public Result<PageResult<UnionPromotionLinkVO>> page(@Valid UnionPromotionLinkQuery query) {
        return Result.success(promotionLinkService.getPromotionLinkPage(query));
    }

}
