# 数据库恢复

备份由 `backup.sh` 生成，格式为 PostgreSQL custom dump，默认保留 7 天。

## 1. 先停写并备份现场

```bash
cd deploy
docker compose --env-file .env -f docker-compose.prod.yml stop admin-server c-server
./backup.sh
```

`admin-web`、`c-h5`、`nginx` 可以继续运行，但恢复期间接口会返回 502。

## 2. 重建空库

以下操作会删除当前数据库，只允许在确认离线备份可用后执行。

```bash
set -a && . ./.env && set +a
docker compose --env-file .env -f docker-compose.prod.yml exec -T postgres \
  psql -U "$DB_USER" -d postgres \
  -c "DROP DATABASE IF EXISTS ${DB_NAME};" \
  -c "CREATE DATABASE ${DB_NAME} OWNER ${DB_USER};"
```

## 3. 恢复 dump

```bash
docker compose --env-file .env -f docker-compose.prod.yml exec -T postgres \
  pg_restore -U "$DB_USER" -d "$DB_NAME" --no-owner --no-privileges \
  < backups/premium-member-YYYYMMDD-HHMMSS.dump
```

## 4. 启动并核对

```bash
docker compose --env-file .env -f docker-compose.prod.yml start admin-server c-server
docker compose --env-file .env -f docker-compose.prod.yml ps
curl -fsS http://127.0.0.1/api/health
```

核对用户、会员、订单数量与备份前一致，再恢复流量。若 dump 来自旧版本，
先停应用，按升级文档执行增量 SQL，再启动新版本。
