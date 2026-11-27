# 操作审计

范围：管理端增删改与登录成败。表 `sys_operation_log`，切面
`OperationLogAspect`，页面 `/system/log`，接口 `GET /system/logs`。

## 表结构

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | BIGINT | 主键 |
| `user_id` | BIGINT | 操作人 ID；登录成功由认证服务补写，登录失败与未认证校验请求为空 |
| `username` | VARCHAR(64) | 操作人登录名；登录失败取请求参数中的用户名 |
| `module` | VARCHAR(64) | 认证 / 会员 / 用户 / 角色 / 菜单 / 部门 |
| `action` | VARCHAR(64) | 登录、新增、编辑、删除、分配角色等 |
| `method` | VARCHAR(16) | HTTP 方法 |
| `path` | VARCHAR(255) | 请求路径 |
| `params` | TEXT | 脱敏后的参数，最长 2000 字符 |
| `ip` | VARCHAR(64) | `X-Forwarded-For` / `X-Real-IP` / remoteAddr |
| `user_agent` | VARCHAR(512) | 截断后的 User-Agent |
| `success` | SMALLINT | 1 成功，0 失败 |
| `error_code` | VARCHAR(64) | 失败原因码：`INVALID_PASSWORD`、`ACCOUNT_NOT_FOUND`、`USER_DISABLED`、`VALIDATION_ERROR` 等 |
| `cost_ms` | BIGINT | 方法执行耗时 |
| `created_at` | TIMESTAMPTZ | 写入时间 |

索引：`created_at DESC`、`user_id`。建表语句在 `schema.sql`，使用
`CREATE TABLE IF NOT EXISTS` / `CREATE INDEX IF NOT EXISTS`，可重复执行。

## 记录方式

- `@AuditLog(module, action)` 标在控制器方法上，AOP 环绕执行。
- 已覆盖：登录、会员增删改、用户增删改/启停/重置密码/分配角色、
  角色增删改/分配菜单/分配权限、菜单增删改、部门增删改。
- 成功与失败都记录；异常处理完再抛出原异常，不改变接口行为。
- 登录成功时 `AuthService` 把用户 ID 放入请求级 `AuditContext`，切面写入 `user_id`。
- 登录失败对外仍统一返回 `INVALID_CREDENTIALS`，审计按实际原因区分：
  `INVALID_PASSWORD` / `ACCOUNT_NOT_FOUND` / `USER_DISABLED`。
- `MethodArgumentNotValidException`、`ConstraintViolationException`、
  `HttpMessageNotReadableException` 在 `GlobalExceptionHandler` 补记，
  模块/动作按路径识别登录或普通请求；字段错误摘要最长 2000 字符，
  `password` / `token` / `secret` / `authorization` 字段的值替换为 `***`，
  请求体无法解析时只记录固定摘要，不落原始报文。
- 参数通过 Jackson 序列化后递归脱敏：字段名包含
  `password` / `token` / `secret` / `authorization` 的值替换为 `***`。
- 写库走 `auditExecutor` 异步线程池：core 1、max 2，队列容量由
  `app.audit.queue-capacity`（默认 500）控制；队列满时丢弃并累加计数，
  主请求不等审计写库。
- 丢弃计数通过 `GET /api/health` 的 `audit.dropped` / `audit.queueCapacity`
  暴露；默认每 60 秒对新增丢弃量打一次 WARN，间隔由
  `app.audit.drop-report-interval-ms` 控制。
- 日志查询仅 `super_admin` 角色可用，其他角色返回 403 `FORBIDDEN`。
- 查询参数：`page`、`pageSize`、`username`、`module`、`startTime`、`endTime`；
  时间使用 ISO-8601。

## 配置

| 配置 | 环境变量 | 默认值 | 说明 |
|---|---|---|---|
| `app.audit.queue-capacity` | `AUDIT_QUEUE_CAPACITY` | `500` | 审计队列容量，0 表示不排队 |
| `app.audit.drop-report-interval-ms` | `AUDIT_DROP_REPORT_INTERVAL_MS` | `60000` | 丢弃量 WARN 汇报间隔 |
| `app.audit.retention-enabled` | `AUDIT_RETENTION_ENABLED` | `true` | 是否启用每日清理 |
| `app.audit.retention-days` | `AUDIT_RETENTION_DAYS` | `90` | 在线保留天数 |
| `app.audit.retention-cron` | `AUDIT_RETENTION_CRON` | `0 0 3 * * *` | 清理时间，默认每天 03:00 |

保留任务用 cron 调度，应用启动不会立即执行；每批删除 1000 行，循环到不足一批。

## 实测

2026-11-27，基线 `8c9c720`，真实接口与 PostgreSQL：

```text
登录成功 user_id:
  admin=1  dept_manager=4  dept_and_child=6  self_manager=5

登录失败:
  admin       error_code=INVALID_PASSWORD  params.password=***
  ghost_user  error_code=ACCOUNT_NOT_FOUND params.password=***
  self_manager（临时停用）error_code=USER_DISABLED params.password=***

参数校验:
  登录 username 为空      -> 400, 审计 module=认证 action=登录
                             error_code=VALIDATION_ERROR, params=username: 不能为空
  会员 username/phone/level 错误 -> 400, 响应仍为第一条错误
                             审计 params=username: 不能为空; phone: 手机号格式不正确; level: 会员等级不合法
  登录 JSON 截断          -> 400, 审计 params=请求参数无法解析

权限:
  admin GET /system/logs        -> 200
  dept_manager                  -> 403 FORBIDDEN
  dept_and_child               -> 403 FORBIDDEN
  self_manager                 -> 403 FORBIDDEN
```

队列丢弃（`AUDIT_QUEUE_CAPACITY=1`，锁定 `sys_operation_log` 表模拟写库阻塞）：

```text
8 个并发失败登录 + 1 次计时请求 -> 3 条入库，6 条丢弃
GET /api/health -> {"audit":{"queueCapacity":1,"dropped":6}}
WARN Audit queue full, dropped 5 operation logs (total 5)
写库被锁期间单次登录仍 0.080s 返回 403，主流程未被审计阻塞
```

保留期清理（为缩短等待使用 `AUDIT_RETENTION_CRON=*/10 * * * * *`）：

```text
启动前插入 created_at = now() - 200 days 的 retention_probe -> old=1
启动后首个 cron 周期 -> Audit retention cleanup finished: deleted=1
最近日志保留，old_probe=0
```

页面截图：

```text
docs/screenshots/system-audit-list.png
docs/screenshots/system-audit-filter.png
```

## 已知限制

- 审计队列满时仍会丢弃日志；当前只保证计数、告警和主流程不被阻塞。
- 字段错误摘要最多 2000 字符，超长错误会被截断。
- 登录请求体无法解析时拿不到尝试的用户名，只能记录路径与固定摘要。
- 现有控制器未使用 `@Validated`，`ConstraintViolationException` 分支已实现，
  但当前接口没有真实触发点，本轮未单独造数。
- 未实现删除前归档导出；生产清理前仍需先做数据库备份。
