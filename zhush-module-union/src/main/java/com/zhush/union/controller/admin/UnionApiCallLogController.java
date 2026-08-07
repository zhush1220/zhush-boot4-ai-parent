package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionApiCallLogParam;
import com.zhush.union.query.UnionApiCallLogQuery;
import com.zhush.union.service.UnionApiCallLogService;
import com.zhush.union.vo.UnionApiCallLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName UnionApiCallLogController
 * @Description 平台API调用日志表 控制器
 * @Author zhush
 * @Date 2026/08/07
 **/
@RestController
@Tag(name = "管理端-平台API调用日志表")
@RequestMapping("/admin/union/api-call-log")
public class UnionApiCallLogController {

    private final UnionApiCallLogService apiCallLogService;

    public UnionApiCallLogController(UnionApiCallLogService apiCallLogService) {
        this.apiCallLogService = apiCallLogService;
    }

}
