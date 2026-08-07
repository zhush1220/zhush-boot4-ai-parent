package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionProductStatusLogParam;
import com.zhush.union.query.UnionProductStatusLogQuery;
import com.zhush.union.service.UnionProductStatusLogService;
import com.zhush.union.vo.UnionProductStatusLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName UnionProductStatusLogController
 * @Description 商品状态变更日志表 控制器
 * @Author zhush
 * @Date 2026/08/07
 **/
@RestController
@Tag(name = "管理端-商品状态变更日志表")
@RequestMapping("/admin/union/product-status-log")
public class UnionProductStatusLogController {

    private final UnionProductStatusLogService productStatusLogService;

    public UnionProductStatusLogController(UnionProductStatusLogService productStatusLogService) {
        this.productStatusLogService = productStatusLogService;
    }

}
