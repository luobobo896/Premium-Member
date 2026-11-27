# 架构与 Maven 模块边界

## 多工程

```text
Premium-Member/
  admin-server/   B 端 API
  admin-web/      vue-vben-admin v5.7.0 基座
  c-server/       C 端 API 骨架
  c-app/          uni-app App/小程序骨架
  c-h5/           Vue3 + Vite + Vant H5 骨架
  docs/           交付文档
  deploy/         PostgreSQL、Redis、nginx 示例
```

## admin-server 模块

```text
admin-bootstrap -> admin-system / admin-member -> admin-framework -> admin-common
```

| 模块 | 包名 | 职责 | 依赖 |
|---|---|---|---|
| `admin-common` | `com.premium.member.common` | `R`、`PageResult`、`PageQuery`、`ErrorCode`、`BusinessException` | 无业务依赖 |
| `admin-framework` | `com.premium.member.framework` | Security/JWT、Redis、MyBatis-Plus、CORS、全局异常、OpenAPI | common |
| `admin-system` | `com.premium.member.system` | 用户/角色/权限/菜单/部门、登录、刷新、登出、用户信息、菜单路由 | framework |
| `admin-member` | `com.premium.member.member` | 会员分页、增删改查、DTO/VO | framework |
| `admin-bootstrap` | `com.premium.member.bootstrap` | 唯一启动类、配置文件、SQL、可执行 jar | system + member |

约束落地：

- 只有 `admin-bootstrap` 配置 `spring-boot-maven-plugin`，其余模块是普通 jar。
- `admin-system` 与 `admin-member` 不互相引用；会员模块暂时不需要系统用户信息。
- 跨模块只共享 common 的响应/分页/异常和 framework 的安全/缓存能力。
- SQL 放在 `admin-bootstrap/src/main/resources/db/`，由 docker compose 在首次启动时执行。

## 请求链路

登录：

```text
AuthController -> AuthService -> SysUserMapper / SysRoleMapper / SysPermissionMapper
                -> JwtTokenService -> TokenStore(Redis)
```

业务请求：

```text
JwtAuthenticationFilter -> SecurityContext(权限码)
Controller @PreAuthorize -> Service -> Mapper -> PostgreSQL
```

菜单：

```text
MenuController -> MenuService -> SysMenuMapper
               -> 普通菜单转 vben RouteRecordStringComponent
               -> 管理端接口保留 button 节点和 authCode
```

## 未来拆服务

admin-server 已经按可变部署边界拆模块，后续拆微服务时：

1. `admin-system` 独立为 auth/system 服务，暴露登录、用户、权限和菜单 API。
2. `admin-member` 独立为 member 服务，只依赖认证服务校验后的用户身份。
3. `admin-framework` 可以抽成公司内部 starter，保留 JWT、Redis、异常和 OpenAPI 配置。
4. 数据库可按 `sys_*` 与 `pm_*` 分库；当前同库事务只覆盖单模块，迁移不需要拆业务事务。

c-server 与 admin-server 不共享 Maven 模块，可独立构建、部署和扩缩容。
