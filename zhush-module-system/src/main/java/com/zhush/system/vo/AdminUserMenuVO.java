package com.zhush.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 管理端用户菜单路由（登录后动态下发给前端渲染路由/菜单）
 *
 * <p>字段契约与前端（vben admin backend 模式）对齐：
 * <ul>
 *     <li>{@code name}：路由名，全局唯一（由路由全路径 PascalCase 生成）；</li>
 *     <li>{@code path}：路由全路径（以 {@code /} 开头）；</li>
 *     <li>{@code component}：前端组件路径（仅叶子菜单，如 {@code /dashboard/analytics/index}）；</li>
 *     <li>{@code meta}：路由元信息（title / icon / order / hideInMenu）。</li>
 * </ul>
 *
 * @author zhush
 */
@Data
@Schema(description = "管理端用户菜单路由")
public class AdminUserMenuVO {

    @Schema(description = "路由名（全局唯一）")
    private String name;

    @Schema(description = "路由全路径")
    private String path;

    @Schema(description = "前端组件路径（目录节点为空）")
    private String component;

    @Schema(description = "路由元信息：title / icon / order / hideInMenu")
    private Map<String, Object> meta;

    @Schema(description = "子菜单")
    private List<AdminUserMenuVO> children;

}
