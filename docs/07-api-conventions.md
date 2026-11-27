# 接口约定

## 统一响应

成功：

```json
{"code":0,"data":{},"error":null,"message":"ok"}
```

失败：

```json
{"code":-1,"data":null,"error":"没有权限","message":"没有权限","bizCode":"FORBIDDEN"}
```

字段：

| 字段 | 说明 |
|---|---|
| `code` | 0 成功，-1 失败；与 vben `successCode: 0` 对齐 |
| `data` | 业务数据，失败时为 null |
| `error` | 错误提示，成功时为 null |
| `message` | 提示信息 |
| `bizCode` | 机器可读错误码，仅失败时输出 |

HTTP 状态码同时使用真实语义：400/401/403/404/405/409/500。前端 axios 先按 HTTP 状态拦截，再按 `code` 判断成功。

## 错误码

| bizCode | HTTP | 场景 |
|---|---|---|
| `VALIDATION_ERROR` | 400 | 参数、验证码校验失败 |
| `UNAUTHORIZED` | 401 | 未登录、token 过期/被拉黑 |
| `TOKEN_INVALID` | 401 | JWT 签名或类型错误 |
| `INVALID_CREDENTIALS` | 403 | 用户名或密码错误 |
| `USER_DISABLED` | 403 | 账号停用 |
| `REFRESH_TOKEN_INVALID` | 403 | refresh token 缺失/过期/已撤销 |
| `FORBIDDEN` | 403 | 权限不足 |
| `NOT_FOUND` | 404 | 路由或资源不存在 |
| `MEMBER_NOT_FOUND` | 404 | 会员不存在 |
| `MEMBER_NO_EXISTS` | 409 | 会员编号冲突 |
| `USER_NOT_FOUND` / `ROLE_NOT_FOUND` / `MENU_NOT_FOUND` | 404 | 系统管理资源不存在 |
| `USERNAME_EXISTS` / `ROLE_CODE_EXISTS` | 409 | 登录名或角色码重复 |
| `ROLE_IN_USE` | 409 | 角色仍绑定用户，禁止删除 |
| `DEPT_HAS_CHILDREN` / `DEPT_IN_USE` | 409 | 部门存在子部门或用户 |
| `MENU_HAS_CHILDREN` / `MENU_IN_USE` | 409 | 菜单存在子节点或被角色引用 |
| `USER_PROTECTED` / `ROLE_PROTECTED` | 403 | 内置 admin 或 super_admin 受保护 |
| `INTERNAL_ERROR` | 500 | 未预期异常，响应不暴露细节 |

## 分页

请求参数与 vben mock 一致：`page` 从 1 开始，`pageSize` 最大 200。

```text
GET /api/members?page=1&pageSize=20&keyword=member&level=GOLD&status=1
```

返回：

```json
{"code":0,"data":{"items":[],"total":0},"error":null,"message":"ok"}
```

分页查询在 Mapper 层设置稳定排序：`created_at DESC, id DESC`，并关闭 count SQL 优化，避免 jsqlparser 版本差异导致 `COUNT()` 语法问题。

## 参数校验

- Controller 入参使用 `@Valid`；DTO 使用 Jakarta Validation 注解。
- 手机号、会员等级、状态、金额、积分都在边界校验。
- 校验失败返回 400，`message` 给出字段名和原因，例如 `username: 不能为空`。
- Service 对资源不存在、冲突、状态非法抛 `BusinessException`，由全局异常处理器转换。

## 异常转换

- `BusinessException`：保留业务码和用户可读消息。
- 参数异常：统一 400。
- `AccessDeniedException`：403。
- 未捕获异常：500，完整堆栈只写日志，响应固定“系统繁忙，请稍后重试”。
- 401/403 同时由 Security 入口点和全局异常处理器兜底。

## CORS

- 默认允许 `http://localhost:5666`、`http://127.0.0.1:5666`。
- 允许 `Authorization`、`Content-Type`、`Accept`、`X-Requested-With`。
- 允许 GET/POST/PUT/PATCH/DELETE/OPTIONS，`allowCredentials=true`。
- 生产通过 `CORS_ALLOWED_ORIGINS` 显式配置，不使用 `*`。

## 时间、金额、ID

- 时间：`OffsetDateTime`，JSON 为 ISO-8601；数据库为 `timestamptz`。
- 金额：`BigDecimal` + `NUMERIC(12,2)`，单位元。
- ID：数据库 `BIGINT`；`/user/info` 的 `userId` 为字符串，兼容 vben 类型，同时保留数字 `id`。
