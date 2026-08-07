package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionPromotionTemplateParam;
import com.zhush.union.query.UnionPromotionTemplateQuery;
import com.zhush.union.service.UnionPromotionTemplateService;
import com.zhush.union.vo.UnionPromotionTemplateVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName UnionPromotionTemplateController
 * @Description 推广文案模板表 控制器
 * @Author zhush
 * @Date 2026/08/07
 **/
@RestController
@Tag(name = "管理端-推广文案模板表")
@RequestMapping("/admin/union/promotion-template")
public class UnionPromotionTemplateController {

    private final UnionPromotionTemplateService promotionTemplateService;

    public UnionPromotionTemplateController(UnionPromotionTemplateService promotionTemplateService) {
        this.promotionTemplateService = promotionTemplateService;
    }

}
