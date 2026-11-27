<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { Button } from 'vant';

import {
  ApiError,
  clearAccessToken,
  createTicket,
  getTicket,
  getTickets,
  type TicketInfo,
} from '../api';

const emit = defineEmits<{ logout: [] }>();

const CATEGORY_OPTIONS = [
  { label: '订单问题', value: 'ORDER' },
  { label: '会员问题', value: 'MEMBER' },
  { label: '权益问题', value: 'BENEFIT' },
  { label: 'AI 创作', value: 'AI' },
  { label: '其他', value: 'OTHER' },
];
const STATUS_LABELS: Record<string, string> = {
  CLOSED: '已关闭',
  OPEN: '待受理',
  PROCESSING: '处理中',
  RESOLVED: '已解决',
};
const STATUS_FLOW = [
  { label: '已提交', value: 'OPEN' },
  { label: '处理中', value: 'PROCESSING' },
  { label: '已解决', value: 'RESOLVED' },
  { label: '已关闭', value: 'CLOSED' },
];

const tickets = ref<TicketInfo[]>([]);
const selected = ref<TicketInfo | null>(null);
const loading = ref(true);
const detailLoading = ref(false);
const creating = ref(false);
const submitting = ref(false);
const error = ref('');
const formError = ref('');
const form = reactive({
  category: 'OTHER',
  content: '',
  orderNo: '',
  title: '',
});

const currentStep = computed(() => {
  const index = STATUS_FLOW.findIndex((step) => step.value === selected.value?.status);
  return index < 0 ? 0 : index;
});

function categoryLabel(value: string): string {
  return CATEGORY_OPTIONS.find((item) => item.value === value)?.label ?? value;
}

function statusPill(status: string): string {
  if (status === 'OPEN' || status === 'FAILED') return 'pill pill--danger';
  if (status === 'PROCESSING') return 'pill pill--warning';
  if (status === 'RESOLVED' || status === 'SUCCESS') return 'pill pill--accent';
  return 'pill';
}

function formatTime(value: null | string | undefined): string {
  if (!value) return '-';
  return new Intl.DateTimeFormat('zh-CN', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value));
}

function stepTime(step: string): string {
  if (!selected.value) return '';
  if (step === 'OPEN') return formatTime(selected.value.createdAt);
  if (step === 'PROCESSING') {
    return selected.value.status === 'PROCESSING'
      ? formatTime(selected.value.updatedAt)
      : '';
  }
  if (step === 'RESOLVED' || step === 'CLOSED') {
    return selected.value.resolvedAt ? formatTime(selected.value.resolvedAt) : '';
  }
  return '';
}

function handleError(ex: unknown, fallback: string) {
  if (ex instanceof ApiError && ex.status === 401) {
    clearAccessToken();
    emit('logout');
    return;
  }
  error.value = ex instanceof Error ? ex.message : fallback;
}

async function load(): Promise<void> {
  loading.value = true;
  error.value = '';
  try {
    tickets.value = await getTickets();
  } catch (ex) {
    handleError(ex, '工单加载失败');
  } finally {
    loading.value = false;
  }
}

async function openDetail(ticket: TicketInfo): Promise<void> {
  detailLoading.value = true;
  error.value = '';
  try {
    selected.value = await getTicket(ticket.ticketNo);
  } catch (ex) {
    handleError(ex, '工单详情加载失败');
  } finally {
    detailLoading.value = false;
  }
}

async function refreshDetail(): Promise<void> {
  if (!selected.value) return;
  await openDetail(selected.value);
}

function startCreate() {
  creating.value = true;
  selected.value = null;
  formError.value = '';
}

function cancelCreate() {
  creating.value = false;
  formError.value = '';
}

async function submit(): Promise<void> {
  if (!form.title.trim()) {
    formError.value = '请填写工单标题';
    return;
  }
  if (!form.content.trim()) {
    formError.value = '请描述遇到的问题';
    return;
  }
  submitting.value = true;
  formError.value = '';
  try {
    const created = await createTicket({
      category: form.category,
      content: form.content.trim(),
      orderNo: form.orderNo.trim() || undefined,
      title: form.title.trim(),
    });
    form.title = '';
    form.content = '';
    form.orderNo = '';
    creating.value = false;
    await load();
    selected.value = created;
  } catch (ex) {
    formError.value = ex instanceof Error ? ex.message : '提交失败，请稍后重试';
  } finally {
    submitting.value = false;
  }
}

