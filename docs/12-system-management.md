# 系统管理四模块

范围：用户、角色、菜单、部门的管理端 CRUD 与授权。后端位于 `admin-system`，前端位于
`admin-web/apps/web-antd/src/views/system`，统一使用 `R` / `PageResult`、`@Validated`、
`@PreAuthorize` 和现有 `requestClient`，没有 mock。

## 接口清单

所有路径均带 context-path `/api`。

### 用户

| 方法 | 路径 | 权限码 | 说明 |
|---|---|---|---|
| GET | `/system/users` | `system:user:list` | 分页；参数 `page`、`pageSize`、`username`、`realName`、`status`、`deptId` |
| POST | `/system/users` | `system:user:create` | 新增；`username`、`password`、`realName`、`email`、`phone`、`deptId`、`status`、`description` |
| PUT | `/system/users/{id}` | `system:user:update` | 编辑；登录名不可改，其余同上（无密码） |
| DELETE | `/system/users/{id}` | `system:user:delete` | 删除；内置 `admin` 与当前登录用户受保护 |
| PUT | `/system/users/{id}/password` | `system:user:update` | 重置密码；body `{"password":"..."}` |
| PUT | `/system/users/{id}/status` | `system:user:update` | 启用/停用；body `{"status":0|1}` |
| PUT | `/system/users/{id}/roles` | `system:user:update` | 分配角色；body `{"roleIds":[2,3]}`，空数组表示清空 |

用户响应含 `deptName` 与 `roleIds`；角色下拉走 `GET /system/roles/options`。

### 角色

| 方法 | 路径 | 权限码 | 说明 |
|---|---|---|---|
| GET | `/system/roles` | `system:role:list` | 分页；参数 `page`、`pageSize`、`code`、`name`、`status` |
| GET | `/system/roles/options` | `system:role:list` | 全部启用角色，供用户分配角色 |
| GET | `/system/roles/permissions` | `system:role:list` | 全部权限点，供角色分配权限 |
| POST | `/system/roles` | `system:role:create` | 新增；`code`、`name`、`status`、`sort`、`remark` |
| PUT | `/system/roles/{id}` | `system:role:update` | 编辑；`super_admin` 的角色码和状态不可修改 |
| DELETE | `/system/roles/{id}` | `system:role:delete` | 删除；`super_admin` 或仍绑定用户的角色受保护 |
| GET | `/system/roles/{id}/menus` | `system:role:list` | 已分配菜单 ID |
| PUT | `/system/roles/{id}/menus` | `system:role:update` | 分配菜单；body `{"menuIds":[...]}` |
| GET | `/system/roles/{id}/permissions` | `system:role:list` | 已分配权限 ID |
| PUT | `/system/roles/{id}/permissions` | `system:role:update` | 分配权限；body `{"permissionIds":[...]}` |

角色码规则：小写字母开头，仅字母、数字、下划线，长度 2-64。

### 菜单

| 方法 | 路径 | 权限码 | 说明 |
|---|---|---|---|
| GET | `/system/menus` | `system:menu:list` | 树形；可选 `title`、`status` 过滤，匹配节点保留祖先 |
| POST | `/system/menus` | `system:menu:create` | 新增菜单/目录/按钮/内嵌/外链 |
| PUT | `/system/menus/{id}` | `system:menu:update` | 编辑 |
| DELETE | `/system/menus/{id}` | `system:menu:delete` | 删除；有子节点或角色引用时拒绝 |

菜单类型为 `catalog`、`menu`、`button`、`embedded`、`link`。父级不能是自身或自身子级。
新增/编辑带 `permissionCode` 时，如果权限点不存在会自动补一条 `sys_permission`，方便角色授权。

### 部门

