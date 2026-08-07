package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionLinkRequestLogParam;
import com.zhush.union.query.UnionLinkRequestLogQuery;
import com.zhush.union.service.UnionLinkRequestLogService;
import com.zhush.union.vo.UnionLinkRequestLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName UnionLinkRequestLogController
 * @Description 转链请求日志表 控制器
 * @Author zhush
 * @Date 2026/08/07
 **/
@RestController
@Tag(name = "管理端-转链请求日志表")
@RequestMapping("/admin/union/link-request-log")
public class UnionLinkRequestLogController {

    private final UnionLinkRequestLogService linkRequestLogService;

    public UnionLinkRequestLogController(UnionLinkRequestLogService linkRequestLogService) {
        this.linkRequestLogService = linkRequestLogService;
    }

}
