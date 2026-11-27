# 权益发放与领取（卡密主路径）

核验日期：2026-11-27。范围：`admin-server`、`admin-web`、`c-server`、`c-h5`、`c-app`（H5）。

## 与需求文档 V1.1 的差异

V1.1 写的是 Node.js 24 + NestJS；本项目已落地 Java 21 + Spring Boot 3.5.16 + MyBatis-Plus + PostgreSQL 17，本期不换栈。C 端与 B 端继续共用同一个 PostgreSQL，卡密加解密由 `admin-server` 与 `c-server` 共用 `BENEFIT_CARD_SECRET`。

## 数据模型

DDL 在 `admin-server/admin-bootstrap/src/main/resources/db/schema.sql`，可重复执行。

| 表 | 关键字段 | 约束 |
|---|---|---|
| `pm_member_period` | `member_id`、`period_no`、`start_at`、`end_at`、`status`、`order_id` | `(member_id, period_no)` 唯一；`status='PENDING'` 时同一会员最多 1 条（部分唯一索引）；`end_at > start_at` |
| `benefit_product` | `sku_code`、`name`、`description`、`rules`、`status`、`sort` | `sku_code` 唯一；首期 SKU ≤ 20 由运营约束 |
| `benefit_card` | `batch_no`、`product_sku`、`code_hash`、`code_cipher`、`code_mask`、`status`、`expire_at` | `code_hash` 唯一防重码；明文只存在 `code_cipher`；查询只返回 `code_mask` |
| `benefit_claim` | `member_id`、`period_id`、`product_sku`、`status`、`card_id`、`claimed_at`、`remark` | `(period_id, product_sku)` 唯一，保证同周期同 SKU 只有一张领取单 |

补充说明：

- 卡密用 AES-256-GCM 加密，`code_hash = SHA-256(明文)` 用于唯一约束和幂等导入；`code_mask` 形如 `COFF****01`。
- `benefit_card.claim_id` 记录卡密当前绑定的领取单，失败回退时按 `claim_id` 条件更新，避免误退后来被重新发出的卡。
- `order_id` 可空，本期没有订单支付链路，保留字段给后续订单关联。

## 状态机

### 会员周期

```text
PENDING --支付/开通成功--> ACTIVE --end_at 到期--> EXPIRED
```

当前实现只认 `status='ACTIVE' AND start_at <= now() < end_at` 的周期；没有生效周期时 C 端返回空权益，不结转上一期。

### 卡密

```text
UNUSED --领取锁定--> CLAIMED
UNUSED --运营停用--> DISABLED
CLAIMED --确切失败/人工判失败--> UNUSED
```

`DISABLED` 不会被领取；`CLAIMED` 回退后重新变为 `UNUSED`，原 `claim_id` 清空。

### 领取单

```text
AVAILABLE(预留) --占名额/锁卡--> PROCESSING --成功--> SUCCESS
                                      |--确切失败--> FAILED（释放名额 + 回退卡密）
                                      |--结果未知--> UNKNOWN（保留名额和卡密）
FAILED/AVAILABLE --同 SKU 重试--> PROCESSING --成功--> SUCCESS
```

- 占用名额的状态：`PROCESSING`、`SUCCESS`、`UNKNOWN`。
- `FAILED` 不计名额；同周期同 SKU 再次领取时复用原领取单重试，不新增第二行。
- 当前是本地卡密主路径，插入 `PROCESSING` 后在同一事务内完成锁卡并更新 `SUCCESS`；因此正常提交后不会留下可见的 `PROCESSING`。
- 将来接入供应商时，建议 `PROCESSING` 事务提交后再调用供应商，结果回写为 `SUCCESS`、`FAILED` 或 `UNKNOWN`；供应商超时/无响应必须落 `UNKNOWN`，不能自动释放。

## 名额与并发规则

1. 每期 2 个名额，按占用状态的领取单计数。
2. 同周期同 SKU 只能有 1 条领取单；重复点击返回同一领取单，不再返回卡密明文。
3. 领取事务顺序：
   - `SELECT ... FROM pm_member_period ... FOR UPDATE` 锁周期行，串行化同一会员的领取；
   - 查同 SKU 领取单；已占用直接返回；
   - 统计占用名额，达到 2 返回 `BENEFIT_QUOTA_EXHAUSTED`；
   - `SELECT ... FROM benefit_card ... FOR UPDATE SKIP LOCKED LIMIT 1` 锁一张可用卡；
   - 插入领取单、标记卡密 `CLAIMED`、更新领取单 `SUCCESS`。
