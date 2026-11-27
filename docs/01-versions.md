# 版本清单与兼容性

以下版本在 2026-11-27 逐项核验。后端不使用 SNAPSHOT、RC、Beta；Maven 坐标以 Maven Central metadata 的 `release`/版本列表为准。

| 组件 | 版本 | 来源与说明 |
|---|---|---|
| Java | 21.0.9 LTS（Azul Zulu） | 本机 `jenv which java` = `/Users/hanson/.jenv/versions/21.0.9/bin/java`；根目录 `.java-version` 为 `21.0.9` |
| Maven | 3.8.8 | `/Users/hanson/work/Env/apache-maven-3.8.8/bin/mvn` |
| Spring Boot | 3.5.16 | [Maven Central metadata](https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-starter-parent/maven-metadata.xml)；3.x 线最新稳定版，4.2.0 已存在但需求限定 3.x |
| Spring Framework | 6.2.19 | 启动日志 `Started AdminApplication` 同次输出 |
| Spring Security | 6.5.11 | `mvn dependency:tree` 实测 |
| MyBatis-Plus | 3.5.18.1 | [Maven Central metadata](https://repo1.maven.org/maven2/com/baomidou/mybatis-plus-spring-boot3-starter/maven-metadata.xml) |
| MyBatis-Plus jsqlparser | 3.5.18.1 | [Maven Central metadata](https://repo1.maven.org/maven2/com/baomidou/mybatis-plus-jsqlparser/maven-metadata.xml)；3.5.9+ 分页插件独立依赖 |
| springdoc-openapi | 2.10.1 | [Maven Central metadata](https://repo1.maven.org/maven2/org/springdoc/springdoc-openapi-starter-webmvc-ui/maven-metadata.xml)；2.x 对接 Boot 3，3.x 对接 Boot 4，不混用 |
| JJWT | 0.13.0 | [Maven Central metadata](https://repo1.maven.org/maven2/io/jsonwebtoken/jjwt-api/maven-metadata.xml) |
| PostgreSQL JDBC | 42.7.15 | [Maven Central metadata](https://repo1.maven.org/maven2/org/postgresql/postgresql/maven-metadata.xml) |
| Lombok | 1.18.46 | Spring Boot 3.5.16 管理，`mvn dependency:tree` 实测 |
| PostgreSQL | `postgres:17-alpine` | Docker 镜像，2026-11-27 拉取成功，容器健康 |
| Redis | `redis:8.2-alpine` | Docker 镜像，2026-11-27 拉取成功，容器健康 |
| vue-vben-admin | v5.7.0 | tag `v5.7.0`，commit `63a38dce49ba109f61607994e21ba921d8e970e9`；源码核对位于 `/tmp/vben-ref` |
| Node.js（实测） | 25.9.0 | 本机 `node -v`；vben 上游 `engines` 写的是 `^22.18.0 || ^24.0.0`，生产建议按上游要求安装 |
| pnpm | 12.23.0 | admin-web 依赖安装实测 |
| c-app | uni-app 3.0.0-5030420261029002、Vue 3.4.21、Vite 5.2.8、TypeScript 4.9.5 | `c-app/package-lock.json` 与 `npm ls --depth=0` |
| c-h5 | Vue 3.5.34、Vite 8.4.0、Vant 4.10.2、TypeScript 6.0.3 | `c-h5/package-lock.json` 与 `npm ls --depth=0` |

## 兼容性说明

- Java 21 是需求指定版本；本机另有 JDK 8/17，构建必须通过 `jenv exec`，不能直接依赖当前 shell 的 `JAVA_HOME`。
- Spring Boot 3.5.16 使用 Jakarta API，Spring Security 6.5 的 `SecurityFilterChain` 与 `@PreAuthorize` 配置已按 6.x 写法实现。
- MyBatis-Plus 3.5.18.1 的分页拦截器依赖 `mybatis-plus-jsqlparser`，两个坐标版本必须一致。
- springdoc 2.10.1 只用于 Spring Boot 3.x；升级 Boot 4 时必须整体切换到 springdoc 3.x。
- JJWT 0.13.0 使用 `Jwts.parser().verifyWith(SecretKey)` 新 API；密钥长度至少 32 字节，当前默认 HS512 需要更长密钥，生产应使用至少 64 字节随机值。
- 开发环境 Redis 使用 DB 0，生产默认 DB 1；key 仍带环境前缀，见 `06-redis.md`。
