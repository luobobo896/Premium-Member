# 运营增强：工单、内容配置与统计

核验日期：2026-11-27。基线 `78ef4c9`。范围：`admin-server`、`c-server`、`admin-web`、`c-app`、`c-h5`。
规则基线：需求文档 V1.1 第 6.1 节（工单、内容配置、统计导出）与第 7 节（运营）。技术栈保持 Java 21 + Spring Boot 3.5.16 + PostgreSQL 17。

## 数据模型

DDL 在 `admin-server/admin-bootstrap/src/main/resources/db/schema.sql`，可重复执行。

| 表 | 关键字段 | 约束 |
|---|---|---|
| `support_ticket` | `ticket_no`、`user_type`、`member_id`、`user_id`、`order_no`、`category`、`title`、`content`、`status`、`priority`、`assignee_admin_id`、`remark`、`created_at`、`updated_at`、`resolved_at` | `ticket_no` 唯一；`user_type` 仅 `C/B`；状态仅 `OPEN/PROCESSING/RESOLVED/CLOSED`；优先级仅 `LOW/NORMAL/HIGH` |
| `support_ticket_reply` | `ticket_id`、`sender_type`、`sender_id`、`sender_name`、`content`、`created_at` | 按 `ticket_id` 级联删除；只追加 |
| `content_item` | `key`、`locale`、`content_json`、`version_no`、`status`、`publish_at`、`updated_by` | `(key, locale, version_no)` 唯一；`key` 仅六类；状态仅 `DRAFT/PUBLISHED` |

工单回复单独成表，原因是后台回复需要保留发送人、时间与顺序；`remark` 只作为内部备注，不对 C 端下发。

内容配置采用“每行一个版本”的追加模型。草稿可以原地编辑，但发布、回滚都新增版本，不覆盖历史。C 端读取同一 `key+locale` 下 `version_no` 最大的已发布行。

## 工单

状态流转：

```text
OPEN -> PROCESSING -> RESOLVED
                   -> CLOSED
```

- 首次回复会把 `OPEN` 推进到 `PROCESSING`。
- `RESOLVED` / `CLOSED` 写入 `resolved_at`；重新打开会清空该时间。
- C 端查询以 `user_type='C' AND user_id=当前 C 用户` 限定；查他人工单返回 404 `TICKET_NOT_FOUND`。
- 后台受理、回复、改状态/备注均写 `sys_operation_log`。

### C 端接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/c/tickets` | `{"title":"...","content":"...","category":"ORDER","orderNo":"...","priority":"HIGH"}` |
| GET | `/c/tickets` | 我的最近 100 条，按提交时间倒序 |
| GET | `/c/tickets/{ticketNo}` | 我的工单详情 + 后台回复；他人 404 |

### B 端接口

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/system/tickets` | `ticket:list` | 状态/分类/关键词/起止日期分页 |
| GET | `/system/tickets/{ticketNo}` | `ticket:list` | 详情 + 回复 |
| POST | `/system/tickets/{ticketNo}/accept` | `ticket:handle` | 受理并绑定当前管理员 |
| POST | `/system/tickets/{ticketNo}/replies` | `ticket:handle` | `{"content":"..."}` |
| PUT | `/system/tickets/{ticketNo}` | `ticket:handle` | `status`、`priority`、`remark`、`assigneeAdminId` |

后台列表对 C 工单优先显示 `pm_member` 昵称/手机号；没有会员档案时回退到 `c_user`。

### C 端页面

- `c-h5`：顶部导航新增“工单”，页面提供我的工单列表、新建工单（分类/标题/问题描述/可选订单号）、详情（状态时间线、后台回复）和刷新进度。
- `c-app`：会员中心新增“我的工单”入口，`pages/ticket/ticket` 复用同一套列表、新建和详情交互；H5 构建可运行。

## 内容配置

支持的 `key`：`PLAN_COPY`、`BANNER`、`ANNOUNCEMENT`、`DOWNLOAD`、`CONTACT`、`AGREEMENT`。

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/system/contents?key=&locale=` | `content:list` | 不传 `key` 返回每项最新版本；传 `key` 返回全部历史版本 |
| GET | `/system/contents/{id}` | `content:list` | 预览单个版本 |
| POST | `/system/contents` | `content:update` | `{"key":"BANNER","locale":"zh-CN","contentJson":"{...}"}`；同项已有草稿则更新草稿，否则新建版本 |
| PUT | `/system/contents/{id}` | `content:update` | 只允许编辑 `DRAFT` |
| POST | `/system/contents/{id}/publish` | `content:publish` | 发布草稿 |
| POST | `/system/contents/{id}/rollback` | `content:rollback` | 把指定已发布版本复制成新版本并立即发布 |
| GET | `/c/contents?keys=BANNER,PLAN_COPY&locale=zh-CN` | 公开 | 只返回 `PUBLISHED` 且 `publish_at <= now()` 的最新版本 |

