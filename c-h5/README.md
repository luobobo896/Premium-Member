# c-h5

移动端 H5，Vue3 + Vite + Vant。页面：登录、会员中心。接口走真实 c-server（默认 `http://127.0.0.1:8081`）。

## 启动

```bash
npm install
npm run dev
```

默认访问 `http://127.0.0.1:5174`。构建：`npm run build`。

后端地址可用 `VITE_C_API_BASE` 覆盖，例如：

```bash
VITE_C_API_BASE=http://127.0.0.1:8081 npm run dev
```

access token 存 `sessionStorage`，refresh token 由 c-server 写入 HttpOnly Cookie。任何接口返回 401 会清理本地 token 并回登录页；退出登录调用 `/c/auth/logout`。

## 实际版本

| 依赖 | 版本 |
|---|---|
| Vue | 3.5.34 |
| Vite | 8.4.0 |
| Vant | 4.10.2 |
| TypeScript | 6.0.3 |
| @vitejs/plugin-vue | 6.0.11 |
| vue-tsc | 3.3.1 |

2026-11-27 实测：`npm run build` 通过；`npm run dev` 监听 `127.0.0.1:5174`，已用 ego-browser 完成登录、会员中心、退出和 401 回登录页验证。截图见 `docs/screenshots/c-*.png`。
