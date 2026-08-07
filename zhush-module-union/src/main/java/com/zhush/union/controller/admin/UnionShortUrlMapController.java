package com.zhush.union.controller.admin;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.union.param.UnionShortUrlMapParam;
import com.zhush.union.query.UnionShortUrlMapQuery;
import com.zhush.union.service.UnionShortUrlMapService;
import com.zhush.union.vo.UnionShortUrlMapVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName UnionShortUrlMapController
 * @Description 短链接映射表 控制器
 * @Author zhush
 * @Date 2026/08/07
 **/
@RestController
@Tag(name = "管理端-短链接映射表")
@RequestMapping("/admin/union/short-url-map")
public class UnionShortUrlMapController {

    private final UnionShortUrlMapService shortUrlMapService;

    public UnionShortUrlMapController(UnionShortUrlMapService shortUrlMapService) {
        this.shortUrlMapService = shortUrlMapService;
    }

}
