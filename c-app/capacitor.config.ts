import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.premium.member.app',
  appName: 'Premium Member',
  webDir: 'dist/build/h5',
  android: {
    allowMixedContent: true,
    // Android WebView 的 env(safe-area-inset-bottom) 为 0；Android 15 强制全屏时让系统栏以边距让开。
    adjustMarginsForEdgeToEdge: 'auto',
  },
};

export default config;