4. 事务内不调用外部服务；任何异常回滚，名额和卡密一起恢复。
5. 只有确切失败才释放名额并回退卡密；`UNKNOWN` 保留占用，人工核实后再决定成功或失败。
6. 卡密明文只在“本次新领取成功”的响应里出现一次；历史列表、后台列表都只返回脱敏值。

## 接口

### B 端

权限码已加入 `sys_permission`，菜单节点 `40 / 401-407`，`super_admin` 自动拥有。

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/api/system/benefit/products` | `benefit:product:list` | 商品列表，带 `stock`、`lowStock` |
| POST | `/api/system/benefit/products` | `benefit:product:create` | 新增商品 |
| PUT | `/api/system/benefit/products/{id}` | `benefit:product:update` | 编辑商品；SKU 创建后不可改 |
| DELETE | `/api/system/benefit/products/{id}` | `benefit:product:delete` | 无卡密/领取单时物理删除，否则下架 |
| GET | `/api/system/benefit/cards` | `benefit:card:list` | 卡密分页，只返回脱敏值 |
| POST | `/api/system/benefit/cards/import` | `benefit:card:import` | 批量导入，返回新增/重复数量 |
| GET | `/api/system/benefit/claims` | `benefit:claim:list` | 领取单分页，可按会员/SKU/状态过滤 |
| POST | `/api/system/benefit/claims/{id}/review` | `benefit:claim:review` | 人工核实/补偿，原因必填并写操作审计 |

库存告警：`stock` 为未使用且未过期卡密数，小于 `app.benefit.low-stock-threshold`（默认 5）时 `lowStock=true`。

人工核实请求：

```json
{"result":"FAILED","reason":"供应商确认未发放"}
```

`result` 只能是 `SUCCESS`、`FAILED`、`COMPENSATED`：

- `SUCCESS`：保留名额，确保卡密已绑定；没有有效绑定时补锁一张。
- `FAILED`：领取单置失败，释放名额，卡密回退 `UNUSED`。
- `COMPENSATED`：人工补偿，不占本期名额，但必须扣减可用库存。

### C 端

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/c/benefits` | 当前周期、剩余名额、商品与每个 SKU 的领取状态 |
| POST | `/c/benefits/{sku}/claim` | 领取；成功响应仅本次返回 `cardCode` |
| GET | `/c/benefits/claims` | 当前登录手机号对应会员的历史领取单，卡密脱敏 |

C 端没有 `memberId` 参数，会员身份只从 JWT 的 `phone` 推导，不能查看他人领取单。

错误码：

| bizCode | HTTP | 场景 |
|---|---|---|
| `BENEFIT_PERIOD_NOT_FOUND` | 404 | 没有生效中的周期 |
| `BENEFIT_PRODUCT_NOT_FOUND` | 404 | SKU 不存在或已下架 |
| `BENEFIT_QUOTA_EXHAUSTED` | 409 | 本期 2 个名额已用尽 |
| `BENEFIT_CARD_OUT_OF_STOCK` | 409 | 该 SKU 没有可用卡密 |
| `MEMBER_DISABLED` | 403 | 会员档案停用 |

## 前端

- `admin-web`：`/benefit` 单页三个页签——权益商品、卡密、领取单；商品 CRUD、卡密批量导入、领取单人工核实都在同一页完成。
- `c-h5`：会员中心顶部增加“权益中心”切换；商品列表展示描述/规则、领取状态、剩余名额，领取后显示一次性明文和兑换说明，历史只显示脱敏卡密。
- `c-app`：新增 `pages/benefit/benefit`，会员中心增加“权益中心”入口；H5 模式已实测。

## 初始化与造数

已有数据库不会自动执行 `deploy` 挂载的初始化脚本，需要手动执行一次幂等 DDL/种子：

```bash
cd deploy
docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U premium_member -d premium_member \
  < ../admin-server/admin-bootstrap/src/main/resources/db/schema.sql
docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U premium_member -d premium_member \
  < ../admin-server/admin-bootstrap/src/main/resources/db/data.sql
```

种子包含 3 个 SKU 和会员 1 的 1 个 ACTIVE 周期。卡密不能写死明文，使用后台导入：

```bash
TOKEN=$(curl -sS -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Admin@123456"}' \
  http://127.0.0.1:5320/api/auth/login | jq -r .data.accessToken)

curl -sS -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"batchNo":"BC","productSku":"COFFEE_30",
       "codes":["COFFEE-0001","COFFEE-0002"],
       "expireAt":"2027-12-31T15:59:59Z"}' \
  http://127.0.0.1:5320/api/system/benefit/cards/import
```

