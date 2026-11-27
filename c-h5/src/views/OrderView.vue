<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { Button, Tag } from 'vant';

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
  <section class="surface benefit-surface" aria-labelledby="order-title">
    <header>
      <p class="eyebrow">我的订单</p>
      <h1 id="order-title">购买记录</h1>
      <p class="lede">周期从支付成功时间起算，展示为北京时间。</p>
    </header>

    <p v-if="loading" class="state-text">正在加载订单…</p>
    <p v-else-if="error" class="form-error" role="alert">{{ error }}</p>
    <p v-else-if="orders.length === 0" class="state-text">还没有订单。</p>

    <div v-else class="benefit-list">
      <article v-for="item in orders" :key="item.id" class="benefit-card">
        <div class="benefit-card-head">
          <div>
            <h2>{{ item.plan?.name ?? '会员套餐' }}</h2>
            <p class="benefit-desc">{{ item.orderNo }}</p>
          </div>
          <Tag :type="item.status === 'PAID' ? 'success' : 'default'">
            {{ STATUS[item.status] ?? item.status }}
          </Tag>
        </div>
        <p class="benefit-meta">
          <span>{{ money(item.amountCents) }}</span>
          <span>下单 {{ formatTime(item.createdAt) }}</span>
        </p>
        <p v-if="item.period" class="benefit-rules">
          {{ periodLabel(item.period.status) }}：
          {{ formatTime(item.period.startAt) }} 至 {{ formatTime(item.period.endAt) }}
        </p>
      </article>
    </div>

    <Button block plain class="secondary-button logout-button" @click="load">
      刷新订单
    </Button>
  </section>
</template>
