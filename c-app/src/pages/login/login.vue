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
  <view class="screen">
    <view class="card">
      <text class="eyebrow">Premium Member</text>
      <text class="title">登录会员账号</text>
      <text class="lede">使用手机号和密码查看会员权益</text>

      <view class="form">
        <text class="label">手机号</text>
        <input
          v-model="phone"
          class="field"
          type="number"
          maxlength="11"
          placeholder="请输入 11 位手机号"
        />
        <text class="label">密码</text>
        <input
          v-model="password"
          class="field"
          password
          placeholder="请输入密码"
          @confirm="submit"
        />

        <text v-if="error" class="error">{{ error }}</text>

        <button class="primary-button" :loading="loading" @click="submit">
          {{ loading ? '登录中' : '登录' }}
        </button>
      </view>
    </view>
  </view>
</template>

<style scoped>
.screen {
  min-height: 100vh;
  padding: 48rpx 32rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f4f2ed;
}

.card {
  width: 100%;
  max-width: 680rpx;
  padding: 56rpx 40rpx 40rpx;
  border: 1rpx solid #ddd9d1;
  border-radius: 32rpx;
  background: #fffdf9;
}

.eyebrow {
  display: block;
  color: #2f6b5f;
  font-size: 24rpx;
  font-weight: 600;
}

.title {
  display: block;
  margin-top: 16rpx;
  color: #1f2421;
  font-size: 56rpx;
  font-weight: 600;
  line-height: 1.25;
}

.lede {
  display: block;
  margin-top: 12rpx;
  color: #5c645f;
  font-size: 28rpx;
}

.form {
  margin-top: 48rpx;
}

.label {
  display: block;
  margin-top: 24rpx;
  color: #1f2421;
  font-size: 30rpx;
  font-weight: 500;
}

.field {
  height: 88rpx;
  margin-top: 12rpx;
  padding: 0 24rpx;
  border: 1rpx solid #ddd9d1;
  border-radius: 16rpx;
  color: #1f2421;
  font-size: 32rpx;
}

.error {
  display: block;
  margin-top: 24rpx;
  color: #a83b32;
  font-size: 28rpx;
}

.primary-button {
  height: 88rpx;
  margin-top: 40rpx;
  border-radius: 16rpx;
  background: #2f6b5f;
  color: #ffffff;
  font-size: 32rpx;
  line-height: 88rpx;
}
</style>
