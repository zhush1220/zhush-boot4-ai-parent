# 数据权限 · 列级业务表接入 Checklist

> 适用范围：新业务表/新 VO 字段接入 `@FieldPermission`（无权限时置空/脱敏）
> 前置阅读：《[数据权限框架审计评估报告.md](./数据权限框架审计评估报告.md)》第二章、《[数据权限使用指南.md](./数据权限使用指南.md)》第四章
> 版本：v1（2026-10）

---

## 0. 一句话决策表

| 你的场景 | 能否接入 | 备注 |
| --- | --- | --- |
| 管理端 VO 的敏感列（手机/邮箱/证件/金额/IP） | ✅ | 全流程 1→6 |
| **会员端 / 小程序端 VO** | ❌ 先改造 | 当前 `PermissionChecker` 是管理端全局单 Bean，标注会被**静默清空**（F-4） |
| 数据通过 `Map` / `List<Map>` 下发 | ❌ 先改 VO | Advice 跳过 Map，列级完全无效（F-2d，fail-open） |
| VO 是 Java `record` | ❌ 先改回类 | record 组件是 final 字段，`Field.set` 抛异常 → 整批 fail-open（F-2c） |
| `int` / `long` / `boolean` 基本类型字段 | ❌ 先改包装类型 | 无法置空，会**原样输出**（F-2b） |
| 字段有对应「导出」接口 | ⚠️ 需额外处理 | 导出走 OutputStream，不经 `ResponseBodyAdvice`（F-3） |
| 需要"根本不该拿到"的凭证（如 `clientSecret`） | ❌ 别用列级 | 应在 Service 层就不组装该字段，CLEAR 只是兜底 |

---

## 1. 权限点命名规范

```
{模块}:{业务实体}:{字段语义}
```

| 段 | 规则 | 示例 |
| --- | --- | --- |
| 模块 | 与菜单一级模块一致：`system` / `union` / `member` / `ai` / `pay` | `union` |
| 业务实体 | **短横线小写**，与菜单实体段保持一致（不要用 `admin-user` 这类带类名前缀的写法） | `withdraw-apply` |
| 字段语义 | 短横线小写，表达"看明文"这件事，推荐后缀 `:xxx` 直接放字段名，只读展示可加 `-plain`/`-ip` 等 | `mobile` |

已落地示例（`sql/31_data_permission_activation.sql`）：`system:user:mobile`、`system:user:email`、`system:user:login-ip`。

约束：

- **一个字段一个权限点**，不要多个字段共用一个点（无法分级授权）；
- 权限点字符串必须**三处完全一致**：SQL `sys_menu.permission`、VO 注解 `value`、前端列配置 `permission`；建议抽成常量放 VO 同包，避免拼写漂移：
  ```java
  public final class UnionWithdrawPerm {
      public static final String MOBILE = "union:withdraw-apply:mobile";
      private UnionWithdrawPerm() {}
  }
  ```
- **禁止**把 `*` 或 `system:user:*` 这类通配授权给非超管角色作为"图省事"（审计 F-5：通配匹配方向本身安全，但授予范围失控）。

---

## 2. SQL 注册权限点（幂等模板）

```sql
-- ============================================================
-- 列级权限点：{业务描述}（MySQL 8.0+，幂等：先删后插）
-- 关联代码：{XxxVO} 的 @FieldPermission（{field}）
-- 父菜单：{实体菜单名} id={parentId}
-- ============================================================
SET NAMES utf8mb4;

DELETE FROM `sys_role_menu` WHERE `menu_id` IN ({menuId});
DELETE FROM `sys_menu`      WHERE `id`      IN ({menuId});

-- type=3（按钮）不进入导航树（AdminMenuService 按 type<>3 过滤），visible 取 1 与既有按钮保持一致
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `path`, `component`, `icon`, `sort`, `visible`, `status`)
VALUES ({menuId}, {parentId}, '{字段中文名}明文', 3, '{module}:{entity}:{field}', '', '', '', {sort}, 1, 1);

-- 授予超级管理员（role_id=1）：其权限标识为 * 通配，显式记录便于审计
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES ({menuId}, 1, {menuId});
```

