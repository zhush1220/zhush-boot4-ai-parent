package com.zhush.system.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @ClassName AdminUserQuery
 * @Description 管理员分页查询请求
 * @Author zhush
 * @Date 2026/7/20 0020 22:30
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "管理员分页查询请求")
public class AdminUserQuery extends BaseQuery {

    @Schema(description = "登录账号（模糊匹配）")
    private String username;

    @Schema(description = "手机号")
    private String mobile;

    @Schema(description = "状态：0-禁用 1-启用")
    private Integer status;

}
