# admin-web

基于 vue-vben-admin v5.7.0（tag `v5.7.0`，commit `63a38dce49ba109f61607994e21ba921d8e970e9`）的管理后台基座。已移除 demo 页面与 vben 演示链接，保留登录、请求封装、权限、菜单、仪表盘与类型定义。

## 启动

前置：Node.js 22.18+ 或 24、pnpm 10+。

```bash
pnpm install
pnpm dev:antd
```

默认访问 `http://127.0.0.1:5666`。接口前缀 `/api` 由 `apps/web-antd/vite.config.ts` 代理到 `http://localhost:5320/api`。

## 与后端联调

- 开发环境：保持 `apps/web-antd/.env` 的 `VITE_GLOB_API_URL=/api`，启动 admin-server 即可。
- 生产环境：修改 `apps/web-antd/.env.production` 的 `VITE_GLOB_API_URL` 为实际 API 地址（含 `/api`）。
- 后端菜单加载：在 `apps/web-antd/src/preferences.ts` 的 `app` 配置中设置 `accessMode: 'backend'`。
- Token 刷新：设置 `app.enableRefreshToken: true`。
