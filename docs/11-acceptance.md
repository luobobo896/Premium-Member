# 验收记录

环境：macOS 15.7.5 arm64，JDK 21.0.9，Maven 3.8.8，Docker 29.8.2，2026-11-27。

## 依赖

```text
$ cd deploy && docker compose up -d
premium-member-postgres  postgres:17-alpine  Up (healthy)  15432
premium-member-redis     redis:8.2-alpine    Up (healthy)  16379
```

初始化后：

```text
users | menus | perms | members
    3 |    27 |    22 |       3
```

## 构建

```text
$ jenv exec /Users/hanson/work/Env/apache-maven-3.8.8/bin/mvn -q -DskipTests package
$ ls -lh admin-bootstrap/target/admin-bootstrap-0.1.0.jar
49M admin-bootstrap-0.1.0.jar
```

`admin-web`：

```text
$ pnpm run build:antd
Tasks: 11 successful, 11 total
Time: 13.067s
```

`c-h5`：`npm run build` 成功，Vite 8.4.0。  
`c-app`：`npm run build:h5` 成功，Compiler version 5.34（vue3）。

## 登录

```text
$ curl -sS -c /tmp/pm-cookies.txt -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Admin@123456"}' \
  http://127.0.0.1:5320/api/auth/login
login=200
```

响应（token 已截断）：

```json
{
  "code": 0,
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...（967 字符）",
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

Cookie：

```text
#HttpOnly_127.0.0.1 FALSE /api FALSE <exp> jwt <refresh-token>
```

## 刷新

```text
$ curl -b /tmp/pm-cookies.txt -c /tmp/pm-cookies.txt -X POST \
  http://127.0.0.1:5320/api/auth/refresh
refresh=200 type=text/plain
new_token=eyJhbGciOiJIUzUxMiJ9.eyJ...ov-_Ncvg
```

返回裸 access token，符合 vben `baseRequestClient` 的读取方式。

## 用户信息 / 菜单 / 权限码

```text
user_info=200
{
  "userId": "1",
  "id": 1,
  "username": "admin",
  "realName": "Administrator",
  "avatar": "",
  "desc": "超级管理员",
  "homePath": "/analytics",
  "roles": ["super_admin"]
}
menu_all=200 menu_count=4
codes=200 code_count=22
```

菜单顶层为 Dashboard、Member、System、About；按钮节点不进入 `/menu/all`。

## 按钮权限

viewer 创建会员：

```text
$ curl -H "Authorization: Bearer $VIEWER_TOKEN" \
  -d '{"username":"blocked","level":"BASIC",...}' \
  http://127.0.0.1:5320/api/members
viewer_create_member=403
{"code":-1,"data":null,"error":"没有权限","message":"没有权限","bizCode":"FORBIDDEN"}
```

admin 创建会员：

```text
admin_create_member=200
memberNo=M20261127139166
```

## 会员分页与 CRUD

```text
member_page=200
{"code":0,"total":3,"items":2,"first":"M202611270003"}

member_update=200
nickname=Alice Updated, level=PLATINUM, balance=20.00

member_delete=200
{"code":0,"data":null,"error":null,"message":"ok"}
```

## 菜单管理 / 部门

```text
system_menu=200 top=4
[{"id":1,"type":"catalog","authCode":"dashboard:view","children":2},
 {"id":10,"type":"catalog","authCode":null,"children":1},
 {"id":20,"type":"catalog","authCode":null,"children":4},
 {"id":30,"type":"menu","authCode":null,"children":0}]

dept_list=200（总公司 -> 运营部/技术部）
```

## OpenAPI / Swagger

```text
openapi=200 bytes=11460
title=Premium-Member Admin API paths=12
swagger_ui=200
```

## 服务端验证码

```text
captcha=200
captcha_id=52355243... image_prefix=data:image/png;base64,iVBORw0K image_len=2106
captcha_login=200 code=0 user=admin
captcha_reuse=400
{"code":-1,"data":null,"error":"验证码错误或已过期","message":"验证码错误或已过期","bizCode":"VALIDATION_ERROR"}
```

验证码文本存 Redis，5 分钟过期；校验后立即删除，重复使用返回 400。

## 401 / 403 / 登出

```text
no_token_userinfo=401
{"code":-1,"data":null,"error":"未登录或登录已过期","message":"未登录或登录已过期","bizCode":"UNAUTHORIZED"}

logout=200
{"code":0,"data":"","error":null,"message":"ok"}

refresh_after_logout=403
{"bizCode":"REFRESH_TOKEN_INVALID"}

userinfo_after_logout=401（未携带 Authorization 的 logout 也能使旧 access token 失效）
```

登出同时撤销 refresh 白名单并把当前 access token 加入 Redis 黑名单。

## admin-web 浏览器联调（2026-11-27）

环境：vben dev `http://127.0.0.1:5666`，admin-server `http://127.0.0.1:5320/api`，后端启动参数 `JWT_ACCESS_TTL=60s`。

```text
login admin/Admin@123456 -> http://127.0.0.1:5666/analytics
菜单：仪表盘 / 会员管理 / 系统管理 / 关于
/members        会员列表 - Premium Member Admin
/workspace      工作台 - Premium Member Admin
/system/menu    菜单管理 - Premium Member Admin
/system/dept    部门管理 - Premium Member Admin
/system/role    角色管理 - Premium Member Admin
/system/user    用户管理 - Premium Member Admin
/about          关于 - Premium Member Admin
```

以上 8 个菜单路由逐个访问均为真实页面或占位页，没有 404。

会员列表分页（`pageSize=2`）：

```text
GET /api/members?page=1&pageSize=2  200  首页 2 条，total=3
GET /api/members?page=2&pageSize=2  401  access token 已过期
POST /api/auth/refresh              200
GET /api/members?page=2&pageSize=2  200  第 2 页出现 M202611270001 张三
```

刷新后 access token 已替换。退出登录后回到 `/auth/login`，本地 access 状态为 `accessToken: null`；登出前保存的 access token 再请求 `/api/members` 返回 401 `UNAUTHORIZED`。

截图：`docs/screenshots/01-login.png`、`02-home.png`、`03-members-page2-after-refresh.png`、`04-members-page1.png`、`05-placeholder-system-user.png`、`06-logged-out.png`。

## 未实测项与风险

1. 系统管理 4 个菜单当前是占位页，只保证菜单结构完整、点击不 404，没有 CRUD 交互。
2. 会员列表只读分页，新增/编辑/删除按钮未接入页面；后端接口已具备。
3. 当前没有角色/权限写接口，权限缓存 10 分钟内可能滞后；新增角色编辑时必须调用 `PermissionCache.evict(userId)`。
4. 没有登录失败锁定、限流和 refresh token 旋转；生产接入网关限流或在后续迭代补充。
5. 生产跨域 Cookie 必须 HTTPS + `Secure` + `SameSite=None`；当前 dev 为 HTTP + Lax。
6. c-server、c-app、c-h5 按指示冻结，只验证了骨架启动/构建，没有业务功能。
