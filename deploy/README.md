# deploy

本地依赖与反向代理示例。

## 启动依赖

在仓库根目录执行：

```bash
docker compose up -d
```

端口：PostgreSQL `15432`，Redis `16379`。默认账号来自 `deploy/docker-compose.yml` 的开发默认值；生产必须通过环境变量覆盖。

## nginx

`nginx.conf` 把 `/api/` 转发到 `admin-server:5320`，把 `/c-api/` 转发到 `c-server:8081`。容器名只在 docker compose 网络中有效，单独部署时替换 upstream 地址。
