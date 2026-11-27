# 游客模式（item 32）

基线：`a7aa60f`。范围：`c-server`、`c-app`、`c-h5`、`docs`；`admin-server` 与 `admin-web` 未改。

## 公开与受保护接口

C 端公开接口只返回目录数据，不读取当前用户、会员周期、卡密或领取记录。

| 方法 | 路径 | 认证 | 返回 |
|---|---|---|---|
| GET | `/c/plans` | 游客 | `id/name/priceCents/durationDays/aiImageQuota/aiVideoQuota/benefitQuota` |
| GET | `/c/benefits/products` | 游客 | `skuCode/name/description/rules/cover` |
| GET | `/c/ai/templates` | 游客 | 模板目录 |
| GET | `/c/contents` | 游客 | 已发布内容 |
| GET | `/c/ai/files/**` | 游客 | 已生成的公开文件；不存在时 404 |

以下接口保持登录保护：

| 路径 | 说明 |
|---|---|
| `/c/orders/**` | 下单、订单列表与详情 |
| `/c/payments/**` | 支付相关；`POST /c/payments/mock/notify` 仍仅 dev 使用 |
| `/c/members/me` | 会员档案 |
| `/c/benefits` | 本期名额、我的领取状态与卡密脱敏值 |
| `/c/benefits/claims` | 我的领取历史 |
| `/c/ai/tasks/**` | 我的 AI 任务 |
| `/c/ai/works/**` | 我的 AI 作品删除 |
| `/c/tickets/**` | 我的工单 |
| `/c/user/**` | 当前登录用户 |

实现位置：

- `SecurityConfig` 只对上述 GET 路径 `permitAll`；其余仍 `authenticated()`。
- `/c/plans` 使用 `PlanCatalogResponse`，不暴露 `code`、`versionNo`、`status`。
- `/c/benefits/products` 使用 `BenefitCatalogProduct`，不含库存、名额、领取状态、卡密。
- `cover` 是设计系统封面标识（`benefit` / `benefit2` / `benefit3`）；当前没有权益图片素材，前端映射为渐变封面，不返回伪图片 URL。
- `JwtAuthenticationFilter` 捕获无效 token 的运行时异常并清空上下文；公开接口继续按游客处理，受保护接口仍由统一 401 响应拦截。

## 前端拦截点

### c-app

- 启动默认进首页，不强制登录；首页、权益目录、AI 模板可游客浏览。
- 「我的」Tab 进入游客版我的页：登录/注册入口 + 会员卖点，不再直接跳登录。
- 权益页游客只展示目录，按钮为「登录后领取」；点击写入待领取 SKU，登录后回跳并自动领取。
- AI 页游客展示全部模板与参数，按钮为「登录后生成」；上传、生成、我的作品均先登录。
- 工单页未登录时带原页面地址跳登录；登录成功回工单页。
- 登录页仍是手机号 + 密码；登录成功按 `pm_login_redirect` 回跳，领取意图通过 `?claim=SKU` 带回。

### c-h5

- 新增首页；导航固定为首页 / 我的 / 权益 / AI 模板 / 套餐 / 订单 / 工单。
- 首页、套餐、权益目录、AI 模板公开；订单、工单、会员数据在点击或加载时拦截登录。
- 游客点击「登录后购买」：保存套餐 ID，登录后回套餐页并自动下单、进入模拟支付结果页。
- 游客点击「登录后领取」：保存 SKU，登录后回权益页并自动领取，成功时展示一次性卡密。
- 游客「我的」页显示登录/注册入口与卖点；登录后回原页面。
- 登录页仍是手机号 + 密码，短信验证码未做。

## 验收证据

### curl

无 token：

```text
/c/plans               200  {"code":0,...}
/c/benefits/products   200  {"code":0,...}
/c/ai/templates        200  {"code":0,...}
/c/contents            200  {"code":0,...}
/c/ai/files/missing.png 404 {"bizCode":"NOT_FOUND"}
```

带无效 token 访问公开接口：

```text
/c/plans               200
/c/benefits/products   200
/c/ai/templates        200
/c/contents            200
```

无 token 访问受保护接口：

