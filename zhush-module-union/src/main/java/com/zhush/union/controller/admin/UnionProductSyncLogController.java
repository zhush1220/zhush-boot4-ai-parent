package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionProductSyncLogParam;
import com.zhush.union.query.UnionProductSyncLogQuery;
import com.zhush.union.service.UnionProductSyncLogService;
import com.zhush.union.vo.UnionProductSyncLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName UnionProductSyncLogController
 * @Description 商品同步日志表 控制器
 * @Author zhush
 * @Date 2026/08/07
 **/
@RestController
@Tag(name = "管理端-商品同步日志表")
@RequestMapping("/admin/union/product-sync-log")
public class UnionProductSyncLogController {

    private final UnionProductSyncLogService productSyncLogService;

    public UnionProductSyncLogController(UnionProductSyncLogService productSyncLogService) {
        this.productSyncLogService = productSyncLogService;
    }

}
