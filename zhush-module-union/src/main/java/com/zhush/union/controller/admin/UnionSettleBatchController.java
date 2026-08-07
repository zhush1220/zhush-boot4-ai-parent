package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionSettleBatchParam;
import com.zhush.union.query.UnionSettleBatchQuery;
import com.zhush.union.service.UnionSettleBatchService;
import com.zhush.union.vo.UnionSettleBatchVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName UnionSettleBatchController
 * @Description 结算批次表 控制器
 * @Author zhush
 * @Date 2026/08/07
 **/
@RestController
@Tag(name = "管理端-结算批次表")
@RequestMapping("/admin/union/settle-batch")
public class UnionSettleBatchController {

    private final UnionSettleBatchService settleBatchService;

    public UnionSettleBatchController(UnionSettleBatchService settleBatchService) {
        this.settleBatchService = settleBatchService;
    }

}
