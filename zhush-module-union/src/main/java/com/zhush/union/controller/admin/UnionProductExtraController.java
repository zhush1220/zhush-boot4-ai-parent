package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionProductExtraParam;
import com.zhush.union.query.UnionProductExtraQuery;
import com.zhush.union.service.UnionProductExtraService;
import com.zhush.union.vo.UnionProductExtraVO;
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
 * @ClassName UnionProductExtraController
 * @Description 商品扩展信息表 控制器
 * @Author zhush
 * @Date 2026/08/08
 **/
@RestController
@Tag(name = "管理端-商品扩展信息", description = "商品扩展信息的增删改查")
@RequestMapping("/admin/union/product-extra")
public class UnionProductExtraController {

    private final UnionProductExtraService productExtraService;

    public UnionProductExtraController(UnionProductExtraService productExtraService) {
        this.productExtraService = productExtraService;
    }

    /**
     * 新增商品扩展信息
     */
    @PostMapping
    @OperateLog(module = "商品扩展信息", type = OperateTypeEnum.CREATE)
    @Operation(summary = "新增商品扩展信息")
    public Result<Long> create(@RequestBody @Valid UnionProductExtraParam param) {
        return Result.success(productExtraService.createProductExtra(param));
    }

    /**
     * 修改商品扩展信息
     */
    @PutMapping
    @OperateLog(module = "商品扩展信息", type = OperateTypeEnum.UPDATE)
    @Operation(summary = "修改商品扩展信息")
    public Result<Void> update(@RequestBody @Valid UnionProductExtraParam param) {
        productExtraService.updateProductExtra(param);
        return Result.success();
    }

    /**
     * 删除商品扩展信息
     */
    @DeleteMapping("/{id}")
    @OperateLog(module = "商品扩展信息", type = OperateTypeEnum.DELETE)
    @Operation(summary = "删除商品扩展信息")
    public Result<Void> delete(@Parameter(description = "ID") @PathVariable Long id) {
        productExtraService.deleteProductExtra(id);
        return Result.success();
    }

    /**
     * 商品扩展信息详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "商品扩展信息详情")
    public Result<UnionProductExtraVO> get(@Parameter(description = "ID") @PathVariable Long id) {
        return Result.success(productExtraService.getProductExtra(id));
    }

    /**
     * 商品扩展信息分页
     */
    @GetMapping("/page")
    @Operation(summary = "商品扩展信息分页")
    public Result<PageResult<UnionProductExtraVO>> page(@Valid UnionProductExtraQuery query) {
        return Result.success(productExtraService.getProductExtraPage(query));
    }

}
