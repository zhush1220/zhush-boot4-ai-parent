package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionWithdrawConfigParam;
import com.zhush.union.query.UnionWithdrawConfigQuery;
import com.zhush.union.service.UnionWithdrawConfigService;
import com.zhush.union.vo.UnionWithdrawConfigVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName UnionWithdrawConfigController
 * @Description 提现配置表 控制器
 * @Author zhush
 * @Date 2026/08/07
 **/
@RestController
@Tag(name = "管理端-提现配置表")
@RequestMapping("/admin/union/withdraw-config")
public class UnionWithdrawConfigController {

    private final UnionWithdrawConfigService withdrawConfigService;

    public UnionWithdrawConfigController(UnionWithdrawConfigService withdrawConfigService) {
        this.withdrawConfigService = withdrawConfigService;
    }

}
