package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionLevelConfigParam;
import com.zhush.union.query.UnionLevelConfigQuery;
import com.zhush.union.service.UnionLevelConfigService;
import com.zhush.union.vo.UnionLevelConfigVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName UnionLevelConfigController
 * @Description 层级佣金配置表 控制器
 * @Author zhush
 * @Date 2026/08/07
 **/
@RestController
@Tag(name = "管理端-层级佣金配置表")
@RequestMapping("/admin/union/level-config")
public class UnionLevelConfigController {

    private final UnionLevelConfigService levelConfigService;

    public UnionLevelConfigController(UnionLevelConfigService levelConfigService) {
        this.levelConfigService = levelConfigService;
    }

}