落位与检查：

- 文件放 `zhush-boot4-parent/sql/`，按序号递增新建（现有最大 31，如 `32_xxx_field_permission.sql`）；
- `id` 取所属菜单下的 `1xx` 延续段，`menu_id` 与 `id` 同值（参照 31 号文件写法）；
- **上线前必须为需要看明文的既有角色补授权**，否则总开关一开该列立即变空：
  ```sql
  INSERT INTO sys_role_menu (id, role_id, menu_id) VALUES (201, 2, {menuId});  -- 运营角色看手机号
  ```
- 同步确认 `zhush.data-permission.enabled=true`（dev/docker/prod 已开，`test` 未开）。

---

## 3. VO 注解位置与策略选型

### 3.1 标注位置

```java
@Data
@Schema(description = "提现申请 VO")
public class UnionWithdrawApplyVO implements Serializable {

    /**
     * 收款手机号：仅拥有 union:withdraw-apply:mobile 权限的用户可见明文。
     * 该字段会回填进编辑表单，必须用 CLEAR —— MASK 会把占位符当作用户输入写回库。
     */
    @FieldPermission(value = "union:withdraw-apply:mobile", strategy = FieldPermission.Strategy.CLEAR)
    @Schema(description = "收款手机号")
    private String mobile;

    /** 只读展示字段，无权限时用占位符，前端可显示"保密"而非空白 */
    @FieldPermission(value = "union:withdraw-apply:id-card", strategy = FieldPermission.Strategy.MASK, maskValue = "****")
    @Schema(description = "证件号")
    private String idCard;
}
```

规则：

| 要点 | 说明 |
| --- | --- |
| 标在 **VO 的字段**上（`@Target(ElementType.FIELD)`），不是 getter、不是 Controller 参数、不是 Entity/Param | Advice 只遍历响应体 |
| VO 必须是**普通类 + 非 final 实例字段**（Lombok `@Data` 即可） | record / final 字段会 fail-open（F-2c） |
| 字段类型必须是**包装类型或 String** | primitive 无法置空且会原样输出（F-2b） |
| 父类字段也会被扫描（`declaredFields` 含父类，已排除 static） | 继承 `BaseEntity` 的 VO 无需重复标注 |
| 嵌套 VO 自动递归，但**总深度 ≤ 4**（`Result → PageResult → VO → 嵌套 VO`） | 超限静默跳过（F-2a）；链路超过 4 层请扁平化 |
| 同一字段不要既 `@FieldPermission` 又在 Service 手工置空 | 双写会让排查困难，保留其一并注释原因 |

### 3.2 CLEAR vs MASK 选型表

| 字段用途 | 策略 | 理由 |
| --- | --- | --- |
| **表单回填 / 会被提交回后端** | **CLEAR**（默认） | MASK 会把 `***` 当真实值回填并可能被保存覆盖 → **污染数据**。这是最容易犯的错 |
| 只读展示（日志、审计、详情） | MASK 可选 | 保留"有值但不可读"的 UI 提示，比空白友好 |
| 列表列（前端会做列显隐） | **CLEAR** | 无权限时列本就被 `filterColumns` 隐藏，值不重要；CLEAR 更省流量 |
| 非 String 字段 | 只能 CLEAR | `MASK` 对非 String **等同 CLEAR**（`FieldPermissionResponseAdvice:128`），且不告警，别指望它出占位符 |
| 凭证类（`clientSecret`、密钥、密码） | 不用列级 | Service 层直接不组装；CLEAR 只是兜底，序列化前仍在内存 |

> 一句话默认：**拿不准就用 CLEAR。** 现有 `mobile`/`email`/`loginIp` 三处均 CLEAR，选型正确。

### 3.3 生效边界（必须知道的四条）

