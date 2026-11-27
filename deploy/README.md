# deploy

本地依赖与反向代理示例。

## 启动依赖

在 `deploy/` 目录执行：

```bash
cd deploy
docker compose up -d
```

端口：PostgreSQL `15432`，Redis `16379`。默认账号来自 `deploy/docker-compose.yml` 的开发默认值；生产必须通过环境变量覆盖。

## nginx

`nginx.conf` 把 `/api/` 转发到 `admin-server:5320`，把 `/c-api/` 转发到 `c-server:8081`。容器名只在 docker compose 网络中有效，单独部署时替换 upstream 地址。

PostgreSQL 首次启动会按顺序执行 `admin-bootstrap/src/main/resources/db/schema.sql` 与 `data.sql`；已有 `pgdata` 卷不会重复执行，需要重置时先 `docker compose down -v`（会删除本地数据）。
