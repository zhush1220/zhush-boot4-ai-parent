# 数据权限 · 行级业务表接入 Checklist

> 适用范围：新业务表/新 Service 查询方法接入 `@DataPermission`（行级过滤）
> 前置阅读：《[数据权限框架审计评估报告.md](./数据权限框架审计评估报告.md)》第一章、《[数据权限使用指南.md](./数据权限使用指南.md)》第三章
> 版本：v1（2026-10）

---

## 0. 一句话决策表

| 你的场景 | 能否接入 | 走哪一步 |
| --- | --- | --- |
| 表有 `dept_id`，列表需按部门隔离 | ✅ | 全流程 1→7 |
| 表有 `create_by`（继承 `BaseEntity`），只需"仅本人" | ✅ | 全流程，`userColumn` 用默认 `create_by` |
| 表既无 `dept_id` 也无 `create_by`（如 `union_order`） | ❌ 先补 DDL | 第 1 步的前置：加归属列 |
| 归属列是业务主体（`member_id` 等） | ⚠️ 需同 ID 空间 | 见第 3 步「ID 空间红线」 |
| 详情 / 更新 / 删除接口 | ⚠️ 注解无效 | 见第 6 步（必须显式归属校验） |
| 监控/缓存/字典等全局共享配置 | ❌ 不该接入 | 接入会让低权限角色看不到系统配置 |

---

## 1. 前置：表结构必须具备归属列

行级过滤只有两种落点（`DataPermissionDialect#buildCondition`）：

```
DEPT / DEPT_AND_CHILD / CUSTOM  →  WHERE <deptColumn> IN (可见部门集合)
SELF                            →  WHERE <userColumn> = 当前用户ID
```

因此目标表**至少要有其一**：

| 归属列 | 命名规范 | 类型 | 填充方式 |
| --- | --- | --- | --- |
| `dept_id` | 固定 `dept_id`（不用 `department_id`/`org_id`） | `BIGINT NULL` | 业务写入时赋值；实体字段 `Long deptId` |
| `create_by` | 继承 `BaseEntity` 即自动具备 | `BIGINT NULL` | `GlobalInsertListener` 从 `LoginUserContextHolder` 自动填充（admin 优先，回退 member） |

**当前现状（审计取证）**：

- 全库仅 `sys_admin_user` 有 `dept_id`（`sql/01_admin_rbac.sql:25`、`sql/09_union.sql:91`）；
- `create_by` / `is_delete` 仅存在于 `sys_*` 系统表；
- **`union_*` 业务表两者皆无**（例：`sql/09_union.sql:1052-1095` 的 `union_order` 只有 `create_time`/`update_time`），实体 `UnionOrder implements Serializable` 也未继承 `BaseEntity`。

所以联盟/AI/会员模块接入行级前，**先做 DDL 变更**（这是排期上的真实前置，不是写代码的事）：

```sql
-- 幂等模板（参照 sql/17_post.sql 风格）
SET @tbl := 'union_withdraw_apply';
SET @col := 'dept_id';
SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @tbl AND COLUMN_NAME = @col);
SET @sql := IF(@exist = 0,
  'ALTER TABLE `union_withdraw_apply` ADD COLUMN `dept_id` BIGINT NULL COMMENT ''归属部门ID（行级数据权限）'' AFTER `create_by`, ADD INDEX `idx_dept_id`(`dept_id`)',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
```

> 索引必须同时建：`dept_id IN (...)` 每页查询都会带，无索引即全表扫。

---

## 2. 模块依赖与开关

```xml
<!-- {module}/pom.xml —— union/ai/member 目前均缺失，接入前必加 -->
<dependency>
    <groupId>com.zhush</groupId>
    <artifactId>zhush-spring-boot-starter-biz-data-permission</artifactId>
</dependency>
```

配置（`application-{dev,docker,prod}.yaml` 已 `enabled: true`，无需改动）：

```yaml
zhush:
  data-permission:
    enabled: true        # 总开关，缺省 false 即整体关闭（无 matchIfMissing）
    row-enabled: true    # 行级子开关
    field-enabled: true  # 列级子开关
```

> ⚠️ `application-test.yaml` 当前 `enabled: false` → **测试环境跑不出任何数据权限行为**，接入前务必在 dev 手工验证越权用例。

---

## 3. 注解参数约定

```java
@DataPermission(deptColumn = "dept_id", userColumn = "create_by")
public PageResult<XxxVO> getXxxPage(XxxQuery query) { ... }
```

