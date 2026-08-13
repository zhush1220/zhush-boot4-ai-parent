package com.zhush.union.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zhush.common.constant.CommonConstant;
import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionShortUrlMapParam;
import com.zhush.union.query.UnionShortUrlMapQuery;
import com.zhush.union.service.UnionShortUrlMapService;
import com.zhush.union.vo.UnionShortUrlMapVO;
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
 * @ClassName UnionShortUrlMapController
 * @Description 短链接映射表 控制器
 * @Author zhush
 * @Date 2026/08/08
 **/
@RestController
@Tag(name = "管理端-短链接映射", description = "短链接映射的增删改查")
@RequestMapping("/admin/union/short-url-map")
public class UnionShortUrlMapController {

    private final UnionShortUrlMapService shortUrlMapService;

    public UnionShortUrlMapController(UnionShortUrlMapService shortUrlMapService) {
        this.shortUrlMapService = shortUrlMapService;
    }

    /**
     * 新增短链接映射
     */
    @PostMapping
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:short-url-map:create")
    @OperateLog(module = "短链接映射", type = OperateTypeEnum.CREATE)
    @Operation(summary = "新增短链接映射")
    public Result<Long> create(@RequestBody @Valid UnionShortUrlMapParam param) {
        return Result.success(shortUrlMapService.createShortUrlMap(param));
    }

    /**
     * 修改短链接映射
     */
    @PutMapping
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:short-url-map:update")
    @OperateLog(module = "短链接映射", type = OperateTypeEnum.UPDATE)
    @Operation(summary = "修改短链接映射")
    public Result<Void> update(@RequestBody @Valid UnionShortUrlMapParam param) {
        shortUrlMapService.updateShortUrlMap(param);
        return Result.success();
    }

    /**
     * 删除短链接映射
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:short-url-map:delete")
    @OperateLog(module = "短链接映射", type = OperateTypeEnum.DELETE)
    @Operation(summary = "删除短链接映射")
    public Result<Void> delete(@Parameter(description = "ID") @PathVariable Long id) {
        shortUrlMapService.deleteShortUrlMap(id);
        return Result.success();
    }

    /**
     * 短链接映射详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:short-url-map:query")
    @Operation(summary = "短链接映射详情")
    public Result<UnionShortUrlMapVO> get(@Parameter(description = "ID") @PathVariable Long id) {
        return Result.success(shortUrlMapService.getShortUrlMap(id));
    }

    /**
     * 短链接映射分页
     */
    @GetMapping("/page")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "union:short-url-map:query")
    @Operation(summary = "短链接映射分页")
    public Result<PageResult<UnionShortUrlMapVO>> page(@Valid UnionShortUrlMapQuery query) {
        return Result.success(shortUrlMapService.getShortUrlMapPage(query));
    }

}
