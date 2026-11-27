# c-server

C 端 API：Spring Boot 3.5.16 + Java 21 + PostgreSQL + Redis。依赖方向：

```text
c-bootstrap -> c-user / c-biz -> c-framework -> c-common
```

| 模块 | 职责 |
|---|---|
| `c-common` | 统一响应、分页、业务异常与错误码 |
| `c-framework` | Security/JWT、Redis、MyBatis-Plus、CORS、异常、OpenAPI |
| `c-user` | 注册、登录、刷新、登出、用户信息 |
| `c-biz` | 按手机号只读关联 `pm_member` |
| `c-bootstrap` | 启动类、配置、`/health`、建表脚本、可执行 jar |

## 接口

| 方法 | 路径 | 认证 |
|---|---|---|
| POST | `/c/auth/register` | 否 |
| POST | `/c/auth/login` | 否，写 `c_refresh` Cookie |
| POST | `/c/auth/refresh` | refresh Cookie |
| POST | `/c/auth/logout` | refresh Cookie |
| GET | `/c/user/info` | Bearer |
| GET | `/c/members/me` | Bearer |
| GET | `/health` | 否 |

## 启动

```bash
jenv local 21.0.9
export DB_PASSWORD=premium_member_dev_only
export REDIS_PASSWORD=premium_member_dev_only
export C_JWT_ACCESS_SECRET="$(openssl rand -base64 48)"
export C_JWT_REFRESH_SECRET="$(openssl rand -base64 48)"
jenv exec /Users/hanson/work/Env/apache-maven-3.8.8/bin/mvn -q -DskipTests package
jenv exec java -jar c-bootstrap/target/c-bootstrap-0.1.0.jar
```

访问 `http://127.0.0.1:8081/health`。首次启动按 `src/main/resources/db/schema.sql` 创建 `c_user`，脚本可重复执行。

## 说明

- 端口由 `C_SERVER_PORT` 覆盖，默认 `8081`。
- 开发连接 `127.0.0.1:15432` 的 PostgreSQL 与 `127.0.0.1:16379` 的 Redis（DB 2），见 `application-dev.yml`。
- JWT 使用 `C_JWT_ACCESS_SECRET` / `C_JWT_REFRESH_SECRET` 与 issuer `premium-member-c`，不复用 admin 密钥。
- refresh token 白名单 key 为 `c:refresh:<jti>`。
- 生产默认不自动建表，`SQL_INIT_MODE` 保持 `never`。
- 与 admin-server 不共享 Maven 模块，可独立打包部署。
