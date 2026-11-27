#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

if [ ! -f .env ]; then
  echo "missing deploy/.env; copy .env.prod.example to .env first" >&2
  exit 1
fi

env_value() {
  awk -F= -v key="$1" '$1 == key { print substr($0, length(key) + 2); exit }' .env
}

for key in DB_PASSWORD REDIS_PASSWORD JWT_ACCESS_SECRET JWT_REFRESH_SECRET \
           C_JWT_ACCESS_SECRET C_JWT_REFRESH_SECRET BENEFIT_CARD_SECRET; do
  value="$(env_value "$key")"
  case "$value" in
    ""|*CHANGE_ME*)
      echo "$key is missing or still CHANGE_ME in deploy/.env" >&2
      exit 1
      ;;
  esac
done

if [ ! -f certs/fullchain.pem ] || [ ! -f certs/privkey.pem ]; then
  mkdir -p certs
  public_host="$(env_value PUBLIC_HOST)"
  public_host="${public_host:-localhost}"
  echo "generating self-signed certificate for ${public_host} (replace with real certs in production)"
  openssl req -x509 -nodes -newkey rsa:2048 -days 365 \
    -keyout certs/privkey.pem -out certs/fullchain.pem \
    -subj "/CN=${public_host}" \
    -addext "subjectAltName=DNS:${public_host},DNS:localhost,IP:127.0.0.1" >/dev/null 2>&1
  chmod 600 certs/privkey.pem
fi

started_at=$(date +%s)
docker compose --env-file .env -f docker-compose.prod.yml up -d --build

services=$(docker compose --env-file .env -f docker-compose.prod.yml config --services | wc -l | tr -d ' ')
for _ in $(seq 1 120); do
  healthy=$(docker compose --env-file .env -f docker-compose.prod.yml ps --format '{{.Health}}' 2>/dev/null | grep -c '^healthy$' || true)
  [ "$healthy" -ge "$services" ] && break
  sleep 3
done

docker compose --env-file .env -f docker-compose.prod.yml ps
elapsed=$(( $(date +%s) - started_at ))
echo "premium-member prod is up in ${elapsed}s: http://127.0.0.1:$(env_value HTTP_PORT || true)"
