# c-server

C 端 API，独立部署的 Maven 多模块骨架：Spring Boot 3.5.16 + Java 21。依赖方向：

```text
c-bootstrap -> c-user / c-biz -> c-framework -> c-common
```

| 模块 | 职责 |
|---|---|
| `c-common` | 统一响应、分页、业务异常与错误码 |
| `c-framework` | Security/JWT、Redis、MyBatis-Plus、CORS、异常、OpenAPI |
| `c-user` | C 端账号域占位 |
| `c-biz` | 会员权益/订单等业务域占位 |
| `c-bootstrap` | 启动类、配置、`/health`、可执行 jar |

## 启动

```bash
jenv local 21.0.9
jenv exec /Users/hanson/work/Env/apache-maven-3.8.8/bin/mvn -q -DskipTests package
jenv exec java -jar c-bootstrap/target/c-bootstrap-0.1.0.jar
```

访问 `http://127.0.0.1:8081/health`。

## 说明

- 端口由 `C_SERVER_PORT` 覆盖，默认 `8081`。
- 暂未接入数据库；`application.yml` 已排除 DataSource/MyBatis-Plus 自动配置，接入时删除 exclude 并补连接占位。
- 与 admin-server 不共享 Maven 模块，可独立打包部署。