| 参数 | 默认值 | 何时必须显式改 |
| --- | --- | --- |
| `deptColumn` | `dept_id` | 表用别名时（建议改表而非改参数，保持全库统一） |
| `userColumn` | `create_by` | 归属人即主键的表（如 `sys_admin_user` → `id`）；`create_by` 恒为 NULL 的系统内置表 |
| `enabled` | `true` | 类级注解 + 个别方法需豁免时，方法上写 `@DataPermission(enabled = false)` 覆盖 |

**放置位置规则**

- 标注在 **Service 方法**上（不是 Controller）——事务、异步、内部复用都在 Service 层，作用域最准；
- 需要整个 Service 类默认过滤时标注在**类**上（切面 `@annotation || @within` 双切点支持），个别方法用 `enabled = false` 豁免；
- 注解必须走 Spring 代理才生效：**同类内部 `this.method()` 自调用不会触发切面**（`DataPermissionAspect` 是 `@Around`）。若需在内部方法级联，请拆到另一个 Bean 或注入自身代理。

**ID 空间红线（重要）**

`AdminDataScopeProvider` 返回的 `DataScope.userId` 来自 `StpAdminUtil.getLoginIdDefaultNull()`，即 **`sys_admin_user.id`**。
因此 `userColumn` 必须与它同 ID 空间：

| 表列 | 能否作 `userColumn` | 说明 |
| --- | --- | --- |
| `create_by` | ✅ | `GlobalInsertListener` 写入的正是登录主体 ID |
| `sys_admin_user.id` | ✅ | 现有 `getUserPage` 用法 |
| `member_id` | ❌（管理端口径下） | 会员 ID 与管理员 ID 不同空间，比较结果恒假 → **数据全部看不到**；若把 `master_member_id` 之类误当 userId 比较还可能命中他人数据 |
| `operator_id`（管理员操作留痕） | ✅ | 语义即"归属管理员" |

> 若确实要"会员只看自己的单"，那是**会员端数据范围**，需要先实现会员侧 `DataScopeProvider`（按会话类型路由），不能复用 admin 的 userId 硬套。

---

## 4. 查询写法红线：禁用顶层 OR（P0 级）

**已实测复现的框架级缺陷**（审计报告 R-1）：`prepareAuth` 用 `queryWrapper.and(cond)` 追加条件，若业务 WHERE 含**顶层 OR**，MySQL 优先级 `AND > OR` 会让过滤条件只约束最后一个分支：

```sql
-- 危险：过滤条件被 OR 短路（实测渲染结果）
SELECT * WHERE `username` LIKE ? OR `mobile` = ? AND `dept_id` IN (1, 2)
--   实际语义：username LIKE ?  OR  (mobile = ? AND dept_id IN (...))  → 越权，且无任何报错
```

`QueryMethods.bracket()` 包裹追加条件、或把整条链交给 `where(...)`，**实测均不能修复**（方言层拿不到已有 WHERE 链，无法整体重包裹）。

### ✅ 唯一安全写法：顶层恒为 AND，OR 组自带括号

```java
QueryWrapperX queryWrapper = QueryWrapperX.create()
        .eq(Xxx::getStatus, query.getStatus())
        // OR 组必须整体括号，作为单个 AND 节点
        .and(QueryMethods.bracket(
                new QueryColumn("username").like(keyword)
                        .or(new QueryColumn("mobile").eq(keyword))));
queryWrapper.and(new QueryColumn("dept_id").in(deptIds));   // 实测括号保留，过滤正确
```

实测渲染：

```sql
WHERE `status` = 1 AND (`username` LIKE '%abc%' OR `mobile` = '138') AND `dept_id` IN (1, 2)   -- ✅
```

### ❌ 禁止写法清单（CR 直接打回）

| 写法 | 后果 |
| --- | --- |
| `queryWrapper.where(A).or(B)` 后接 `@DataPermission` | 过滤只约束 B，A 分支越权 |
| `QueryWrapperX.or(...)` / `.or(String, Object...)` | 同上，`QueryWrapperX` 未覆写 `or`，继承即可用 = 陷阱 |
| 手写 Mapper XML / `@Select` 原生 SQL | **完全不经过 `prepareAuth`**，行级权限静默失效（需改为 QueryWrapper 或自行拼归属条件） |
| `selectOneById` / `deleteById` / `update(entity)` / `selectListByIds` | abstract 方法，不经过 `prepareAuth`（见第 6 步） |

> 建议同步在 `QueryWrapperX` 增加受控 API（审计最小修复方案 M-1）：
> ```java
> /** 安全 OR 分组：整组括号，保证可被 @DataPermission 正确 AND */
> public QueryWrapperX orGroup(QueryCondition condition) {
>     return (QueryWrapperX) super.and(QueryMethods.bracket(condition));
> }
> ```

---

## 5. `ignore()` 包裹判定表

