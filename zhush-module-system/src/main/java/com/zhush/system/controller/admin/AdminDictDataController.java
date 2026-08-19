package com.zhush.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zhush.common.constant.CommonConstant;
import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.protection.idempotent.annotation.Idempotent;
import com.zhush.system.param.AdminDictDataParam;
import com.zhush.system.query.AdminDictDataQuery;
import com.zhush.system.service.AdminDictDataService;
import com.zhush.system.vo.AdminDictDataVO;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 字典数据管理接口
 *
 * @author zhush
 */
@RestController
@Tag(name = "管理端-字典数据管理", description = "字典数据的增删改查、按类型取数")
@RequestMapping("/admin/system/dict-data")
public class AdminDictDataController {

    private final AdminDictDataService dictDataService;

    public AdminDictDataController(AdminDictDataService dictDataService) {
        this.dictDataService = dictDataService;
    }

    /**
     * 新增字典数据
     */
    @PostMapping
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:dict-data:create")
    @Idempotent
    @Operation(summary = "新增字典数据")
    public Result<Long> create(@RequestBody @Valid AdminDictDataParam param) {
        return Result.success(dictDataService.createDictData(param));
    }

    /**
     * 修改字典数据
     */
    @PutMapping
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:dict-data:update")
    @Idempotent
    @Operation(summary = "修改字典数据")
    public Result<Void> update(@RequestBody @Valid AdminDictDataParam param) {
        dictDataService.updateDictData(param);
        return Result.success();
    }

    /**
     * 删除字典数据
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:dict-data:delete")
    @Idempotent
    @Operation(summary = "删除字典数据")
    public Result<Void> delete(@Parameter(description = "字典数据ID") @PathVariable Long id) {
        dictDataService.deleteDictData(id);
        return Result.success();
    }

    /**
     * 字典数据详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:dict-data:query")
    @Operation(summary = "字典数据详情")
    public Result<AdminDictDataVO> get(@Parameter(description = "字典数据ID") @PathVariable Long id) {
        return Result.success(dictDataService.getDictData(id));
    }

    /**
     * 字典数据分页
     */
    @GetMapping("/page")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:dict-data:query")
    @Operation(summary = "字典数据分页")
    public Result<PageResult<AdminDictDataVO>> page(@Valid AdminDictDataQuery query) {
        return Result.success(dictDataService.getDictDataPage(query));
    }

    /**
     * 按字典类型编码查询启用中的字典数据（下拉框等公共取数入口，走 Redis 缓存）
     */
    @GetMapping("/list")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:dict-data:query")
    @Operation(summary = "按字典类型编码查询字典数据（启用中）")
    public Result<List<AdminDictDataVO>> list(@Parameter(description = "字典类型编码")
                                              @RequestParam String dictType) {
        return Result.success(dictDataService.getDictDataList(dictType));
    }
}
