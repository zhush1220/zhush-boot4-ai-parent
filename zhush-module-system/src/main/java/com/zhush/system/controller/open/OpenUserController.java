package com.zhush.system.controller.open;

import com.zhush.common.response.Result;
import com.zhush.oauth2.annotation.OpenApiScope;
import com.zhush.oauth2.core.OAuth2Scopes;
import com.zhush.oauth2.core.OpenApiContext;
import com.zhush.system.entity.SysAdminUser;
import com.zhush.system.enums.SystemErrorCode;
import com.zhush.system.mapper.SysAdminUserMapper;
import com.zhush.system.vo.OpenUserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * 开放 API - 用户信息（示例）
 *
 * <p>供第三方以 {@code Authorization: Bearer access_token} 调用，鉴权、scope 校验、
 * 限流与调用日志由 {@code OpenApiInterceptor} 统一处理，Controller 内通过
 * {@link OpenApiContext} 读取授权用户上下文，返回对第三方裁剪后的资料。
 *
 * @author zhush
 */
@RestController
@Tag(name = "开放API-用户", description = "第三方获取授权用户信息（OAuth2 令牌鉴权）")
@RequestMapping("/open/v1/user")
public class OpenUserController {

    private final SysAdminUserMapper adminUserMapper;

    public OpenUserController(SysAdminUserMapper adminUserMapper) {
        this.adminUserMapper = adminUserMapper;
    }

    /**
     * 当前授权用户信息（需 userinfo scope 的用户级令牌）
     */
    @GetMapping("/me")
    @OpenApiScope(value = OAuth2Scopes.USERINFO, requireUser = true)
    @Operation(summary = "当前授权用户信息", description = "需授权码模式签发、含 userinfo scope 的 access_token")
    public Result<OpenUserVO> me() {
        Long loginId = OpenApiContext.getLoginId();
        SysAdminUser user = adminUserMapper.selectOneById(loginId);
        assertNotNull(user, SystemErrorCode.USER_NOT_FOUND, loginId);

        OpenUserVO vo = new OpenUserVO();
        vo.setUserId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        return Result.success(vo);
    }

}
