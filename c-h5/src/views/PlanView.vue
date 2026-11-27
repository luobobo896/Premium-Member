<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { Button, Checkbox } from 'vant';

import AppIcon from '../components/AppIcon.vue';
import {
  ApiError,
  clearAccessToken,
  consumePendingPurchase,
  createOrder,
  getAccessToken,
  getContents,
  getPlans,
  type OrderInfo,
  type PlanInfo,
} from '../api';

const emit = defineEmits<{
  login: [action: { planId: number }];
  logout: [];
  ordered: [order: OrderInfo];
}>();

const loggedIn = Boolean(getAccessToken());
const plans = ref<PlanInfo[]>([]);
const selectedId = ref<number | null>(null);
const agreed = ref(false);
const loading = ref(true);
const creating = ref(false);
const error = ref('');

const DEFAULT_RULES = [
  '会员每月 1 号自动代扣续费，成功后发放当月 AI 积分。',
  '每月 1 号清零上月剩余积分，积分不可提现、不可抵扣现金。',
  '每个有效周期含 2 个权益名额，同一 SKU 每期最多领取一次，过期不结转。',
  '代扣失败进入 3 天宽限期，宽限内补扣成功仍发放积分，超时停用。',
];
const title = ref('选择购买');
const lede = ref('支付成功后立即开通，周期按服务端时间计算。');
const rules = ref(DEFAULT_RULES);

const PLAN_TONES = ['cover--photo', 'cover--dress', 'cover--video'];

function tone(index: number): string {
  return PLAN_TONES[index % PLAN_TONES.length];
}

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
  if (!getAccessToken()) {
    emit('login', { planId: selectedId.value });
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

onMounted(async () => {
  const pendingPlanId = consumePendingPurchase();
  await Promise.all([load(), loadCopy()]);
  if (pendingPlanId && getAccessToken()) {
    selectedId.value = plans.value.some((plan) => plan.id === pendingPlanId)
      ? pendingPlanId
      : selectedId.value;
    agreed.value = true;
    await submit();
  }
});
</script>

<template>
  <section class="page" aria-labelledby="plan-title">
    <header class="page-head">
      <div>
        <p class="eyebrow">会员套餐</p>
        <h1 id="plan-title" class="page-title">{{ title }}</h1>
        <p class="lede">{{ lede }}</p>
      </div>
    </header>

    <p v-if="loading" class="state-text">正在加载套餐…</p>
    <p v-else-if="error && !plans.length" class="form-error" role="alert">{{ error }}</p>

    <div v-if="!loading" class="card-list">
      <button
        v-for="(plan, index) in plans"
        :key="plan.id"
        :aria-pressed="selectedId === plan.id"
        class="media-card plan-card"
        :class="{ selected: selectedId === plan.id }"
        type="button"
        @click="selectedId = plan.id"
      >
        <span class="cover plan-cover" :class="tone(index)">
          <span class="cover__eyebrow">会员套餐 · {{ plan.durationDays }} 天</span>
          <span class="cover__title">{{ plan.name }}</span>
          <span class="cover__price">{{ money(plan.priceCents) }}</span>
          <span v-if="selectedId === plan.id" class="plan-check">
            <AppIcon name="check" :size="16" />
          </span>
        </span>
        <span class="card-body">
          <span class="card-title">{{ plan.durationDays }} 天会员周期</span>
          <span class="card-sub">
            每月 {{ plan.monthlyPoints }} 积分 · 权益 {{ plan.benefitQuota }} 项
          </span>
        </span>
      </button>
    </div>

    <section aria-labelledby="rules-title">
      <div class="section-head">
        <h2 id="rules-title" class="section-title">购买须知</h2>
        <p class="section-sub">按服务端周期计算</p>
      </div>
      <ul class="rule-list">
        <li v-for="rule in rules" :key="rule">{{ rule }}</li>
      </ul>
    </section>

    <p v-if="error" class="form-error" role="alert">{{ error }}</p>

    <div class="agree-row">
      <Checkbox v-model="agreed">
        我已阅读并同意《会员服务协议》，知悉周期起止与名额规则
      </Checkbox>
    </div>

    <Button
      block
      class="submit-button"
      :disabled="!selectedId || !agreed"
      :loading="creating"
      type="primary"
      @click="submit"
    >
      {{ loggedIn ? '提交订单' : '登录后购买' }}
    </Button>
  </section>
</template>

<style scoped>
.plan-card {
  position: relative;
  border-color: var(--border);
  transition: border-color 180ms var(--ease);
}

.plan-card.selected {
  border-color: var(--accent);
}

.plan-cover {
  min-height: 132px;
}

.plan-check {
  position: absolute;
  top: var(--space-3);
  right: var(--space-3);
  z-index: 2;
  display: grid;
  width: 26px;
  height: 26px;
  place-items: center;
  border-radius: var(--radius-pill);
  background: var(--accent);
  color: var(--accent-deep);
}

.rule-list {
  margin: 0;
  padding: var(--space-3) var(--space-4) var(--space-3) 28px;
  border: 1px solid var(--border);
  border-radius: var(--radius-card);
  background: var(--surface);
  color: var(--text-muted);
  font-size: var(--font-sub);
  line-height: 1.6;
}

.rule-list li + li {
  margin-top: var(--space-2);
}

.agree-row {
  padding: 0 var(--space-1);
}
</style>
