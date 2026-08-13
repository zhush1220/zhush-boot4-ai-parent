package com.zhush.union.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zhush.common.constant.CommonConstant;
import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionPlatformCategoryMapParam;
import com.zhush.union.query.UnionPlatformCategoryMapQuery;
import com.zhush.union.service.UnionPlatformCategoryMapService;
import com.zhush.union.vo.UnionPlatformCategoryMapVO;
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
 * @ClassName UnionPlatformCategoryMapController
 * @Description 平台类目映射表 控制器
 * @Author zhush
 * @Date 2026/08/08
 **/
@RestController
@Tag(name = "管理端-平台类目映射", description = "平台类目映射的增删改查")
@RequestMapping("/admin/union/platform-category-map")
public class UnionPlatformCategoryMapController {

    private final UnionPlatformCategoryMapService platformCategoryMapService;

    public UnionPlatformCategoryMapController(UnionPlatformCategoryMapService platformCategoryMapService) {
        this.platformCategoryMapService = platformCategoryMapService;
    }

    /**
     * 新增平台类目映射
     */
    @PostMapping
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:platform-category-map:create")
    @OperateLog(module = "平台类目映射", type = OperateTypeEnum.CREATE)
    @Operation(summary = "新增平台类目映射")
    public Result<Long> create(@RequestBody @Valid UnionPlatformCategoryMapParam param) {
        return Result.success(platformCategoryMapService.createPlatformCategoryMap(param));
    }

    /**
     * 修改平台类目映射
     */
    @PutMapping
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:platform-category-map:update")
    @OperateLog(module = "平台类目映射", type = OperateTypeEnum.UPDATE)
    @Operation(summary = "修改平台类目映射")
    public Result<Void> update(@RequestBody @Valid UnionPlatformCategoryMapParam param) {
        platformCategoryMapService.updatePlatformCategoryMap(param);
        return Result.success();
    }

    /**
     * 删除平台类目映射
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:platform-category-map:delete")
    @OperateLog(module = "平台类目映射", type = OperateTypeEnum.DELETE)
    @Operation(summary = "删除平台类目映射")
    public Result<Void> delete(@Parameter(description = "ID") @PathVariable Long id) {
        platformCategoryMapService.deletePlatformCategoryMap(id);
        return Result.success();
    }

    /**
     * 平台类目映射详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:platform-category-map:query")
    @Operation(summary = "平台类目映射详情")
    public Result<UnionPlatformCategoryMapVO> get(@Parameter(description = "ID") @PathVariable Long id) {
        return Result.success(platformCategoryMapService.getPlatformCategoryMap(id));
    }

    /**
     * 平台类目映射分页
     */
    @GetMapping("/page")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:platform-category-map:query")
    @Operation(summary = "平台类目映射分页")
    public Result<PageResult<UnionPlatformCategoryMapVO>> page(@Valid UnionPlatformCategoryMapQuery query) {
        return Result.success(platformCategoryMapService.getPlatformCategoryMapPage(query));
    }

}
