package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionProductParam;
import com.zhush.union.query.UnionProductQuery;
import com.zhush.union.service.UnionProductService;
import com.zhush.union.vo.UnionProductVO;
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
 * @ClassName UnionProductController
 * @Description 联盟商品主表 控制器
 * @Author zhush
 * @Date 2026/08/08
 **/
@RestController
@Tag(name = "管理端-联盟商品主", description = "联盟商品主的增删改查")
@RequestMapping("/admin/union/product")
public class UnionProductController {

    private final UnionProductService productService;

    public UnionProductController(UnionProductService productService) {
        this.productService = productService;
    }

    /**
     * 新增联盟商品主
     */
    @PostMapping
    @OperateLog(module = "联盟商品主", type = OperateTypeEnum.CREATE)
    @Operation(summary = "新增联盟商品主")
    public Result<Long> create(@RequestBody @Valid UnionProductParam param) {
        return Result.success(productService.createProduct(param));
    }

    /**
     * 修改联盟商品主
     */
    @PutMapping
    @OperateLog(module = "联盟商品主", type = OperateTypeEnum.UPDATE)
    @Operation(summary = "修改联盟商品主")
    public Result<Void> update(@RequestBody @Valid UnionProductParam param) {
        productService.updateProduct(param);
        return Result.success();
    }

    /**
     * 删除联盟商品主
     */
    @DeleteMapping("/{id}")
    @OperateLog(module = "联盟商品主", type = OperateTypeEnum.DELETE)
    @Operation(summary = "删除联盟商品主")
    public Result<Void> delete(@Parameter(description = "ID") @PathVariable Long id) {
        productService.deleteProduct(id);
        return Result.success();
    }

    /**
     * 联盟商品主详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "联盟商品主详情")
    public Result<UnionProductVO> get(@Parameter(description = "ID") @PathVariable Long id) {
        return Result.success(productService.getProduct(id));
    }

    /**
     * 联盟商品主分页
     */
    @GetMapping("/page")
    @Operation(summary = "联盟商品主分页")
    public Result<PageResult<UnionProductVO>> page(@Valid UnionProductQuery query) {
        return Result.success(productService.getProductPage(query));
    }

}