`ignore()` 只对「会被 `prepareAuth` 拦截 **且** 目标表无归属列」的调用必要。滥加是噪音，漏加会 `Unknown column` 报错。

判定方法（`javap com.mybatisflex.core.BaseMapper` 取证）：

| Mapper 调用形态 | 是否被拦截 | 目标表无归属列时的结果 |
| --- | --- | --- |
| `default` 方法：`selectListByQuery` / `selectOneByQuery` / `selectCountByQuery` / `selectPageX` / `selectListByMap` / `deleteByQuery` / `update(entity, queryWrapper)`，以及项目内 `selectListByUserId`/`selectListByUserIds` 这类 `QueryWrapper.create()` 封装 | ✅ **拦截** | ❌ `Unknown column 'dept_id'` → **必须 `ignore()`** |
| `abstract` 方法：`selectOneById` / `deleteById` / `update(entity)` / `selectListByIds` / `insert(entity)` | ❌ 不拦截 | ✅ 不受影响，**不要加 `ignore()`** |
| Mapper XML / `@Select` 原生 SQL | ❌ 不拦截（但行级权限也保护不到它） | ✅ 不报错 |

写法：

```java
@DataPermission(deptColumn = "dept_id", userColumn = "id")
public PageResult<AdminUserVO> getUserPage(AdminUserQuery query) {
    Page<SysAdminUser> page = adminUserMapper.selectPageX(query, queryWrapper);
    List<AdminUserVO> list = converter.convert(page.getRecords(), AdminUserVO.class);
    // sys_admin_user_role 无 dept_id / create_by，且 selectListByUserIds 走 QueryWrapper 会被拦截
    // → 必须跳过，否则 Unknown column 'dept_id'
    DataPermissionHelper.ignore(() -> fillRoleIds(list));
    return PageResult.of(list, page.getTotalRow());
}
```

`DataPermissionHelper.ignore` 保存并恢复 `original` 标记，**支持嵌套**；`AdminDataScopeProvider` 内部查角色/部门/用户已由方言层 `setIgnore(true)/finally setIgnore(false)` 自动保护，**不要**再额外包一层。

新增查询时的检查动作：

1. 在 `@DataPermission` 方法调用链上列出所有 Mapper 调用；
2. 逐个判定是否 `default`（QueryWrapper）方法；
3. 对"QueryWrapper 方法 + 目标表无归属列"的组合，用 `ignore()` 包裹，并写注释说明**为什么必须包**（防止后人删掉）。

---

## 6. 详情 / 写操作必须显式归属校验（P0 级）

**审计结论（R-2）**：`selectOneById` / `deleteById` / `update(entity)` 是 mybatis-flex `BaseMapper` 的 abstract 方法，不构造 `QueryWrapper`，**不会被 `prepareAuth` 追加过滤条件**。所以：

> **`@DataPermission` 只保护列表，不保护 `GET /{id}`、`PUT`、`DELETE`、密码重置、角色分配。**
> 拥有"本部门"范围的管理员可直接用他人 ID 读改删 —— IDOR，且 `assignRoles`/`resetPassword` 可提权到超管。

接入任何新模块时，以下三选一**必须做**，否则不得上线：

### 方案 A（推荐，最小改动）：Service 层守卫

```java
/** byId 系列不走 QueryWrapper，需在 Service 层显式校验目标行归属 */
private void assertVisible(SysXxx entity) {
    DataScope scope = dataScopeProvider.getDataScope();
    if (scope == null || scope.getType() == DataScopeType.ALL) {
        return;
    }
    boolean ok = switch (scope.getType()) {
        case DEPT, DEPT_AND_CHILD, CUSTOM -> scope.getDeptIds().contains(entity.getDeptId());
        case SELF -> Objects.equals(scope.getUserId(), entity.getCreateBy());
        default -> false;
    };
    // 复用"不存在"语义，避免越权探测泄露记录是否存在
    assertTrue(ok, XxxErrorCode.NOT_FOUND, entity.getId());
}
```

调用位点：`getXxx(id)` / `updateXxx` / `deleteXxx` / 任何"按 ID 改状态/分配关系"的方法，紧跟在 `selectOneById` 之后（**零额外 SQL**）。

### 方案 B：改用 QueryWrapper 形态（让框架自动过滤）

```java
// 读：把 selectOneById 换成条件查询，会被 prepareAuth 拦截
SysXxx entity = xxxMapper.selectOneByQuery(
        QueryWrapper.create().where(XXX.ID.eq(id)));
// 删：deleteByQuery 同样受保护
xxxMapper.deleteByQuery(QueryWrapper.create().where(XXX.ID.eq(id)));
// 改：带条件的 update
xxxMapper.update(entity, QueryWrapper.create().where(XXX.ID.eq(id)));
```

