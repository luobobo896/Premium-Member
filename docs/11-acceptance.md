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

## 系统管理 API

```text
GET /api/system/users?page=1&pageSize=5        200 total=3
GET /api/system/roles?page=1&pageSize=5        200 total=3
GET /api/system/roles/options                  200 size=3
GET /api/system/roles/permissions              200 size=22
GET /api/system/roles/2/menus                  200 size=9
GET /api/system/roles/2/permissions            200 size=6
GET /api/system/depts                          200 1 棵树
GET /api/system/menus                          200 4 棵树
```

部门、角色、菜单、用户均完成新增/编辑/删除链路，所有请求 200：

```text
dept_create=200  dept_update=200  dept_delete=200
role_create=200  role_update=200  role_menus=200  role_perms=200  role_delete=200
menu_create=200  menu_update=200  menu_delete=200
user_create=200  user_roles=200   user_update=200  user_password=200  user_delete=200
```

保护规则：

```text
role_in_use=409             biz=ROLE_IN_USE
role_protected_delete=403   biz=ROLE_PROTECTED
role_protected_menu=403     biz=ROLE_PROTECTED
role_protected_status=403   biz=ROLE_PROTECTED
user_protected_roles=403    biz=USER_PROTECTED
dept_has_children=409       biz=DEPT_HAS_CHILDREN
menu_in_use=409             biz=MENU_IN_USE
username_exists=409         biz=USERNAME_EXISTS
role_code_exists=409        biz=ROLE_CODE_EXISTS
```

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
/system/user    用户管理 - Premium Member Admin（真实列表：3 条种子用户、角色标签、编辑/分配角色/重置密码/启停/删除）
/about          关于 - Premium Member Admin
```

以上 8 个菜单路由逐个访问均为真实页面，没有 404。

会员列表分页（`pageSize=2`）：

```text
GET /api/members?page=1&pageSize=2  200  首页 2 条，total=3
GET /api/members?page=2&pageSize=2  401  access token 已过期
POST /api/auth/refresh              200
GET /api/members?page=2&pageSize=2  200  第 2 页出现 M202611270001 张三
```

刷新后 access token 已替换。退出登录后回到 `/auth/login`，本地 access 状态为 `accessToken: null`；登出前保存的 access token 再请求 `/api/members` 返回 401 `UNAUTHORIZED`。

截图：`docs/screenshots/01-login.png`、`02-home.png`、`03-members-page2-after-refresh.png`、`04-members-page1.png`、`05-system-user-list.png`、`06-logged-out.png`、`07-system-user-create.png`。

## 未实测项与风险

1. 会员列表已接入新增/编辑/删除/详情抽屉（item 12 阶段 1），删除为逻辑删除。
2. 角色/权限变更会清理相关用户的权限缓存；但 access token 内的权限快照最长保留 2 小时，立即生效需要重新登录或缩短 access TTL。
3. 没有登录失败锁定、限流和 refresh token 旋转；生产接入网关限流或在后续迭代补充。
4. 生产跨域 Cookie 必须 HTTPS + `Secure` + `SameSite=None`；当前 dev 为 HTTP + Lax。
5. c-server、c-app、c-h5 按指示冻结，只验证了骨架启动/构建，没有业务功能。

## 系统管理四模块（item 8，2026-11-27 补充）

本节覆盖用户、角色、菜单、部门四模块。

### 构建与前端检查

```text
$ jenv exec /Users/hanson/work/Env/apache-maven-3.8.8/bin/mvn -q -DskipTests package
BUILD SUCCESS（admin-common/framework/system/member/bootstrap 全部编译通过）

$ pnpm -F @vben/web-antd run typecheck
vue-tsc --noEmit --skipLibCheck
通过（Node 25 触发上游 engines 警告，不影响结果）

$ pnpm run build:antd
Tasks: 11 successful, 11 total
```

### 删除保护原文补充

上半部分已列 bizCode；这里补关键错误文案，确认真实接口和页面提示一致：

```text
admin 删除自己  USER_PROTECTED「内置 admin 用户不能删除」
临时用户删自己  USER_PROTECTED「不能删除当前登录用户」
super_admin     ROLE_PROTECTED「super_admin 角色受保护」
父菜单          MENU_HAS_CHILDREN「存在子菜单，不能删除」
被角色引用菜单  MENU_IN_USE「菜单已被角色引用，不能删除」
父部门          DEPT_HAS_CHILDREN「存在子部门，不能删除」
有用户部门      DEPT_IN_USE「部门下存在用户，不能删除」
viewer 用户列表 FORBIDDEN「没有权限」
```

参数校验：

```text
重复登录名 -> 409 USERNAME_EXISTS「登录名已存在」
密码 3 位  -> 400 VALIDATION_ERROR「password: 个数必须在6和64之间」
```

### 浏览器真跑（ego-browser）

环境：`pnpm dev:antd` @ `http://127.0.0.1:5666`，admin-server @ `http://127.0.0.1:5320/api`，
账号 `admin / Admin@123456`。