1. 仅对 **Servlet Web 的 JSON 响应**生效（`ResponseBodyAdvice`）。
2. **导出不生效**（F-3）：`ExcelUtils.export(...)` 写 `HttpServletResponse` 输出流，返回 `void` → 列级被完全绕过。有 `/export` 的接口（现 `LoginLogController`、`OperateLogController`）若纳管 IP 类字段，必须在导出前**手工调用同一套判定**过滤列，或去掉该列。
3. **Map 响应不生效**（F-2d）：`Result<Map<String,Object>>` / `List<Map<...>>`（现 `DruidMonitorService` 4 个方法）无法纳管 → 敏感数据必须定义 VO。
4. 未登录用户 `hasPermission` 恒 false → 字段被清空（**fail-closed，安全方向正确**）。异常路径被 `beforeBodyWrite` 的 try/catch 吞掉并告警，不阻断响应。

---

## 4. 前端列显隐接入（复用既有 hook）

后端清空了值，前端还要**隐藏列**，否则用户看到满屏空白会当 bug 报。直接复用 `apps/web-antdv-next/src/hooks/use-field-permission.ts`：

```ts
import { filterColumns, type FieldPermissionColumn } from '#/hooks/use-field-permission';

interface ColumnConfig extends FieldPermissionColumn {
  field: string;
  label: string;
  // ...
}

const ALL_COLUMN_CONFIGS: ColumnConfig[] = [
  { field: 'username', label: '账号' },
  // permission 字符串必须与后端 @FieldPermission / sys_menu.permission 完全一致
  { field: 'mobile', label: '手机号', permission: 'system:user:mobile' },
  { field: 'email', label: '邮箱', permission: 'system:user:email' },
];

// 无权限的列直接不进列设置，用户无法勾选出来
const DEFAULT_COLUMN_CONFIGS = filterColumns(ALL_COLUMN_CONFIGS);
```

模板内单值展示（非表格列）用 `canViewColumn`：

```vue
<DescriptionsItem label="最后登录IP" v-if="canViewColumn('system:user:login-ip')">
  {{ record.loginIp || '-' }}
</DescriptionsItem>
```

要点：

- **不要把权限判断硬编码成 `hasMobileAccess` 之类的散装 computed**（用户管理页已改造为 hook 驱动，新页面沿用同一模式）；
- 后端用 CLEAR 时字段是 `null`，前端渲染需 `|| '-'` 兜底；用 MASK 时值是占位符，不要再做格式化（如手机号脱敏正则）；
- 若同时存在行级数据范围（角色/部门），列显隐只解决"看什么列"，不解决"看哪些行"，两者独立。

---

## 5. 跨端与新增会话类型的硬门槛（F-4）

`zhush-server` 依赖 starter → `FieldPermissionResponseAdvice` 是**全局** `@RestControllerAdvice`；
`PermissionChecker` 由 `@ConditionalOnMissingBean` 保证**全应用只有一个实现**，当前是 `AdminPermissionChecker`（`StpAdminUtil.isLogin() && hasPermission(...)`）。

后果与红线：

| 情况 | 结果 |
| --- | --- |
| 管理端 VO 标注 | ✅ 正常 |
| 会员端（`uni`）VO 标注（如 `MemberUserVO.mobile`、`MemberInfoVO.mobile`） | ❌ 会员会话下 `StpAdminUtil.isLogin()` = false → **字段永远为空**（业务错误，不泄露） |
| 未来为会员端注册新 `PermissionChecker` 覆盖 | ❌ 会让管理端判定也走会员口径 → **管理端字段全员放行 = 泄露**。绝不允许简单替换 |

**若需要多端并存**（联盟/会员端纳管的前置改造，尚未实施）：

```java
/** 按会话类型路由的字段权限校验器（替换 AdminPermissionChecker 成为唯一 Bean） */
@Component
@Primary
public class RoutingPermissionChecker implements PermissionChecker {

    @Override
    public boolean hasPermission(String permission) {
        if (StpAdminUtil.isLogin()) {
            return StpAdminUtil.hasPermission(permission);        // 管理端口径
        }
        if (StpMemberUtil.isLogin()) {
            // 会员端：仅允许"看自己的敏感列"，须结合 resourceId 判定，不能简单 hasPermission
            return isSelfOwned(permission);
        }
        return false;                                              // 未登录一律拒绝
    }
}
```

