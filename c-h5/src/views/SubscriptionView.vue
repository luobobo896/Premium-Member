<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { Button } from 'vant';

import AppIcon from '../components/AppIcon.vue';
import {
  ApiError,
  clearAccessToken,
  getSubscription,
  type SubscriptionInfo,
} from '../api';

const emit = defineEmits<{
  login: [];
  logout: [];
  navigate: ['points' | 'plans'];
}>();

const subscription = ref<SubscriptionInfo | null>(null);
const loading = ref(true);
const error = ref('');

function statusClass(status: string): string {
  if (status === 'ACTIVE') return 'pill--accent';
  if (status === 'SUSPENDED' || status === 'CANCELLED') return 'pill--danger';
  return 'pill--warning';
}

function money(cents: number): string {
  return `¥${(cents / 100).toFixed(2)}`;
}

function formatDate(value: null | string): string {
  if (!value) return '-';
  return new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium' }).format(new Date(value));
}

async function load(): Promise<void> {
  loading.value = true;
  error.value = '';
  try {
    subscription.value = await getSubscription();
  } catch (ex) {
    if (ex instanceof ApiError && ex.status === 401) {
      clearAccessToken();
      emit('logout');
      return;
    }
    error.value = ex instanceof Error ? ex.message : '订阅加载失败';
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <section class="page" aria-labelledby="subscription-title">
    <header class="page-head">
      <div>
        <p class="eyebrow">我的</p>
        <h1 id="subscription-title" class="page-title">我的订阅</h1>
        <p class="lede">每月 1 号自动续费，成功后发放当月积分。</p>
      </div>
    </header>

    <p v-if="loading" class="state-text">正在加载订阅…</p>
    <p v-else-if="error" class="form-error" role="alert">{{ error }}</p>

    <template v-else-if="subscription">
      <article class="media-card">
        <div class="cover cover--member profile-cover">
          <span class="cover__eyebrow">订阅状态</span>
          <h2 class="cover__title">{{ subscription.planName }}</h2>
          <span class="pill profile-status" :class="statusClass(subscription.status)">
            {{ subscription.statusText }}
          </span>
        </div>
        <div class="card-body">
          <dl class="stat-grid">
            <div>
              <dt>当前积分</dt>
              <dd>{{ subscription.balancePoints.toLocaleString('zh-CN') }}</dd>
            </div>
            <div>
              <dt>每月积分</dt>
              <dd>{{ subscription.monthlyPoints.toLocaleString('zh-CN') }}</dd>
            </div>
            <div>
              <dt>下次扣款</dt>
              <dd>{{ formatDate(subscription.nextChargeAt) }}</dd>
            </div>
            <div>
              <dt>月费</dt>
              <dd>{{ money(subscription.priceCents) }}</dd>
            </div>
          </dl>
          <p v-if="subscription.graceUntil" class="card-sub">
            宽限期至 {{ formatDate(subscription.graceUntil) }}
          </p>
          <p v-if="subscription.failureReason" class="form-error">
            {{ subscription.failureReason }}
          </p>
          <p class="card-sub">
            每月 1 号清零上月剩余积分，积分不可提现或抵扣现金。
          </p>
        </div>
      </article>

      <div class="action-stack">
        <Button block type="primary" class="submit-button" @click="emit('navigate', 'points')">
          查看积分明细
        </Button>
        <Button plain block class="secondary-button" @click="emit('navigate', 'plans')">
          查看套餐
        </Button>
      </div>
    </template>

    <article v-else class="media-card">
      <div class="card-body">
        <p class="card-title">还没有订阅</p>
        <p class="card-sub">开通会员后每月 1 号自动续费并发放积分。</p>
        <Button block type="primary" class="submit-button" @click="emit('navigate', 'plans')">
          去开通会员
        </Button>
      </div>
    </article>

    <div class="list">
      <button class="list-item" type="button" @click="emit('navigate', 'points')">
        <span class="list-item__icon"><AppIcon name="sparkles" :size="20" /></span>
        <span class="list-item__body">
          <span class="list-item__title">积分明细</span>
          <span class="list-item__sub">发放、消耗、清零与退还</span>
        </span>
        <span class="list-item__arrow"><AppIcon name="chevron-right" :size="18" /></span>
      </button>
    </div>
  </section>
</template>
