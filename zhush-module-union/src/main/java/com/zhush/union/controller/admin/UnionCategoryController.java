package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionCategoryParam;
import com.zhush.union.query.UnionCategoryQuery;
import com.zhush.union.service.UnionCategoryService;
import com.zhush.union.vo.UnionCategoryVO;
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
 * @ClassName UnionCategoryController
 * @Description 商品分类表 控制器
 * @Author zhush
 * @Date 2026/08/08
 **/
@RestController
@Tag(name = "管理端-商品分类", description = "商品分类的增删改查")
@RequestMapping("/admin/union/category")
public class UnionCategoryController {

    private final UnionCategoryService categoryService;

    public UnionCategoryController(UnionCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * 新增商品分类
     */
    @PostMapping
    @OperateLog(module = "商品分类", type = OperateTypeEnum.CREATE)
    @Operation(summary = "新增商品分类")
    public Result<Long> create(@RequestBody @Valid UnionCategoryParam param) {
        return Result.success(categoryService.createCategory(param));
    }

    /**
     * 修改商品分类
     */
    @PutMapping
    @OperateLog(module = "商品分类", type = OperateTypeEnum.UPDATE)
    @Operation(summary = "修改商品分类")
    public Result<Void> update(@RequestBody @Valid UnionCategoryParam param) {
        categoryService.updateCategory(param);
        return Result.success();
    }

    /**
     * 删除商品分类
     */
    @DeleteMapping("/{id}")
    @OperateLog(module = "商品分类", type = OperateTypeEnum.DELETE)
    @Operation(summary = "删除商品分类")
    public Result<Void> delete(@Parameter(description = "ID") @PathVariable Long id) {
        categoryService.deleteCategory(id);
        return Result.success();
    }

    /**
     * 商品分类详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "商品分类详情")
    public Result<UnionCategoryVO> get(@Parameter(description = "ID") @PathVariable Long id) {
        return Result.success(categoryService.getCategory(id));
    }

    /**
     * 商品分类分页
     */
    @GetMapping("/page")
    @Operation(summary = "商品分类分页")
    public Result<PageResult<UnionCategoryVO>> page(@Valid UnionCategoryQuery query) {
        return Result.success(categoryService.getCategoryPage(query));
    }

}
