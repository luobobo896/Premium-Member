<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';

import {
  ApiError,
  clearAccessToken,
  getPointLedger,
  type PointLedgerItem,
} from '../api';

const emit = defineEmits<{ logout: [] }>();

const TYPE_LABELS: Record<string, string> = {
  ADJUST: '人工调整',
  CONSUME: 'AI 消耗',
  EXPIRE: '过期',
  GRANT: '每月发放',
  REFUND: '失败退还',
  RESET: '月初清零',
};

const now = new Date();
const month = ref(
  `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`,
);
const items = ref<PointLedgerItem[]>([]);
const loading = ref(true);
const error = ref('');

const total = computed(() => items.value.reduce((sum, item) => sum + item.delta, 0));

function typeLabel(type: string): string {
  return TYPE_LABELS[type] ?? type;
}

function typeClass(type: string): string {
  if (type === 'GRANT' || type === 'REFUND' || type === 'ADJUST') return 'pill--accent';
  if (type === 'CONSUME' || type === 'RESET' || type === 'EXPIRE') return 'pill--warning';
  return '';
}

function signed(value: number): string {
  return value > 0 ? `+${value}` : String(value);
}

function formatDate(value: string): string {
  return new Intl.DateTimeFormat('zh-CN', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(new Date(value));
}

async function load(): Promise<void> {
  loading.value = true;
  error.value = '';
  try {
    items.value = await getPointLedger(month.value || undefined);
  } catch (ex) {
    if (ex instanceof ApiError && ex.status === 401) {
      clearAccessToken();
      emit('logout');
      return;
    }
    error.value = ex instanceof Error ? ex.message : '积分明细加载失败';
  } finally {
    loading.value = false;
  }
}

function clearMonth(): void {
  month.value = '';
  load();
}

onMounted(load);
</script>

<template>
  <section class="page" aria-labelledby="points-title">
    <header class="page-head">
      <div>
        <p class="eyebrow">我的</p>
        <h1 id="points-title" class="page-title">积分明细</h1>
        <p class="lede">发放、消耗、清零与退还可按月份筛选。</p>
      </div>
    </header>

    <div class="filter-row">
      <input v-model="month" class="month-input" type="month" @change="load" />
      <button class="filter-clear" type="button" @click="clearMonth">全部</button>
    </div>

    <p v-if="loading" class="state-text">正在加载积分明细…</p>
    <p v-else-if="error" class="form-error" role="alert">{{ error }}</p>
    <p v-else-if="items.length === 0" class="empty-state">这个月份还没有积分记录。</p>

    <template v-else>
      <p class="card-sub">当月变动合计 {{ signed(total) }} 积分</p>
      <div class="list">
        <article v-for="item in items" :key="item.id" class="media-card ledger-item">
          <div class="card-body ledger-body">
            <div class="ledger-copy">
              <p class="card-title">{{ typeLabel(item.type) }}</p>
              <p class="card-sub">{{ formatDate(item.createdAt) }}</p>
              <p v-if="item.operator" class="card-sub">操作人 {{ item.operator }}</p>
            </div>
            <div class="ledger-value">
              <span class="pill" :class="typeClass(item.type)">{{ signed(item.delta) }}</span>
              <span class="card-sub">结余 {{ item.balanceAfter }}</span>
            </div>
          </div>
        </article>
      </div>
    </template>
  </section>
</template>

<style scoped>
.filter-row {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.month-input {
  flex: 1;
  height: 44px;
  padding: 0 var(--space-3);
  border: 1px solid var(--border);
  border-radius: var(--radius-control);
  background: var(--surface);
  color: var(--text);
  font-size: var(--font-body);
}

.filter-clear {
  height: 44px;
  padding: 0 var(--space-4);
  border: 1px solid var(--border);
  border-radius: var(--radius-control);
  background: var(--surface);
  color: var(--text-muted);
}

.ledger-item {
  margin-top: 0;
}

.ledger-body {
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
}

.ledger-copy {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.ledger-value {
  display: flex;
  flex: none;
  flex-direction: column;
  align-items: flex-end;
  gap: var(--space-1);
}
</style>
