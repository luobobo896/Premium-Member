# 本地启动、联调与部署

## 前置

- JDK 21，仓库根 `.java-version` 为 `21.0.9`，构建统一用 `jenv exec`。
- Maven 3.8.8。
- Docker 29+ / Docker Compose v5。
- Node.js 22.18+ 或 24（admin-web 上游要求），pnpm 10+。

## 1. 启动依赖

```bash
cp .env.example .env
set -a && source .env && set +a
cd deploy
docker compose up -d
docker compose ps
```

预期：

```text
premium-member-postgres (postgres:17-alpine) Up (healthy) [15432]
premium-member-redis    (redis:8.2-alpine)    Up (healthy) [16379]
```

首次启动自动执行 `schema.sql` 与 `data.sql`。

## 2. 构建 admin-server

```bash
cd admin-server
jenv exec /Users/hanson/work/Env/apache-maven-3.8.8/bin/mvn -q -DskipTests package
ls -lh admin-bootstrap/target/admin-bootstrap-0.1.0.jar
```

只有 `admin-bootstrap` 是可执行 fat jar；其他模块产物是普通 jar。

## 3. 启动 API

环境变量必须包含数据库密码、Redis 密码和两个 JWT 密钥：

```bash
export DB_PASSWORD=premium_member_dev_only
export REDIS_PASSWORD=premium_member_dev_only
export JWT_ACCESS_SECRET="$(openssl rand -base64 48)"
export JWT_REFRESH_SECRET="$(openssl rand -base64 48)"
jenv exec java -jar admin-bootstrap/target/admin-bootstrap-0.1.0.jar
```

预期日志：

```text
Tomcat started on port 5320 (http) with context path '/api'
Started AdminApplication in 3.671 seconds
```

验证：

```bash
curl -s http://127.0.0.1:5320/api/health
curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1:5320/api/v3/api-docs
```

## 4. 联调 admin-web

```bash
cd admin-web
pnpm install
pnpm dev:antd
```

访问 `http://127.0.0.1:5666`，用 `admin / Admin@123456` 登录。

关键配置已提交在 `admin-web/apps/web-antd/`：

| 文件 | 配置 | 作用 |
|---|---|---|
| `.env.development` | `VITE_NITRO_MOCK=false` | 关闭上游 mock，全部请求走后端 |
| `.env.development` | `VITE_ACCESS_MODE=backend` | 动态菜单来自 `GET /api/menu/all` |
| `.env.development` | `VITE_GLOB_API_URL=/api` | 浏览器侧 API 基地址 |
| `vite.config.ts` | target `http://127.0.0.1:5320/api` | 开发代理转发 |
| `src/preferences.ts` | `enableRefreshToken: true` | access token 过期时自动刷新并重放 |

端口与路径：

- vben dev：`5666`
- admin-server：`5320`，context-path `/api`
- 浏览器请求 `http://127.0.0.1:5666/api/**` 经 Vite 代理到 `http://127.0.0.1:5320/api/**`
- refresh token 只存在 `jwt` HttpOnly Cookie 中，路径 `/api`，前端 JS 不读取

登录闭环：

1. `POST /api/auth/login` 返回 `accessToken` 并写 refresh Cookie。
2. 前端带 `Authorization: Bearer <accessToken>` 请求 `/api/user/info`、`/api/auth/codes`、`/api/menu/all`。
3. access token 过期后接口返回 401，前端 `POST /api/auth/refresh`（仅 Cookie）换新 access token，并自动重放原请求。
4. 退出登录调用 `POST /api/auth/logout`，清 Cookie 和本地状态，回到登录页；旧 access token 再访问返回 401。

刷新实测方法：启动后端时设 `JWT_ACCESS_TTL=60s`，登录后等 token 过期，再点会员列表第 2 页。浏览器 Network 应依次出现：

```text
GET /api/members?page=2&pageSize=2   401
POST /api/auth/refresh              200
GET /api/members?page=2&pageSize=2   200
```

2026-11-27 实测 `pnpm run build:antd` 成功：11 个任务全部通过，生产包 `admin-web/apps/web-antd/dist`。

常见问题：

- 登录后立刻回登录页：确认后端已在 `5320` 启动、`/api` 代理 target 正确，且数据库种子已执行。
- `/menu/all` 返回 401：先确认请求头有 `Authorization: Bearer`；若刚过期，检查 browser 是否有 `jwt` Cookie（Path 必须是 `/api`）。
- 直连 `http://127.0.0.1:5320/api` 而不是走代理时，后端必须允许来源 `http://127.0.0.1:5666`（默认已允许），且跨域请求要带 `withCredentials`。
- 菜单点击 404：检查后端菜单 `component` 与 `src/views` 下文件路径是否一致；当前 8 个菜单组件路径均已覆盖。

## 5. c-app / c-h5

```bash
cd c-app && npm install && npm run dev:h5
cd c-h5  && npm install && npm run dev
```

端口分别为 5173、5174。c-h5 与 c-app 均已有会员、权益、订单和 AI 创作页面；AI 上传文件默认落在
`c-server/data/ai`，可用 `AI_STORAGE_DIR` 改目录，Mock 供应商用 `AI_PROVIDER=mock`。

## 6. 打包与部署

- API 制品：`admin-bootstrap/target/admin-bootstrap-0.1.0.jar`。
- 容器部署时把 jar 放入镜像，启动命令 `java -jar app.jar`。
- 生产环境设置 `SPRING_PROFILES_ACTIVE=prod`，补齐 `DB_*`、`REDIS_*`、JWT 密钥。
- 生产 Cookie 为 `Secure` + `SameSite=None`，必须 HTTPS。
- nginx 示例见 `deploy/nginx.conf`：`/api/` 转发到 `admin-server:5320`。
- OpenAPI 默认关闭；按需设置 `SPRINGDOC_ENABLED=true`。

## 7. 回滚

- 应用回滚：切回上一个 jar 或镜像 digest，不需要回滚数据库结构。
- 本轮新增 `sys_operation_log` 表，并给 `sys_role` 增加 `data_scope`、给 `pm_member`
  增加 `owner_id`（带默认值或可空），旧版本应用可以继续运行。
- 本地数据回滚：`cd deploy && docker compose down -v`，会删除本地卷。
- 生产数据库变更必须先备份，再按扩展-迁移-切换-清理执行。

## 8. 当前远端状态

仓库 `https://github.com/luobobo896/Premium-Member`，分支 `main`，不 force push。提交按表结构/common/framework、认证权限、会员业务、docs/deploy 四段拆分。
