package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionAutoAuditConfigParam;
import com.zhush.union.query.UnionAutoAuditConfigQuery;
import com.zhush.union.service.UnionAutoAuditConfigService;
import com.zhush.union.vo.UnionAutoAuditConfigVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName UnionAutoAuditConfigController
 * @Description 自动审核配置表 控制器
 * @Author zhush
 * @Date 2026/08/07
 **/
@RestController
@Tag(name = "管理端-自动审核配置表")
@RequestMapping("/admin/union/auto-audit-config")
public class UnionAutoAuditConfigController {

    private final UnionAutoAuditConfigService autoAuditConfigService;

    public UnionAutoAuditConfigController(UnionAutoAuditConfigService autoAuditConfigService) {
        this.autoAuditConfigService = autoAuditConfigService;
    }

}