当前内容结构由前端约定，后端只校验 JSON 合法性：

- `BANNER`：`{"items":[{"title":"...","imageUrl":"...","link":"..."}]}`
- `PLAN_COPY`：`{"title":"...","lede":"...","rules":["..."]}`

前端回退：c-app 无 `BANNER` 配置时显示“会员专属权益”默认轮播；c-h5 无 `PLAN_COPY` 或 JSON 异常时使用内置套餐标题、说明和三条规则。

## 统计

`GET /system/stats/daily?from=&to=&source=` 返回区间内每天一行（无数据的日期补 0）。`source` 对应 `orders.source`，可选 `H5/APP/MOCK`。

口径：

| 指标 | 口径 |
|---|---|
| 订单数 | 当天创建的订单数（`orders.created_at`） |
| 实付金额（分） | 当天支付的订单金额合计（`status='PAID'` 且按 `paid_at`） |
| 有效会员数 | 当天与会员周期有交集、且周期状态为 `ACTIVE/EXPIRED` 的去重会员数 |
| 权益申请/成功/异常 | 当天创建的 `benefit_claim` 总数 / `SUCCESS` / `FAILED+UNKNOWN` |
| AI 图片/视频 | 当天创建的 `ai_task`，图片为 `PHOTO+DRESSUP`，视频为 `VIDEO` |

日期边界统一按 `Asia/Shanghai` 自然日计算。指定 `source` 时，订单相关指标直接按来源过滤；会员、权益、AI 指标限定为“有过该来源订单的会员”，避免同一筛选条件下各列口径不一致。

`GET /system/stats/export` 返回 UTF-8 BOM 的 CSV，列与日报一致。单次上限 50,000 行；超限返回 400。导出通过 `@AuditLog` 写入 `sys_operation_log`（操作者、起止日期、来源、路径、成功状态）。

## 初始化

已有库执行：

```bash
cd deploy
docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U premium_member -d premium_member \
  < ../admin-server/admin-bootstrap/src/main/resources/db/schema.sql
docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U premium_member -d premium_member \
  < ../admin-server/admin-bootstrap/src/main/resources/db/data.sql
```

权限来自 `data.sql`，已有环境需清一次权限缓存：

```bash
docker exec premium-member-redis redis-cli -a premium_member_dev_only \
  --scan --pattern 'pm:dev:auth:perm:*' | xargs -r -n1 \
  docker exec -i premium-member-redis redis-cli -a premium_member_dev_only DEL
```

新增管理员权限：`ticket:list`、`ticket:handle`；`content:list/update/publish/rollback`；`stats:daily/export`。菜单：`工单管理 /tickets`、`内容配置 /contents`、`运营统计 /stats`。

## 验收记录

环境：本地 Docker PostgreSQL 17 + Redis 8.2，admin-server `5320/api`，c-server `8081`，admin-web `5666`，c-h5 `5174`，c-app H5 `5175`。

### 工单

