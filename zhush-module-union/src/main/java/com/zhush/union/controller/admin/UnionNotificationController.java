package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionNotificationParam;
import com.zhush.union.query.UnionNotificationQuery;
import com.zhush.union.service.UnionNotificationService;
import com.zhush.union.vo.UnionNotificationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName UnionNotificationController
 * @Description 消息通知表 控制器
 * @Author zhush
 * @Date 2026/08/07
 **/
@RestController
@Tag(name = "管理端-消息通知表")
@RequestMapping("/admin/union/notification")
public class UnionNotificationController {

    private final UnionNotificationService notificationService;

    public UnionNotificationController(UnionNotificationService notificationService) {
        this.notificationService = notificationService;
    }

}
