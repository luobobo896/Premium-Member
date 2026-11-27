# 生产化部署

本文覆盖镜像分层、首次部署、重建/升级、健康检查、重启语义、回滚、日志与常见故障。
基线 `a350238`，2026-11-27。

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

`/c-api/` 的口径：`proxy_pass http://c-server:8081/`（末尾单斜杠，去掉 `/c-api` 前缀后直连 c-server 根），
前端请求路径自带 `/c`，例如 `GET /c-api/c/plans`。若把这里改成 `.../c/`，所有 C 端接口会变成 `/c/c/...` 并返回 401。
`deploy/nginx.conf` 是历史遗留文件，没有任何 Dockerfile 使用，改网关请改 `deploy/nginx/gateway-locations.inc`。

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
curl -fsS  http://127.0.0.1/healthz
curl -fsS  http://127.0.0.1/api/health
curl -fsS  http://127.0.0.1/c-api/health
curl -kfsS https://127.0.0.1/healthz
curl -kfsS https://127.0.0.1/api/health
curl -kfsS https://127.0.0.1/c-api/health
```

### 健康检查

七个容器都定义了 healthcheck，`docker compose ps` 的 `STATUS` 必须同时出现 `Up` 和
`(healthy)`，少一个都不算启动完成：

```bash
cd deploy
docker compose --env-file .env -f docker-compose.prod.yml ps
docker compose --env-file .env -f docker-compose.prod.yml ps --format '{{.Name}} {{.Status}}'
```

逐个看探针结果和最近一次输出：

```bash
docker inspect -f '{{.State.Health.Status}} {{range .State.Health.Log}}{{.ExitCode}} {{end}}' \
  premium-member-prod-admin-server
```

| 容器 | 探针 |
|---|---|
| postgres | `pg_isready` |
| redis | `redis-cli -a ... ping` 返回 `PONG` |
| admin-server | `GET 127.0.0.1:5320/api/health` |
| c-server | `GET 127.0.0.1:8081/health` |
| admin-web / c-h5 | `GET 127.0.0.1:8080/` |
| nginx | `GET 127.0.0.1:8080/healthz` |

登录与核心接口冒烟（自签证书加 `-k`，真实证书去掉）：

```bash
curl -k -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Admin@123456"}' \
  https://127.0.0.1/api/auth/login

curl -k -H 'Content-Type: application/json' \
  -d '{"phone":"13916064900","password":"Test123456","nickname":"部署验证"}' \
  https://127.0.0.1/c-api/c/auth/register

TOKEN=<login 返回的 accessToken>
curl -k -H "Authorization: Bearer $TOKEN" \
  'https://127.0.0.1/api/members?page=1&size=2'
```

## 3. 重建与升级

### 3.1 标准步骤

先备份，再拉代码、重建镜像、重建容器。`build` 只生成镜像，不会替换正在运行的容器；
必须再执行 `up -d`，Docker 才会按新镜像重建容器。

```bash
cd deploy
./backup.sh
git pull --rebase
docker compose --env-file .env -f docker-compose.prod.yml build
docker compose --env-file .env -f docker-compose.prod.yml up -d
docker compose --env-file .env -f docker-compose.prod.yml ps
```

`up -d --build` 等价于上面两条，适合一次性操作；拆开执行更容易确认构建产物和启动结果。
构建默认带 BuildKit 缓存，源码未变的层会复用；镜像 ID 仍会随基础镜像/构建元数据变化。

数据库结构变化必须先备份，按「扩展 → 迁移 → 切换应用 → 清理旧字段」执行。
`application-prod.yml` 固定 `spring.sql.init.mode=never`，应用启动不会改表。

### 3.2 确认新镜像真的生效

不要只看 `up -d` 返回成功。以下三项必须同时成立：

```bash
# 1) 容器使用的镜像 ID == 当前 tag 的镜像 ID
docker inspect -f '{{.Image}}' premium-member-prod-admin-server
docker image inspect -f '{{.Id}}' premium-member/admin-server:prod

# 2) 容器启动时间晚于镜像内制品时间
docker inspect -f '{{.State.StartedAt}}' premium-member-prod-admin-server
docker exec premium-member-prod-admin-server ls -l /app/app.jar