| 方法 | 路径 | 权限码 | 说明 |
|---|---|---|---|
| GET | `/system/depts` | `system:dept:list` | 树形；可选 `name`、`status` 过滤，匹配节点保留祖先 |
| POST | `/system/depts` | `system:dept:create` | 新增；`parentId`、`name`、`leader`、`phone`、`sort`、`status` |
| PUT | `/system/depts/{id}` | `system:dept:update` | 编辑 |
| DELETE | `/system/depts/{id}` | `system:dept:delete` | 删除；有子部门或部门下有用户时拒绝 |

旧的 `GET /system/menu/list`、`GET /system/dept/list` 保留，分别返回启用菜单树和启用部门树。

## 错误码

| bizCode | HTTP | 场景 |
|---|---|---|
| `VALIDATION_ERROR` | 400 | 参数校验失败 |
| `USER_PROTECTED` | 403 | 删除/停用内置 `admin`、修改 admin 角色，或操作当前登录用户 |
| `ROLE_PROTECTED` | 403 | 修改 `super_admin` 角色码/状态、菜单或权限点，或删除该角色 |
| `ROLE_IN_USE` | 409 | 角色仍绑定用户，禁止删除 |
| `USER_NOT_FOUND` / `ROLE_NOT_FOUND` / `MENU_NOT_FOUND` / `DEPT_NOT_FOUND` | 404 | 目标不存在 |
| `USERNAME_EXISTS` / `ROLE_CODE_EXISTS` | 409 | 登录名/角色码重复 |
| `MENU_HAS_CHILDREN` / `MENU_IN_USE` | 409 | 菜单有子节点/被角色引用 |
| `DEPT_HAS_CHILDREN` / `DEPT_IN_USE` | 409 | 部门有子部门/有用户 |
| `FORBIDDEN` | 403 | 当前账号缺少接口权限 |

## 页面操作

| 页面 | 路径 | 操作 |
|---|---|---|
| 用户管理 | `/system/user` | 搜索、分页、新增/编辑抽屉、分配角色、重置密码、启用/停用、删除 |
| 角色管理 | `/system/role` | 搜索、分页、新增/编辑抽屉、分配菜单树、分配权限点、删除 |
| 菜单管理 | `/system/menu` | 树表、搜索、新增/编辑抽屉、删除 |
| 部门管理 | `/system/dept` | 树表、搜索、新增/编辑抽屉、删除 |

按钮通过 `v-access:code` 读取后端 `/auth/codes` 返回的权限码，不在前端硬编码角色名。
新增/编辑使用抽屉 + VbenForm，列表使用 VbenVxeGrid：

```text
列表 -> 新增 -> 编辑 -> 删除
角色 -> 分配菜单 / 分配权限
用户 -> 分配角色 / 重置密码 / 启用停用
```

## 启动与验证

```bash
cd deploy && docker compose up -d
cd ../admin-server
export DB_PASSWORD=premium_member_dev_only REDIS_PASSWORD=premium_member_dev_only
export JWT_ACCESS_SECRET="$(openssl rand -base64 48)"
export JWT_REFRESH_SECRET="$(openssl rand -base64 48)"
jenv exec /Users/hanson/work/Env/apache-maven-3.8.8/bin/mvn -q -DskipTests package
jenv exec java -jar admin-bootstrap/target/admin-bootstrap-0.1.0.jar
```

```bash
cd admin-web && pnpm dev:antd
pnpm -F @vben/web-antd run typecheck
```

浏览器访问 `http://127.0.0.1:5666`，使用 `admin / Admin@123456`。

## 已知行为

- 分配角色/菜单/权限后会清理受影响用户的 Redis 权限缓存；已有 access token 内嵌的权限在下次登录或刷新后才会变化。
- `status=0` 的菜单不会进入 `/menu/all` 路由，但管理端树仍可见并可在树表中编辑。
- 删除菜单不会级联删除自动生成的权限点，避免误删仍被角色引用的授权。
- 开发环境 Node 25 会触发上游 `^22.18 || ^24` 引擎警告，不影响本次 typecheck、dev 和真实浏览器验收。

## 截图

`docs/screenshots/system-*.png`，包含用户/角色/菜单/部门的列表、新增、编辑、授权和删除保护。
