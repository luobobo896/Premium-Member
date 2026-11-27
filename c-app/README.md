# c-app

C 端 App 与小程序共用端，技术栈已确认：uni-app + Vue3 + TypeScript。当前只有最小可运行页面，不包含业务逻辑。

## 启动

```bash
npm install
npm run dev:h5
```

H5 调试默认访问 `http://127.0.0.1:5173`。

微信小程序：

```bash
npm run dev:mp-weixin
```

产物在 `dist/dev/mp-weixin`，用微信开发者工具导入该目录。开发者工具需要单独安装，并在 `src/manifest.json` 填入小程序 AppID（当前为空）。

App（Android/iOS）：用 HBuilderX 打开本目录运行，或用 uni-app CLI 的 `npm run dev:app` 后通过 HBuilderX 真机运行。HBuilderX 需要单独安装。

构建：`npm run build:h5`、`npm run build:mp-weixin`。

## 实际版本

| 依赖 | 版本 |
|---|---|
| @dcloudio/uni-app | 3.0.0-5030420261029002 |
| @dcloudio/uni-h5 | 3.0.0-5030420261029002 |
| @dcloudio/uni-mp-weixin | 3.0.0-5030420261029002 |
| @dcloudio/vite-plugin-uni | 3.0.0-5030420261029002 |
| Vue | 3.4.21 |
| Vite | 5.2.8 |
| TypeScript | 4.9.5 |
| vue-tsc | 1.8.27 |

2026-11-27 实测：`npm install` 成功，`npm run dev:h5` 监听 `127.0.0.1:5173`，`curl -I` 返回 200；`npm run build:h5` 通过（Compiler version 5.34）。
