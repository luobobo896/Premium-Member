# 订阅制与积分

核验日期：2026-11-27。范围：`admin-server`、`c-server`、`admin-web`、`c-h5`、`c-app`。
本项是 item 43，接在 item 42 之后；旧的 image/video 次数额度只保留历史数据，不再参与扣减。

## 业务规则

1. 会员套餐按月收费，`plan_version.monthly_points` 决定每月积分（种子里 `PREMIUM_30` 为 6000）。
2. 每月 1 号 00:05（Asia/Shanghai）逐订阅执行：清零上月剩余 → 创建代扣单 → 调用代扣渠道 → 成功发放积分。
3. 代扣失败不发积分，订阅进入 `PAST_DUE`，默认 3 天宽限；宽限内补扣成功仍发，超时 `SUSPENDED` 并通知。
4. 积分只用于 AI 能力，不可提现、不可抵扣现金。能力单价通过环境变量配置：
   `POINTS_PHOTO_PRICE=100`、`POINTS_DRESSUP_PRICE=100`、`POINTS_VIDEO_PRICE=500`。
5. 每期 2 项实物权益不变，仍由权益中心按 `pm_member_period` 名额领取。

## 数据模型

DDL 在 `admin-server/admin-bootstrap/src/main/resources/db/schema.sql`，种子在 `data.sql`，均可重复执行。

| 表 | 关键字段 | 约束 |
|---|---|---|
| `subscription` | `member_id`、`plan_id`、`status`、`monthly_points`、`price_cents`、`billing_day`、`next_charge_at`、`last_charge_at`、`grace_until`、`failure_reason`、`sign_contract_no`、`version` | 一个会员一条订阅；状态仅 `ACTIVE/PAST_DUE/SUSPENDED/CANCELLED` |
| `point_account` | `member_id`、`balance_points`、`updated_at` | `member_id` 唯一；余额不允许为负 |
| `point_ledger` | `member_id`、`delta`、`balance_after`、`type`、`ref_type`、`ref_id`、`period`、`idempotency_key`、`operator`、`created_at` | 只追加；`idempotency_key` 唯一；`type` 仅 `GRANT/CONSUME/EXPIRE/RESET/REFUND/ADJUST` |
| `charge_record` | `charge_no`、`subscription_id`、`period`、`amount_cents`、`status`、`provider`、`channel_txn_no`、`fail_reason`、`attempt_no` | `(subscription_id, period, attempt_no)` 唯一 |

`ai_task` 和 `quota_ledger` 保留，但 AI 提交不再写 `quota_ledger`；积分流水统一写 `point_ledger`。

## 状态机

```text
ACTIVE --1号代扣失败--> PAST_DUE --宽限内补扣成功--> ACTIVE
                            |--宽限期结束/人工强制--> SUSPENDED
ACTIVE/PAST_DUE --人工取消--> CANCELLED
```

文案映射：

| 状态 | C 端文案 |
|---|---|
| `ACTIVE` | 正常；到点未执行任务时显示“发放中” |
| `PAST_DUE` | 待扣款（宽限至 12-04） |
| `SUSPENDED` | 已停用 |
| `CANCELLED` | 已取消 |

## 1 号任务与幂等

- 入口：`SubscriptionJob.monthly`，cron `0 5 0 1 * *`，时区 `Asia/Shanghai`；可在 `SUBSCRIPTION_BILLING_CRON` 覆盖。
- 宽限重试：`SubscriptionJob.dunning`，每小时 30 分执行，cron `0 30 * * * *`。
- 单订阅事务顺序：锁 `subscription` → 检查 `GRANT:{subscriptionId}:{period}` → `RESET` 清零 → 插入代扣单 → 调 Provider → 成功写 `GRANT` 并推进 `next_charge_at`。
- 清零一定写负向 `RESET` 流水（余额为 0 时也写 0 值流水），不允许只改余额。
- `RESET:{subscriptionId}:{period}`、`GRANT:{subscriptionId}:{period}`、`CONSUME:{taskNo}`、`REFUND:{taskNo}` 都是唯一幂等键；重复执行不会重复清零或发放。
- 手工补发写 `ADJUST`，`ref_id` 为原因摘要，操作人取当前登录用户；`@AuditLog` 记录完整原因。

