# Android App 构建（Capacitor）

本文记录 C 端 Android debug APK 的构建方式。方案是用 Capacitor 把 `c-app` 的 H5 构建产物打成本地壳，不写原生业务。基线 `5b07af4`。

## 1. 产物

| 项 | 值 |
|---|---|
| APK | `c-app/dist/android/premium-member-1.0.0-debug.apk` |
| 大小 | 4,489,957 字节（约 4.3 MiB） |
| SHA-256 | `d012ed6f50eef41fdb170827303b6c5f76f6d3a85b27142aa08e867f0bcda315` |
| 包名 / 版本 | `com.premium.member.app` / versionCode 1，versionName 1.0.0 |
| 签名 | Android Debug 证书，SHA-256 `f0d1de9c6b56f1d7fdc8745625dcf82a8750014888ec340d45669a7a382fe5d8` |

APK 与 `c-app/android/` 原生工程都是生成物，不入库（见根 `.gitignore`），仓库只保留 `capacitor.config.ts` 和 `scripts/build-android.sh`。

## 2. 环境要求

| 项 | 要求 / 本机实际 |
|---|---|
| JDK | 21，`jenv exec` 取根 `.java-version`（21.0.9） |
| Android SDK | `/Users/hanson/Library/Android/sdk`，构建时 `ANDROID_HOME` 指向它 |
| Node / npm | Node 25.9.0 / npm 11.21.3 |
| Capacitor | 7.6.11（`@capacitor/core`、`cli`、`android`） |
| Gradle | 8.11.1（Capacitor 模板自带 wrapper，首次构建自动下载） |

实际安装的 SDK 组件（缺失时由 AGP 自动下载，licenses 已接受）：

| 组件 | 版本 |
|---|---|
| `platforms;android-35` | 35（Capacitor 7 模板 `compileSdk`/`targetSdk`；本次自动安装） |
| `build-tools` | 34.0.0（AGP 默认，本次自动安装）；36.0.0（本机原有，用于校验） |
| `platform-tools` | 已存在 |
| licenses | `android-sdk-license` 已接受 |

## 3. 构建

```bash
cd c-app
npm install
VITE_API_BASE_URL=http://10.0.2.2:9081 npm run build:android   # = scripts/build-android.sh
```

### 3.1 必填 VITE_API_BASE_URL

App 构建必须显式传入 API 地址，脚本未设置时直接退出；`src/api.ts` 里的 `127.0.0.1:8081` 只是 H5/本机调试的缺省值，模拟器和真机不能依赖它。

| 场景 | VITE_API_BASE_URL |
|---|---|
| Android 模拟器 | `http://10.0.2.2:9081`（c-server 监听 9081）；用默认端口时为 `http://10.0.2.2:8081` |
| 真机 + adb reverse | `adb reverse tcp:9081 tcp:9081` 后填 `http://127.0.0.1:9081` |
| 真机 + 局域网 | 电脑局域网 IP，如 `http://192.168.1.10:9081` |
| 生产 | 必须是 HTTPS 域名，不能使用 http 或明文 IP |

### 3.2 网络与明文

`build-android.sh` 每次构建重新生成 `network_security_config.xml`：`base-config` 禁止明文，仅 `10.0.2.2`、`localhost`、`127.0.0.1` 放开调试明文。生产域名不在白名单，只能走 HTTPS。

`android/` 不入库，网络配置、Manifest 中的 `networkSecurityConfig`、系统栏背景都必须在 `build-android.sh` 里生成。直接手改 `android/` 的内容会在下一次 `cap sync` 或换机器构建时丢失。

### 3.3 系统栏安全区

Android WebView 的 `env(safe-area-inset-bottom)` 实测为 0（顶部有值），只靠 CSS 会把 TabBar 压到手势条下面。`capacitor.config.ts` 设置 `adjustMarginsForEdgeToEdge: 'auto'`，让 Android 15 强制全屏时由 Capacitor 给 WebView 留出系统栏边距；脚本同时把 `android:windowBackground` 写成 `#0B0B0D`，避免边距区域露出主题白底。

