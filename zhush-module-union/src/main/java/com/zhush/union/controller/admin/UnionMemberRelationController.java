package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionMemberRelationParam;
import com.zhush.union.query.UnionMemberRelationQuery;
import com.zhush.union.service.UnionMemberRelationService;
import com.zhush.union.vo.UnionMemberRelationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName UnionMemberRelationController
 * @Description 推广者关系表 控制器
 * @Author zhush
 * @Date 2026/08/07
 **/
@RestController
@Tag(name = "管理端-推广者关系表")
@RequestMapping("/admin/union/member-relation")
public class UnionMemberRelationController {

    private final UnionMemberRelationService memberRelationService;

    public UnionMemberRelationController(UnionMemberRelationService memberRelationService) {
        this.memberRelationService = memberRelationService;
    }

}
