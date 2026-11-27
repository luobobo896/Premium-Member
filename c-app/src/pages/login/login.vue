<script setup lang="ts">
import { ref } from 'vue';
import { onLoad } from '@dcloudio/uni-app';

import { ApiError, getToken, login } from '../../api';

const phone = ref('');
const password = ref('');
const error = ref('');
const loading = ref(false);

onLoad(() => {
  if (getToken()) {
    uni.reLaunch({ url: '/pages/member/member' });
  }
});

async function submit(): Promise<void> {
  error.value = '';
  if (!/^1[3-9]\d{9}$/.test(phone.value.trim())) {
    error.value = '请输入正确的 11 位手机号';
    return;
  }
  if (!password.value) {
    error.value = '请输入密码';
    return;
  }

  loading.value = true;
  try {
    await login(phone.value.trim(), password.value);
    uni.reLaunch({ url: '/pages/member/member' });
  } catch (ex) {
    error.value = ex instanceof ApiError ? ex.message : '登录失败，请稍后重试';
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <view class="pm-page pm-page--plain login">
    <view class="glow"></view>

    <text class="brand">Premium Member</text>
    <text class="pm-title title">登录会员账号</text>
    <text class="lede">使用手机号和密码查看会员权益</text>

    <view class="form">
      <text class="label">手机号</text>
      <input
        v-model="phone"
        class="pm-input phone"
        type="number"
        maxlength="11"
        placeholder="请输入 11 位手机号"
        placeholder-class="pm-placeholder"
      />

      <text class="label label-gap">密码</text>
      <input
        v-model="password"
        class="pm-input password"
        password
        placeholder="请输入密码"
        placeholder-class="pm-placeholder"
        @confirm="submit"
      />

      <text v-if="error" class="pm-notice error">{{ error }}</text>

      <button class="pm-btn pm-btn--primary submit" :loading="loading" @click="submit">
        {{ loading ? '登录中' : '登录' }}
      </button>
    </view>
  </view>
</template>

<style scoped>
.login {
  position: relative;
  display: flex;
  min-height: 100vh;
  flex-direction: column;
  overflow: hidden;
  padding-top: calc(env(safe-area-inset-top) + 72px);
}

.glow {
  position: absolute;
  top: -150px;
  right: -110px;
  width: 340px;
  height: 340px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(61, 220, 151, 0.22), transparent 66%);
  pointer-events: none;
}

.brand {
  position: relative;
  color: var(--text-muted);
  font-size: var(--font-sub);
  letter-spacing: 0.6px;
}

.title {
  position: relative;
  margin-top: var(--space-3);
}

.lede {
  position: relative;
  display: block;
  margin-top: var(--space-2);
  color: var(--text-muted);
  font-size: var(--font-body);
  line-height: 1.6;
}

.form {
  position: relative;
  display: flex;
  flex-direction: column;
  margin-top: var(--space-8);
}

.label {
  color: var(--text-muted);
  font-size: var(--font-sub);
}

.label-gap {
  margin-top: var(--space-4);
}

.phone,
.password {
  margin-top: var(--space-2);
}

.error {
  margin-top: var(--space-4);
}

.submit {
  margin-top: var(--space-6);
}
</style>