页面与 TabBar 仍保留 `env(safe-area-inset-top/bottom)`，用于 H5 和 iOS；Android 由上面的原生边距兜底。

脚本依次执行：

1. `npm run build:h5`，产物在 `c-app/dist/build/h5`（`capacitor.config.ts` 的 `webDir`）。
2. `npx cap add android`（仅当 `android/` 不存在）→ `npx cap sync android`。
3. 生成 `network_security_config.xml` 并在 Manifest 中引用；给 `styles.xml` 补 `windowBackground`。
4. 把 `android/app/build.gradle` 的 `versionName` 写回 `1.0.0`（`android/` 不入库，模板默认 `1.0`）。
5. `cd android && jenv exec ./gradlew assembleDebug`。
6. 复制 APK 到 `dist/android/premium-member-1.0.0-debug.apk` 并打印 SHA-256。

首次构建会下载 Gradle 发行版和 AGP 依赖（本机约 20 分钟，之后增量构建为分钟级）。

## 4. 校验

```bash
SDK=/Users/hanson/Library/Android/sdk
$SDK/build-tools/36.0.0/apksigner verify --print-certs c-app/dist/android/premium-member-1.0.0-debug.apk
$SDK/build-tools/36.0.0/aapt dump badging c-app/dist/android/premium-member-1.0.0-debug.apk
```

关键输出：

```
package: name='com.premium.member.app' versionCode='1' versionName='1.0.0' compileSdkVersion='35'
sdkVersion:'23'
targetSdkVersion:'35'
uses-permission: name='android.permission.INTERNET'
uses-permission: name='com.premium.member.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'
application-label:'Premium Member'
```

`DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` 是 AndroidX 自动生成的签名级权限，非业务申请。`apksigner` 对 `META-INF/*.version` 的 “not protected by signature” 是 debug APK 的正常告警。

## 5. 模拟器验收

pm35（Android 15）用 `VITE_API_BASE_URL=http://10.0.2.2:9081` 构建安装，登录 `13900000001 / Test123456` 后验证：

- 首页、创作、权益、我的四个 Tab 与中间凸起按钮完整可见、可点击，手势条不再遮挡文字。
- 我的页会员卡正常，创作页图片额度剩余 20 次。
- 权益页周期、剩余名额、商品与领取历史正常加载。

截图：

```
docs/screenshots/android-home.png
docs/screenshots/android-member.png
docs/screenshots/android-benefit.png
docs/screenshots/android-ai.png
```

## 6. 安装

```bash
adb devices
adb install -r c-app/dist/android/premium-member-1.0.0-debug.apk
```

也可把 APK 拷到手机手动安装（需允许安装未知来源应用）。

## 7. 云打包（当前阻塞）

AppID `__UNI__1987B0D` 已写入 `c-app/src/manifest.json`，HBuilderX v5.34 也能识别项目（PremiumMember）并填好包名/公共测试证书，但目前仍无法出包：

- HBuilderX 未登录 DCloud，`cli pack` 直接返回“此功能需要先登录”。
- 打包被“当前操作依赖插件【App真机运行】”挡住，点击安装不生效。
- 以上两项都需要业主在 HBuilderX 内登录 DCloud 并安装插件，不能在命令行绕过。

恢复后按以下方式执行（`--project` 用项目名 `c-app`，传绝对路径会报 `Project not found`）：

```bash
cli pack --project c-app --platform android \
  --android.packagename com.premium.member.app --android.androidpacktype 1
cli packstatus
cli packartifact
```

## 8. 已知限制

- debug 签名（Android Debug 证书），未做 release 签名，不能上架应用商店。
- 已在 pm35（Android 15）模拟器验证登录与四个主页面；未在真机上验证安装与运行。
- H5 资源在构建时打进 APK，不热更新；改前端需重新构建。
- `android/` 不入库，首次在新机器构建需重新 `cap add android` 并下载 SDK/Gradle 依赖。
