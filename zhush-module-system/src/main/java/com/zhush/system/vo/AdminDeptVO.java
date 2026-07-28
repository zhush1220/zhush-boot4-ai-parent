package com.zhush.system.vo;

import com.zhush.common.tree.TreeNode;
import com.zhush.system.entity.SysDept;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * @ClassName AdminDeptVO
 * @Description 部门详情 / 树形节点返回
 * @Author zhush
 * @Date 2026/7/13
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SysDept.class)
@Schema(description = "部门详情 / 树形节点返回")
public class AdminDeptVO extends TreeNode<AdminDeptVO> {

    @Schema(description = "部门名称")
    private String name;

    @Schema(description = "显示顺序")
    private Integer sort;

    @Schema(description = "负责人用户ID")
    private Long leaderUserId;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "状态：0-禁用 1-启用")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
