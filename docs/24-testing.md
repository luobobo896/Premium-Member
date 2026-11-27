# 自动化测试

测试分三层：后端单元测试（surefire）、后端集成测试（failsafe + Testcontainers）、
Playwright 端到端冒烟。CI 定义在 `.github/workflows/ci.yml`。

## 后端集成测试

目录：

- `admin-server/admin-bootstrap/src/test/java/**/*IT.java`
- `c-server/c-bootstrap/src/test/java/**/*IT.java`

覆盖范围：

- 认证：登录、refresh、无 token 401、退出后 refresh 失效、错误密码 403。
- 权限：viewer 写操作 403；SELF 数据范围读取他人会员 404。
- 交易：admin 退款超过实付 409 且不落退款单；C 端重复建单 409；重复模拟支付回调只产生一条成功流水和一个周期。
- 权益：2 个名额耗尽 409、同 SKU 重复领取返回原领取单、卡密库存不足 409、领取成功返回卡密。
- AI 额度：RESERVE/SETTLE/RELEASE 账本、额度耗尽 409、同一幂等键一单、跨会员复用幂等键 409。

实现方式：每个 bootstrap 模块用 Testcontainers 启动 `postgres:17-alpine` 与
`redis:8.2-alpine`；PostgreSQL 的 `/docker-entrypoint-initdb.d` 直接执行真实
`schema.sql` / `data.sql`，因此 `DO $$` 块不会被 Spring SQL 初始化按分号拆断。
测试通过真实随机端口 HTTP 调用接口，并断言 HTTP 状态码与 `bizCode`。

本地执行（Docker 必须可用）：

```bash
cd admin-server && mvn verify
cd c-server && mvn verify
```

只跑集成测试：

```bash
cd admin-server
mvn -pl admin-bootstrap -am verify -Dit.test=AuthPermissionIT

cd c-server
mvn -pl c-bootstrap -am verify -Dit.test=OrderPaymentIT
```

说明：

- 名称以 `IT` 结尾的用例由 maven-failsafe 在 `verify` 阶段执行；`mvn package` 不会跑它们。
- 父 POM 的 maven-compiler-plugin 显式使用 Java 21，避免用户级 Maven settings
  里的旧 JDK profile 干扰。
- admin 模块在测试内嵌容器中实际挂在根路径，用例直接请求 `/auth`、`/members`；
  打包运行的应用仍使用 `/api` 上下文路径。

## Playwright E2E

目录 `e2e/`，一条 happy path：游客浏览套餐与权益目录 → 登录回跳 → 自动建单 →
模拟支付 → 订单页显示“已支付”。测试开始前通过 admin API 创建唯一会员，再通过
C 端注册同手机号账号，因此可重复运行，不依赖种子账号的订单状态。

本地执行前确保：

1. `deploy/docker-compose.yml` 的 PostgreSQL/Redis 已启动。
2. admin-server（5320）、c-server（8081）以 `dev` profile 启动。
3. c-h5 dev server 在 5174 启动。

```bash
cd e2e
pnpm install
pnpm exec playwright install chromium
pnpm test
```

自定义地址：

```bash
E2E_BASE_URL=http://127.0.0.1:5174 \
E2E_C_API_BASE=http://127.0.0.1:8081 \
E2E_ADMIN_API_BASE=http://127.0.0.1:5320/api \
pnpm test
```

生产 compose 的 `prod` profile 不注册 `MockPaymentProvider`，模拟支付会返回
`PAYMENT_CHANNEL_UNSUPPORTED`，因此这条全链路 E2E 固定跑 dev server；生产 compose
只用于人工浏览或真实支付渠道验证。

失败时 `test-results/` 保留截图、视频和 trace；HTML 报告在 `playwright-report/`。
CI 失败会以 `playwright-artifacts` 上传。

## CI

- `backend`：两个后端模块 `mvn package`（单元测试）。
- `backend-it`：两个后端模块 `mvn verify`，Testcontainers 启动 PG/Redis。
- `admin-web`、`c-h5`、`docker`：原有构建校验，不 push 镜像。
- `e2e`：docker compose 启动 PG/Redis，构建并启动 admin/c server 与 c-h5，
  安装 Chromium 后跑 Playwright；失败上传截图/trace。

Maven 与 pnpm 均使用 actions 缓存。E2E job 超时 35 分钟。

## 排障

| 现象 | 处理 |
|---|---|
| Testcontainers 拉镜像慢/失败 | 预拉 `postgres:17-alpine`、`redis:8.2-alpine`，确认 Docker daemon 可用 |
| 集成测试初始化 SQL 失败 | 确认走的是容器 `/docker-entrypoint-initdb.d`，不是 Spring `sql.init` |
| E2E 卡在登录后 | 确认 c-server 以 `dev` 启动（mock 支付 bean 只有在 dev 注册） |
| E2E 登录 401 | 检查 Redis 密码与 c-server/admin-server 环境变量一致 |
| E2E 创建会员失败 | 检查 admin-server 的 `/api/health` 与 JWT 密钥 |
| 页面元素找不到 | 先看 `e2e/test-results` 截图或 `pnpm exec playwright show-trace <trace.zip>` |
