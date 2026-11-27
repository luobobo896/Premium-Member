# Android App 构建（Capacitor）

本文记录 C 端 Android debug APK 的构建方式。方案是用 Capacitor 把 `c-app` 的 H5 构建产物打成本地壳，不写原生业务。基线 `a3af882`。

## 1. 产物

| 项 | 值 |
|---|---|
| APK | `c-app/dist/android/premium-member-1.0.0-debug.apk` |
| 大小 | 4,190,678 字节（约 4.0 MiB） |
| SHA-256 | `d563ca63b11ef29122752c09beda52ab64d0be5cdc018f5b6aefeb78c0b37bc4` |
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
npm run build:android   # = scripts/build-android.sh
```

脚本依次执行：

1. `npm run build:h5`，产物在 `c-app/dist/build/h5`（`capacitor.config.ts` 的 `webDir`）。
2. `npx cap add android`（仅当 `android/` 不存在）→ `npx cap sync android`。
3. 把 `android/app/build.gradle` 的 `versionName` 写回 `1.0.0`（`android/` 不入库，模板默认 `1.0`）。
4. `cd android && jenv exec ./gradlew assembleDebug`。
5. 复制 APK 到 `dist/android/premium-member-1.0.0-debug.apk` 并打印 SHA-256。

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

## 5. 安装

```bash
adb devices
adb install -r c-app/dist/android/premium-member-1.0.0-debug.apk
```

也可把 APK 拷到手机手动安装（需允许安装未知来源应用）。

## 6. 云打包（当前阻塞）

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

## 7. 已知限制

- debug 签名（Android Debug 证书），未做 release 签名，不能上架应用商店。
- 未在真机上验证安装与运行，仅完成构建和静态校验。
- H5 资源在构建时打进 APK，不热更新；改前端需重新构建。
- `android/` 不入库，首次在新机器构建需重新 `cap add android` 并下载 SDK/Gradle 依赖。
