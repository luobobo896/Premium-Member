# c-h5

移动端 H5，技术栈已确认：Vue3 + Vite + Vant。当前只有最小可运行页面，不包含业务逻辑。

## 启动

```bash
npm install
npm run dev
```

默认访问 `http://127.0.0.1:5174`。构建：`npm run build`。

## 实际版本

| 依赖 | 版本 |
|---|---|
| Vue | 3.5.34 |
| Vite | 8.4.0 |
| Vant | 4.10.2 |
| TypeScript | 6.0.3 |
| @vitejs/plugin-vue | 6.0.11 |
| vue-tsc | 3.3.1 |

2026-11-27 实测：`npm install` 成功，`npm run dev` 监听 `127.0.0.1:5174`，`curl -I` 返回 200；`npm run build` 通过。
