# admin-server

B 端 API，Java 21 + Spring Boot 3.5.16 + PostgreSQL + Redis。当前为骨架，完整实现见 `docs/`。

## 启动

```bash
jenv local 21.0.9
jenv exec /Users/hanson/work/Env/apache-maven-3.8.8/bin/mvn -q -DskipTests package
jenv exec java -jar target/premium-member-admin-server-0.1.0.jar
```

默认端口 `5320`，上下文路径 `/api`。
