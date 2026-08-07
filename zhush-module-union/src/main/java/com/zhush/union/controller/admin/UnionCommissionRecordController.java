package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionCommissionRecordParam;
import com.zhush.union.query.UnionCommissionRecordQuery;
import com.zhush.union.service.UnionCommissionRecordService;
import com.zhush.union.vo.UnionCommissionRecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName UnionCommissionRecordController
 * @Description 佣金记录表 控制器
 * @Author zhush
 * @Date 2026/08/07
 **/
@RestController
@Tag(name = "管理端-佣金记录表")
@RequestMapping("/admin/union/commission-record")
public class UnionCommissionRecordController {

    private final UnionCommissionRecordService commissionRecordService;

    public UnionCommissionRecordController(UnionCommissionRecordService commissionRecordService) {
        this.commissionRecordService = commissionRecordService;
    }

}
