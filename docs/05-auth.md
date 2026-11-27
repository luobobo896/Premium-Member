# 认证、权限与菜单

## 接口

所有路径都在后端 context-path `/api` 下。

| 方法 | 路径 | 认证 | 说明 |
|---|---|---|---|
| POST | `/api/auth/login` | 否 | 用户名密码登录，返回 access token，写 `jwt` refresh Cookie |
| POST | `/api/auth/refresh` | Cookie | 用 refresh Cookie 换新 access token，返回裸字符串 |
| POST | `/api/auth/logout` | Cookie/可选 Bearer | 撤销 refresh token，拉黑当前 access token，清 Cookie |
| GET | `/api/auth/codes` | Bearer | 返回权限码数组，供按钮权限使用 |
| GET | `/api/auth/captcha` | 否 | 返回验证码 ID 与 PNG data URL |
| GET | `/api/user/info` | Bearer | 返回 vben `BasicUserInfo` 所需字段 |
| GET | `/api/menu/all` | Bearer | 返回 vben 后端模式路由树 |
| GET | `/api/system/menu/list` | Bearer + `system:menu:list` | 管理端菜单树，包含按钮 `authCode` |
| GET | `/api/system/dept/list` | Bearer + `system:dept:list` | 部门树 |

## 登录

请求：

```json
{"username":"admin","password":"Admin@123456"}
```

响应字段：

```json
{
  "code": 0,
  "data": {
    "accessToken": "<JWT>",
    "userId": 1,
    "username": "admin",
    "realName": "Administrator",
    "avatar": "",
    "description": "超级管理员",
    "homePath": "/analytics",
    "roles": ["super_admin"]
  },
  "error": null,
  "message": "ok"
}
```

同时设置：

```text
Set-Cookie: jwt=<refresh-token>; Path=/api; HttpOnly; SameSite=Lax
```

生产 HTTPS 下为 `Secure; SameSite=None`。

## Token

- access token：2 小时，JWT 内含 `uid`、`username`、`roles`、`perms`。
- refresh token：30 天，JWT 内含 `uid`、`username`、`jti`；`jti` 同时写入 Redis 白名单。
- 刷新时不旋转 refresh token，与 vben mock 行为一致；logout 删除白名单。
- vben `logoutApi` 使用 `baseRequestClient`，不会带 Authorization。后端从 refresh Cookie 解析用户 ID，并把该用户登出前签发的 access token 全部置为失效（Redis 用户级撤销时间）；因此只带 Cookie 的 logout 也能立即让旧 access token 返回 401。
- 请求头格式：`Authorization: Bearer <accessToken>`。

刷新接口返回裸字符串，不包 `R`。这是 vben `baseRequestClient` 的读取方式决定的，不能改成统一响应。

## 权限模型

```text
用户 -> sys_user_role -> 角色 -> sys_role_permission -> 权限码
                              -> sys_role_menu       -> 菜单/按钮
```

- `/auth/codes` 返回 `sys_permission.code` 去重列表。
- 后端 `@PreAuthorize("hasAuthority('member:create')")` 做最终鉴权。
- 前端按钮权限用同一批权限码；`/system/menu/list` 的按钮节点 `authCode` 与该列表对应。
- 菜单按钮节点不进入 `/menu/all`，只进入管理端菜单树。

## 菜单转换

`/menu/all` 按 vben `RouteRecordStringComponent` 输出：

```json
{
  "id": 1,
  "name": "Dashboard",
  "path": "/dashboard",
  "redirect": "/analytics",
  "meta": {
    "title": "仪表盘",
    "icon": "lucide:layout-dashboard",
    "order": -1,
    "keepAlive": false,
    "affixTab": false,
    "hideInMenu": false
  },
  "children": []
}
```

- `catalog` 节点 `component` 为空，vben 用基础布局承载子路由。
- `menu` 节点使用数据库 `component`。
- `link`/`embedded` 且未配置组件时输出 `IFrameView`。
- `button` 节点不进入路由树。
- `visible=0` 或 `hide_in_menu=1` 输出 `meta.hideInMenu=true`。

## vben 契约差异与前端配置

以 `/tmp/vben-ref`（tag v5.7.0）源码核对，差异记录如下：

1. **成功码**：vben `defaultResponseInterceptor` 使用 `successCode: 0`，字段 `code/data`。后端一致；错误码额外提供 `bizCode`，`code` 仍为 `-1`。
2. **刷新响应**：`baseRequestClient` 默认 `responseReturn: 'raw'`，`refreshTokenApi()` 读取 `resp.data`，所以 `/auth/refresh` 必须返回裸字符串。后端已按此实现，实测 `content-type: text/plain`。
3. **refresh 失败状态**：vben mock 刷新失败返回 HTTP 403；后端沿用 403 + `REFRESH_TOKEN_INVALID`。
4. **密码错误状态**：vben mock 返回 HTTP 403；后端沿用 403 + `INVALID_CREDENTIALS`，不返回 401，避免触发前端刷新流程。
5. **`/user/info` 类型冲突**：vben 的 `UserInfo` TypeScript 类型要求 `userId/avatar/desc/token`，但 mock 运行时代码返回 `id/realName/roles/username/homePath`。后端同时返回 `userId`（字符串）和 `id`（数字），并补齐 `avatar/desc/homePath/roles`；不返回 `token`，因为运行时没有消费者。
6. **前端权限模式**：`admin-web/apps/web-antd/src/preferences.ts` 从 `VITE_ACCESS_MODE` 读取权限模式，`.env.development` / `.env.production` 均设为 `backend`；同时固定 `enableRefreshToken: true`。无需再手改源码：

```ts
app: {
  accessMode: import.meta.env.VITE_ACCESS_MODE,
  enableRefreshToken: true,
}
```

7. **开发代理**：`admin-web/apps/web-antd/vite.config.ts` 把 `/api` 重写到 `http://127.0.0.1:5320/api`，浏览器侧基地址仍为 `/api`。Nitro mock 已在 `.env.development` 关闭。
8. **菜单组件路径**：种子菜单的组件路径均已在 admin-web 落地：`/member/list/index` 是真实会员列表，`/system/menu|dept|role|user/index` 是最小占位页，保留后端菜单结构且不再 404。
9. **Cookie 时长**：vben mock 的 `jwt` Cookie `maxAge` 是 24 小时，但 refresh JWT 是 30 天；后端统一为 30 天，避免 Cookie 先于 token 失效。
10. **验证码**：后端提供 `/auth/captcha` 并在 `LoginRequest` 中支持可选 `captchaId/captchaCode`；不传时不强制。admin-web 登录页已移除上游 mock 的滑块和账号下拉，只保留真实用户名/密码表单。

## 安全约束

- 登录失败统一返回“用户名或密码错误”，不区分用户不存在与密码错误。
- 停用账号登录返回 403 `USER_DISABLED`。
- 认证失败返回 401，权限不足返回 403；响应不包含堆栈、SQL 或内部类名。
- `/auth/codes`、`/user/info`、`/menu/all` 都必须带有效 access token。