# 3) 前端静态资源来自本次构建
docker exec premium-member-prod-c-h5 ls -l /usr/share/nginx/html/index.html
docker exec premium-member-prod-c-h5 ls -l /usr/share/nginx/html/assets | head
docker exec premium-member-prod-admin-web ls -l /usr/share/nginx/html/index.html
```

`admin-server` / `c-server` 的 `/app/app.jar` mtime 应与本次镜像创建时间一致（镜像构建时
`COPY` 保留构建阶段时间）。`admin-web` 若源码未变会复用构建缓存，`index.html` 的 mtime
可能早于本次构建，这是正常的；此时以「容器镜像 ID == tag 镜像 ID」为准。

出现「容器镜像 ID 仍是旧的」时，执行 `docker compose ... up -d --force-recreate <service>`；
仍不生效则检查是否连错了 compose 项目（`docker compose ls`）或 Docker context。

### 3.3 只升级单个服务

```bash
docker compose --env-file .env -f docker-compose.prod.yml up -d --build admin-server
```

### 3.4 重启语义

七个服务都配置了 `restart: unless-stopped`：

- 进程崩溃、`docker kill`、容器内 `kill 1`（非 0 退出）→ 自动拉起。
- `docker stop` / `docker compose stop` 是人工停止，不会被自动拉起；即使 Docker 重启也保持停止。
- 修改 `.env` 或镜像后，`up -d` 会重建有变化的容器；`restart` 只重启现有容器，不换镜像。
- 只替换容器内文件（`docker cp app.jar`、覆盖静态资源）不会重启进程，也不会自动生效。

验证自动拉起：

```bash
before=$(docker inspect -f '{{.RestartCount}} {{.State.Health.Status}}' premium-member-prod-c-server)
docker exec premium-member-prod-c-server sh -c 'kill 1'
sleep 5
docker inspect -f '{{.RestartCount}} {{.State.Status}} {{.State.Health.Status}}' premium-member-prod-c-server
docker compose --env-file .env -f docker-compose.prod.yml ps premium-member-prod-c-server
```

本轮实测：`RestartCount` 从 0 变 1，约 4 秒恢复 `healthy`，经网关
`GET /c-api/health` 返回 200。

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
docker compose --env-file .env -f docker-compose.prod.yml logs --since=30m nginx
docker logs --tail=200 premium-member-prod-admin-server
```

排查顺序：网关日志看状态码与上游，应用日志看异常栈，`docker inspect` 看退出码、重启次数和
healthcheck 输出。容器反复重启时先看 `State.ExitCode`，不要直接删卷。

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
| 改了代码或换了 jar，接口行为仍是旧的 | 容器没重建或 Java 进程没重启，见 7.1 |
| 启动报 `bind: address already in use` | 80/443 被占用，见 7.2 |
| 浏览器证书错误、`curl` 报证书校验失败 | 自签证书或证书域名不匹配，见 7.3 |
| compose 报 `required variable ... is missing a value` | `.env` 缺必填变量，见 7.4 |
| 页面 502 | `docker compose ps` 看后端是否 healthy；再查 `admin-server`/`c-server` 日志 |
| 登录后接口 401 | 确认访问的是 nginx 入口而不是容器端口；C 端 Cookie Path 必须是 `/c-api/c/auth` |
| 首次启动没有表 | `pgdata` 卷已存在时 initdb 不会重跑；确认是否需要备份后重建卷 |
| 备份失败 | 确认 PostgreSQL healthy，`.env` 中 `DB_USER`/`DB_NAME` 与库一致 |
| 图生视频失败 `AI_PROVIDER_INPUT_UNREACHABLE` | OSS 未配置时检查 `PUBLIC_ASSET_BASE_URL`；OSS 模式检查签名 URL 是否可被公网访问 |
| DashScope 失败 `AI_PROVIDER_NOT_ENABLED` | 百炼控制台未开通原因中的模型名 |
| OSS 上传失败 HTTP 403 | 检查 AK/SK、bucket 区域与 endpoint 是否匹配 |

