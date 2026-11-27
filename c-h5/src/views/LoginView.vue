<script setup lang="ts">
import { ref } from 'vue';
import { Button, Field } from 'vant';

import { ApiError, login } from '../api';

const emit = defineEmits<{ 'logged-in': [] }>();

const phone = ref('');
const password = ref('');
const error = ref('');
const loading = ref(false);

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
    emit('logged-in');
  } catch (ex) {
    error.value = ex instanceof ApiError ? ex.message : '登录失败，请稍后重试';
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <section class="surface auth-card" aria-labelledby="login-title">
    <header class="auth-header">
      <p class="eyebrow">Premium Member</p>
      <h1 id="login-title">登录会员账号</h1>
      <p class="lede">使用手机号和密码查看会员权益</p>
    </header>

    <div class="auth-form">
      <Field
        v-model="phone"
        name="phone"
        label="手机号"
        type="tel"
        inputmode="numeric"
        maxlength="11"
        autocomplete="tel"
        placeholder="请输入 11 位手机号"
      />
      <Field
        v-model="password"
        name="password"
        label="密码"
        type="password"
        autocomplete="current-password"
        placeholder="请输入密码"
        @keyup.enter="submit"
      />

      <p v-if="error" class="form-error" role="alert">{{ error }}</p>

      <Button
        class="submit-button"
        type="primary"
        block
        :loading="loading"
        loading-text="登录中"
        @click="submit"
      >
        登录
      </Button>
    </div>
  </section>
</template>