```text
/c/benefits           401  {"error":"未登录或登录已过期","bizCode":"UNAUTHORIZED"}
/c/benefits/claims    401
/c/orders             401
/c/members/me         401
/c/ai/tasks           401
/c/tickets            401
/c/user/info          401
```

公开响应字段核对：

- `/c/plans`：只有目录展示字段，无 `code/versionNo/status`。
- `/c/benefits/products`：只有 `skuCode/name/description/rules/cover`，无 `stock/quota/claimed/cardMasked`。
- `/c/ai/templates`、`/c/contents`：无用户字段。

### 浏览器

环境：c-server `127.0.0.1:8081`，c-h5 `127.0.0.1:5174`，c-app H5 `127.0.0.1:5173`，390×844。

1. 游客 c-h5：首页 → 套餐 → 权益目录 → AI 模板，均可浏览。
2. 游客 c-h5 在套餐页勾选协议并点「登录后购买」→ 登录页 → 输入 `13900000004 / Test123456` → 回套餐并自动下单 → 模拟支付成功，显示订单号与生效周期。
3. 游客 c-h5 在权益目录点「登录后领取」→ 登录页 → 登录后回权益页并自动领取，成功浮层展示一次性卡密「GUEST-STORAGE-2-20261127」。
4. 游客 c-app：首页展示 8 个 AI 模板与权益入口；权益页展示 4 个目录商品；AI 页展示模板与「登录后生成」；我的页展示登录/注册与卖点。
5. 游客 c-app 在权益目录点「登录后领取」→ 登录 → 回权益页自动领取电影票，浮层展示卡密「GUEST-MOVIE-2-20261127」。
6. 游客 c-app 在 AI 页点「登录后生成」→ 登录 → 回 AI 页，保留 AI 写真类型。

## 截图

| 文件 | 内容 |
|---|---|
| `docs/screenshots/guest-h5-home.png` | c-h5 游客首页 |
| `docs/screenshots/guest-h5-plans.png` | c-h5 游客套餐 |
| `docs/screenshots/guest-h5-benefit.png` | c-h5 游客权益目录 |
| `docs/screenshots/guest-h5-ai.png` | c-h5 游客 AI 模板 |
| `docs/screenshots/guest-h5-member.png` | c-h5 游客我的页 |
| `docs/screenshots/guest-h5-login.png` | c-h5 登录拦截 |
| `docs/screenshots/guest-h5-purchase-result.png` | 登录回跳后购买成功 |
| `docs/screenshots/guest-h5-claim-result.png` | 登录回跳后领取成功 |
| `docs/screenshots/guest-app-home.png` | c-app 游客首页 |
| `docs/screenshots/guest-app-benefit.png` | c-app 游客权益目录 |
| `docs/screenshots/guest-app-ai.png` | c-app 游客 AI 模板 |
| `docs/screenshots/guest-app-member.png` | c-app 游客我的页 |
| `docs/screenshots/guest-app-login.png` | c-app 登录页 |
| `docs/screenshots/guest-app-claim-result.png` | c-app 登录回跳后领取成功 |
| `docs/screenshots/guest-app-ai-return.png` | c-app 登录回跳 AI 页 |

## 验证命令

```bash
cd c-server && jenv exec /Users/hanson/work/Env/apache-maven-3.8.8/bin/mvn -q -DskipTests package
cd ../c-app && npm run type-check && npm run build:h5
cd ../c-h5 && npm run build
```

## 已知限制

- 权益 `cover` 目前是渐变封面标识，不是图片 URL；B 端商品表没有封面字段，本次未改 B 端表结构。
- c-app 基线没有套餐/订单页面；完整下单与模拟支付在 c-h5 验收，c-app 负责游客浏览、领取、AI 与我的页拦截。
- 登录/注册入口复用现有手机号 + 密码登录页；自助注册仍走 `/c/auth/register`，短信验证码未做。
- c-app H5 直连 `8081`，开发 CORS 默认加入 `5173`；生产仍由 `CORS_ALLOWED_ORIGINS` 控制。
- 游客态不写本地“已领取/已看过”标记；刷新后重新从公开目录加载。