### 7.1 旧进程读旧 jar（改了不生效 / 偶发 500）

Java 进程在启动时建立 classpath，类按需加载。覆盖运行中进程使用的 jar 不会热更新：
已经加载的类继续用内存里的旧字节码；之后第一次用到尚未加载的类时，才会去读磁盘上的新文件。
如果文件被截断、复制中断或换成了不完整包，就会抛 `ClassNotFoundException` /
`ZipException` / `NoClassDefFoundError`，接口表现为 500。前端静态资源同理：容器里文件换了，
nginx 不重启也可能继续用旧缓存/旧文件。

最小复现（临时目录，不碰生产数据）：

```text
临时写一个 20 行 Demo：/version 返回类常量 v1，/lazy 首次执行 Class.forName("LazyFeature")
java -cp demo.jar Demo
curl /version                -> 200 v1
head -c 512 /dev/urandom > demo.jar   # 模拟 jar 复制中断
curl /version                -> 200 v1   # 旧进程不受影响，仍跑旧代码
curl /lazy                   -> 500 class load failed: ClassNotFoundException: LazyFeature
```

线上判断：

```bash
docker inspect -f '{{.State.StartedAt}}' premium-member-prod-admin-server
docker exec premium-member-prod-admin-server ls -l /app/app.jar
docker exec premium-member-prod-admin-server sha256sum /app/app.jar
```

`StartedAt` 早于 jar 的 mtime，说明进程启动后 jar 又被改过，当前运行的仍是旧内容。
正确做法是重建镜像并让容器重建，而不是 `docker cp` 后继续跑：

```bash
cd deploy
docker compose --env-file .env -f docker-compose.prod.yml build admin-server
docker compose --env-file .env -f docker-compose.prod.yml up -d --force-recreate admin-server
```

本项目不支持 jar 热替换；任何 `app.jar` 或前端产物更新都必须重启/重建对应容器。

### 7.2 端口占用

80/443 或自定义端口被其他进程占用时，nginx 容器起不来，日志为
`bind() to 0.0.0.0:80 failed (98: Address already in use)`。

```bash
lsof -nP -iTCP:80 -iTCP:443 -sTCP:LISTEN
docker ps --format '{{.Names}} {{.Ports}}' | grep -E ':(80|443)->'
```

释放占用进程，或改 `deploy/.env` 的 `HTTP_PORT` / `HTTPS_PORT` 后重新 `up -d`。
注意别的 compose 项目也可能占端口，`docker compose ls` 可确认项目名。

### 7.3 证书问题

`up.sh` 在缺证书时生成 `CN=localhost` 的自签证书，浏览器会提示不受信任，
`curl` 不带 `-k` 会报证书校验失败。这只用于本机验收；生产必须换成域名匹配的正式证书。

```bash
openssl x509 -in deploy/certs/fullchain.pem -noout -subject -dates
openssl s_client -connect 127.0.0.1:443 -servername <域名> </dev/null 2>/dev/null | openssl x509 -noout -subject
docker compose --env-file deploy/.env -f deploy/docker-compose.prod.yml logs --tail=50 nginx
```

常见原因：`fullchain.pem` 与 `privkey.pem` 不匹配、证书过期、证书没有覆盖访问域名。
替换证书后执行 `docker compose ... up -d --force-recreate nginx`（证书是只读挂载）。

### 7.4 环境变量缺失

compose 对必填项用了 `${NAME:?}`，缺失时在创建容器前就会失败，例如：

```text
error while interpolating services.admin-server.environment.JWT_ACCESS_SECRET:
required variable JWT_ACCESS_SECRET is missing a value
```

只检查键名、不打印值：

```bash
awk -F= 'NF && $0 !~ /^#/ {print $1"=" (length($2)>0?"set":"empty")}' deploy/.env
```

补齐 `deploy/.env` 后重新 `up -d`；环境变量变化会触发容器重建。不要把密钥写进
`docker-compose.prod.yml`、Dockerfile 或日志。

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

环境：macOS 15.7.5 arm64，Docker 29.8.2，Docker Compose v5.5.0，自签证书
`CN=localhost`。基线 `a350238`，2026-11-27 16:05–16:09。

