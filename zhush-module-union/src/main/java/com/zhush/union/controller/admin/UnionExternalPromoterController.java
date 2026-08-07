package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionExternalPromoterParam;
import com.zhush.union.query.UnionExternalPromoterQuery;
import com.zhush.union.service.UnionExternalPromoterService;
import com.zhush.union.vo.UnionExternalPromoterVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName UnionExternalPromoterController
 * @Description 外部推广者/影子推广者表 控制器
 * @Author zhush
 * @Date 2026/08/07
 **/
@RestController
@Tag(name = "管理端-外部推广者/影子推广者表")
@RequestMapping("/admin/union/external-promoter")
public class UnionExternalPromoterController {

    private final UnionExternalPromoterService externalPromoterService;

    public UnionExternalPromoterController(UnionExternalPromoterService externalPromoterService) {
        this.externalPromoterService = externalPromoterService;
    }

}
