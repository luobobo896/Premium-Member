<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { Button } from 'vant';

import { ApiError, clearAccessToken, getOrders, type OrderInfo } from '../api';

const emit = defineEmits<{ logout: [] }>();

const orders = ref<OrderInfo[]>([]);
const loading = ref(true);
const error = ref('');

const STATUS: Record<string, string> = {
  CREATED: '待支付',
  PAID: '已支付',
  CANCELLED: '已取消',
  REFUNDING: '退款中',
  REFUNDED: '已退款',
  CLOSED: '已关闭',
};
const TONES = ['cover--photo', 'cover--dress', 'cover--video'];

function tone(index: number): string {
  return TONES[index % TONES.length];
}

function money(cents: number): string {
  return `¥${(cents / 100).toFixed(2)}`;
}

function formatTime(value: null | string): string {
  if (!value) return '-';
  return new Intl.DateTimeFormat('zh-CN', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value));
}

function periodLabel(status: string): string {
  if (status === 'ACTIVE') return '生效周期';
  if (status === 'PENDING') return '待生效周期';
  if (status === 'TERMINATED') return '已终止周期';
  return '已到期周期';
}

async function load(): Promise<void> {
  loading.value = true;
  error.value = '';
  try {
    orders.value = await getOrders();
  } catch (ex) {
    if (ex instanceof ApiError && ex.status === 401) {
      clearAccessToken();
      emit('logout');
      return;
    }
    error.value = ex instanceof Error ? ex.message : '订单加载失败';
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <section class="page" aria-labelledby="order-title">
    <header class="page-head">
      <div>
        <p class="eyebrow">我的订单</p>
        <h1 id="order-title" class="page-title">购买记录</h1>
        <p class="lede">周期从支付成功时间起算，展示为北京时间。</p>
      </div>
    </header>

    <p v-if="loading" class="state-text">正在加载订单…</p>
    <p v-else-if="error" class="form-error" role="alert">{{ error }}</p>
    <p v-else-if="orders.length === 0" class="empty-state">还没有订单。</p>

    <div v-else class="card-list">
      <article v-for="(item, index) in orders" :key="item.id" class="media-card">
        <div class="cover cover--compact" :class="tone(index)">
          <span class="cover__eyebrow">订单 {{ item.orderNo }}</span>
          <span class="cover__title">{{ item.plan?.name ?? '会员套餐' }}</span>
          <span class="cover__price">{{ money(item.amountCents) }}</span>
          <span
            class="pill order-status"
            :class="item.status === 'PAID' ? 'pill--accent' : ''"
          >
            {{ STATUS[item.status] ?? item.status }}
          </span>
        </div>
        <div class="card-body">
          <h2 class="card-title">{{ item.plan?.name ?? '会员套餐' }}</h2>
          <p class="card-sub">下单 {{ formatTime(item.createdAt) }}</p>
          <p v-if="item.period" class="card-sub">
            {{ periodLabel(item.period.status) }}：{{ formatTime(item.period.startAt) }} 至
            {{ formatTime(item.period.endAt) }}
          </p>
        </div>
      </article>
    </div>

    <Button block plain class="secondary-button" @click="load">刷新订单</Button>
  </section>
</template>

<style scoped>
.order-status {
  position: absolute;
  top: var(--space-3);
  right: var(--space-3);
  z-index: 2;
}
</style>