C 端账号按手机号关联会员；例如注册 `13900000001` 后即可看到会员 1 的周期。若刚通过 SQL 新增了权限，需要清一次权限缓存（TTL 10 分钟）：

```bash
docker compose exec -T redis redis-cli -a "$REDIS_PASSWORD" --no-auth-warning \
  --scan --pattern 'pm:dev:auth:perm:*'
```

## 验收记录

构建：

```text
admin-server  mvn -q -DskipTests package          成功
c-server      mvn -q -DskipTests package          成功
admin-web     pnpm run build:antd                 11 tasks successful
c-h5          npm run build                       成功
c-app         npm run build:h5                    成功
```

名额与幂等（会员 1，COFFEE_30 / MOVIE_1 / STORAGE_50G）：

```text
第一次 COFFEE_30   SUCCESS claimId=3 cardCode=COFFEE-0001
第一次 MOVIE_1     SUCCESS claimId=4 cardCode=MOVIE-0001
第三次 STORAGE_50G 409 BENEFIT_QUOTA_EXHAUSTED "本期权益名额已用尽"
再次 COFFEE_30     200 claimId=3 alreadyClaimed=true cardCode=null
```

真实并发（10 个并行 curl 同一 SKU）：

```text
responses=10  success=10  claimIds=[2]  cardCodes=1  alreadyClaimed=9
DB: benefit_claim 行数=1，distinct card_id=1
```

卡密耗尽：

```text
STORAGE_50G 两张卡临时置 DISABLED
POST /c/benefits/STORAGE_50G/claim
409 {"bizCode":"BENEFIT_CARD_OUT_OF_STOCK","message":"卡密库存不足"}
```

人工核实与审计：

```text
POST /api/system/benefit/claims/4/review {"result":"FAILED","reason":"测试：供应商确认未发放"}
  -> claim.status=FAILED，卡密回退 UNUSED，剩余名额 1
POST /api/system/benefit/claims/4/review {"result":"COMPENSATED","reason":"核实后补发一张"}
  -> claim.status=SUCCESS，card 3 重新 CLAIMED，claim_id=4
sys_operation_log: module=权益 action=人工核实 params 包含 result/reason
```

越权检查：

```text
会员 2 登录后 GET /c/benefits/claims -> []
```

过期不结转（把会员 2 周期 `end_at` 临时改为过去 1 分钟）：

```text
GET /c/benefits -> period=null quotaRemaining=2 products=[]
恢复 end_at 后 -> periodNo=2026-11 quotaRemaining=1 products=3
```

截图（`docs/screenshots/`）：

| 文件 | 内容 |
|---|---|
| `benefit-admin-products.png` | B 端商品列表、库存与告警 |
| `benefit-admin-cards.png` | B 端卡密脱敏列表与导入入口 |
| `benefit-admin-claims.png` | B 端领取单列表与核实入口 |
| `benefit-admin-review.png` | 人工核实弹窗（结果 + 必填原因） |
| `benefit-h5-center.png` | H5 权益中心、名额用尽、历史 |
| `benefit-h5-claim.png` | H5 领取成功后一次性明文与兑换说明 |
| `benefit-c-app.png` | c-app H5 权益中心 |

## 未验证项与风险

- 未新增自动化单测/集成测试；本期用构建、curl 与 ego-browser 实测。并发验证是真实并行请求，不是模拟。
- 周期创建、支付开通、过期批处理不在本期接口范围；当前用种子/手工 SQL 造 ACTIVE 周期。
- 供应商 API 与回调未接入；`PROCESSING`/`UNKNOWN` 已留状态和人工核实用例，但没有真实外部调用。
- 卡密 `DISABLED` 只有数据层状态，后台暂未提供单独停用按钮。
- 后台领取单查询目前按权限码控制，未叠加 `DataScopeService` 数据范围；如要把权益查询开放给部门角色，需要补数据范围过滤。
- c-app 只验证 H5 构建与页面；微信小程序/原生 App 未验证。
- 浏览器验收截图中的会员 2 领取场景额外插入了一个 ACTIVE 周期，仅作为演示数据；仓库种子只包含会员 1 的周期。

## 生产注意事项

- `BENEFIT_CARD_SECRET` 必须同时配置到 admin-server 和 c-server，且生产使用独立强密钥；更换密钥会导致旧卡密无法解密。
- 卡密导入请求在审计日志中通过 `@JsonProperty(WRITE_ONLY)` 省略 `codes`，避免明文卡密落审计表；手工核实原因照常写入审计。
- 生产执行新增权限/菜单 SQL 后要清理权限缓存或等待 TTL，否则已登录管理员可能短暂 403。