代价：记录不存在与无权访问无法区分（都返回 0 行/0 影响），需在提示语上处理。

### 方案 C：条件补齐（批量操作）

批量更新/删除统一用 `update(entity, queryWrapper)` / `deleteByQuery(queryWrapper)`，并在 wrapper 里带上归属条件；不要用 `updateBatch` / `deleteBatchByIds`。

> 补充：`insert` 不受行级过滤保护（设计如此）。需保证新数据落在操作者自己的可见范围内 —— 例如低权限管理员不应能把记录的 `dept_id` 指定成别的部门。可在 Param → Entity 转换后校验 `deptId` 是否属于 `dataScope.deptIds`。

---

## 7. 缓存失效接入

数据范围由 `sys_role.data_scope` + 用户归属部门共同决定，`AdminDataScopeProvider` 按用户缓存 60s。
**凡是能让"某人能看到哪些行"发生变化的操作，都要在 Service 里加失效调用**，否则最迟 60s 才生效（且多实例部署下各节点独立，见 R-5）。

| 变更类型 | 调用 | 已有/待补 |
| --- | --- | --- |
| 角色数据范围调整、角色启停、角色删除 | `dataScopeProvider.invalidateAll()` | ✅ 已在 `AdminRoleService` |
| 用户改归属部门、用户角色分配、用户删除 | `dataScopeProvider.invalidate(userId)` | ✅ 已在 `AdminUserService` |
| 部门增删改（影响"本部门及以下"集合） | — | ❌ 待补：发 `DeptDataScopeChangedEvent`，Provider 用 `@EventListener` 监听 `invalidateAll()`（**单向依赖，不得反向注入 Provider 造成启动期循环依赖**） |
| 新模块影响数据归属（如批量改部门） | 同上按维度选 `invalidateAll` / `invalidate` | 接入时同步补 |

接入动作：在 Service 构造器注入 `AdminDataScopeProvider`（业务模块内若跨模块不可注入，则改为发事件），并在事务方法**末尾**调用失效。

---

## 8. 验收清单（提交前逐条打勾）

- [ ] 目标表有 `dept_id` 或 `create_by`，且已建索引
- [ ] 模块 `pom.xml` 已加 starter 依赖
- [ ] `@DataPermission` 标注在 Service 方法上，`deptColumn`/`userColumn` 已按第 3 步核对
- [ ] `userColumn` 与 `DataScope.userId` **同 ID 空间**（不得拿 `member_id` 硬套管理员 ID）
- [ ] 方法调用链内**无顶层 `.or(...)`**；OR 组已按第 4 步括号包裹
- [ ] 无 Mapper XML / `@Select` 原生 SQL 承担该列表查询
- [ ] 所有"QueryWrapper 形态 + 无归属列表"调用已用 `DataPermissionHelper.ignore()` 包裹并写注释
- [ ] 详情/更新/删除已按第 6 步补显式归属校验或改用 QueryWrapper 形态
- [ ] 影响数据范围的写操作已接缓存失效（第 7 步）
- [ ] **手工越权用例验证通过**：用"本部门"角色账号，验证 ①列表看不到他部门 ②`GET /{id}` 拿不到他部门 ③`PUT`/`DELETE` 改不动他部门 ④搜索条件带 OR 时 ①②仍成立
- [ ] `mvn -o -pl :{module} test` 全绿；为新守卫逻辑补单测
- [ ] 若目标表含敏感列，同步按《[数据权限-列级业务表接入checklist.md](./数据权限-列级业务表接入checklist.md)》接列级

---

## 9. 常见故障速查

| 现象 | 根因 | 处置 |
| --- | --- | --- |
| `Unknown column 'dept_id' in 'where clause'` | `@DataPermission` 方法内调了 QueryWrapper 形态的关联表查询，未 `ignore()` | 第 5 步判定表包 `ignore()` |
| 列表能看到、`GET /{id}` 也能看到别人数据 | byId 不经过方言（预期行为） | 第 6 步补显式校验 |
| 加了注解但完全没过滤 | ①同类自调用未走代理 ②Mapper XML 原生 SQL ③登录人是超管/角色 `data_scope=1` ④`test` profile 总开关关闭 | 逐项排除 |
| 低权限账号列表**全空** | `userColumn` 用错 ID 空间；或 `create_by` 历史为 NULL；或部门集合解析为空且退化 SELF | 核对第 3 步；查数据；看 `DataScope` 日志 |
| 搜索框带 OR 条件后越权 | 第 4 步短路 | 改 `QueryMethods.bracket` 括号写法 |
| 调整角色数据范围后不生效 | 60s TTL / 多实例未广播 | 确认调了 `invalidateAll()`；文档口径"单实例立即、多实例最迟 60s" |
