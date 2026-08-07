package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionProductCategoryParam;
import com.zhush.union.query.UnionProductCategoryQuery;
import com.zhush.union.service.UnionProductCategoryService;
import com.zhush.union.vo.UnionProductCategoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName UnionProductCategoryController
 * @Description 商品分类关联表 控制器
 * @Author zhush
 * @Date 2026/08/07
 **/
@RestController
@Tag(name = "管理端-商品分类关联表")
@RequestMapping("/admin/union/product-category")
public class UnionProductCategoryController {

    private final UnionProductCategoryService productCategoryService;

    public UnionProductCategoryController(UnionProductCategoryService productCategoryService) {
        this.productCategoryService = productCategoryService;
    }

}