onMounted(load);
</script>

<template>
  <section class="page" aria-labelledby="ticket-title">
    <header class="page-head">
      <div>
        <p class="eyebrow">客服支持</p>
        <h1 id="ticket-title" class="page-title">我的工单</h1>
        <p class="lede">提交问题后由客服受理，处理进度与回复会显示在这里。</p>
      </div>
      <button
        v-if="!creating"
        class="btn btn--secondary btn--small ticket-new"
        type="button"
        @click="startCreate"
      >
        新建工单
      </button>
    </header>

    <p v-if="loading" class="state-text">正在加载工单…</p>
    <p v-else-if="error" class="form-error" role="alert">{{ error }}</p>

    <form v-if="creating" class="form-panel" @submit.prevent="submit">
      <div class="form-panel__head">
        <div>
          <p class="eyebrow">新建工单</p>
          <h2 class="section-title">描述遇到的问题</h2>
          <p class="card-sub">补充发生时间、操作步骤和期望结果，客服会按序处理。</p>
        </div>
      </div>
      <label class="field">
        <span class="field__label">分类</span>
        <select v-model="form.category" class="field__select">
          <option v-for="item in CATEGORY_OPTIONS" :key="item.value" :value="item.value">
            {{ item.label }}
          </option>
        </select>
      </label>
      <label class="field">
        <span class="field__label">标题</span>
        <input
          v-model="form.title"
          class="field__input"
          maxlength="128"
          placeholder="一句话说明问题"
        />
      </label>
      <label class="field">
        <span class="field__label">订单号（可选）</span>
        <input
          v-model="form.orderNo"
          class="field__input"
          maxlength="40"
          placeholder="如 PM202611270001"
        />
      </label>
      <label class="field">
        <span class="field__label">问题描述</span>
        <textarea
          v-model="form.content"
          class="field__textarea"
          maxlength="5000"
          placeholder="请补充发生时间、操作步骤和期望结果"
          rows="5"
        ></textarea>
      </label>
      <p v-if="formError" class="form-error" role="alert">{{ formError }}</p>
      <div class="inline-actions">
        <Button plain class="secondary-button" @click="cancelCreate">取消</Button>
        <Button
          block
          class="submit-button"
          :loading="submitting"
          type="primary"
          native-type="submit"
        >
          提交工单
        </Button>
      </div>
    </form>

    <article v-if="selected" class="media-card" aria-live="polite">
      <div class="cover cover--benefit ticket-detail-cover">
        <span class="cover__eyebrow">工单详情 · {{ categoryLabel(selected.category) }}</span>
        <span class="cover__title">{{ selected.title }}</span>
        <span class="cover__sub">{{ selected.ticketNo }}</span>
        <span class="ticket-detail-status" :class="statusPill(selected.status)">
          {{ STATUS_LABELS[selected.status] ?? selected.status }}
        </span>
      </div>
      <div class="card-body">
        <button class="btn btn--ghost ticket-back" type="button" @click="selected = null">
          ← 返回列表
        </button>
        <p class="meta-row">
          <span>{{ categoryLabel(selected.category) }}</span>
          <span v-if="selected.orderNo">订单 {{ selected.orderNo }}</span>
          <span>{{ formatTime(selected.createdAt) }}</span>
        </p>
        <p class="ticket-content">{{ selected.content }}</p>

        <h3 class="ticket-subtitle">处理进度</h3>
        <ol class="timeline">
          <li
            v-for="(step, index) in STATUS_FLOW"
            :key="step.value"
            :class="{ current: index === currentStep, done: index <= currentStep }"
          >
            <span class="timeline__dot" aria-hidden="true"></span>
            <div>
              <strong>{{ step.label }}</strong>
              <small v-if="stepTime(step.value)">{{ stepTime(step.value) }}</small>
            </div>
          </li>
        </ol>

        <h3 class="ticket-subtitle">客服回复</h3>
        <p v-if="!selected.replies?.length" class="state-text">
          暂无回复，客服受理后会在这里展示。
        </p>
        <div v-else class="ticket-replies">
          <div
            v-for="reply in selected.replies"
            :key="reply.createdAt + reply.content"
            class="ticket-reply"
          >
            <p class="ticket-reply-head">
              {{ reply.senderName || '客服' }} · {{ formatTime(reply.createdAt) }}
            </p>
            <p>{{ reply.content }}</p>
          </div>
        </div>
        <Button plain block class="secondary-button" @click="refreshDetail">刷新进度</Button>
      </div>
    </article>

    <template v-if="!creating && !selected">
      <p v-if="!loading && !error && tickets.length === 0" class="empty-state">
        还没有工单，遇到问题可以新建一条。
      </p>
      <div v-else class="card-list">
        <button
          v-for="ticket in tickets"
          :key="ticket.id"
          class="media-card ticket-item"
          type="button"
          @click="openDetail(ticket)"
        >
          <span class="cover cover--benefit2 cover--compact">
            <span class="cover__eyebrow">{{ categoryLabel(ticket.category) }}</span>
            <span class="cover__title">{{ ticket.title }}</span>
            <span class="cover__sub">
              {{ ticket.ticketNo }} · {{ formatTime(ticket.createdAt) }}
            </span>
            <span class="ticket-item-status" :class="statusPill(ticket.status)">
              {{ STATUS_LABELS[ticket.status] ?? ticket.status }}
            </span>
          </span>
          <span class="card-body">
            <span class="card-sub">
              {{ ticket.replyCount ? `${ticket.replyCount} 条回复` : '暂无回复' }}
            </span>
          </span>
        </button>
      </div>
    </template>
    <p v-if="detailLoading" class="state-text">正在加载详情…</p>
  </section>
