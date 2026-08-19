package com.zhush.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zhush.common.constant.CommonConstant;
import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.system.query.LoginLogQuery;
import com.zhush.system.service.LoginLogService;
import com.zhush.system.vo.LoginLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录日志接口
 *
 * @author zhush
 */
@RestController
@Tag(name = "管理端-登录日志", description = "登录日志的分页查询与详情")
@RequestMapping("/admin/system/login-log")
public class LoginLogController {

    private final LoginLogService loginLogService;

    public LoginLogController(LoginLogService loginLogService) {
        this.loginLogService = loginLogService;
    }

    /**
     * 登录日志分页
     */
    @GetMapping("/page")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:login-log:query")
    @Operation(summary = "登录日志分页")
    public Result<PageResult<LoginLogVO>> page(@Valid LoginLogQuery query) {
        return Result.success(loginLogService.getLoginLogPage(query));
    }

    /**
     * 登录日志详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:login-log:query")
    @Operation(summary = "登录日志详情")
    public Result<LoginLogVO> get(@Parameter(description = "日志ID") @PathVariable Long id) {
        return Result.success(loginLogService.getLoginLog(id));
    }

}
