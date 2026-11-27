# OpenAPI 与 Swagger

## 地址

后端 context-path 为 `/api`：

| 资源 | URL |
|---|---|
| OpenAPI JSON | `http://127.0.0.1:5320/api/v3/api-docs` |
| Swagger UI | `http://127.0.0.1:5320/api/swagger-ui/index.html` |
| Actuator health | `http://127.0.0.1:5320/api/actuator/health` |
| 自定义健康检查 | `http://127.0.0.1:5320/api/health` |

实测：`/v3/api-docs` 返回 200，JSON 11,460 字节，标题 `Premium-Member Admin API`，共 12 个 path；Swagger UI 返回 200。

## 已暴露 path

```text
/auth/captcha
/auth/codes
/auth/login
/auth/logout
/auth/refresh
/health
/members
/members/{id}
/menu/all
/system/dept/list
/system/menu/list
/user/info
```

## 鉴权

OpenAPI 定义了 `bearerAuth`（HTTP Bearer JWT）。在 Swagger UI 右上角 Authorize 填入登录返回的 access token，不需要手动加 `Bearer ` 前缀。

登录、刷新、登出、验证码、健康检查、OpenAPI 文档本身在 `SecurityConfig` 中放行；其余接口需要认证，会员和系统接口再经过 `@PreAuthorize` 权限校验。

## 生产

`application-prod.yml` 默认关闭：

```yaml
springdoc:
  api-docs:
    enabled: ${SPRINGDOC_ENABLED:false}
  swagger-ui:
    enabled: ${SPRINGDOC_ENABLED:false}
```

只在受控网络或临时排障时通过 `SPRINGDOC_ENABLED=true` 打开，不把 Swagger 暴露到公网。