已走通：

```text
用户：列表 -> 新增 -> 编辑 -> 分配角色 -> 删除；admin 删除被阻止
角色：列表 -> 新增 -> 编辑 -> 分配菜单（23 个菜单 ID 落库）-> 分配权限（4 个权限 ID 落库）-> 删除；
      super_admin 删除被阻止
菜单：树表 -> 新增目录 -> 新增子菜单 -> 编辑 -> 删除父级被阻止 -> 删除子级和父级
部门：树表 -> 新增父部门 -> 新增子部门 -> 编辑 -> 删除父级被阻止 -> 删除子级和父级；
      删除有用户的技术部被阻止
```

截图：

```text
docs/screenshots/05-system-user-list.png
docs/screenshots/07-system-user-create.png
docs/screenshots/system-user-created.png
docs/screenshots/system-user-edit.png
docs/screenshots/system-user-roles.png
docs/screenshots/system-user-delete-protected.png
docs/screenshots/system-role-list.png
docs/screenshots/system-role-create.png
docs/screenshots/system-role-menus.png
docs/screenshots/system-role-permissions.png
docs/screenshots/system-role-delete-protected.png
docs/screenshots/system-menu-list.png
docs/screenshots/system-menu-create.png
docs/screenshots/system-menu-edit.png
docs/screenshots/system-menu-delete-protected.png
docs/screenshots/system-dept-list.png
docs/screenshots/system-dept-create.png
docs/screenshots/system-dept-edit.png
docs/screenshots/system-dept-delete-protected.png
docs/screenshots/system-dept-in-use.png
```

### 收尾状态与未验证项

测试记录已删除，最后执行 `docker compose down -v && docker compose up -d` 重建开发库，
重新确认种子数据为用户 3、角色 3、菜单根 4、部门根 1、权限点 22。

未验证/已知限制：

1. 自身删除只在接口层验证（临时用户 + 超级管理员角色），没有单独做浏览器截图；浏览器实测了 admin 和 super_admin 两条保护。
2. 角色/菜单/权限调整后会清理 Redis 权限缓存，但当前 access token 内嵌权限不变，需要重新登录或刷新才生效。
3. 菜单新增带权限标识会自动补权限点；删除菜单不级联删除权限点，避免误删仍被角色引用的授权。
4. 浏览器验收使用本机 Node 25，和上游声明的 `^22.18 || ^24` 不一致，但 typecheck、dev、页面操作均通过。

## 会员 CRUD 前端（item 12 阶段 1，2026-11-27）

环境：vben dev `http://127.0.0.1:5666`，admin-server `http://127.0.0.1:5320/api`，
账号 `admin / Admin@123456`。

浏览器实际走通：

```text
/members -> 新增会员
  username=qa_member_01 nickname=验收会员 phone=13911112222 level=GOLD
  points=88 balance=12.34 status=启用
  -> 保存成功，total=4，仍停留第 1 页

同页 -> 编辑
  nickname=验收会员已编辑 balance=20.50 level=PLATINUM
  -> 保存成功，列表即时刷新，仍停留第 1 页

同页 -> 详情抽屉
  -> 会员编号、登录名、昵称、手机号、等级、积分、余额、状态、入会时间、备注齐全

同页 -> 删除 -> 确认弹窗 -> 确定
  -> 已删除，total=3，分页保持第 1 页
```

后端校验错误按原提示展示，抽屉不关闭：

```text
phone=123 保存 -> phone: 手机号格式不正确
```

截图：

```text
docs/screenshots/member-create.png
docs/screenshots/member-edit.png
docs/screenshots/member-detail.png
docs/screenshots/member-delete-confirm.png
```

## 数据权限（item 12 阶段 2，2026-11-27）

后端 `sys_role.data_scope` + `DataScopeService`，前端角色表单增加「数据范围」下拉；
`/system/users` 与 `/members` 按当前用户最严格角色范围过滤。

真实接口对比：

```text
admin        members=4  users=5
dept_manager members=2  users=3
self_manager members=1  users=1
self_manager GET /members/2 -> 404 MEMBER_NOT_FOUND
```

`DEPT_AND_CHILD` 用临时子部门验证：`DEPT` 时 3 用户/2 会员，切到
`DEPT_AND_CHILD` 后 4 用户/3 会员，改回后恢复。临时数据已清理。

截图：

```text
docs/screenshots/system-role-datascope.png
docs/screenshots/data-scope-dept.png
docs/screenshots/data-scope-self.png
```

详细规则与迁移见 `docs/14-data-scope.md`。

## 操作审计（item 12 阶段 3，2026-11-27）

`sys_operation_log` + `@AuditLog` AOP 异步写入，登录成败与会员/用户/角色/
菜单/部门增删改均已落库；参数中的密码替换为 `***`。

真实记录：

```text
登录成功        success=1 password=***
登录失败        success=0 error_code=INVALID_CREDENTIALS
会员新增/编辑/删除  success=1
重复角色码新增   success=0 error_code=ROLE_CODE_EXISTS
self_manager GET /system/logs -> 403 FORBIDDEN
admin GET /system/logs        -> total=8
用户名过滤 self_manager       -> total=1
模块过滤 认证                  -> total=4
```

