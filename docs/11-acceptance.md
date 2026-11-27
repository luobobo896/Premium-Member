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

## 未实测项与风险

1. admin-web 只做了生产构建，没有用浏览器完整走一次登录页交互；接口契约和 SPA 构建均通过，浏览器端仍建议人工点一遍。
2. admin-web 基座没有会员/系统管理页面，种子菜单指向的 `/member/list/index`、`/system/menu/index` 等组件在基座中不存在，点击会落到 not-found。需要前端补页面，后端菜单结构不需要改。
3. vben 默认 `accessMode=frontend`、`enableRefreshToken=false`，不改配置就不会调用 `/menu/all`，也不会自动刷新。
4. 当前没有角色/权限写接口，权限缓存 10 分钟内可能滞后；新增角色编辑时必须调用 `PermissionCache.evict(userId)`。
5. 没有登录失败锁定、限流和 refresh token 旋转；生产接入网关限流或在后续迭代补充。
6. 生产跨域 Cookie 必须 HTTPS + `Secure` + `SameSite=None`；当前 dev 为 HTTP + Lax。
7. c-server、c-app、c-h5 按指示冻结，只验证了骨架启动/构建，没有业务功能。