## Mock 与真实微信代扣

配置项：

| 环境变量 | 默认 | 说明 |
|---|---|---|
| `SUBSCRIPTION_ENABLED` | `true` | 关闭定时任务 |
| `SUBSCRIPTION_PROVIDER` | `mock` | `mock` 或 `wechat` |
| `SUBSCRIPTION_MOCK_RESULT` | `SUCCESS` | Mock 默认结果，可设 `FAIL` |
| `SUBSCRIPTION_GRACE_DAYS` | `3` | 宽限天数 |

`MockWithholdProvider` 默认成功；`WechatPayWithholdProvider` 已留类，但真实接入需要商户号、委托代扣资质、签约模板，未接入时明确失败，不会静默成功。
停用通知当前由 `SubscriptionNotifier` 落 warn 日志；接短信/微信模板消息时只替换该类。

## 接口

### C 端

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/c/subscription` | 当前订阅；无订阅返回 `null` |
| GET | `/c/points/ledger?month=YYYY-MM` | 积分明细，月份可空 |
| GET | `/c/quota` | 积分余额、每月积分、重置时间、能力单价 |
| POST | `/c/ai/tasks` | 提交时按模板类型扣积分；不足返回 `POINTS_INSUFFICIENT` |

AI 账本成对规则：提交写 `CONSUME`，失败/拒绝写 `REFUND`，成功不重复写；`UNKNOWN` 保留消费。

### B 端

| 方法 | 路径 | 权限 |
|---|---|---|
| GET | `/api/system/subscriptions` | `subscription:list` |
| GET | `/api/system/subscriptions/charges` | `subscription:charge:list` |
| POST | `/api/system/subscriptions/{id}/grant-points` | `subscription:grant`，原因必填 + 审计 |
| POST | `/api/system/subscriptions/run-billing` | `subscription:run`，验收/补偿用 |
| POST | `/api/system/subscriptions/run-dunning` | `subscription:run`，宽限重试/强制停用 |

`run-billing` 请求体：

```json
{"subscriptionId":1,"mockResult":"SUCCESS","graceDays":3,"period":"2026-12"}
```

- 带 `subscriptionId` 时按“今天对应账期”执行，同一天重复点击仍是同一 period，重复执行不重复发放。
- 不带 `subscriptionId` 时只处理 `next_charge_at <= now()` 的 `ACTIVE` 订阅。
- `mockResult=FAIL` 可模拟代扣失败；`graceDays=0` 直接停用。
- `run-dunning` 的 `forceSuspend=true` 用于验收“宽限期后 SUSPENDED”，正常生产不传。

## 前端映射

| 端 | 位置 | 变化 |
|---|---|---|
| c-h5 | 会员中心 | 会员卡显示等级、本月积分 `x/6000`、下次扣款、订阅状态；删除金额展示；新增“我的订阅”“积分明细” |
| c-h5 | AI 创作 | 额度条“本月积分 6000 · 12-01 重置”；模板卡右下角单价；创作条“本次消耗 100 积分 · 剩余 x”；不足置灰并提示开通 |
| c-h5 | 积分明细 | 按月份筛选，展示发放/消耗/清零/退还/调整、变动值、结余 |
| c-app | 会员中心、AI、订阅、积分 | 与 H5 同口径，新增 `pages/subscription/subscription`、`pages/points/points` |
| admin-web | 交易管理 | 套餐新增“月积分”，价格沿用现有字段 |
| admin-web | 订阅与积分 `/subscriptions` | 订阅列表、代扣记录、手动补发、执行 1 号任务、宽限重试 |

## 从次数额度迁移

- 旧 `plan_version.ai_image_quota/ai_video_quota` 和 `quota_ledger` 保留用于历史查询，不再扣减，也不再展示为“剩余次数”。
- 新会员首次支付成功时创建订阅并按当前账期发放一次积分；后续月份由 1 号任务发放。
- 已有 `pm_member.points` 是旧会员档案字段，C 端不再展示；真实积分以 `point_account.balance_points` 为准。
- 老会员上线前需要一次性把可用余额迁入 `point_account`，并写 `ADJUST` 流水；没有历史次数换算规则时不做自动换算。

已有库执行一次幂等 DDL/种子：

```bash
cd deploy
docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U premium_member -d premium_member \
  < ../admin-server/admin-bootstrap/src/main/resources/db/schema.sql
docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U premium_member -d premium_member \
  < ../admin-server/admin-bootstrap/src/main/resources/db/data.sql
```

`data.sql` 不覆盖历史套餐价格；`PREMIUM_30` 的旧库价格需要运营在交易管理里生成新版本。

## 验收命令

```bash
# B 端登录
TOKEN=$(curl -sS -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Admin@123456"}' \
  http://127.0.0.1:5320/api/auth/login | jq -r .data.accessToken)

# 1 号任务：清零 → Mock 代扣成功 → 发 6000
curl -sS -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"subscriptionId":1,"mockResult":"SUCCESS"}' \
  http://127.0.0.1:5320/api/system/subscriptions/run-billing | jq

# 重复执行，granted 应为 0
curl -sS -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"subscriptionId":1,"mockResult":"SUCCESS"}' \
  http://127.0.0.1:5320/api/system/subscriptions/run-billing | jq

# 代扣失败：PAST_DUE、不发积分
curl -sS -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"subscriptionId":1,"mockResult":"FAIL","graceDays":3}' \
  http://127.0.0.1:5320/api/system/subscriptions/run-billing | jq

# 宽限期后停用（验收用）
curl -sS -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"subscriptionId":1,"forceSuspend":true}' \
  http://127.0.0.1:5320/api/system/subscriptions/run-dunning | jq
```

积分与账本可用 SQL 对账：

```sql
SELECT member_id, balance_points FROM point_account WHERE member_id = 1;
SELECT type, delta, balance_after, period, idempotency_key
FROM point_ledger WHERE member_id = 1 ORDER BY id;
SELECT status, period, amount_cents, fail_reason
FROM charge_record WHERE subscription_id = 1 ORDER BY id;
```

## 实测记录（2026-11-27）

构建：

```text
admin-server  mvn test / package   通过（含 SubscriptionBillingIT）
c-server      mvn test / package   通过（含 AiQuotaIT）
admin-web     pnpm run build:antd  11 tasks successful
c-h5          npm run build        通过
c-app         npm run build:h5     通过
```

1 号任务与幂等：

```text
第一次 {"period":"2026-12","processed":1,"granted":1,"failed":0,"suspended":0}
第二次 {"period":"2026-12","processed":1,"granted":0,"failed":0,"suspended":0}
余额 6000
GRANT 6000 6000 2026-11
RESET -6000 0 2026-12
GRANT 6000 6000 2026-12
```

代扣失败与停用：

```text
失败 {"period":"2026-12","processed":1,"granted":0,"failed":1,"suspended":0}
订阅 PAST_DUE，grace=2026-11-30，余额 0，只新增 RESET 无 GRANT
强制宽限结束 {"processed":1,"failed":0,"suspended":1}
订阅 SUSPENDED，余额仍为 0
```

AI 扣积分与返还：

```text
成功任务：CONSUME -100，余额 5900，无 REFUND
失败任务：CONSUME -100，REFUND +100，余额回到 5900
重复提交同一 idempotencyKey 返回同一 taskNo，只扣一次
```

旧文案自查：

```text
$ rg -n "次数|剩余次数|余额" c-h5/src c-app/src
(no matches)
```

## 截图

- `docs/screenshots/subscription-member.png`
- `docs/screenshots/subscription-ai.png`
- `docs/screenshots/subscription-ai-create.png`
- `docs/screenshots/subscription-points.png`
- `docs/screenshots/subscription-entry.png`
- `docs/screenshots/subscription-detail.png`
- `docs/screenshots/subscription-admin.png`

