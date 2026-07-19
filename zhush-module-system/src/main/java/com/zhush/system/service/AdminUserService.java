package com.zhush.system.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.system.controller.admin.vo.user.AdminUserPageReqVO;
import com.zhush.system.controller.admin.vo.user.AdminUserRespVO;
import com.zhush.system.controller.admin.vo.user.AdminUserSaveReqVO;
import com.zhush.system.controller.admin.vo.user.AdminUserUpdateReqVO;
import com.zhush.system.entity.SysAdminUser;
import com.zhush.system.entity.SysAdminUserRole;
import com.zhush.system.enums.SystemErrorCode;
import com.zhush.system.mapper.SysAdminUserMapper;
import com.zhush.system.mapper.SysAdminUserRoleMapper;
import com.zhush.system.mapper.SysRoleMapper;
import io.github.linpeilie.Converter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;
import static com.zhush.common.exception.ServiceExceptionUtil.assertTrue;

/**
 * 管理员用户管理服务（CRUD + 角色分配 + 密码重置）
 *
 * @author zhush
 */
@Service
public class AdminUserService {

    private static final int STATUS_ENABLED = 1;

    private final SysAdminUserMapper adminUserMapper;
    private final SysAdminUserRoleMapper adminUserRoleMapper;
    private final SysRoleMapper roleMapper;
    private final Converter converter;

    public AdminUserService(SysAdminUserMapper adminUserMapper,
                            SysAdminUserRoleMapper adminUserRoleMapper,
                            SysRoleMapper roleMapper,
                            Converter converter) {
        this.adminUserMapper = adminUserMapper;
        this.adminUserRoleMapper = adminUserRoleMapper;
        this.roleMapper = roleMapper;
        this.converter = converter;
    }

    /**
     * 新增管理员
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createUser(AdminUserSaveReqVO reqVO) {
        validateUsernameUnique(null, reqVO.getUsername());
        SysAdminUser user = converter.convert(reqVO, SysAdminUser.class);
        user.setPassword(BCrypt.hashpw(reqVO.getPassword()));
        if (user.getStatus() == null) {
            user.setStatus(STATUS_ENABLED);
        }
        adminUserMapper.insert(user);
        saveUserRoles(user.getId(), reqVO.getRoleIds());
        return user.getId();
    }

    /**
     * 修改管理员
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(AdminUserUpdateReqVO reqVO) {
        SysAdminUser existing = adminUserMapper.selectOneById(reqVO.getId());
        assertNotNull(existing, SystemErrorCode.USER_NOT_FOUND, reqVO.getId());

        SysAdminUser update = converter.convert(reqVO, SysAdminUser.class);
        // 账号与密码不在此更新
        update.setUsername(null);
        update.setPassword(null);
        adminUserMapper.update(update);

        // roleIds 非 null 时覆盖更新用户角色
        if (reqVO.getRoleIds() != null) {
            adminUserRoleMapper.deleteByUserId(reqVO.getId());
            saveUserRoles(reqVO.getId(), reqVO.getRoleIds());
        }
    }

    /**
     * 删除管理员（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        SysAdminUser existing = adminUserMapper.selectOneById(id);
        assertNotNull(existing, SystemErrorCode.USER_NOT_FOUND, id);
        adminUserMapper.deleteById(id);
        adminUserRoleMapper.deleteByUserId(id);
    }

    /**
     * 管理员详情
     */
    public AdminUserRespVO getUser(Long id) {
        SysAdminUser user = adminUserMapper.selectOneById(id);
        assertNotNull(user, SystemErrorCode.USER_NOT_FOUND, id);
        AdminUserRespVO vo = converter.convert(user, AdminUserRespVO.class);
        vo.setRoleIds(listRoleIdsByUserId(id));
        return vo;
    }

    /**
     * 管理员分页
     */
    public PageResult<AdminUserRespVO> getUserPage(AdminUserPageReqVO reqVO) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .like(SysAdminUser::getUsername, reqVO.getUsername())
                .eq(SysAdminUser::getMobile, reqVO.getMobile())
                .eq(SysAdminUser::getStatus, reqVO.getStatus());
        queryWrapper.orderBy("id", false);
        Page<SysAdminUser> page = adminUserMapper.selectPageX(reqVO, queryWrapper);
        List<AdminUserRespVO> list = converter.convert(page.getRecords(), AdminUserRespVO.class);
        fillRoleIds(list);
        return PageResult.of(list, page.getTotalRow());
    }

    /**
     * 分配角色
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, List<Long> roleIds) {
        SysAdminUser existing = adminUserMapper.selectOneById(userId);
        assertNotNull(existing, SystemErrorCode.USER_NOT_FOUND, userId);
        adminUserRoleMapper.deleteByUserId(userId);
        saveUserRoles(userId, roleIds);
    }

    /**
     * 重置密码
     */
    public void resetPassword(Long id, String rawPassword) {
        SysAdminUser existing = adminUserMapper.selectOneById(id);
        assertNotNull(existing, SystemErrorCode.USER_NOT_FOUND, id);
        SysAdminUser update = new SysAdminUser();
        update.setId(id);
        update.setPassword(BCrypt.hashpw(rawPassword));
        adminUserMapper.update(update);
    }

    // ================ 内部方法 ================

    private void validateUsernameUnique(Long id, String username) {
        SysAdminUser user = adminUserMapper.selectByUsername(username);
        if (user == null) {
            return;
        }
        assertTrue(user.getId().equals(id), SystemErrorCode.USER_ALREADY_EXISTS, username);
    }

    private void saveUserRoles(Long userId, List<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return;
        }
        // 过滤不存在的角色
        List<Long> validRoleIds = roleMapper.selectListByIds(roleIds).stream()
                .map(com.zhush.system.entity.SysRole::getId)
                .toList();
        List<SysAdminUserRole> relations = validRoleIds.stream().map(roleId -> {
            SysAdminUserRole relation = new SysAdminUserRole();
            relation.setUserId(userId);
            relation.setRoleId(roleId);
            return relation;
        }).toList();
        if (!relations.isEmpty()) {
            adminUserRoleMapper.insertBatchWithDb(relations);
        }
    }

    private List<Long> listRoleIdsByUserId(Long userId) {
        return adminUserRoleMapper.selectListByUserId(userId).stream()
                .map(SysAdminUserRole::getRoleId)
                .toList();
    }

    private void fillRoleIds(List<AdminUserRespVO> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        Collection<Long> userIds = list.stream().map(AdminUserRespVO::getId).toList();
        Map<Long, List<Long>> userRoleMap = adminUserRoleMapper.selectListByUserIds(userIds).stream()
                .collect(Collectors.groupingBy(SysAdminUserRole::getUserId,
                        Collectors.mapping(SysAdminUserRole::getRoleId, Collectors.toList())));
        list.forEach(vo -> vo.setRoleIds(userRoleMap.getOrDefault(vo.getId(), List.of())));
    }

}
