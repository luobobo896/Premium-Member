<script setup lang="ts">
import { ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';

import {
  ApiError,
  clearToken,
  getPointLedger,
  getToken,
  requireLogin,
  type PointLedgerItem,
} from '../../api';
import AppIcon from '../../components/AppIcon.vue';

const TYPE_LABELS: Record<string, string> = {
  ADJUST: '人工调整',
  CONSUME: 'AI 消耗',
  EXPIRE: '过期',
  GRANT: '每月发放',
  REFUND: '失败退还',
  RESET: '月初清零',
};

const date = new Date();
const month = ref(
  `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`,
);
const items = ref<PointLedgerItem[]>([]);
const loading = ref(true);
const error = ref('');

function typeLabel(type: string): string {
  return TYPE_LABELS[type] ?? type;
}

function typeClass(type: string): string {
  if (type === 'GRANT' || type === 'REFUND' || type === 'ADJUST') return 'pm-pill--accent';
  if (type === 'CONSUME' || type === 'RESET' || type === 'EXPIRE') return 'pm-pill--warning';
  return '';
}

function signed(value: number): string {
  return value > 0 ? `+${value}` : String(value);
}

function formatDate(value: string): string {
  const parsed = new Date(value);
  return `${parsed.getFullYear()}-${String(parsed.getMonth() + 1).padStart(2, '0')}-${String(
    parsed.getDate(),
  ).padStart(2, '0')} ${String(parsed.getHours()).padStart(2, '0')}:${String(
    parsed.getMinutes(),
  ).padStart(2, '0')}`;
}

function back(): void {
  uni.navigateBack();
}

async function load(): Promise<void> {
  if (!getToken()) {
    requireLogin('/pages/points/points');
    return;
  }
  loading.value = true;
  error.value = '';
  try {
    items.value = await getPointLedger(month.value || undefined);
  } catch (ex) {
    if (ex instanceof ApiError && ex.statusCode === 401) {
      clearToken();
      requireLogin('/pages/points/points');
      return;
    }
    error.value = ex instanceof Error ? ex.message : '积分明细加载失败';
  } finally {
    loading.value = false;
  }
}

function changeMonth(event: { detail: { value: string } }): void {
  month.value = event.detail.value;
  load();
}

function showAll(): void {
  month.value = '';
  load();
}

onShow(load);
</script>

<template>
  <view class="pm-page">
    <view class="nav">
      <view class="back pm-tap" aria-label="返回" @click="back">
        <AppIcon name="arrow-left" :size="22" />
      </view>
      <text class="nav-title">积分明细</text>
      <view class="nav-space"></view>
    </view>

    <view class="filter">
      <picker mode="date" fields="month" :value="month" @change="changeMonth">
        <view class="pm-tap filter-month">
          <text>{{ month || '全部月份' }}</text>
          <AppIcon name="chevron-right" :size="16" />
        </view>
      </picker>
      <text class="pm-sub filter-all pm-tap" @click="showAll">全部</text>
    </view>

    <view v-if="loading" class="pm-skeleton page-skeleton"></view>
    <text v-else-if="error" class="pm-notice">{{ error }}</text>
    <view v-else-if="items.length === 0" class="pm-empty">这个月份还没有积分记录。</view>

    <view v-else class="ledger">
      <view v-for="item in items" :key="item.id" class="pm-card row">
        <view class="row-copy">
          <text class="pm-card-title">{{ typeLabel(item.type) }}</text>
          <text class="pm-sub">{{ formatDate(item.createdAt) }}</text>
          <text v-if="item.operator" class="pm-sub">操作人 {{ item.operator }}</text>
        </view>
        <view class="row-value">
          <text class="pm-pill" :class="typeClass(item.type)">{{ signed(item.delta) }}</text>
          <text class="pm-sub">结余 {{ item.balanceAfter }}</text>
        </view>
      </view>
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

.filter {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-top: var(--space-4);
}

.filter-month {
  display: flex;
  height: 44px;
  flex: 1;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-3);
  border: 1px solid var(--border);
  border-radius: var(--radius-control);
  background: var(--surface);
  color: var(--text);
}

.filter-all {
  padding: 0 var(--space-3);
}

.ledger {
  margin-top: var(--space-4);
}

.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  margin-top: var(--space-3);
}

.row-copy {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  min-width: 0;
}

.row-value {
  display: flex;
  flex: none;
  flex-direction: column;
  align-items: flex-end;
  gap: var(--space-1);
}
</style>
