package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionMemberDeviceParam;
import com.zhush.union.query.UnionMemberDeviceQuery;
import com.zhush.union.service.UnionMemberDeviceService;
import com.zhush.union.vo.UnionMemberDeviceVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName UnionMemberDeviceController
 * @Description 推广者设备信息表 控制器
 * @Author zhush
 * @Date 2026/08/07
 **/
@RestController
@Tag(name = "管理端-推广者设备信息表")
@RequestMapping("/admin/union/member-device")
public class UnionMemberDeviceController {

    private final UnionMemberDeviceService memberDeviceService;

    public UnionMemberDeviceController(UnionMemberDeviceService memberDeviceService) {
        this.memberDeviceService = memberDeviceService;
    }

}
