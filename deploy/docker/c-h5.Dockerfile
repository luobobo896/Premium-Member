# syntax=docker/dockerfile:1.7
FROM node:24-alpine AS build
ENV CI=true
ARG VITE_C_API_BASE=/c-api
ENV VITE_C_API_BASE=${VITE_C_API_BASE}
WORKDIR /src
COPY c-h5/ c-h5/
WORKDIR /src/c-h5
RUN npm ci
RUN npx vue-tsc --noEmit && npx vite build --base=/h5/

FROM nginxinc/nginx-unprivileged:1.29-alpine
COPY deploy/nginx/c-h5.conf /etc/nginx/conf.d/default.conf
COPY --from=build /src/c-h5/dist /usr/share/nginx/html
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --retries=5 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/ || exit 1
