# c-server

C 端 API，当前为最小可启动骨架：Spring Boot 3.5.16 + Java 21，仅提供 `/health`，不包含业务逻辑。

## 启动

```bash
jenv local 21.0.9
jenv exec /Users/hanson/work/Env/apache-maven-3.8.8/bin/mvn -q -DskipTests package
jenv exec java -jar target/premium-member-c-server-0.1.0.jar
```

访问 `http://127.0.0.1:8081/health`。

## 说明

- 端口由 `C_SERVER_PORT` 覆盖，默认 `8081`。
- 暂未接入数据库与 Redis；接入时新增依赖并补充 `application.yml` 占位。
