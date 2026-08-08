package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionMemberTeamSnapshotParam;
import com.zhush.union.query.UnionMemberTeamSnapshotQuery;
import com.zhush.union.service.UnionMemberTeamSnapshotService;
import com.zhush.union.vo.UnionMemberTeamSnapshotVO;
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
 * @ClassName UnionMemberTeamSnapshotController
 * @Description 推广者团队快照表 控制器
 * @Author zhush
 * @Date 2026/08/08
 **/
@RestController
@Tag(name = "管理端-推广者团队快照", description = "推广者团队快照的增删改查")
@RequestMapping("/admin/union/member-team-snapshot")
public class UnionMemberTeamSnapshotController {

    private final UnionMemberTeamSnapshotService memberTeamSnapshotService;

    public UnionMemberTeamSnapshotController(UnionMemberTeamSnapshotService memberTeamSnapshotService) {
        this.memberTeamSnapshotService = memberTeamSnapshotService;
    }

    /**
     * 新增推广者团队快照
     */
    @PostMapping
    @OperateLog(module = "推广者团队快照", type = OperateTypeEnum.CREATE)
    @Operation(summary = "新增推广者团队快照")
    public Result<Long> create(@RequestBody @Valid UnionMemberTeamSnapshotParam param) {
        return Result.success(memberTeamSnapshotService.createMemberTeamSnapshot(param));
    }

    /**
     * 修改推广者团队快照
     */
    @PutMapping
    @OperateLog(module = "推广者团队快照", type = OperateTypeEnum.UPDATE)
    @Operation(summary = "修改推广者团队快照")
    public Result<Void> update(@RequestBody @Valid UnionMemberTeamSnapshotParam param) {
        memberTeamSnapshotService.updateMemberTeamSnapshot(param);
        return Result.success();
    }

    /**
     * 删除推广者团队快照
     */
    @DeleteMapping("/{id}")
    @OperateLog(module = "推广者团队快照", type = OperateTypeEnum.DELETE)
    @Operation(summary = "删除推广者团队快照")
    public Result<Void> delete(@Parameter(description = "ID") @PathVariable Long id) {
        memberTeamSnapshotService.deleteMemberTeamSnapshot(id);
        return Result.success();
    }

    /**
     * 推广者团队快照详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "推广者团队快照详情")
    public Result<UnionMemberTeamSnapshotVO> get(@Parameter(description = "ID") @PathVariable Long id) {
        return Result.success(memberTeamSnapshotService.getMemberTeamSnapshot(id));
    }

    /**
     * 推广者团队快照分页
     */
    @GetMapping("/page")
    @Operation(summary = "推广者团队快照分页")
    public Result<PageResult<UnionMemberTeamSnapshotVO>> page(@Valid UnionMemberTeamSnapshotQuery query) {
        return Result.success(memberTeamSnapshotService.getMemberTeamSnapshotPage(query));
    }

}
