package com.zhush.system.service;

import cn.hutool.core.util.StrUtil;
import com.zhush.common.tree.TreeUtils;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.system.entity.SysMenu;
import com.zhush.system.entity.SysRole;
import com.zhush.system.enums.SystemErrorCode;
import com.zhush.system.mapper.SysMenuMapper;
import com.zhush.system.mapper.SysRoleMenuMapper;
import com.zhush.system.param.AdminMenuParam;
import com.zhush.system.query.AdminMenuQuery;
import com.zhush.system.vo.AdminMenuVO;
import com.zhush.system.vo.AdminUserMenuVO;
import io.github.linpeilie.Converter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;
import static com.zhush.common.exception.ServiceExceptionUtil.assertTrue;

/**
 * 菜单管理服务（CRUD + 树形 + 用户菜单路由）
 *
 * @author zhush
 */
@Service
public class AdminMenuService {

    private static final long ROOT_PARENT_ID = 0L;
    private static final int STATUS_ENABLED = 1;
    private static final int VISIBLE_SHOW = 1;
    /**
     * 菜单类型：按钮（不参与路由/菜单下发）
     */
    private static final int TYPE_BUTTON = 3;

    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final AdminPermissionService permissionService;
    private final Converter converter;

    public AdminMenuService(SysMenuMapper menuMapper,
                            SysRoleMenuMapper roleMenuMapper,
                            AdminPermissionService permissionService,
                            Converter converter) {
        this.menuMapper = menuMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.permissionService = permissionService;
        this.converter = converter;
    }

