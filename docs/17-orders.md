# 订单与会员周期

核验日期：2026-11-27。范围：`admin-server`（B 端）、`c-server`（C 端）、`admin-web`、`c-h5`。
规则基线：需求文档 V1.1 第 3 节（套餐周期与交易规则）、第 9 节（数据模型与关键约束）。

## 数据模型

DDL 在 `admin-server/admin-bootstrap/src/main/resources/db/schema.sql`，可重复执行。已有库需手动跑一次 schema 与 data。

| 表 | 关键字段 | 约束 |
|---|---|---|
| `plan_version` | `code`、`name`、`price_cents`、`duration_days`、`ai_image_quota`、`ai_video_quota`、`benefit_quota`、`status`、`version_no` | `(code, version_no)` 唯一；同一 `code` 仅一条 `status=1`（部分唯一索引） |
| `orders` | `order_no`、`member_id`、`user_id`、`plan_version_id`、`amount_cents`、`status`、`channel`、`source`、`purpose`、`created_at`、`paid_at` | `order_no` 唯一；`(user_id, purpose)` 在 `CREATED/REFUNDING` 时唯一 |
| `payment_transactions` | `txn_no`、`order_id`、`channel`、`amount_cents`、`status`、`raw_payload`、`paid_at` | `txn_no` 唯一；同一订单最多一条 `SUCCESS`（部分唯一索引） |
| `refunds` | `refund_no`、`order_id`、`amount_cents`、`status`、`operator`、`reason`、`processed_at` | `refund_no` 唯一；同一订单最多一条 `APPLIED/APPROVED` |
| `pm_member_period` | `member_id`、`period_no`、`start_at`、`end_at`、`status`、`order_id` | `order_id` 唯一（非空时）；同会员最多一条 `PENDING`；状态新增 `TERMINATED` |

设计要点：

- 金额全部使用整数“分”。下单金额由服务端读取 `plan_version.price_cents`，客户端不传价格。
- 改价/改规则复制为新 `plan_version` 行，旧版本 `status=0`；订单只引用版本 ID，历史价格与额度不被覆盖。
- 支付成功、交易流水、订单状态与周期在同一事务内提交。
- 周期按服务端支付成功时间 `paid_at` 起算，数据库 `timestamptz`（UTC），前端按北京时间展示。

## 周期规则

1. 支付成功时锁会员行，串行化同一会员的开通。
2. 没有生效周期：创建 30 天 `ACTIVE` 周期，`start_at = paid_at`，`end_at = start_at + duration_days`。
3. 已有 `ACTIVE`：只创建 1 个 `PENDING`，从当前期 `end_at` 开始，不增加当前期额度；已有 `PENDING` 时拒绝新订单。
4. 读取 C 端订单/权益时惰性处理：已到期 `ACTIVE` 置 `EXPIRED`，到点的 `PENDING` 置 `ACTIVE`。
5. 退款成功：该订单对应周期置 `TERMINATED`；未用名额随周期失效，已发出的卡密不回收。

## 接口

### C 端

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/c/plans` | 在售套餐版本 |
| POST | `/c/orders` | 下单，请求 `{"planVersionId":1,"source":"H5"}` |
| POST | `/c/payments/mock/notify` | 仅 dev，请求 `{"orderNo":"..."}`；重复调用返回同一结果 |
| GET | `/c/orders` | 当前登录账号订单 |
| GET | `/c/orders/{orderNo}` | 当前登录账号订单详情，不能查看他人订单 |

`PaymentProvider` 抽象位于 `c-biz/order/payment`：

- `MockPaymentProvider`：仅 `dev` profile，返回 `payMode=MOCK`。
- `WechatPayProvider`：预留，`createPayment` 直接抛 `PAYMENT_CHANNEL_UNSUPPORTED`。
- 微信商户号、API v3 密钥、私钥路径、证书序列号只从环境变量读取（`.env.example` 占位），不写数据库。

### B 端

| 方法 | 路径 | 权限 |
|---|---|---|
| GET | `/system/plans` | `plan:list` |
| POST | `/system/plans` | `plan:create`；同 code 自动生成新版本 |
| PUT | `/system/plans/{id}` | `plan:update`；复制为新版本 |
| DELETE | `/system/plans/{id}` | `plan:delete`；下架 |
| GET | `/system/orders` | `order:list`，支持订单号/状态/会员 ID |
| GET | `/system/orders/{orderNo}` | `order:list`，返回套餐快照、周期、支付流水、退款 |
| POST | `/system/orders/{orderNo}/refund` | `order:refund`，`result=APPROVE/REJECT` |
| GET | `/system/payments` | `payment:list` |

退款请求：

```json
{"amountCents":5900,"reason":"用户申请整单退款","result":"APPROVE"}
```

- 金额必须大于 0 且不超过订单实付。
- `REJECT` 只记录驳回，不回收周期。
- `APPROVE` 写入 `APPLIED/APPROVED/SUCCESS`，订单置 `REFUNDED`，周期置 `TERMINATED`。
- 周期仍有 `PROCESSING/UNKNOWN` 领取单时拒绝退款，避免未知结果被直接释放。
- `REFUNDING` 状态的订单在 C 端领取时返回 `ORDER_REFUNDING`，冻结新领取。

错误码：

| bizCode | HTTP | 场景 |
|---|---|---|
| `PLAN_NOT_FOUND` | 404 | 套餐不存在或下架 |
| `ORDER_PENDING_EXISTS` | 409 | 已有未完成订单或待生效周期 |
| `ORDER_STATE_INVALID` | 409 | 当前状态不允许支付/退款 |
| `ORDER_REFUNDING` | 409 | 退款处理中，不能领取 |
| `REFUND_AMOUNT_EXCEEDS` | 409 | 退款金额超过实付 |
| `REFUND_CLAIM_PENDING` | 409 | 周期还有处理中/未知领取单 |
| `BENEFIT_CARD_INVALID` | 409 | 卡密解密失败（AEAD 校验失败等），不再返回 500 |
| `PAYMENT_CHANNEL_UNSUPPORTED` | 409 | 微信支付/退款尚未接入 |

卡密解密失败时 `c-server` 记录 WARN 并累加 Micrometer 指标 `benefit.card.decrypt.failures`，日志带累计次数；不改写卡密数据。

## 前端

- `c-h5`：顶部新增“购买”“订单”。套餐页展示价格、30 天周期、图片/视频额度、2 个权益名额、协议勾选；下单后进入模拟支付结果页，自动调用 Mock 回调并展示生效周期；我的订单展示支付/退款状态与周期。
- `admin-web`：`/trade` 交易管理，三个页签——套餐管理、订单管理（详情 + 退款审核弹窗）、支付流水。
- 截图：`docs/screenshots/order-admin-plans.png`、`order-admin-orders.png`、`order-admin-refund.png`、`order-h5-plans.png`、`order-h5-result.png`、`order-h5-orders.png`。

## 初始化

已有库执行一次：

```bash
cd deploy
docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U premium_member -d premium_member \
  < ../admin-server/admin-bootstrap/src/main/resources/db/schema.sql
docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U premium_member -d premium_member \
  < ../admin-server/admin-bootstrap/src/main/resources/db/data.sql
```

种子新增 `PREMIUM_30`（5900 分、30 天、20 图、3 视频、2 权益）与 `plan/order/payment` 权限、菜单 `50`。
已有权限缓存需清一次：`pm:dev:auth:perm:*`。

## 验收记录

构建：

```text
admin-server  mvn -q -DskipTests package   成功
c-server      mvn -q -DskipTests package   成功
admin-web     pnpm run build:antd          11 tasks successful
c-h5          npm run build                成功
```

重复回调与建单：

```text
第一次 POST /c/orders -> 200 orderNo=PM202611270437450D61F1
第二次 POST /c/orders -> 409 ORDER_PENDING_EXISTS
第一次 mock notify -> PAID, period=PENDING
第二次 mock notify -> PAID, 同一 periodId，未新增周期
DB: payment_transactions=1, pm_member_period(order_id=1)=1
```

待生效期唯一（会员 2 已有 ACTIVE）：

```text
下单并支付 -> 生成 PENDING 周期
再次下单 -> 409 ORDER_PENDING_EXISTS
DB: 同一会员 PENDING 行数=1
```

退款：

```text
金额 6000 > 实付 5900 -> 409 REFUND_AMOUNT_EXCEEDS
金额 5900 APPROVE -> 200
orders.status=REFUNDED
pm_member_period.status=TERMINATED
```

退款冻结与套餐版本：

```text
orders.status 临时置 REFUNDING
POST /c/benefits/COFFEE_30/claim
409 ORDER_REFUNDING "订单退款处理中，暂时无法领取"

PUT /system/plans/1 同价生成新版本
plan_version: id=1 status=0 v1（历史订单仍引用）, id=2 status=1 v2
GET /system/orders: planVersionId=1, planVersionNo=1, amount=5900
```

卡密解密失败：

```text
POST /c/benefits/BAD_CARD_1/claim
HTTP 409
{"bizCode":"BENEFIT_CARD_INVALID","message":"卡片失效，请联系客服"}
日志：WARN 卡密解密失败，按卡片失效处理：AEADBadTagException，累计 1 次
```

## 未验证/边界

- 微信 H5 支付、真实退款、对账未接入：`WechatPayProvider` 只抛 `UNSUPPORTED`，当前仅 Mock 渠道可完成闭环。
- Mock 退款在审核事务内同步成功，正常流程不会长期停留在 `REFUNDING`；冻结校验是用临时 `REFUNDING` 状态实测的。
- 退款为整单语义：接口允许金额不超过实付，但成功即终止该订单对应周期；未做按天/按次数部分退款。
- 没有支付超时关单定时任务；`CREATED` 订单会占用同账号未完成订单唯一位，需后续补关单策略。
- 待生效期激活是读取时惰性更新，不是独立定时任务；未被 C 端读取前，库内状态可能暂时保留 `PENDING`。
- 配置缓存与新权限需重启 admin-server 或清 Redis 权限缓存后生效。
