package com.zhush.system.service;

import com.zhush.common.tree.TreeUtils;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.system.controller.admin.vo.menu.AdminMenuListReqVO;
import com.zhush.system.controller.admin.vo.menu.AdminMenuRespVO;
import com.zhush.system.controller.admin.vo.menu.AdminMenuSaveReqVO;
import com.zhush.system.controller.admin.vo.menu.AdminMenuUpdateReqVO;
import com.zhush.system.entity.SysMenu;
import com.zhush.system.enums.SystemErrorCode;
import com.zhush.system.mapper.SysMenuMapper;
import com.zhush.system.mapper.SysRoleMenuMapper;
import io.github.linpeilie.Converter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;
import static com.zhush.common.exception.ServiceExceptionUtil.assertTrue;

/**
 * 菜单管理服务（CRUD + 树形）
 *
 * @author zhush
 */
@Service
public class AdminMenuService {

    private static final long ROOT_PARENT_ID = 0L;
    private static final int STATUS_ENABLED = 1;
    private static final int VISIBLE_SHOW = 1;

    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final Converter converter;

    public AdminMenuService(SysMenuMapper menuMapper,
                            SysRoleMenuMapper roleMenuMapper,
                            Converter converter) {
        this.menuMapper = menuMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.converter = converter;
    }

    /**
     * 新增菜单
     */
    public Long createMenu(AdminMenuSaveReqVO reqVO) {
        SysMenu menu = converter.convert(reqVO, SysMenu.class);
        if (menu.getParentId() == null) {
            menu.setParentId(ROOT_PARENT_ID);
        }
        if (menu.getStatus() == null) {
            menu.setStatus(STATUS_ENABLED);
        }
        if (menu.getVisible() == null) {
            menu.setVisible(VISIBLE_SHOW);
        }
        menuMapper.insert(menu);
        return menu.getId();
    }

    /**
     * 修改菜单
     */
    public void updateMenu(AdminMenuUpdateReqVO reqVO) {
        SysMenu existing = menuMapper.selectOneById(reqVO.getId());
        assertNotNull(existing, SystemErrorCode.MENU_NOT_FOUND, reqVO.getId());
        // 父级不能设置为自身
        assertTrue(!reqVO.getId().equals(reqVO.getParentId()),
                SystemErrorCode.MENU_PARENT_ERROR);

        SysMenu update = converter.convert(reqVO, SysMenu.class);
        menuMapper.update(update);
    }

    /**
     * 删除菜单（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteMenu(Long id) {
        SysMenu existing = menuMapper.selectOneById(id);
        assertNotNull(existing, SystemErrorCode.MENU_NOT_FOUND, id);
        // 存在子菜单时不可删除
        assertTrue(menuMapper.selectListByParentId(id).isEmpty(),
                SystemErrorCode.MENU_HAS_CHILDREN);
        menuMapper.deleteById(id);
        // 清理角色-菜单关联
        roleMenuMapper.deleteByMenuId(id);
    }

    /**
     * 菜单详情
     */
    public AdminMenuRespVO getMenu(Long id) {
        SysMenu menu = menuMapper.selectOneById(id);
        assertNotNull(menu, SystemErrorCode.MENU_NOT_FOUND, id);
        return converter.convert(menu, AdminMenuRespVO.class);
    }

    /**
     * 菜单列表（扁平，按过滤条件）
     */
    public List<AdminMenuRespVO> listMenus(AdminMenuListReqVO reqVO) {
        return converter.convert(queryMenus(reqVO), AdminMenuRespVO.class);
    }

    /**
     * 菜单树
     */
    public List<AdminMenuRespVO> treeMenus(AdminMenuListReqVO reqVO) {
        List<AdminMenuRespVO> list = converter.convert(queryMenus(reqVO), AdminMenuRespVO.class);
        return TreeUtils.buildTrees(list);
    }

    // ================ 内部方法 ================

    private List<SysMenu> queryMenus(AdminMenuListReqVO reqVO) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .like(SysMenu::getName, reqVO.getName())
                .eq(SysMenu::getStatus, reqVO.getStatus());
        queryWrapper.orderBy("sort", true);
        return menuMapper.selectListByQuery(queryWrapper);
    }

}