重建（源码未变，命中 BuildKit 缓存）：

```text
build_start=2026-11-27 16:05:40
build_end=2026-11-27 16:06:07   # 27s
Image premium-member/admin-server:prod Built
Image premium-member/c-server:prod   Built
Image premium-member/admin-web:prod  Built
Image premium-member/c-h5:prod       Built
Image premium-member/gateway:prod    Built
```

镜像 ID 前后对比（证明不是旧镜像）：

```text
                 重建前            重建后
admin-server     0722fb703425      e2180dbae72b
c-server         1e65c06dad16      fc7be60c02c7
admin-web        a00f1139e472      aecc8fcf14c0
c-h5             3b586c46b671      3dbae266aa4e
gateway          100304a64f61      9dc2e54e7ee9
```

容器与镜像一致、制品时间在启动之前：

```text
admin-server container_image=sha256:e2180dbae72b... image_id=sha256:e2180dbae72b... same=yes
  /app/app.jar  2026-11-27 08:06:02 UTC  53,819,389 bytes  sha256=ddd7cd9c...
c-server     container_image=sha256:fc7be60c02c7... image_id=sha256:fc7be60c02c7... same=yes
  /app/app.jar  2026-11-27 08:06:01 UTC  49,050,568 bytes  sha256=32d6fe22...
c-h5         index.html 2026-11-27 08:06:06 UTC，assets/index-BkFR04j-.js 08:06
admin-web    容器镜像 ID 与 tag 一致；index.html 05:18:18 UTC 为构建缓存复用（源码未变）
```

启动（保留原数据卷，`up -d` 重建容器）：

```text
elapsed=33s healthy=7
premium-member-prod-admin-server   Up (healthy)
premium-member-prod-c-server       Up (healthy)
premium-member-prod-admin-web      Up (healthy)
premium-member-prod-c-h5           Up (healthy)
premium-member-prod-nginx          Up (healthy)
premium-member-prod-postgres       Up (healthy)
premium-member-prod-redis          Up (healthy)
```

网关 HTTPS 冒烟（自签证书，`-k`）：

```text
POST /api/auth/login              HTTP=200 code=0（admin / Admin@123456）
POST /api/auth/refresh            HTTP=200
POST /c-api/c/auth/register       HTTP=200 code=0  id=3  phone=13916064900
POST /c-api/c/auth/login          HTTP=200 code=0
GET  /c-api/c/user/info           HTTP=200 code=0
GET  /api/members?page=1&size=2   HTTP=200 code=0（返回会员分页）
GET  /healthz                     HTTP=200 ok
GET  /api/health                  HTTP=200 {"status":"UP"}
GET  /c-api/health                HTTP=200 {"status":"UP"}
```

本次注册在验收库新增 1 个 C 端账号（`13916064900`），用于验证「注册 → 登录 → 带 token 访问」。

重启语义：

```text
before: RestartCount=0  running  healthy
docker exec premium-member-prod-c-server sh -c 'kill 1'
after:  RestartCount=1  running  healthy（约 4s）
GET /c-api/health -> 200
```

旧进程读旧 jar 复现：覆盖运行中进程的 jar 后，`/version` 仍返回旧值 200；首次加载新类
`/lazy` 返回 500 `ClassNotFoundException`。详见 7.1。

备份：

```text
./backup.sh
backup written: backups/premium-member-20261127-160943.dump（84.5K）
```

截图（本轮重新采集 health、登录页、登录后首页）：

```text
docs/screenshots/deploy-health.png
docs/screenshots/deploy-admin-login.png
docs/screenshots/deploy-admin-home.png
```

未验证项：

1. 只用自签证书验证了 TLS 入口，没有接入真实 CA 证书和域名。
2. `restore.md` 的恢复命令未在现有数据上执行，避免破坏验收库。
3. 本轮未重跑 GitHub Actions；以 `a350238` 对应的 CI run 为准。
4. 外部 AI、OSS、微信支付链路未在本次验收中实际调用。
5. 只验证单机 Compose，没有验证多节点编排、外部对象存储或托管数据库。
6. 旧进程读旧 jar 是本机最小复现，不是生产事故现场记录。