截图：

```text
docs/screenshots/system-audit-list.png
docs/screenshots/system-audit-filter.png
```

保留策略与限制见 `docs/15-audit.md`。

## 审计可靠性修复（item 16，2026-11-27）

基线 `8c9c720`。环境：macOS 15.7.5 arm64，JDK 21.0.9，PostgreSQL 17.11，
Redis 8.2，后端 `http://127.0.0.1:5320/api`。

构建：

```text
$ cd admin-server
$ jenv exec /Users/hanson/work/Env/apache-maven-3.8.8/bin/mvn -q -DskipTests package
BUILD SUCCESS
```

重建种子库后：

```text
users=7  depts=4  roles=6  members=6
总公司 1 -> 运营部 2 -> 运营二组 6
技术部 3 为总公司下的并列部门
```

四账号真实接口（`pageSize=1` 取 `data.total`）：

```text
admin          members=6  users=7  GET /system/logs=200
dept_manager   members=3  users=4  GET /system/logs=403 FORBIDDEN
dept_and_child members=4  users=5  GET /system/logs=403 FORBIDDEN
self_manager   members=1  users=1  GET /system/logs=403 FORBIDDEN
```

`DEPT_AND_CHILD` 会员/用户数严格大于 `DEPT` 且小于 `ALL`。

登录审计：

```text
成功登录 user_id: admin=1 dept_manager=4 dept_and_child=6 self_manager=5
错误密码      success=0 error_code=INVALID_PASSWORD
账号不存在    success=0 error_code=ACCOUNT_NOT_FOUND
账号停用      success=0 error_code=USER_DISABLED
三类失败 params.password 均为 ***
```

参数校验审计（响应内容保持原样，第一条字段错误）：

```text
登录 username 为空       HTTP 400  审计 params=username: 不能为空
会员 phone/level 非法    HTTP 400  审计 params=username: 不能为空; phone: 手机号格式不正确; level: 会员等级不合法
登录 JSON 截断          HTTP 400  审计 params=请求参数无法解析
```

异步队列与告警（`AUDIT_QUEUE_CAPACITY=1`，锁表模拟写库阻塞）：

```text
8 个并发失败登录 + 1 次计时请求 -> 3 条入库，6 条丢弃
GET /api/health -> {"audit":{"queueCapacity":1,"dropped":6}}
WARN Audit queue full, dropped 5 operation logs (total 5)
写库阻塞期间登录仍 0.080s 返回 403，主流程未被审计拖住
```

保留期清理（为缩短等待使用 `AUDIT_RETENTION_CRON=*/10 * * * * *`）：

```text
启动前插入 created_at = now() - 200 days 的 retention_probe -> old=1
启动后首个 cron 周期 -> deleted=1，最近日志与 old_probe=0
```

说明：上面两次验证通过环境变量临时缩短队列与 cron；生产默认值仍为
队列 500、每天 03:00、保留 90 天，见 `docs/15-audit.md`。

`ConstraintViolationException` 分支已在全局异常处理里实现，但现有控制器
没有 `@Validated`，本轮没有真实触发点，未单独造数。

本轮未改 admin-web、c-app、c-h5，未重新跑前端构建。

## AI 创作（item 26，2026-11-27）

基线 `465dae0`。详细设计与接口见 `docs/18-ai.md`。

构建：

```text
admin-server  mvn -q -DskipTests package   成功
c-server      mvn -q -DskipTests package   成功
admin-web     pnpm run build:antd          11 tasks successful
c-h5          npm run build                成功
c-app         npm run build:h5             成功
```

真实接口验收：

```text
GET /c/ai/templates?type=PHOTO -> 200, 8 条
POST /c/ai/assets               -> 200, 本地 PNG
POST /c/ai/tasks                -> 200, QUEUED
GET /c/ai/tasks/{taskNo}        -> RUNNING -> SUCCESS, 输出 900x1200 PNG
DELETE /c/ai/works/{id}         -> 200, 产出 URL 随后 404
```

额度与幂等：

```text
同 idempotency_key 并发 5 次 -> ai_task 计数 = 1
图片额度 20/20 后的下一次请求 -> HTTP 409, AI_QUOTA_EXHAUSTED
视频额度 3/3 后第 4 次     -> HTTP 409, AI_QUOTA_EXHAUSTED
FAIL 任务 -> FAILED, 账本 RELEASE +1
UNKNOWN 任务 -> UNKNOWN, 无 RELEASE, 额度保持占用
```

跨期结算：

```text
提交任务时 period=8
完成前把会员切到新周期
完成详情: task_period=8
账本: settle_period=8
```

截图见 `docs/screenshots/ai-*.png`。未接入真实供应商、未做 RUNNING 悬挂任务对账、
文件下载未接签名 URL、c-app 未做真机验证，详见 `docs/18-ai.md`。
