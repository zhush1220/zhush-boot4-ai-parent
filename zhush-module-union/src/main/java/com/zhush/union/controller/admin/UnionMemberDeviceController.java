package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionMemberDeviceParam;
import com.zhush.union.query.UnionMemberDeviceQuery;
import com.zhush.union.service.UnionMemberDeviceService;
import com.zhush.union.vo.UnionMemberDeviceVO;
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
 * @ClassName UnionMemberDeviceController
 * @Description 推广者设备信息表 控制器
 * @Author zhush
 * @Date 2026/08/08
 **/
@RestController
@Tag(name = "管理端-推广者设备信息", description = "推广者设备信息的增删改查")
@RequestMapping("/admin/union/member-device")
public class UnionMemberDeviceController {

    private final UnionMemberDeviceService memberDeviceService;

    public UnionMemberDeviceController(UnionMemberDeviceService memberDeviceService) {
        this.memberDeviceService = memberDeviceService;
    }

    /**
     * 新增推广者设备信息
     */
    @PostMapping
    @OperateLog(module = "推广者设备信息", type = OperateTypeEnum.CREATE)
    @Operation(summary = "新增推广者设备信息")
    public Result<Long> create(@RequestBody @Valid UnionMemberDeviceParam param) {
        return Result.success(memberDeviceService.createMemberDevice(param));
    }

    /**
     * 修改推广者设备信息
     */
    @PutMapping
    @OperateLog(module = "推广者设备信息", type = OperateTypeEnum.UPDATE)
    @Operation(summary = "修改推广者设备信息")
    public Result<Void> update(@RequestBody @Valid UnionMemberDeviceParam param) {
        memberDeviceService.updateMemberDevice(param);
        return Result.success();
    }

    /**
     * 删除推广者设备信息
     */
    @DeleteMapping("/{id}")
    @OperateLog(module = "推广者设备信息", type = OperateTypeEnum.DELETE)
    @Operation(summary = "删除推广者设备信息")
    public Result<Void> delete(@Parameter(description = "ID") @PathVariable Long id) {
        memberDeviceService.deleteMemberDevice(id);
        return Result.success();
    }

    /**
     * 推广者设备信息详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "推广者设备信息详情")
    public Result<UnionMemberDeviceVO> get(@Parameter(description = "ID") @PathVariable Long id) {
        return Result.success(memberDeviceService.getMemberDevice(id));
    }

    /**
     * 推广者设备信息分页
     */
    @GetMapping("/page")
    @Operation(summary = "推广者设备信息分页")
    public Result<PageResult<UnionMemberDeviceVO>> page(@Valid UnionMemberDeviceQuery query) {
        return Result.success(memberDeviceService.getMemberDevicePage(query));
    }

}
