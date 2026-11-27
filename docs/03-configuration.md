# 配置说明

## 文件

| 文件 | 作用 |
|---|---|
| `admin-bootstrap/src/main/resources/application.yml` | 端口、context-path、JWT、Cookie、CORS、MyBatis-Plus、OpenAPI、Actuator |
| `application-dev.yml` | 本地 PostgreSQL/Redis、debug 日志、OpenAPI 开启 |
| `application-prod.yml` | 生产数据源、Redis DB 1、Cookie Secure、OpenAPI 默认关闭 |
| `.env.example` | 本地环境变量模板，不含真实密钥 |

## 环境变量

| 变量 | 必填 | 默认/说明 |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | 否 | `dev` |
| `ADMIN_SERVER_PORT` | 否 | `5320` |
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` | dev 有默认值 | dev 默认 `127.0.0.1:15432/premium_member` |
| `DB_PASSWORD` | 是 | 不提供默认值 |
| `REDIS_HOST` / `REDIS_PORT` | dev 有默认值 | dev 默认 `127.0.0.1:16379` |
| `REDIS_PASSWORD` | 是 | 不提供默认值 |
| `REDIS_DATABASE` | 否 | dev `0`，prod `1` |
| `JWT_ACCESS_SECRET` | 是 | 至少 32 字节，建议 64 字节随机值 |
| `JWT_REFRESH_SECRET` | 是 | 与 access 不同的随机值 |
| `JWT_ACCESS_TTL` / `JWT_REFRESH_TTL` | 否 | `2h` / `30d` |
| `COOKIE_SECURE` | 否 | dev `false`；prod 配置固定 `true` |
| `CORS_ALLOWED_ORIGINS` | 否 | 默认 `http://localhost:5666,http://127.0.0.1:5666` |
| `CACHE_PREFIX` | 否 | 默认 `pm:dev`，生产应改为 `pm:prod` |
| `SPRINGDOC_ENABLED` | 否 | prod 默认 `false`，需要临时打开时设为 `true` |

## 本地加载

```bash
cp .env.example .env
set -a && source .env && set +a
```

`.env` 已被 `.gitignore` 忽略。不要把真实密码或 token 写进 `application*.yml`。

## Cookie

- 名称 `jwt`，HttpOnly，Path `/api`，与 vben mock 一致。
- 开发环境 HTTP 下 `SameSite=Lax`、`Secure=false`。
- 生产环境 `application-prod.yml` 强制 `Secure=true`，CookieFactory 同时使用 `SameSite=None`，适合前后端跨域 HTTPS 部署。
- 如果生产 API 与页面同域同源，可以按实际域名调整 `app.cookie.path`，但必须与 `/api` 上下文路径一致。
