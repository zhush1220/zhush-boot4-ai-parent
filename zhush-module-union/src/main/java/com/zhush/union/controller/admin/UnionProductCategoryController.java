package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionProductCategoryParam;
import com.zhush.union.query.UnionProductCategoryQuery;
import com.zhush.union.service.UnionProductCategoryService;
import com.zhush.union.vo.UnionProductCategoryVO;
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
 * @ClassName UnionProductCategoryController
 * @Description 商品分类关联表 控制器
 * @Author zhush
 * @Date 2026/08/08
 **/
@RestController
@Tag(name = "管理端-商品分类关联", description = "商品分类关联的增删改查")
@RequestMapping("/admin/union/product-category")
public class UnionProductCategoryController {

    private final UnionProductCategoryService productCategoryService;

    public UnionProductCategoryController(UnionProductCategoryService productCategoryService) {
        this.productCategoryService = productCategoryService;
    }

    /**
     * 新增商品分类关联
     */
    @PostMapping
    @OperateLog(module = "商品分类关联", type = OperateTypeEnum.CREATE)
    @Operation(summary = "新增商品分类关联")
    public Result<Long> create(@RequestBody @Valid UnionProductCategoryParam param) {
        return Result.success(productCategoryService.createProductCategory(param));
    }

    /**
     * 修改商品分类关联
     */
    @PutMapping
    @OperateLog(module = "商品分类关联", type = OperateTypeEnum.UPDATE)
    @Operation(summary = "修改商品分类关联")
    public Result<Void> update(@RequestBody @Valid UnionProductCategoryParam param) {
        productCategoryService.updateProductCategory(param);
        return Result.success();
    }

    /**
     * 删除商品分类关联
     */
    @DeleteMapping("/{id}")
    @OperateLog(module = "商品分类关联", type = OperateTypeEnum.DELETE)
    @Operation(summary = "删除商品分类关联")
    public Result<Void> delete(@Parameter(description = "ID") @PathVariable Long id) {
        productCategoryService.deleteProductCategory(id);
        return Result.success();
    }

    /**
     * 商品分类关联详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "商品分类关联详情")
    public Result<UnionProductCategoryVO> get(@Parameter(description = "ID") @PathVariable Long id) {
        return Result.success(productCategoryService.getProductCategory(id));
    }

    /**
     * 商品分类关联分页
     */
    @GetMapping("/page")
    @Operation(summary = "商品分类关联分页")
    public Result<PageResult<UnionProductCategoryVO>> page(@Valid UnionProductCategoryQuery query) {
        return Result.success(productCategoryService.getProductCategoryPage(query));
    }

}
