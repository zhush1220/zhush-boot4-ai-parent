package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionPromotionTemplateParam;
import com.zhush.union.query.UnionPromotionTemplateQuery;
import com.zhush.union.service.UnionPromotionTemplateService;
import com.zhush.union.vo.UnionPromotionTemplateVO;
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
 * @ClassName UnionPromotionTemplateController
 * @Description 推广文案模板表 控制器
 * @Author zhush
 * @Date 2026/08/08
 **/
@RestController
@Tag(name = "管理端-推广文案模板", description = "推广文案模板的增删改查")
@RequestMapping("/admin/union/promotion-template")
public class UnionPromotionTemplateController {

    private final UnionPromotionTemplateService promotionTemplateService;

    public UnionPromotionTemplateController(UnionPromotionTemplateService promotionTemplateService) {
        this.promotionTemplateService = promotionTemplateService;
    }

    /**
     * 新增推广文案模板
     */
    @PostMapping
    @OperateLog(module = "推广文案模板", type = OperateTypeEnum.CREATE)
    @Operation(summary = "新增推广文案模板")
    public Result<Long> create(@RequestBody @Valid UnionPromotionTemplateParam param) {
        return Result.success(promotionTemplateService.createPromotionTemplate(param));
    }

    /**
     * 修改推广文案模板
     */
    @PutMapping
    @OperateLog(module = "推广文案模板", type = OperateTypeEnum.UPDATE)
    @Operation(summary = "修改推广文案模板")
    public Result<Void> update(@RequestBody @Valid UnionPromotionTemplateParam param) {
        promotionTemplateService.updatePromotionTemplate(param);
        return Result.success();
    }

    /**
     * 删除推广文案模板
     */
    @DeleteMapping("/{id}")
    @OperateLog(module = "推广文案模板", type = OperateTypeEnum.DELETE)
    @Operation(summary = "删除推广文案模板")
    public Result<Void> delete(@Parameter(description = "ID") @PathVariable Long id) {
        promotionTemplateService.deletePromotionTemplate(id);
        return Result.success();
    }

    /**
     * 推广文案模板详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "推广文案模板详情")
    public Result<UnionPromotionTemplateVO> get(@Parameter(description = "ID") @PathVariable Long id) {
        return Result.success(promotionTemplateService.getPromotionTemplate(id));
    }

    /**
     * 推广文案模板分页
     */
    @GetMapping("/page")
    @Operation(summary = "推广文案模板分页")
    public Result<PageResult<UnionPromotionTemplateVO>> page(@Valid UnionPromotionTemplateQuery query) {
        return Result.success(promotionTemplateService.getPromotionTemplatePage(query));
    }

}
