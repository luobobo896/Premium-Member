# 数据库设计与 SQL

数据库：PostgreSQL 17。DDL 与种子数据：

- `admin-server/admin-bootstrap/src/main/resources/db/schema.sql`
- `admin-server/admin-bootstrap/src/main/resources/db/data.sql`

## 表

| 表 | 说明 |
|---|---|
| `sys_user` | 用户，BCrypt 密码哈希、部门、状态、主页 |
| `sys_role` | 角色，`code` 对应 vben `roles`，`data_scope` 控制数据范围 |
| `sys_permission` | 权限码，`code` 对应 vben `/auth/codes` 与按钮权限 |
| `sys_menu` | 目录/菜单/按钮/外链，`permission_code` 关联权限码 |
| `sys_dept` | 部门树 |
| `sys_user_role` | 用户-角色 |
| `sys_role_permission` | 角色-权限 |
| `sys_role_menu` | 角色-菜单 |
| `pm_member` | 会员业务示例，`owner_id` 关联创建人用于数据权限 |
| `pm_member_period` | 会员权益周期，PENDING/ACTIVE/EXPIRED |
| `benefit_product` | 权益商品 SKU |
| `benefit_card` | 卡密，密文/哈希/掩码存储 |
| `benefit_claim` | 权益领取单，周期 + SKU 唯一 |
| `plan_version` | 套餐版本，改价/改额度/改月积分生成新版本 |
| `subscription` | 会员订阅，记录下次扣款、宽限期与订阅状态 |
| `point_account` | 会员积分余额，一个会员一条 |
| `point_ledger` | 积分流水，幂等键唯一，涵盖发放/消耗/清零/退还/调整 |
| `charge_record` | 自动续费代扣记录，订阅 + 账期 + 尝试次数唯一 |
| `orders` | 会员套餐订单，同账号同用途未完成订单唯一 |
| `payment_transactions` | 支付交易流水，交易号唯一、回调幂等 |
| `refunds` | 整单退款申请与审核结果 |
| `sys_operation_log` | 操作审计日志，AOP 异步写入 |

关系：

```text
sys_user 1---N sys_user_role N---1 sys_role
sys_role 1---N sys_role_permission N---1 sys_permission
sys_role 1---N sys_role_menu N---1 sys_menu
sys_user N---1 sys_dept
```

## 设计要点

- 时间统一 `timestamptz`；JDBC 返回 `OffsetDateTime`，API 输出 ISO-8601。
- 金额 `NUMERIC(12,2)`，Java 使用 `BigDecimal`；不使用浮点数。
- 逻辑删除字段 `deleted`，值为 0/1；唯一索引使用 partial index，只约束未删除行。
- 用户名、角色码、权限码、会员编号都有唯一索引。
- 会员等级与状态有 CHECK 约束；积分、余额不允许负数。
- 角色数据范围 `data_scope` 限定为 `ALL/DEPT_AND_CHILD/DEPT/SELF`。
- 审计日志按 `created_at DESC`、`user_id` 建索引，保留策略见 `15-audit.md`。
- 排序字段 `sort` 用于菜单和部门树稳定排序；菜单位置由 `parent_id + sort + id` 决定。

## 首次初始化

```bash
cd deploy
docker compose up -d
```

PostgreSQL 容器首次启动时按文件名顺序执行：

1. `01-schema.sql`（挂载自 `schema.sql`）
2. `02-data.sql`（挂载自 `data.sql`）

检查：

```bash
docker compose exec -T postgres \
  psql -U premium_member -d premium_member \
  -c "SELECT table_name FROM information_schema.tables WHERE table_schema='public' ORDER BY table_name;"
```

实测结果：10 张表，`sys_user=5`、`sys_role=5`、`sys_menu=28`、`sys_permission=23`、
`pm_member=5`、`sys_operation_log` 从 0 开始累计。

## 种子账号

仅用于本地开发：

| 用户名 | 密码 | 角色 |
|---|---|---|
| `admin` | `Admin@123456` | `super_admin` |
| `operator` | `Operator@123456` | `operator` |
| `viewer` | `Viewer@123456` | `viewer` |
| `dept_manager` | `Admin@123456` | `dept_manager`（DEPT） |
| `self_manager` | `Admin@123456` | `self_manager`（SELF） |

密码以 BCrypt `$2y$` 哈希存储，Spring `BCryptPasswordEncoder` 可直接校验。生产环境不得复用这些账号或密码。

## 重置

本地需要重新执行 DDL 时：

```bash
cd deploy
docker compose down -v
docker compose up -d
```

`down -v` 会删除本地 PostgreSQL 与 Redis 数据卷，属于破坏性操作，只允许在开发环境使用。
