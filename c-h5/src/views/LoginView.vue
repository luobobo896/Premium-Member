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
  <section class="page" aria-labelledby="login-title">
    <article class="media-card">
      <div class="cover cover--hero login-cover">
        <span class="brand-mark" aria-hidden="true">PM</span>
        <span class="cover__eyebrow">Premium Member</span>
        <h1 id="login-title" class="cover__title">登录会员账号</h1>
        <span class="cover__sub">使用手机号和密码查看会员权益</span>
      </div>

      <div class="card-body auth-form">
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
    </article>

    <p class="login-note">登录后回到刚才浏览的页面，名额与订单按账号展示。</p>
  </section>
</template>

<style scoped>
.login-cover {
  min-height: 228px;
}

.brand-mark {
  display: grid;
  width: 52px;
  height: 52px;
  margin-bottom: var(--space-2);
  place-items: center;
  border-radius: var(--radius-card);
  background: var(--accent-gradient);
  color: var(--accent-deep);
  font-size: var(--font-section);
  font-weight: 700;
}

.auth-form {
  gap: var(--space-3);
  padding-top: var(--space-3);
}

.login-note {
  margin: 0;
  padding: 0 var(--space-1);
  color: var(--text-muted);
  font-size: var(--font-sub);
  line-height: 1.6;
  text-align: center;
}
</style>
