package com.zhush.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zhush.common.constant.CommonConstant;
import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.protection.idempotent.annotation.Idempotent;
import com.zhush.system.param.Oauth2ClientParam;
import com.zhush.system.query.Oauth2ClientQuery;
import com.zhush.system.service.Oauth2ClientService;
import com.zhush.system.vo.Oauth2ClientSecretVO;
import com.zhush.system.vo.Oauth2ClientVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * OAuth2 客户端管理接口
 *
 * @author zhush
 */
@RestController
@Tag(name = "管理端-OAuth2客户端", description = "第三方应用的注册、密钥重置与启停管理")
@RequestMapping("/admin/system/oauth2-client")
public class AdminOauth2ClientController {

    private final Oauth2ClientService oauth2ClientService;

    public AdminOauth2ClientController(Oauth2ClientService oauth2ClientService) {
        this.oauth2ClientService = oauth2ClientService;
    }

    /**
     * 新增客户端（密钥明文仅本次返回）
     */
    @PostMapping
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:oauth2-client:add")
    @Idempotent
    @Operation(summary = "新增客户端", description = "client_secret 由服务端生成，明文仅本次返回")
    public Result<Oauth2ClientSecretVO> create(@Valid @RequestBody Oauth2ClientParam param) {
        return Result.success(oauth2ClientService.createClient(param));
    }

    /**
     * 修改客户端
     */
    @PutMapping
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:oauth2-client:update")
    @Idempotent
    @Operation(summary = "修改客户端", description = "clientId 与密钥不可经此修改")
    public Result<Void> update(@Valid @RequestBody Oauth2ClientParam param) {
        oauth2ClientService.updateClient(param);
        return Result.success();
    }

    /**
     * 删除客户端
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:oauth2-client:delete")
    @Idempotent
    @Operation(summary = "删除客户端", description = "逻辑删除，存量令牌立即失效")
    public Result<Void> delete(@Parameter(description = "客户端ID") @PathVariable Long id) {
        oauth2ClientService.deleteClient(id);
        return Result.success();
    }

    /**
     * 重置密钥（密钥明文仅本次返回）
     */
    @PutMapping("/reset-secret/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:oauth2-client:update")
    @Idempotent(timeout = 10, message = "密钥重置中，请勿重复提交")
    @Operation(summary = "重置密钥", description = "旧密钥立即作废，明文仅本次返回")
    public Result<Oauth2ClientSecretVO> resetSecret(@Parameter(description = "客户端ID") @PathVariable Long id) {
        return Result.success(oauth2ClientService.resetSecret(id));
    }

    /**
     * 客户端详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:oauth2-client:query")
    @Operation(summary = "客户端详情")
    public Result<Oauth2ClientVO> get(@Parameter(description = "客户端ID") @PathVariable Long id) {
        return Result.success(oauth2ClientService.getClient(id));
    }

    /**
     * 客户端分页
     */
    @GetMapping("/page")
    @SaCheckPermission(type = CommonConstant.TYPE, value = "system:oauth2-client:query")
    @Operation(summary = "客户端分页")
    public Result<PageResult<Oauth2ClientVO>> page(@Valid Oauth2ClientQuery query) {
        return Result.success(oauth2ClientService.getClientPage(query));
    }

}
