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

访问 `http://127.0.0.1:5666`。开发代理把 `/api` 转发到 `http://localhost:5320/api`。

默认 vben 配置是前端权限模式且关闭自动刷新。要用后端菜单与刷新，按 `05-auth.md` 修改：

```ts
app: { accessMode: 'backend', enableRefreshToken: true }
```

2026-11-27 实测 `pnpm run build:antd` 成功：11 个任务全部通过，生产包 `admin-web/apps/web-antd/dist`。

## 5. c-app / c-h5

```bash
cd c-app && npm install && npm run dev:h5
cd c-h5  && npm install && npm run dev
```

端口分别为 5173、5174。两者本轮冻结，只保留最小页面。

## 6. 打包与部署

- API 制品：`admin-bootstrap/target/admin-bootstrap-0.1.0.jar`。
- 容器部署时把 jar 放入镜像，启动命令 `java -jar app.jar`。
- 生产环境设置 `SPRING_PROFILES_ACTIVE=prod`，补齐 `DB_*`、`REDIS_*`、JWT 密钥。
- 生产 Cookie 为 `Secure` + `SameSite=None`，必须 HTTPS。
- nginx 示例见 `deploy/nginx.conf`：`/api/` 转发到 `admin-server:5320`。
- OpenAPI 默认关闭；按需设置 `SPRINGDOC_ENABLED=true`。

## 7. 回滚

- 应用回滚：切回上一个 jar 或镜像 digest，不需要回滚数据库结构。
- 本轮 DDL 只新增表，没有修改已有表；旧版本应用可以继续运行。
- 本地数据回滚：`cd deploy && docker compose down -v`，会删除本地卷。
- 生产数据库变更必须先备份，再按扩展-迁移-切换-清理执行。

## 8. 当前远端状态

仓库 `https://github.com/luobobo896/Premium-Member`，分支 `main`，不 force push。提交按表结构/common/framework、认证权限、会员业务、docs/deploy 四段拆分。
