import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.premium.member.app',
  appName: 'Premium Member',
  webDir: 'dist/build/h5',
  android: {
    allowMixedContent: true,
  },
};

export default config;
