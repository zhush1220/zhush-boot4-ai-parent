package com.zhush.system.vo;

import com.zhush.system.entity.SysAdminUser;
import io.github.linpeilie.annotations.AutoMapper;
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
public class AdminUserVO {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 登录账号
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 头像地址
     */
    private String avatar;

    /**
     * 手机号
     */
    private String mobile;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 状态：0-禁用 1-启用
     */
    private Integer status;

    /**
     * 最后登录IP
     */
    private String loginIp;

    /**
     * 最后登录时间
     */
    private LocalDateTime loginDate;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 角色ID列表
     */
    private List<Long> roleIds;

}
