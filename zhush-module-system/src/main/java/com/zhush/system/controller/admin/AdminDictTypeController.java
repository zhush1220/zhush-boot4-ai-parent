package com.zhush.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zhush.common.constant.CommonConstant;
import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.protection.idempotent.annotation.Idempotent;
import com.zhush.system.param.AdminDictTypeParam;
import com.zhush.system.query.AdminDictTypeQuery;
import com.zhush.system.service.AdminDictTypeService;
import com.zhush.system.vo.AdminDictTypeVO;
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

import java.util.List;

/**
 * 字典类型管理接口
 *
 * @author zhush
 */
@RestController
@Tag(name = "管理端-字典类型管理", description = "字典类型的增删改查")
@RequestMapping("/admin/system/dict-type")
public class AdminDictTypeController {

    private final AdminDictTypeService dictTypeService;

    public AdminDictTypeController(AdminDictTypeService dictTypeService) {
        this.dictTypeService = dictTypeService;
    }

    /**
     * 新增字典类型
     */
    @PostMapping
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:dict-type:create")
    @Idempotent
    @Operation(summary = "新增字典类型")
    public Result<Long> create(@RequestBody @Valid AdminDictTypeParam param) {
        return Result.success(dictTypeService.createDictType(param));
    }

    /**
     * 修改字典类型
     */
    @PutMapping
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:dict-type:update")
    @Idempotent
    @Operation(summary = "修改字典类型")
    public Result<Void> update(@RequestBody @Valid AdminDictTypeParam param) {
        dictTypeService.updateDictType(param);
        return Result.success();
    }

    /**
     * 删除字典类型
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:dict-type:delete")
    @Idempotent
    @Operation(summary = "删除字典类型")
    public Result<Void> delete(@Parameter(description = "字典类型ID") @PathVariable Long id) {
        dictTypeService.deleteDictType(id);
        return Result.success();
    }

    /**
     * 批量删除字典类型
     */
    @DeleteMapping("/batch")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:dict-type:delete")
    @Idempotent
    @Operation(summary = "批量删除字典类型")
    public Result<Void> deleteBatch(@Parameter(description = "字典类型ID列表") @RequestBody List<Long> ids) {
        dictTypeService.deleteDictTypeBatch(ids);
        return Result.success();
    }

    /**
     * 字典类型详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:dict-type:query")
    @Operation(summary = "字典类型详情")
    public Result<AdminDictTypeVO> get(@Parameter(description = "字典类型ID") @PathVariable Long id) {
        return Result.success(dictTypeService.getDictType(id));
    }

    /**
     * 字典类型分页
     */
    @GetMapping("/page")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:dict-type:query")
    @Operation(summary = "字典类型分页")
    public Result<PageResult<AdminDictTypeVO>> page(@Valid AdminDictTypeQuery query) {
        return Result.success(dictTypeService.getDictTypePage(query));
    }
}
