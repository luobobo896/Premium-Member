# 生产化部署

本文覆盖镜像分层、首次部署、升级、回滚、日志与常见故障。基线 `91dddb4`。

## 1. 产物与入口

| 容器 | 构建 | 运行 | 对外 |
|---|---|---|---|
| `admin-server` | Maven 3.9 + JDK 21 | `eclipse-temurin:21-jre-alpine`，非 root `app` | 仅 compose 内网 `5320` |
| `c-server` | Maven 3.9 + JDK 21 | `eclipse-temurin:21-jre-alpine`，非 root `app` | 仅 compose 内网 `8081` |
| `admin-web` | Node 24 + pnpm | `nginx-unprivileged` 静态托管，SPA fallback | 仅内网 `8080` |
| `c-h5` | Node 24 + npm | `nginx-unprivileged` 静态托管，构建 base `/h5/` | 仅内网 `8080` |
| `nginx` | `nginx-unprivileged` | 统一 HTTP/TLS 入口 | `80`、`443` |

统一路由：

| 路径 | 目标 |
|---|---|
| `/` | admin-web |
| `/api/` | admin-server，保留 `/api` 前缀 |
| `/h5/` | c-h5 |
| `/c-api/` | c-server，转发时去掉 `/c-api` 前缀 |
| `/healthz` | 网关自身健康检查 |

PostgreSQL、Redis 不映射宿主机端口，只在 compose 网络内可达。

## 2. 首次部署

前置：Docker 29+、Docker Compose v5+、可用的 80/443 端口。

```bash
cd deploy
cp .env.prod.example .env
```

必须用 `openssl` 生成的键：

```bash
openssl rand -hex 24     # DB_PASSWORD
openssl rand -hex 24     # REDIS_PASSWORD
openssl rand -base64 48  # JWT_ACCESS_SECRET
openssl rand -base64 48  # JWT_REFRESH_SECRET
openssl rand -base64 48  # C_JWT_ACCESS_SECRET
openssl rand -base64 48  # C_JWT_REFRESH_SECRET
openssl rand -base64 48  # BENEFIT_CARD_SECRET
```

四个 JWT 密钥必须互不相同，C 端与管理端不能复用。其余键见 `deploy/.env.prod.example`。

AI 与 OSS（不配置 OSS 时回退本地 `ai-data` 卷，但生产图生视频需要供应商可访问的输入图）：

```bash
AI_PROVIDER=dashscope
DASHSCOPE_API_KEY=...
DASHSCOPE_BASE_URL=https://dashscope.aliyuncs.com
OSS_ENDPOINT=https://oss-cn-shenzhen.aliyuncs.com
OSS_BUCKET=demopt
OSS_ACCESS_KEY_ID=...
OSS_ACCESS_KEY_SECRET=...
OSS_PREFIX=premium-member/ai/prod
```

OSS 对象 key 为随机 UUID，C 端只拿 30 分钟签名 URL。生产优先使用 OSS；本地无 OSS 时保留
`/c/assets/{token}` 临时输入兜底，需配置 `PUBLIC_ASSET_BASE_URL`。

生产证书放到 `deploy/certs/fullchain.pem` 与 `deploy/certs/privkey.pem`。若文件不存在，
`up.sh` 会生成仅用于本地验证的自签证书：

```bash
cd deploy
./up.sh
```

`up.sh` 会校验必填密钥、生成缺省证书、执行 `docker compose -f docker-compose.prod.yml up -d --build`，
并等待全部服务 health。

首次启动 PostgreSQL 会按顺序执行 `schema.sql`、`data.sql`、C 端 `schema.sql`。
种子管理员 `admin / Admin@123456` 只用于首次进入，登录后立即在「系统管理 → 用户」改密码。

验证：

```bash
curl -fsS http://127.0.0.1/healthz
curl -fsS http://127.0.0.1/api/health
curl -fsS http://127.0.0.1/c-api/health
curl -kfsS https://127.0.0.1/healthz
```

## 3. 升级

```bash
cd deploy
./backup.sh
git pull --rebase
docker compose --env-file .env -f docker-compose.prod.yml up -d --build
docker compose --env-file .env -f docker-compose.prod.yml ps
```

数据库结构变化必须先备份，按「扩展 → 迁移 → 切换应用 → 清理旧字段」执行。
`application-prod.yml` 固定 `spring.sql.init.mode=never`，应用启动不会改表。

只升级单个服务：

```bash
docker compose --env-file .env -f docker-compose.prod.yml up -d --build admin-server
```

## 4. 回滚

应用回滚：

```bash
git checkout <上一个已验证的 commit>
docker compose --env-file .env -f docker-compose.prod.yml up -d --build
```

数据库只在发生不兼容迁移时回滚，步骤见 `deploy/restore.md`。旧应用可以继续读新增可空列，
优先回滚应用而不是数据库。

## 5. 日志

```bash
cd deploy
docker compose --env-file .env -f docker-compose.prod.yml logs -f --tail=200
docker compose --env-file .env -f docker-compose.prod.yml logs -f admin-server
docker compose --env-file .env -f docker-compose.prod.yml logs -f c-server
```

默认输出明文日志。设置 `LOG_JSON_FORMAT=ecs`（或 `logstash`、`gelf`）后，
Spring Boot 以结构化 JSON 输出，便于采集。

Actuator 只暴露 `health,info`，`show-details=never`，且网关不转发 `/actuator`。

## 6. 备份

```bash
cd deploy
./backup.sh
```

输出到 `deploy/backups/premium-member-YYYYMMDD-HHMMSS.dump`，自动删除 7 天前的文件。
恢复见 `deploy/restore.md`。

