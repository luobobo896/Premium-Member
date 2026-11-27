# Premium-Member

Premium-Member 是面向会员业务的多端系统：B 端管理后台、C 端 API、移动 App、移动 H5。当前状态：开发中，骨架已可启动，业务接口持续补齐（最近验证 2026-11-27）。

## 目录职责

| 目录 | 技术栈 | 职责 |
|---|---|---|
| `admin-server/` | Java 21 + Spring Boot 3.5.16 + PostgreSQL + Redis | B 端 API，登录/权限/菜单/会员模块 |
| `admin-web/` | vue-vben-admin v5.7.0 | B 端管理后台基座，已移除 demo 页面 |
| `c-server/` | Java 21 + Spring Boot 3.5.16 | C 端 API，仅最小可启动骨架 |
| `c-app/` | uni-app + Vue3 + TypeScript | App 与小程序共用端 |
| `c-h5/` | Vue3 + Vite + Vant | 移动端 H5 |
| `docs/` | Markdown | 版本、架构、API、数据库、部署文档 |
| `deploy/` | Docker Compose / nginx | 本地依赖与反向代理示例 |

## 快速开始

前置：JDK 21（jenv）、Maven 3.8.8、Docker 29+、Node.js 22.18+ 或 24、pnpm 10+。

```bash
jenv local 21.0.9
cp .env.example .env
set -a && source .env && set +a
cd deploy && docker compose up -d
```

预期：PostgreSQL 监听 `15432`，Redis 监听 `16379`。

```bash
cd admin-server
jenv exec /Users/hanson/work/Env/apache-maven-3.8.8/bin/mvn -q -DskipTests package
jenv exec java -jar admin-bootstrap/target/admin-bootstrap-0.1.0.jar
```

预期：`http://127.0.0.1:5320/api/v3/api-docs` 与 `http://127.0.0.1:5320/api/swagger-ui/index.html` 可访问。

```bash
cd admin-web
pnpm install
pnpm dev:antd
```

预期：管理后台在 `http://127.0.0.1:5666`，`/api` 代理到 `127.0.0.1:5320/api`。已默认 `VITE_ACCESS_MODE=backend`、关闭 Nitro mock，登录账号 `admin / Admin@123456`。

```bash
cd c-server
jenv exec /Users/hanson/work/Env/apache-maven-3.8.8/bin/mvn -q -DskipTests package
jenv exec java -jar c-bootstrap/target/c-bootstrap-0.1.0.jar
```

预期：`http://127.0.0.1:8081/health` 返回 `{"status":"UP",...}`。

`c-app` 与 `c-h5` 的启动命令见各自 README。

## 文档

- 版本与兼容性：`docs/01-versions.md`
- 架构与目录：`docs/02-architecture.md`
- 配置说明：`docs/03-configuration.md`
- 自动化测试：`docs/24-testing.md`
- 数据库：`docs/04-database.md`
- 认证与权限：`docs/05-auth.md`
- Redis：`docs/06-redis.md`
- 接口约定：`docs/07-api-conventions.md`
- 会员模块：`docs/08-member-module.md`
- OpenAPI：`docs/09-openapi.md`
- 部署：`docs/10-deploy.md`
- 验收记录：`docs/11-acceptance.md`
- 系统管理：`docs/12-system-management.md`
- 订单与会员周期：`docs/17-orders.md`

## 联系与反馈

维护人：项目业主；问题反馈：GitHub Issues（https://github.com/luobobo896/Premium-Member/issues）。

## 许可

内部项目，未声明开源许可。
