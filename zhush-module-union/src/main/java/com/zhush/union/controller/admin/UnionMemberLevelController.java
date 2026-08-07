package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionMemberLevelParam;
import com.zhush.union.query.UnionMemberLevelQuery;
import com.zhush.union.service.UnionMemberLevelService;
import com.zhush.union.vo.UnionMemberLevelVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName UnionMemberLevelController
 * @Description 推广者等级配置表 控制器
 * @Author zhush
 * @Date 2026/08/07
 **/
@RestController
@Tag(name = "管理端-推广者等级配置表")
@RequestMapping("/admin/union/member-level")
public class UnionMemberLevelController {

    private final UnionMemberLevelService memberLevelService;

    public UnionMemberLevelController(UnionMemberLevelService memberLevelService) {
        this.memberLevelService = memberLevelService;
    }

}
