# syntax=docker/dockerfile:1.7
FROM node:24-alpine AS build
ENV CI=true
RUN corepack enable
WORKDIR /src
COPY admin-web/ admin-web/
WORKDIR /src/admin-web
RUN pnpm install --frozen-lockfile
RUN pnpm run build:antd

FROM nginxinc/nginx-unprivileged:1.29-alpine
COPY deploy/nginx/admin-web.conf /etc/nginx/conf.d/default.conf
COPY --from=build /src/admin-web/apps/web-antd/dist /usr/share/nginx/html
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --retries=5 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/ || exit 1