```text
C 提交 T20261127909710F8 -> OPEN
后台受理 -> PROCESSING，处理人 Administrator
后台回复 "已核实支付流水，正在补发会员周期"
后台关闭 -> CLOSED，resolved_at 有值
C 查询 -> status=CLOSED，回复可见
另一 C 用户查询同工单 -> HTTP 404，bizCode=TICKET_NOT_FOUND
```

审计表可查到“受理 / 回复 / 改状态/备注”三条成功记录。

浏览器闭环（c-h5 页面提交，后台 API 受理/回复）：

```text
c-h5 新建工单 -> T202611274CA52409，状态“待受理”
后台受理 -> “处理中”，处理人 Administrator
后台回复 "已核实支付记录，会员通道稍后自动生效，请 10 分钟后刷新会员中心。"
c-h5 打开详情 -> 状态“处理中”，时间线推进到“处理中”，客服回复可见
```

### 内容

```text
BANNER v1 草稿 -> 发布 -> C 返回 v1 "双十一特惠"
BANNER v2 草稿 -> 发布 -> C 返回 v2 "双十二特惠"
回滚 v1 -> 生成 v3（v1 内容），C 返回 v3 "双十一特惠"
版本历史 -> v3 / v2 / v1 均保留且状态为 PUBLISHED
```

c-h5 套餐页读取 `PLAN_COPY` 后标题显示“选择你的会员方案”；c-app 会员首页读取 `BANNER` 后显示“双十一特惠”轮播。

### 统计

2026-11-27 接口返回值：

```text
orderCount=6, paidAmountCents=17700, activeMembers=4,
benefitApplied=4, benefitSuccess=4, benefitException=0,
aiImageTasks=6, aiVideoTasks=1
```

直接 SQL 聚合结果一致。导出 `2026-11-25..2026-11-27`、来源 `H5` 后，`sys_operation_log` 新增：

```text
admin | 运营统计 | 导出 CSV | ["2026-11-25","2026-11-27","H5"] | success=1
```

### 构建

```text
admin-server: mvn -q -DskipTests package 通过
c-server:     mvn -q -DskipTests package 通过
admin-web:    pnpm run build:antd 通过（11 tasks）
c-h5:         npm run build 通过
c-app:        npm run type-check && npm run build:h5 通过
```

`admin-web` 的 `vue-tsc` 仍有基线遗留错误（`views/ai`、`views/benefit`、`views/trade` 的表格插槽类型）；本次新增的 `views/tickets`、`views/content`、`views/stats` 无类型错误。

### 截图

- `docs/screenshots/ops-ticket-list.png`
- `docs/screenshots/ops-ticket-detail.png`
- `docs/screenshots/ops-content-list.png`
- `docs/screenshots/ops-content-versions.png`
- `docs/screenshots/ops-content-edit.png`
- `docs/screenshots/ops-stats.png`
- `docs/screenshots/ops-c-h5-plan-copy.png`
- `docs/screenshots/ops-c-app-banner.png`
- `docs/screenshots/ops-c-ticket-create.png`
- `docs/screenshots/ops-c-ticket-list.png`
- `docs/screenshots/ops-c-ticket-status.png`
- `docs/screenshots/ops-c-ticket-reply.png`
- `docs/screenshots/ops-c-ticket-app.png`

## 未验证项与风险

- c-app 只验证了 H5 构建与运行，未跑微信小程序/原生 App 端。
- `stats:export` 的按钮级权限用 `v-access` 控制，服务端用 `@PreAuthorize`；未用受限角色在 UI 上逐按钮回归。
- 来源筛选只造了 `H5` 订单数据；`APP/MOCK` 与真实支付渠道流量未压测。
- 5 万行上限是防御性校验，按日聚合在正常日期区间远达不到；未做超大区间性能测试。
- 内容版本号由 `max(version_no)+1` 生成，极端并发下依赖唯一索引兜底，未做并发发布压测。
- 工单状态流转未限制 `CLOSED -> OPEN` 等任意回退；后台仍可改状态，需求未定义更细的状态机。
