<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { Button } from 'vant';

import AppIcon from '../components/AppIcon.vue';
import { ApiError, clearAccessToken, mockPay, type OrderInfo } from '../api';

const props = defineProps<{ order: OrderInfo }>();
const emit = defineEmits<{ logout: []; orders: []; plans: [] }>();

const order = ref<OrderInfo>(props.order);
const paying = ref(true);
const error = ref('');

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

async function pay(): Promise<void> {
  paying.value = true;
  error.value = '';
  try {
    order.value = await mockPay(order.value.orderNo);
  } catch (ex) {
    if (ex instanceof ApiError && ex.status === 401) {
      clearAccessToken();
      emit('logout');
      return;
    }
    error.value = ex instanceof Error ? ex.message : '支付结果确认失败';
  } finally {
    paying.value = false;
  }
}

onMounted(pay);
</script>

<template>
  <section class="page" aria-labelledby="pay-title">
    <div class="cover cover--member result-hero">
      <span class="result-icon">
        <AppIcon name="check" :size="28" />
      </span>
      <span class="cover__eyebrow">模拟支付</span>
      <h1 id="pay-title" class="cover__title result-title">
        {{ order.status === 'PAID' ? '支付成功' : '确认支付中' }}
      </h1>
      <span class="cover__sub">订单号 {{ order.orderNo }}</span>
    </div>

    <p v-if="paying" class="state-text">正在向服务端确认支付结果…</p>
    <p v-else-if="error" class="form-error" role="alert">{{ error }}</p>

    <article class="media-card">
      <div class="cover cover--photo cover--compact">
        <span class="cover__eyebrow">订单摘要</span>
        <span class="cover__title">{{ order.plan?.name ?? '会员套餐' }}</span>
        <span class="cover__sub">实付 {{ money(order.amountCents) }}</span>
      </div>
      <dl class="info-list">
        <div>
          <dt>套餐</dt>
          <dd>{{ order.plan?.name ?? '-' }}</dd>
        </div>
        <div>
          <dt>实付</dt>
          <dd>{{ money(order.amountCents) }}</dd>
        </div>
        <div>
          <dt>支付时间</dt>
          <dd>{{ formatTime(order.paidAt) }}</dd>
        </div>
        <div>
          <dt>状态</dt>
          <dd>{{ order.status === 'PAID' ? '已开通' : order.status }}</dd>
        </div>
      </dl>
    </article>

    <article v-if="order.period" class="media-card">
      <div class="cover cover--benefit cover--compact">
        <span class="cover__eyebrow">
          {{ order.period.status === 'ACTIVE' ? '本周期' : '待生效周期' }}
        </span>
        <span class="cover__title">{{ order.period.periodNo }}</span>
        <span class="cover__sub">
          {{ formatTime(order.period.startAt) }} 至 {{ formatTime(order.period.endAt) }}
        </span>
      </div>
    </article>

    <div class="action-stack">
      <Button block class="submit-button" type="primary" @click="emit('orders')">
        查看我的订单
      </Button>
      <Button block plain class="secondary-button" @click="emit('plans')">返回套餐</Button>
    </div>
  </section>
</template>

<style scoped>
.result-hero {
  min-height: 236px;
  justify-content: flex-end;
}

.result-icon {
  position: absolute;
  top: var(--space-6);
  right: var(--space-6);
  z-index: 2;
  display: grid;
  width: 56px;
  height: 56px;
  place-items: center;
  border-radius: var(--radius-pill);
  background: var(--accent);
  color: var(--accent-deep);
  box-shadow: var(--shadow-accent);
}

.result-title {
  font-size: var(--font-page);
}

.info-list {
  margin: 0;
  padding: var(--space-2) var(--space-4);
}

.info-list > div {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--space-3);
  padding: var(--space-3) 0;
}

.info-list > div + div {
  border-top: 1px solid var(--line);
}

.info-list dt {
  flex: none;
  color: var(--text-muted);
  font-size: var(--font-sub);
}

.info-list dd {
  margin: 0;
  overflow: hidden;
  color: var(--text);
  font-size: var(--font-body);
  font-variant-numeric: tabular-nums;
  font-weight: 600;
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
