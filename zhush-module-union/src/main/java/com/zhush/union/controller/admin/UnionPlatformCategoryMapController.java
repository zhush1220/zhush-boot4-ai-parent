package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionPlatformCategoryMapParam;
import com.zhush.union.query.UnionPlatformCategoryMapQuery;
import com.zhush.union.service.UnionPlatformCategoryMapService;
import com.zhush.union.vo.UnionPlatformCategoryMapVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName UnionPlatformCategoryMapController
 * @Description 平台类目映射表 控制器
 * @Author zhush
 * @Date 2026/08/07
 **/
@RestController
@Tag(name = "管理端-平台类目映射表")
@RequestMapping("/admin/union/platform-category-map")
public class UnionPlatformCategoryMapController {

    private final UnionPlatformCategoryMapService platformCategoryMapService;

    public UnionPlatformCategoryMapController(UnionPlatformCategoryMapService platformCategoryMapService) {
        this.platformCategoryMapService = platformCategoryMapService;
    }

}
