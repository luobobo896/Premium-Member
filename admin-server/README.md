# admin-server

B 端 API，Java 21 + Spring Boot 3.5.16 + PostgreSQL + Redis。Maven 多模块结构，依赖方向：

```text
admin-bootstrap -> admin-system / admin-member -> admin-framework -> admin-common
```

| 模块 | 职责 |
|---|---|
| `admin-common` | 统一响应、分页、业务异常与错误码 |
| `admin-framework` | Security/JWT、Redis、MyBatis-Plus、CORS、异常、OpenAPI |
| `admin-system` | 用户、角色、权限、菜单、部门、认证 |
| `admin-member` | 会员业务示例 |
| `admin-bootstrap` | 启动类、配置、SQL、可执行 jar |

## 启动

```bash
jenv local 21.0.9
jenv exec /Users/hanson/work/Env/apache-maven-3.8.8/bin/mvn -q -DskipTests package
jenv exec java -jar admin-bootstrap/target/admin-bootstrap-0.1.0.jar
```

默认端口 `5320`，上下文路径 `/api`。
