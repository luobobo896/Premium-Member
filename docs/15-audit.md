# 操作审计

范围：管理端增删改与登录成败。表 `sys_operation_log`，切面
`OperationLogAspect`，页面 `/system/log`，接口 `GET /system/logs`。

## 表结构

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | BIGINT | 主键 |
| `user_id` | BIGINT | 操作人 ID；登录请求尚未建立会话时为空 |
| `username` | VARCHAR(64) | 操作人登录名；登录失败取请求参数中的用户名 |
| `module` | VARCHAR(64) | 认证 / 会员 / 用户 / 角色 / 菜单 / 部门 |
| `action` | VARCHAR(64) | 登录、新增、编辑、删除、分配角色等 |
| `method` | VARCHAR(16) | HTTP 方法 |
| `path` | VARCHAR(255) | 请求路径 |
| `params` | TEXT | 脱敏后的参数，最长 2000 字符 |
| `ip` | VARCHAR(64) | `X-Forwarded-For` / `X-Real-IP` / remoteAddr |
| `user_agent` | VARCHAR(512) | 截断后的 User-Agent |
| `success` | SMALLINT | 1 成功，0 失败 |
| `error_code` | VARCHAR(64) | 失败业务码，如 `INVALID_CREDENTIALS` |
| `cost_ms` | BIGINT | 方法执行耗时 |
| `created_at` | TIMESTAMPTZ | 写入时间 |

索引：`created_at DESC`、`user_id`。建表语句在 `schema.sql`，使用
`CREATE TABLE IF NOT EXISTS` / `CREATE INDEX IF NOT EXISTS`，可重复执行。

## 记录方式

- `@AuditLog(module, action)` 标在控制器方法上，AOP 环绕执行。
- 已覆盖：登录、会员增删改、用户增删改/启停/重置密码/分配角色、
  角色增删改/分配菜单/分配权限、菜单增删改、部门增删改。
- 成功与失败都记录；异常处理完再抛出原异常，不改变接口行为。
- 参数通过 Jackson 序列化后递归脱敏：字段名包含
  `password` / `token` / `secret` / `authorization` 的值替换为 `***`。
- 写库走 `auditExecutor` 异步线程池：core 1、max 2、队列 500，
  队列满时丢弃日志；写库异常只打应用日志，不影响主请求。
- 日志查询仅 `super_admin` 角色可用，其他角色返回 403 `FORBIDDEN`。
- 查询参数：`page`、`pageSize`、`username`、`module`、`startTime`、`endTime`；
  时间使用 ISO-8601。

## 实测

2026-11-27，真实接口与数据库：

```text
登录成功   -> success=1, params.password=***, username=admin
登录失败   -> success=0, error_code=INVALID_CREDENTIALS
会员新增   -> module=会员 action=新增 success=1 cost_ms=35
会员编辑   -> module=会员 action=编辑 success=1 cost_ms=29
会员删除   -> module=会员 action=删除 success=1 cost_ms=22
重复角色码 -> module=角色 action=新增 success=0 error_code=ROLE_CODE_EXISTS
self_manager GET /system/logs -> 403 FORBIDDEN
用户名过滤 self_manager       -> total=1
模块过滤 认证                  -> total=4
时间过滤 2026-11-27 12:05 起  -> total=8
```

页面截图：

```text
docs/screenshots/system-audit-list.png
docs/screenshots/system-audit-filter.png
```

## 保留策略（TODO）

目标：在线保留 180 天，避免日志表无限增长。

- TODO：增加每日 03:00 定时任务，分批删除 `created_at < now() - interval '180 days'`。
- TODO：删除前按天导出归档；生产先备份再清理。
- TODO：监控表大小与 `created_at` 最老记录，超过阈值告警。
- 当前状态：本轮只建表和索引，没有实现自动清理任务；上线前必须补。
- 容量参考：按每行 1 KB、每天 1 万次操作估算，180 天约 1.8 GB，
  需要结合真实 QPS 复核。

## 已知限制

- 登录请求在认证上下文建立前执行，`user_id` 为空，仅记录用户名。
- `@Valid` 校验失败发生在控制器方法调用前，不会进入本切面。
- 审计线程池满时日志会被丢弃；需要强审计时应改为持久化队列。
- 参数只保留前 2000 字符，超长请求体不会完整落库。
- 日志表没有删除接口和自动清理任务，保留策略仍是 TODO。