    /**
     * 新增菜单
     */
    public Long createMenu(AdminMenuParam param) {
        SysMenu menu = converter.convert(param, SysMenu.class);
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
    public void updateMenu(AdminMenuParam param) {
        SysMenu existing = menuMapper.selectOneById(param.getId());
        assertNotNull(existing, SystemErrorCode.MENU_NOT_FOUND, param.getId());
        // 父级不能设置为自身
        assertTrue(!param.getId().equals(param.getParentId()),
                SystemErrorCode.MENU_PARENT_ERROR);

        SysMenu update = converter.convert(param, SysMenu.class);
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
    public AdminMenuVO getMenu(Long id) {
        SysMenu menu = menuMapper.selectOneById(id);
        assertNotNull(menu, SystemErrorCode.MENU_NOT_FOUND, id);
        return converter.convert(menu, AdminMenuVO.class);
    }

    /**
     * 菜单列表（扁平，按过滤条件）
     */
    public List<AdminMenuVO> listMenus(AdminMenuQuery query) {
        return converter.convert(queryMenus(query), AdminMenuVO.class);
    }

    /**
     * 菜单树（管理端 CRUD 用）
     */
    public List<AdminMenuVO> treeMenus(AdminMenuQuery query) {
        List<AdminMenuVO> list = converter.convert(queryMenus(query), AdminMenuVO.class);
        return TreeUtils.buildTrees(list);
    }

    // ================ 用户菜单路由（登录后下发前端） ================

    /**
     * 用户菜单路由树（按角色过滤 + 祖先补全 + sort 排序）
     *
     * <p>规则：仅下发启用且非按钮的菜单；超级管理员全量；普通角色取角色关联菜单并向上补齐祖先目录；
     * 无权限（无任何可见菜单）返回空数组，前端自行提示。
     */
    public List<AdminUserMenuVO> getUserMenuTree(Long userId) {
        List<SysMenu> menus = listUserMenus(userId);
        return buildRouteTree(menus);
    }

    /**
     * 用户首页路径（菜单树深度优先首个可见叶子菜单；无菜单返回 {@code null}）
     */
    public String getHomePath(Long userId) {
        String[] home = new String[1];
        findHomePath(getUserMenuTree(userId), home);
        return home[0];
    }

    // ================ 内部方法 ================

    /**
     * 查询用户可访问的菜单（启用、非按钮，含祖先补全，按 sort 升序）
     */
    private List<SysMenu> listUserMenus(Long userId) {
        List<SysRole> roles = permissionService.listEnabledRoles(userId);
        if (roles.isEmpty()) {
            return Collections.emptyList();
        }
        List<SysMenu> allMenus = menuMapper.selectListByQuery(QueryWrapperX.create()
                .eq(SysMenu::getStatus, STATUS_ENABLED)
                .ne(SysMenu::getType, TYPE_BUTTON)
                .orderBy("sort", true));
        // 超级管理员：全量菜单
        boolean superAdmin = roles.stream()
                .anyMatch(r -> AdminPermissionService.SUPER_ADMIN_CODE.equals(r.getCode()));
        if (superAdmin) {
            return allMenus;
        }
        // 普通角色：角色关联菜单 + 向上补齐祖先目录（避免父级目录缺失导致路由断裂）
        List<Long> roleIds = roles.stream().map(SysRole::getId).toList();
        Set<Long> grantedIds = roleMenuMapper.selectListByRoleIds(roleIds).stream()
                .map(com.zhush.system.entity.SysRoleMenu::getMenuId)
                .collect(Collectors.toSet());
        if (grantedIds.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, SysMenu> menuMap = new HashMap<>();
        for (SysMenu menu : allMenus) {
            menuMap.put(menu.getId(), menu);
        }
        Set<Long> accessIds = new HashSet<>();
        for (Long menuId : grantedIds) {
            Long current = menuId;
            while (current != null && current != ROOT_PARENT_ID
                    && menuMap.containsKey(current) && accessIds.add(current)) {
                current = menuMap.get(current).getParentId();
            }
        }
        return allMenus.stream().filter(m -> accessIds.contains(m.getId())).toList();
    }

    /**
     * 扁平菜单列表 → 路由树（入参已按 sort 升序，树内顺序天然保持）
     */
    private List<AdminUserMenuVO> buildRouteTree(List<SysMenu> menus) {
        if (menus.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, SysMenu> menuMap = new HashMap<>();
        for (SysMenu menu : menus) {
            menuMap.put(menu.getId(), menu);
        }
        Map<Long, AdminUserMenuVO> voMap = new LinkedHashMap<>();
        for (SysMenu menu : menus) {
            voMap.put(menu.getId(), toRouteVO(menu, menuMap));
        }
        List<AdminUserMenuVO> roots = new ArrayList<>();
        for (SysMenu menu : menus) {
            AdminUserMenuVO vo = voMap.get(menu.getId());
            Long parentId = menu.getParentId();
            AdminUserMenuVO parent = parentId == null ? null : voMap.get(parentId);
            if (parentId == null || parentId == ROOT_PARENT_ID || parent == null) {
                roots.add(vo);
            } else {
                if (parent.getChildren() == null) {
                    parent.setChildren(new ArrayList<>());
                }
                parent.getChildren().add(vo);
                // 有子节点的目录不下发 component（前端框架会忽略/报错）
                parent.setComponent(null);
            }
        }
        return roots;
    }

    private AdminUserMenuVO toRouteVO(SysMenu menu, Map<Long, SysMenu> menuMap) {
        String fullPath = resolveFullPath(menu, menuMap);
        AdminUserMenuVO vo = new AdminUserMenuVO();
        vo.setName(toRouteName(fullPath, menu.getId()));
        vo.setPath(fullPath);
        vo.setComponent(StrUtil.isNotBlank(menu.getComponent()) ? menu.getComponent() : null);
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("title", menu.getName());
        if (StrUtil.isNotBlank(menu.getIcon())) {
            meta.put("icon", menu.getIcon());
        }
        if (menu.getSort() != null) {
            meta.put("order", menu.getSort());
        }
        if (menu.getVisible() != null && menu.getVisible() != VISIBLE_SHOW) {
            meta.put("hideInMenu", true);
        }
        vo.setMeta(meta);
        return vo;
    }

    /**
     * 拼接菜单全路径（祖先路径段逐级拼接，以 {@code /} 开头）
     */
    private String resolveFullPath(SysMenu menu, Map<Long, SysMenu> menuMap) {
        LinkedList<String> segments = new LinkedList<>();
        SysMenu current = menu;
        while (current != null) {
            segments.addFirst(StrUtil.removePrefix(StrUtil.nullToEmpty(current.getPath()), "/"));
            Long parentId = current.getParentId();
            current = (parentId == null || parentId == ROOT_PARENT_ID) ? null : menuMap.get(parentId);
        }
        String joined = segments.stream().filter(StrUtil::isNotBlank).collect(Collectors.joining("/"));
        return "/" + joined;
    }

    /**
     * 全路径 → 路由名（PascalCase，如 /dashboard/analytics → DashboardAnalytics）
     */
    private String toRouteName(String fullPath, Long fallbackId) {
        String name = Arrays.stream(fullPath.split("/"))
                .filter(StrUtil::isNotBlank)
                .map(seg -> StrUtil.upperFirst(StrUtil.toCamelCase(seg.toLowerCase())))
                .collect(Collectors.joining());
        return name.isEmpty() ? "Menu" + fallbackId : name;
    }

    /**
     * 深度优先找首个可见叶子菜单路径（跳过隐藏菜单）
     */
    private boolean findHomePath(List<AdminUserMenuVO> nodes, String[] home) {
        if (nodes == null) {
            return false;
        }
        for (AdminUserMenuVO node : nodes) {
            if (node.getMeta() != null && Boolean.TRUE.equals(node.getMeta().get("hideInMenu"))) {
                continue;
            }
            if (node.getChildren() == null || node.getChildren().isEmpty()) {
                home[0] = node.getPath();
                return true;
            }
            if (findHomePath(node.getChildren(), home)) {
                return true;
            }
        }
        return false;
    }

    private List<SysMenu> queryMenus(AdminMenuQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .like(SysMenu::getName, query.getName())
                .eq(SysMenu::getStatus, query.getStatus());
        queryWrapper.orderBy("sort", true);
        return menuMapper.selectListByQuery(queryWrapper);
    }

}
