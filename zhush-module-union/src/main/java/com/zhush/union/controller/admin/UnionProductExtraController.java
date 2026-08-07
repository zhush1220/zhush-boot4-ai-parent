package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionProductExtraParam;
import com.zhush.union.query.UnionProductExtraQuery;
import com.zhush.union.service.UnionProductExtraService;
import com.zhush.union.vo.UnionProductExtraVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName UnionProductExtraController
 * @Description 商品扩展信息表 控制器
 * @Author zhush
 * @Date 2026/08/07
 **/
@RestController
@Tag(name = "管理端-商品扩展信息表")
@RequestMapping("/admin/union/product-extra")
public class UnionProductExtraController {

    private final UnionProductExtraService productExtraService;

    public UnionProductExtraController(UnionProductExtraService productExtraService) {
        this.productExtraService = productExtraService;
    }

}
