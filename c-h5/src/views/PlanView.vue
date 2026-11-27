<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { Button, Checkbox } from 'vant';

import {
  ApiError,
  clearAccessToken,
  createOrder,
  getContents,
  getPlans,
  type OrderInfo,
  type PlanInfo,
} from '../api';

const emit = defineEmits<{ logout: []; ordered: [order: OrderInfo] }>();

const plans = ref<PlanInfo[]>([]);
const selectedId = ref<number | null>(null);
const agreed = ref(false);
const loading = ref(true);
const creating = ref(false);
const error = ref('');

const DEFAULT_RULES = [
  '会员周期从服务端支付成功时间起算，连续 30 天，到期不自动续费。',
  '每个有效周期含 2 个权益名额，同一 SKU 每期最多领取一次，过期不结转。',
  '已有生效周期时再次购买会生成 1 个待生效周期，紧接当前期结束后开始。',
];
const title = ref('选择购买');
const lede = ref('支付成功后立即开通，周期按服务端时间计算。');
const rules = ref(DEFAULT_RULES);

function money(cents: number): string {
  return `¥${(cents / 100).toFixed(2)}`;
}

async function load(): Promise<void> {
  loading.value = true;
  error.value = '';
  try {
    plans.value = await getPlans();
    selectedId.value = plans.value[0]?.id ?? null;
  } catch (ex) {
    if (ex instanceof ApiError && ex.status === 401) {
      clearAccessToken();
      emit('logout');
      return;
    }
    error.value = ex instanceof Error ? ex.message : '套餐加载失败';
  } finally {
    loading.value = false;
  }
}

async function loadCopy(): Promise<void> {
  try {
    const items = await getContents(['PLAN_COPY']);
    const item = items.find((row) => row.key === 'PLAN_COPY');
    if (!item) return;
    const copy = JSON.parse(item.contentJson) as {
      lede?: string;
      rules?: string[];
      title?: string;
    };
    if (copy.title) title.value = copy.title;
    if (copy.lede) lede.value = copy.lede;
    if (copy.rules?.length) rules.value = copy.rules;
  } catch {
    // 无配置或 JSON 异常时保留默认文案。
  }
}

async function submit(): Promise<void> {
  if (!selectedId.value) return;
  if (!agreed.value) {
    error.value = '请先阅读并勾选会员服务协议';
    return;
  }
  creating.value = true;
  error.value = '';
  try {
    const order = await createOrder(selectedId.value);
    emit('ordered', order);
  } catch (ex) {
    error.value = ex instanceof Error ? ex.message : '下单失败';
  } finally {
    creating.value = false;
  }
}

onMounted(() => {
  load();
  loadCopy();
});
</script>

<template>
  <section class="surface benefit-surface" aria-labelledby="plan-title">
    <header>
      <p class="eyebrow">会员套餐</p>
      <h1 id="plan-title">{{ title }}</h1>
      <p class="lede">{{ lede }}</p>
    </header>

    <p v-if="loading" class="state-text">正在加载套餐…</p>
    <p v-else-if="error" class="form-error" role="alert">{{ error }}</p>

    <div v-if="!loading" class="benefit-list">
      <button
        v-for="plan in plans"
        :key="plan.id"
        :aria-pressed="selectedId === plan.id"
        class="plan-card"
        :class="{ selected: selectedId === plan.id }"
        type="button"
        @click="selectedId = plan.id"
      >
        <span class="plan-head">
          <strong>{{ plan.name }}</strong>
          <span class="plan-price">{{ money(plan.priceCents) }}</span>
        </span>
        <span class="benefit-meta">
          <span>{{ plan.durationDays }} 天</span>
          <span>图片 {{ plan.aiImageQuota }} 次</span>
          <span>视频 {{ plan.aiVideoQuota }} 次</span>
          <span>权益 {{ plan.benefitQuota }} 项</span>
        </span>
      </button>
    </div>

    <ul class="plan-rules">
      <li v-for="rule in rules" :key="rule">{{ rule }}</li>
    </ul>

    <Checkbox v-model="agreed" class="plan-agree">
      我已阅读并同意《会员服务协议》，知悉周期起止与名额规则
    </Checkbox>

    <Button
      block
      class="submit-button"
      :disabled="!selectedId || !agreed"
      :loading="creating"
      type="primary"
      @click="submit"
    >
      提交订单
    </Button>
  </section>
</template>
