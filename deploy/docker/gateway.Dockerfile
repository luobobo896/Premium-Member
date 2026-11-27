FROM nginxinc/nginx-unprivileged:1.29-alpine
COPY deploy/nginx/gateway.conf /etc/nginx/conf.d/default.conf
COPY deploy/nginx/gateway-locations.inc /etc/nginx/conf.d/gateway-locations.inc
EXPOSE 8080 8443
