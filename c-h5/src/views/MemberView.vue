<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { Button } from 'vant';

import {
  ApiError,
  clearAccessToken,
  getInfo,
  getMember,
  logout,
  type CUserInfo,
  type MemberInfo,
} from '../api';

const emit = defineEmits<{ logout: [] }>();

const LEVELS: Record<string, string> = {
  BASIC: '基础会员',
  SILVER: '白银会员',
  GOLD: '黄金会员',
  PLATINUM: '铂金会员',
};

const user = ref<CUserInfo | null>(null);
const member = ref<MemberInfo | null>(null);
const loading = ref(true);
const error = ref('');
const loggingOut = ref(false);

function levelName(level: string): string {
  return LEVELS[level] ?? level;
}

function formatDate(value: string): string {
  return new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium' }).format(new Date(value));
}

function formatBalance(value: number): string {
  return `¥${value.toFixed(2)}`;
}

async function load(): Promise<void> {
  loading.value = true;
  error.value = '';
  try {
    const [info, memberInfo] = await Promise.all([getInfo(), getMember()]);
    user.value = info;
    member.value = memberInfo;
  } catch (ex) {
    if (ex instanceof ApiError && ex.status === 401) {
      clearAccessToken();
      emit('logout');
      return;
    }
    if (ex instanceof ApiError && ex.bizCode === 'MEMBER_NOT_FOUND') {
      user.value = await getInfo();
      member.value = null;
      return;
    }
    error.value = ex instanceof Error ? ex.message : '加载失败，请稍后重试';
  } finally {
    loading.value = false;
  }
}

async function signOut(): Promise<void> {
  loggingOut.value = true;
  try {
    await logout();
  } finally {
    emit('logout');
    loggingOut.value = false;
  }
}

onMounted(load);
</script>

<template>
  <section class="surface member-card" aria-labelledby="member-title">
    <header class="member-header">
      <div>
        <p class="eyebrow">会员中心</p>
        <h1 id="member-title">{{ user?.nickname || '会员账号' }}</h1>
        <p class="lede">{{ user?.phone }}</p>
      </div>
      <span v-if="member" class="status-pill" :class="{ inactive: member.status !== 1 }">
        {{ member.status === 1 ? '正常' : '停用' }}
      </span>
    </header>

    <p v-if="loading" class="state-text">正在加载会员信息…</p>

    <div v-else-if="error" class="state-block">
      <p class="form-error" role="alert">{{ error }}</p>
      <Button plain block class="secondary-button" @click="load">重新加载</Button>
    </div>

    <div v-else-if="!member" class="state-block">
      <p class="state-text">该手机号还没有会员档案，请联系运营人员开通。</p>
    </div>

    <template v-else>
      <p class="member-no">{{ member.memberNo }}</p>

      <dl class="member-grid">
        <div>
          <dt>等级</dt>
          <dd>{{ levelName(member.level) }}</dd>
        </div>
        <div>
          <dt>积分</dt>
          <dd>{{ member.points.toLocaleString('zh-CN') }}</dd>
        </div>
        <div>
          <dt>余额</dt>
          <dd>{{ formatBalance(member.balance) }}</dd>
        </div>
        <div>
          <dt>入会时间</dt>
          <dd>{{ formatDate(member.joinedAt) }}</dd>
        </div>
      </dl>
    </template>

    <Button
      plain
      block
      class="secondary-button logout-button"
      :loading="loggingOut"
      @click="signOut"
    >
      退出登录
    </Button>
  </section>
</template>
