package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionRiskExecutionLogParam;
import com.zhush.union.query.UnionRiskExecutionLogQuery;
import com.zhush.union.service.UnionRiskExecutionLogService;
import com.zhush.union.vo.UnionRiskExecutionLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName UnionRiskExecutionLogController
 * @Description 风控执行记录表 控制器
 * @Author zhush
 * @Date 2026/08/07
 **/
@RestController
@Tag(name = "管理端-风控执行记录表")
@RequestMapping("/admin/union/risk-execution-log")
public class UnionRiskExecutionLogController {

    private final UnionRiskExecutionLogService riskExecutionLogService;

    public UnionRiskExecutionLogController(UnionRiskExecutionLogService riskExecutionLogService) {
        this.riskExecutionLogService = riskExecutionLogService;
    }

}
