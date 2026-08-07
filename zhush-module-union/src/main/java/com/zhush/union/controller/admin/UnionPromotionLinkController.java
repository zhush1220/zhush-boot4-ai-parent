package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionPromotionLinkParam;
import com.zhush.union.query.UnionPromotionLinkQuery;
import com.zhush.union.service.UnionPromotionLinkService;
import com.zhush.union.vo.UnionPromotionLinkVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName UnionPromotionLinkController
 * @Description 推广链接表 控制器
 * @Author zhush
 * @Date 2026/08/07
 **/
@RestController
@Tag(name = "管理端-推广链接表")
@RequestMapping("/admin/union/promotion-link")
public class UnionPromotionLinkController {

    private final UnionPromotionLinkService promotionLinkService;

    public UnionPromotionLinkController(UnionPromotionLinkService promotionLinkService) {
        this.promotionLinkService = promotionLinkService;
    }

}
