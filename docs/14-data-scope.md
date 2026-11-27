# 数据权限

角色级数据范围，控制 `/system/users` 与 `/members` 可见数据。实现位于
`admin-system` 的 `DataScopeService`，会员侧通过 `pm_member.owner_id` 映射到用户。

## 数据范围

`sys_role.data_scope` 取值：

| 值 | 含义 |
|---|---|
| `ALL` | 全部数据 |
| `DEPT_AND_CHILD` | 本部门及所有下级部门 |
| `DEPT` | 本部门 |
| `SELF` | 仅本人 |

规则：

- `super_admin` 角色恒为 `ALL`，即使绑定其他角色也不收紧。
- 普通用户取所有启用角色中最严格的范围；优先级 `ALL < DEPT_AND_CHILD < DEPT < SELF`。
- 用户没有部门时，非 `ALL` 范围统一降级为 `SELF`。
- 新角色未传 `dataScope` 时默认 `SELF`。
- `/system/users` 按用户 ID 过滤；`/members` 按会员 `owner_id` 过滤。
- 会员详情、编辑、删除也会校验同一范围，越权访问返回 `MEMBER_NOT_FOUND`。
- 用户编辑、删除、重置密码、启停、分配角色同样校验范围，越权返回 `USER_NOT_FOUND`。
- `pm_member.owner_id` 为创建人；历史数据为空时只有 `ALL` 范围可见。

## 迁移

`schema.sql` 使用幂等 DDL，旧库重复执行不会报错：

```sql
ALTER TABLE sys_role ADD COLUMN IF NOT EXISTS data_scope VARCHAR(16) NOT NULL DEFAULT 'SELF';
ALTER TABLE pm_member ADD COLUMN IF NOT EXISTS owner_id BIGINT;
CREATE INDEX IF NOT EXISTS idx_pm_member_owner ON pm_member (owner_id);
```

`data_scope` 的 CHECK 约束通过 `pg_constraint` 判断后补充，可重复执行。
`data.sql` 为现有角色补齐 `ALL / DEPT / SELF`，新增 `dept_manager`、
`self_manager`、`dept_and_child` 三个验收角色，并固化三层部门
`总公司 -> 运营部 -> 运营二组`。用户分布在不同层级：`dept_manager` /
`self_manager` / `dept_and_child` 在运营部，`group_user` 在运营二组。

## 验收账号

开发种子密码均为 `Admin@123456`：

| 账号 | 部门 | 角色 | 数据范围 |
|---|---|---|---|
| `admin` | 总公司 1 | `super_admin` | `ALL` |
| `dept_manager` | 运营部 2 | `dept_manager` | `DEPT` |
| `dept_and_child` | 运营部 2 | `dept_and_child` | `DEPT_AND_CHILD` |
| `self_manager` | 运营部 2 | `self_manager` | `SELF` |
| `group_user` | 运营二组 6 | `viewer` | `SELF`（仅作为下级部门数据样本） |

种子会员归属：`member001 -> admin`、`member002 -> operator`、`member003 -> viewer`、
`member004 -> dept_manager`、`member005 -> self_manager`、`member006 -> group_user`。

## 实测

2026-11-27，真实接口：

```text
admin          members total=6  users total=7
dept_manager   members total=3  users total=4   (DEPT)
dept_and_child members total=4  users total=5   (DEPT_AND_CHILD)
self_manager   members total=1  users total=1
```

严格关系（会员、用户）：`DEPT 3 < DEPT_AND_CHILD 4 < ALL 6`、
`DEPT 4 < DEPT_AND_CHILD 5 < ALL 7`。三个非超管账号访问
`GET /system/logs` 均返回 403 `FORBIDDEN`。

`DEPT_AND_CHILD` 不再依赖临时数据：`dept_and_child` 属于运营部 2，
可见运营部及其子部门运营二组 6 的用户与会员；`dept_manager` 仅可见运营部 2。

越权直查：

```text
self_manager GET /members/2 -> 404 MEMBER_NOT_FOUND
self_manager GET /members/5 -> 200
```

角色表单数据范围下拉见 `docs/screenshots/system-role-datascope.png`；
两个 scope 的会员列表对比见 `docs/screenshots/data-scope-dept.png` 与
`docs/screenshots/data-scope-self.png`。

## 已知限制

- 范围在每次列表/详情请求时实时计算，不进入 JWT；角色调整立即影响下一次请求。
- `pm_member` 只有创建人维度，没有独立“会员归属部门”；跨部门转移会员需改 `owner_id`，
  当前页面未提供该字段。
- 用户新建接口未按范围限制目标部门；本轮只限制查询与已有数据操作。
