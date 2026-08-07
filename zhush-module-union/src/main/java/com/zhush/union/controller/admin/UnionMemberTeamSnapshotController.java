package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionMemberTeamSnapshotParam;
import com.zhush.union.query.UnionMemberTeamSnapshotQuery;
import com.zhush.union.service.UnionMemberTeamSnapshotService;
import com.zhush.union.vo.UnionMemberTeamSnapshotVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName UnionMemberTeamSnapshotController
 * @Description 推广者团队快照表 控制器
 * @Author zhush
 * @Date 2026/08/07
 **/
@RestController
@Tag(name = "管理端-推广者团队快照表")
@RequestMapping("/admin/union/member-team-snapshot")
public class UnionMemberTeamSnapshotController {

    private final UnionMemberTeamSnapshotService memberTeamSnapshotService;

    public UnionMemberTeamSnapshotController(UnionMemberTeamSnapshotService memberTeamSnapshotService) {
        this.memberTeamSnapshotService = memberTeamSnapshotService;
    }

}