## 7. 常见故障

| 现象 | 排查 |
|---|---|
| `up.sh` 报某密钥仍是 `CHANGE_ME` | 按第 2 节重新生成并写入 `deploy/.env` |
| `nginx` 起不来，日志提示缺证书 | 放置 `deploy/certs/{fullchain,privkey}.pem`，或让 `up.sh` 生成自签证书 |
| 后端反复重启，日志提示占位符无法解析 | `.env` 缺少 DB/Redis/JWT/卡密变量；不要删除 prod 配置中无默认值的 `${...}` |
| 页面 502 | `docker compose ps` 看后端是否 healthy；再查 `admin-server`/`c-server` 日志 |
| 登录后接口 401 | 确认访问的是 nginx 入口而不是容器端口；C 端 Cookie Path 必须是 `/c-api/c/auth` |
| 首次启动没有表 | `pgdata` 卷已存在时 initdb 不会重跑；确认是否需要备份后重建卷 |
| 备份失败 | 确认 PostgreSQL healthy，`.env` 中 `DB_USER`/`DB_NAME` 与库一致 |
| 图生视频失败 `AI_PROVIDER_INPUT_UNREACHABLE` | OSS 未配置时检查 `PUBLIC_ASSET_BASE_URL`；OSS 模式检查签名 URL 是否可被公网访问 |
| DashScope 失败 `AI_PROVIDER_NOT_ENABLED` | 百炼控制台未开通原因中的模型名 |
| OSS 上传失败 HTTP 403 | 检查 AK/SK、bucket 区域与 endpoint 是否匹配 |

## 8. 安全清单

- `deploy/.env`、`deploy/certs/`、`deploy/backups/` 均被 `.gitignore` 忽略。
- Java 镜像创建并切换到非 root 用户；前端与网关使用 `nginx-unprivileged`。
- 数据库与 Redis 不在 compose 中发布端口。
- `application-prod.yml` 不含默认数据库密码、Redis 密码、JWT 密钥或卡密密钥。
- CORS 只读取 `ADMIN_CORS_ALLOWED_ORIGINS` / `C_CORS_ALLOWED_ORIGINS` 白名单。
- 生产 Cookie 为 `Secure` + `SameSite=None`，必须走 HTTPS。
- `DASHSCOPE_API_KEY`、`OSS_ACCESS_KEY_ID/SECRET` 只放环境变量；日志禁止输出密钥和完整签名串。
- OSS 当前先用 bucket `public-read` + 主账号 AK 跑通。TODO：改 private ACL，创建只具备该 bucket
  `PutObject/GetObject/DeleteObject` 的 RAM 子账号，并给输入前缀配置 7 天生命周期。

## 9. CI

`.github/workflows/ci.yml` 在 PR 和 `main` push 时执行：

- `admin-server`、`c-server`：Java 21，`mvn -B package`
- `admin-web`：pnpm 10 + Node 24，`pnpm run build:antd`
- `c-h5`：npm ci + build
- 五个 Dockerfile：`docker build` 验证，不 push

首轮运行 `45945896557` 全部通过：3 个构建 job 和 5 个 Docker build job 均成功，
只有 GitHub 对 Node 20 action 的弃用提醒，不影响结果。

## 10. 验收记录

环境：macOS 15.7.5 arm64，JDK 21.0.9，Maven 3.8.8，Docker 29.8.2，
Docker Compose v5.5.0。基线 `91dddb4`，2026-11-27。

首次构建（含拉取基础镜像与两套 Maven 依赖）：

```text
premium-member prod is up in 323s
```

删除数据卷后的最终镜像冷启动：

```text
docker compose down -v
docker compose up -d
elapsed=32s healthy=7
admin-server Up (healthy)  c-server Up (healthy)  nginx Up (healthy)
postgres Up (healthy)      redis Up (healthy)
admin-web Up (healthy)     c-h5 Up (healthy)
```

网关登录：

```text
admin_login=200
admin_refresh=200
c_login=200
c_refresh=200
c_cookie_path=/c-api/c/auth
```

健康与安全：

```text
GET /healthz             200 ok
GET /api/health          200 {"status":"UP"}
GET /c-api/health        200 {"status":"UP"}
https://127.0.0.1/healthz 200（自签证书）
GET /api/actuator/health 200 {"status":"UP"}
GET /api/actuator/metrics 401（未公开）
postgres / redis docker port 无输出（未发布宿主机端口）
admin-server / c-server 运行用户 app；nginx 用户 101
```

重启验证：

```text
docker exec premium-member-prod-c-server sh -c 'kill 1'
RestartCount: 0 -> 1
premium-member-prod-c-server Up (healthy)
```

备份：

```text
./backup.sh
backup written: backups/premium-member-20261127-132524.dump（84.3K）
```

截图：

```text
docs/screenshots/deploy-admin-login.png
docs/screenshots/deploy-admin-home.png
docs/screenshots/deploy-c-login.png
docs/screenshots/deploy-c-home.png
docs/screenshots/deploy-health.png
```

未验证项：

1. 只用自签证书验证了 TLS 入口，没有接入真实 CA 证书和域名。
2. `restore.md` 的恢复命令未在现有数据上执行，避免破坏验收库。
3. GitHub Actions 首轮已通过；后续提交的 CI 结果以对应 run 为准。
4. 微信支付仍是占位实现，AI 供应商仍为 mock；本轮未接真实外部服务。
5. 只验证单机 Compose，没有验证多节点编排、外部对象存储或托管数据库。