改造完成前，**只推广到管理端口径的 VO**。这是本框架目前唯一明确的能力边界。

---

## 6. 待纳管清单（建议优先级）

| 优先级 | VO / 字段 | 建议权限点 | 策略 | 前置 |
| --- | --- | --- | --- | --- |
| P1 | `AdminDeptVO.phone` / `email` | `system:dept:phone` / `system:dept:email` | CLEAR | 无，可直接做 |
| P1 | `AdminUserVO.realName` | `system:user:real-name` | CLEAR | 无 |
| P2 | `LoginLogVO.loginIp`、`OperateLogVO.userIp`、`SqlAuditLogVO.userIp` | `system:login-log:login-ip` 等 | MASK（只读展示） | **须先处理导出盲区**（3.3 第 2 条） |
| P2 | `Oauth2ClientVO.clientSecret` | 不用列级 | — | 改 Service 不组装 |
| P3 | `UnionExternalPromoterVO.name` / `mobile` | `union:external-promoter:name` / `:mobile` | CLEAR | union 模块加 starter 依赖 + 权限点注册 |
| P3 | `MemberUserVO.mobile` / `MemberInfoVO.mobile` | `member:user:mobile`（仅本人可见语义） | CLEAR | **必须先完成第 5 节 `PermissionChecker` 路由改造** |

---

## 7. 验收清单（提交前逐条打勾）

- [ ] 权限点命名符合 `{module}:{entity}:{field}`，三处（SQL / VO / 前端）字符串一致
- [ ] 新建 `sql/{NN}_*.sql` 幂等脚本：`sys_menu`(type=3) + `sys_role_menu`，并**为需要明文的既有角色补授权**
- [ ] VO 是普通类、字段是包装类型/String、非 final；未使用 `record`
- [ ] 表单回填字段用 **CLEAR**；只读展示字段才考虑 MASK；非 String 字段没写 MASK
- [ ] 嵌套链路深度 ≤ 4（`Result → PageResult → VO → 嵌套 VO`）
- [ ] 敏感数据未通过 `Map` / `List<Map>` 下发
- [ ] 该 VO 若有导出接口，导出链路已同步做列过滤（或该列不进导出）
- [ ] 前端用 `filterColumns` / `canViewColumn` 做列显隐，未硬编码散装判断
- [ ] 目标是**管理端** VO（会员端须先完成第 5 节改造）
- [ ] **手工越权用例**：用无该权限点的角色账号 ①列表该列为 `null` ②列设置里勾不出该列 ③编辑表单保存后库里原值未被占位符覆盖 ④导出文件不含该列明文
- [ ] `mvn -o -pl :{module} test` 全绿；必要时补 starter 侧 `FieldPermissionResponseAdviceTest` 用例（现 10 例）

---

## 8. 常见故障速查

| 现象 | 根因 | 处置 |
| --- | --- | --- |
| 加了注解但值还是明文返回 | ①总开关关闭（`test` profile 默认关）②VO 不在响应链上（返回的是 Entity/Map）③用户有该权限点 | 逐项排除 |
| 有权限的人看到空值 | 权限点字符串三处不一致；角色未授权 `sys_role_menu`；`type` 不是 3 导致菜单口径差异 | 比对字符串；查授权 |
| 会员端"我的"页手机号永远空 | F-4：全局单 `PermissionChecker` 按管理端判定 | 撤下注解，或先做第 5 节改造 |
| 保存表单后库里变成 `***` | 用了 MASK 且该字段会回填提交 | 改 CLEAR（3.2） |
| 某列所有用户都变空，且日志有 `字段级权限过滤异常` | VO 用了 `record`/final 字段 → 整批 fail-open 或全清 | 改回普通类；单字段异常隔离（待 M-3 落地） |
| 列表正常、导出 Excel 里有明文 | 导出不经 `ResponseBodyAdvice`（F-3） | 导出链路手工过滤 |
| `int`/`long` 字段无权限仍返回真实值 | primitive 无法置空，保留原值并 warn（F-2b） | 改包装类型 |
