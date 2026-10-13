package com.zhush.system.vo;

import com.zhush.data.permission.annotation.FieldPermission;
import com.zhush.system.entity.SysAdminUser;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @ClassName AdminUserVO
 * @Description 管理员详情 / 列表返回
 * @Author zhush
 * @Date 2026/7/20 0020 22:30
 **/
@Data
@AutoMapper(target = SysAdminUser.class)
@Schema(description = "管理员详情 / 列表返回")
public class AdminUserVO {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "登录账号")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "头像地址")
    private String avatar;

    @Schema(description = "性别：0-未知 1-男 2-女")
    private Integer sex;

    /**
     * 手机号：列级数据权限，无 system:user:mobile 权限时置空
     * <p>
     * 采用置空（CLEAR）而非脱敏（MASK），因为编辑表单会回填该字段，
     * 脱敏值被提交会污染库中的真实手机号；置空后提交时该字段为 null，更新时忽略不覆盖原值。
     */
    @FieldPermission(value = "system:user:mobile", strategy = FieldPermission.Strategy.CLEAR)
    @Schema(description = "手机号")
    private String mobile;

    /**
     * 邮箱：列级数据权限，无 system:user:email 权限时置空
     * <p>
     * 与手机号同为表单回填字段，按规范取置空（CLEAR）而非脱敏（MASK）。
     */
    @FieldPermission(value = "system:user:email", strategy = FieldPermission.Strategy.CLEAR)
    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "岗位ID")
    private Long postId;

    @Schema(description = "状态：0-禁用 1-启用")
    private Integer status;

    /**
     * 最后登录 IP：列级数据权限，无 system:user:login-ip 权限时置空
     * <p>
     * 只读展示字段（列表页仅在 record.loginIp 非空时拼括号），置空不影响登录时间列的展示。
     */
    @FieldPermission(value = "system:user:login-ip", strategy = FieldPermission.Strategy.CLEAR)
    @Schema(description = "最后登录IP")
    private String loginIp;

    @Schema(description = "最后登录时间")
    private LocalDateTime loginDate;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "角色ID列表")
    private List<Long> roleIds;

}
