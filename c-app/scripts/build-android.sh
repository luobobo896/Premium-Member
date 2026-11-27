#!/usr/bin/env bash
# 输出: c-app/dist/android/premium-member-1.0.0-debug.apk
set -euo pipefail

cd "$(dirname "$0")/.."
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"

if [ -z "${VITE_API_BASE_URL:-}" ]; then
  echo "VITE_API_BASE_URL is required for app builds (e.g. http://10.0.2.2:9081 for the emulator)" >&2
  exit 1
fi

npm run build:h5
[ -d android ] || npx cap add android
npx cap sync android

# android/ 未入库，每次构建补上调试用明文白名单（仅模拟器/本机地址）
mkdir -p android/app/src/main/res/xml
cat > android/app/src/main/res/xml/network_security_config.xml <<'XML'
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="false" />
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">10.0.2.2</domain>
        <domain includeSubdomains="true">localhost</domain>
        <domain includeSubdomains="true">127.0.0.1</domain>
    </domain-config>
</network-security-config>
XML

if ! grep -q 'android:networkSecurityConfig' android/app/src/main/AndroidManifest.xml; then
  sed -i '' 's|<application|<application android:networkSecurityConfig="@xml/network_security_config"|' \
    android/app/src/main/AndroidManifest.xml
fi

# Android 15 强制全屏时 adjustMarginsForEdgeToEdge 会给 WebView 留出系统栏边距，
# 边距区域取窗口背景色；不写死会露出 Theme 默认白底。
if ! grep -q 'android:windowBackground' android/app/src/main/res/values/styles.xml; then
  perl -0pi -e 's|(<style name="AppTheme\.NoActionBar"[^>]*>)|$1\n        <item name="android:windowBackground">#0B0B0D</item>|' \
    android/app/src/main/res/values/styles.xml
fi

# android/ 未入库，模板默认 versionName "1.0"，每次构建写回
sed -i '' 's/versionName ".*"/versionName "1.0.0"/' android/app/build.gradle

(cd android && jenv exec ./gradlew assembleDebug)

mkdir -p dist/android
cp android/app/build/outputs/apk/debug/app-debug.apk dist/android/premium-member-1.0.0-debug.apk
shasum -a 256 dist/android/premium-member-1.0.0-debug.apk
