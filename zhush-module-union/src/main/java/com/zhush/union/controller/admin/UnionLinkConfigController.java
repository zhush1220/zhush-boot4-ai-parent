package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionLinkConfigParam;
import com.zhush.union.query.UnionLinkConfigQuery;
import com.zhush.union.service.UnionLinkConfigService;
import com.zhush.union.vo.UnionLinkConfigVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName UnionLinkConfigController
 * @Description 转链配置表 控制器
 * @Author zhush
 * @Date 2026/08/07
 **/
@RestController
@Tag(name = "管理端-转链配置表")
@RequestMapping("/admin/union/link-config")
public class UnionLinkConfigController {

    private final UnionLinkConfigService linkConfigService;

    public UnionLinkConfigController(UnionLinkConfigService linkConfigService) {
        this.linkConfigService = linkConfigService;
    }

}