</template>

<style scoped>
.ticket-new {
  flex: none;
}

.ticket-detail-cover {
  min-height: 150px;
}

.ticket-detail-status,
.ticket-item-status {
  position: absolute;
  top: var(--space-3);
  right: var(--space-3);
  z-index: 2;
}

.ticket-back {
  width: auto;
  height: 32px;
  align-self: flex-start;
  padding: 0;
}

.ticket-content {
  margin: 0;
  padding: var(--space-3);
  border-radius: var(--radius-control);
  background: var(--overlay);
  color: var(--text);
  font-size: var(--font-body);
  white-space: pre-wrap;
}

.ticket-subtitle {
  margin: var(--space-2) 0 0;
  color: var(--text);
  font-size: var(--font-body);
  font-weight: 600;
}

.timeline {
  margin: 0;
  padding: 0;
  list-style: none;
}

.timeline li {
  position: relative;
  display: flex;
  gap: var(--space-2);
  padding-bottom: var(--space-4);
  color: var(--text-disabled);
}

.timeline li:last-child {
  padding-bottom: 0;
}

.timeline li::before {
  position: absolute;
  top: 12px;
  bottom: 0;
  left: 4px;
  width: 1px;
  background: var(--line);
  content: '';
}

.timeline li:last-child::before {
  display: none;
}

.timeline__dot {
  width: 9px;
  height: 9px;
  flex: none;
  margin-top: 5px;
  border: 2px solid var(--line);
  border-radius: 50%;
  background: var(--surface);
}

.timeline li.done {
  color: var(--text);
}

.timeline li.done .timeline__dot {
  border-color: var(--accent);
  background: var(--accent);
}

.timeline li.current strong {
  color: var(--accent);
}

.timeline small {
  display: block;
  color: var(--text-muted);
  font-size: 11px;
}

.ticket-replies {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.ticket-reply {
  padding: var(--space-3);
  border: 1px solid var(--border);
  border-radius: var(--radius-control);
}

.ticket-reply p {
  margin: 0;
  font-size: var(--font-body);
}

.ticket-reply-head {
  margin-bottom: var(--space-1) !important;
  color: var(--text-muted);
  font-size: var(--font-sub) !important;
}

.ticket-item {
  text-align: left;
}
</style>
