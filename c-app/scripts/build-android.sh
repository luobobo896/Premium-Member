#!/usr/bin/env bash
# 输出: c-app/dist/android/premium-member-1.0.0-debug.apk
set -euo pipefail

cd "$(dirname "$0")/.."
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"

npm run build:h5
[ -d android ] || npx cap add android
npx cap sync android

# android/ 未入库，模板默认 versionName "1.0"，每次构建写回
sed -i '' 's/versionName ".*"/versionName "1.0.0"/' android/app/build.gradle

(cd android && jenv exec ./gradlew assembleDebug)

mkdir -p dist/android
cp android/app/build/outputs/apk/debug/app-debug.apk dist/android/premium-member-1.0.0-debug.apk
shasum -a 256 dist/android/premium-member-1.0.0-debug.apk
