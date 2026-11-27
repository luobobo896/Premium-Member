#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

if [ ! -f .env ]; then
  echo "missing deploy/.env" >&2
  exit 1
fi

env_value() {
  awk -F= -v key="$1" '$1 == key { print substr($0, length(key) + 2); exit }' .env
}

mkdir -p backups
stamp="$(date +%Y%m%d-%H%M%S)"
target="backups/premium-member-${stamp}.dump"

docker compose --env-file .env -f docker-compose.prod.yml exec -T postgres \
  pg_dump -U "$(env_value DB_USER)" -d "$(env_value DB_NAME)" -Fc > "$target"

find backups -type f -name 'premium-member-*.dump' -mtime +7 -delete
echo "backup written: ${target}"
