<script setup lang="ts">
import { ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';

import {
  ApiError,
  clearToken,
  getSubscription,
  getToken,
  requireLogin,
  type SubscriptionInfo,
} from '../../api';
import AppIcon from '../../components/AppIcon.vue';

const subscription = ref<SubscriptionInfo | null>(null);
const loading = ref(true);
const error = ref('');

function statusClass(status: string): string {
  if (status === 'ACTIVE') return 'pm-pill--accent';
  if (status === 'SUSPENDED' || status === 'CANCELLED') return 'pm-pill--danger';
  return 'pm-pill--warning';
}

function money(cents: number): string {
  return `¥${(cents / 100).toFixed(2)}`;
}

function formatDate(value: null | string): string {
  if (!value) return '-';
  const date = new Date(value);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(
    date.getDate(),
  ).padStart(2, '0')}`;
}

function back(): void {
  uni.navigateBack();
}

function openPoints(): void {
  uni.navigateTo({ url: '/pages/points/points' });
}

function openPlans(): void {
  uni.reLaunch({ url: '/pages/index/index' });
}

async function load(): Promise<void> {
  if (!getToken()) {
    requireLogin('/pages/subscription/subscription');
    return;
  }
  loading.value = true;
  error.value = '';
  try {
    subscription.value = await getSubscription();
  } catch (ex) {
    if (ex instanceof ApiError && ex.statusCode === 401) {
      clearToken();
      requireLogin('/pages/subscription/subscription');
      return;
    }
    error.value = ex instanceof Error ? ex.message : '订阅加载失败';
  } finally {
    loading.value = false;
  }
}

onShow(load);
</script>

<template>
  <view class="pm-page">
    <view class="nav">
      <view class="back pm-tap" aria-label="返回" @click="back">
        <AppIcon name="arrow-left" :size="22" />
      </view>
      <text class="nav-title">我的订阅</text>
      <view class="nav-space"></view>
    </view>

    <view v-if="loading" class="pm-skeleton page-skeleton"></view>
    <text v-else-if="error" class="pm-notice">{{ error }}</text>

    <template v-else-if="subscription">
      <view class="pm-card sub-card">
        <view class="sub-head">
          <text class="pm-card-title">{{ subscription.planName }}</text>
          <text class="pm-pill" :class="statusClass(subscription.status)">
            {{ subscription.statusText }}
          </text>
        </view>
        <view class="pm-stats">
          <view class="stat">
            <text class="pm-sub">当前积分</text>
            <text class="stat-value">{{ subscription.balancePoints }}</text>
          </view>
          <view class="stat">
            <text class="pm-sub">每月积分</text>
            <text class="stat-value">{{ subscription.monthlyPoints }}</text>
          </view>
          <view class="stat">
            <text class="pm-sub">下次扣款</text>
            <text class="stat-date">{{ formatDate(subscription.nextChargeAt) }}</text>
          </view>
          <view class="stat">
            <text class="pm-sub">月费</text>
            <text class="stat-date">{{ money(subscription.priceCents) }}</text>
          </view>
        </view>
        <text v-if="subscription.graceUntil" class="pm-sub sub-note">
          宽限期至 {{ formatDate(subscription.graceUntil) }}
        </text>
        <text v-if="subscription.failureReason" class="pm-notice sub-note">
          {{ subscription.failureReason }}
        </text>
        <text class="pm-sub sub-note">
          每月 1 号清零上月剩余积分，积分不可提现或抵扣现金。
        </text>
      </view>

      <button class="pm-btn pm-btn--primary" @click="openPoints">查看积分明细</button>
      <button class="pm-btn pm-btn--secondary sub-plans" @click="openPlans">查看套餐</button>
    </template>

    <view v-else class="pm-card empty-card">
      <text class="pm-card-title">还没有订阅</text>
      <text class="pm-sub">开通会员后每月 1 号自动续费并发放积分。</text>
      <button class="pm-btn pm-btn--primary empty-action" @click="openPlans">去开通会员</button>
    </view>
  </view>
</template>

<style scoped>
.nav {
  display: flex;
  height: 48px;
  align-items: center;
  justify-content: space-between;
}

.back {
  display: flex;
  width: 44px;
  height: 44px;
  align-items: center;
  justify-content: center;
  color: var(--text);
}

.nav-title {
  color: var(--text);
  font-size: var(--font-hero);
  font-weight: 600;
}

.nav-space {
  width: 44px;
}

.page-skeleton {
  height: 320px;
  margin-top: var(--space-4);
}

.sub-card {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  margin-top: var(--space-4);
  border-color: var(--accent);
}

.sub-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
}

.pm-stats {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
}

.stat {
  display: flex;
  flex: 1 1 calc(50% - 8px);
  flex-direction: column;
  gap: var(--space-1);
}

.stat-value {
  color: var(--accent);
  font-size: var(--font-hero);
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.stat-date {
  color: var(--text);
  font-size: var(--font-body);
  font-variant-numeric: tabular-nums;
}

.sub-note {
  margin-top: var(--space-1);
}

.sub-plans {
  margin-top: var(--space-3);
}

.empty-card {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  margin-top: var(--space-4);
}

.empty-action {
  margin-top: var(--space-2);
}
</style>
