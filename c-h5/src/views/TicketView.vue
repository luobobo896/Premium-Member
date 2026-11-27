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

function statusClass(status: string): string {
  return `ticket-status--${status.toLowerCase()}`;
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
  <section class="surface ticket-surface" aria-labelledby="ticket-title">
    <header class="ticket-header">
      <div>
        <p class="eyebrow">客服支持</p>
        <h1 id="ticket-title">我的工单</h1>
        <p class="lede">提交问题后由客服受理，处理进度与回复会显示在这里。</p>
      </div>
      <Button v-if="!creating" plain class="ticket-new" @click="startCreate">
        新建工单
      </Button>
    </header>

    <p v-if="loading" class="ticket-state">正在加载工单…</p>
    <p v-else-if="error" class="form-error" role="alert">{{ error }}</p>

    <form v-if="creating" class="ticket-form" @submit.prevent="submit">
      <h2>新建工单</h2>
      <label class="ticket-field">
        <span>分类</span>
        <select v-model="form.category">
          <option v-for="item in CATEGORY_OPTIONS" :key="item.value" :value="item.value">
            {{ item.label }}
          </option>
        </select>
      </label>
      <label class="ticket-field">
        <span>标题</span>
        <input v-model="form.title" maxlength="128" placeholder="一句话说明问题" />
      </label>
      <label class="ticket-field">
        <span>订单号（可选）</span>
        <input v-model="form.orderNo" maxlength="40" placeholder="如 PM202611270001" />
      </label>
      <label class="ticket-field">
        <span>问题描述</span>
        <textarea
          v-model="form.content"
          maxlength="5000"
          placeholder="请补充发生时间、操作步骤和期望结果"
          rows="5"
        ></textarea>
      </label>
      <p v-if="formError" class="form-error" role="alert">{{ formError }}</p>
      <div class="ticket-form-actions">
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

    <article v-if="selected" class="ticket-detail" aria-live="polite">
      <div class="ticket-detail-head">
        <button class="ticket-back" type="button" @click="selected = null">← 返回列表</button>
        <span class="ticket-status" :class="statusClass(selected.status)">
          {{ STATUS_LABELS[selected.status] ?? selected.status }}
        </span>
      </div>
      <p class="ticket-no">{{ selected.ticketNo }}</p>
      <h2>{{ selected.title }}</h2>
      <p class="ticket-meta">
        {{ categoryLabel(selected.category) }}
        <template v-if="selected.orderNo"> · 订单 {{ selected.orderNo }}</template>
      </p>
      <p class="ticket-content">{{ selected.content }}</p>

      <h3>处理进度</h3>
      <ol class="ticket-timeline">
        <li
          v-for="(step, index) in STATUS_FLOW"
          :key="step.value"
          :class="{ current: index === currentStep, done: index <= currentStep }"
        >
          <span class="ticket-dot" aria-hidden="true"></span>
          <div>
            <strong>{{ step.label }}</strong>
            <small v-if="stepTime(step.value)">{{ stepTime(step.value) }}</small>
          </div>
        </li>
      </ol>

      <h3>客服回复</h3>
      <p v-if="!selected.replies?.length" class="ticket-state">
        暂无回复，客服受理后会在这里展示。
      </p>
      <div v-else class="ticket-replies">
        <div v-for="reply in selected.replies" :key="reply.createdAt + reply.content" class="ticket-reply">
          <p class="ticket-reply-head">
            {{ reply.senderName || '客服' }} · {{ formatTime(reply.createdAt) }}
          </p>
          <p>{{ reply.content }}</p>
        </div>
      </div>
      <Button plain block class="secondary-button ticket-refresh" @click="refreshDetail">
        刷新进度
      </Button>
    </article>

    <template v-if="!creating && !selected">
      <p v-if="!loading && !error && tickets.length === 0" class="ticket-state">
        还没有工单，遇到问题可以新建一条。
      </p>
      <div v-else class="ticket-list">
        <button
          v-for="ticket in tickets"
          :key="ticket.id"
          class="ticket-item"
          type="button"
          @click="openDetail(ticket)"
        >
          <span class="ticket-item-head">
            <strong>{{ ticket.title }}</strong>
            <span class="ticket-status" :class="statusClass(ticket.status)">
              {{ STATUS_LABELS[ticket.status] ?? ticket.status }}
            </span>
          </span>
          <span class="ticket-meta">
            {{ categoryLabel(ticket.category) }} · {{ formatTime(ticket.createdAt) }}
          </span>
          <span class="ticket-item-foot">
            {{ ticket.ticketNo }}
            <template v-if="ticket.replyCount"> · {{ ticket.replyCount }} 条回复</template>
          </span>
        </button>
      </div>
    </template>
    <p v-if="detailLoading" class="ticket-state">正在加载详情…</p>
  </section>
</template>

<style scoped>
.ticket-surface {
  max-width: 460px;
}

.ticket-header,
.ticket-detail-head,
.ticket-item-head,
.ticket-form-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.ticket-new {
  flex: none;
  padding-inline: 14px;
  border-color: var(--c-border);
  color: var(--c-accent);
  font-size: 13px;
}

.ticket-form,
.ticket-detail {
  margin-top: 22px;
  padding: 18px;
  border: 1px solid var(--c-border);
  border-radius: 12px;
}

.ticket-form h2,
.ticket-detail h2 {
  margin: 0 0 14px;
  font-size: 20px;
}

.ticket-field {
  display: block;
  margin-top: 14px;
}

.ticket-field:first-of-type {
  margin-top: 0;
}

.ticket-field > span {
  display: block;
  margin-bottom: 6px;
  color: var(--c-muted);
  font-size: 13px;
}

.ticket-field input,
.ticket-field select,
.ticket-field textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--c-border);
  border-radius: 8px;
  background: var(--c-surface);
  color: var(--c-text);
  font: inherit;
}

.ticket-field textarea {
  resize: vertical;
}

.ticket-form-actions {
  margin-top: 18px;
}

.ticket-form-actions .submit-button {
  margin-top: 0;
}

.ticket-form-actions .secondary-button {
  flex: none;
  margin-top: 0;
}

.ticket-status {
  flex: none;
  padding: 2px 9px;
  border-radius: 999px;
  background: #f1eee8;
  color: var(--c-muted);
  font-size: 12px;
  font-weight: 600;
}

.ticket-status--open {
  background: #f4e5e3;
  color: var(--c-danger);
}

.ticket-status--processing {
  background: var(--c-accent-soft);
  color: var(--c-accent);
}

.ticket-status--resolved {
  background: #e4f1e7;
  color: #2d6b3d;
}

.ticket-back {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--c-accent);
  cursor: pointer;
  font-size: 13px;
}

.ticket-no {
  margin: 12px 0 0;
  color: var(--c-muted);
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12px;
}

.ticket-detail h2 {
  margin: 6px 0 0;
}

.ticket-detail h3 {
  margin: 22px 0 10px;
  font-size: 14px;
}

.ticket-meta {
  margin: 6px 0 0;
  color: var(--c-muted);
  font-size: 12px;
}

.ticket-content {
  margin: 14px 0 0;
  padding: 12px;
  border-radius: 8px;
  background: #f7f5f0;
  font-size: 14px;
  white-space: pre-wrap;
}

.ticket-timeline {
  margin: 0;
  padding: 0;
  list-style: none;
}

.ticket-timeline li {
  position: relative;
  display: flex;
  gap: 10px;
  padding-bottom: 16px;
  color: #9a9f9c;
}

.ticket-timeline li:last-child {
  padding-bottom: 0;
}

.ticket-timeline li::before {
  position: absolute;
  top: 12px;
  bottom: 0;
  left: 4px;
  width: 1px;
  background: var(--c-border);
  content: '';
}

.ticket-timeline li:last-child::before {
  display: none;
}

.ticket-dot {
  flex: none;
  width: 9px;
  height: 9px;
  margin-top: 5px;
  border: 2px solid var(--c-border);
  border-radius: 50%;
  background: var(--c-surface);
}

.ticket-timeline li.done {
  color: var(--c-text);
}

.ticket-timeline li.done .ticket-dot {
  border-color: var(--c-accent);
  background: var(--c-accent);
}

.ticket-timeline li.current strong {
  color: var(--c-accent);
}

.ticket-timeline small {
  display: block;
  margin-top: 1px;
  color: var(--c-muted);
  font-size: 11px;
}

.ticket-replies {
  display: grid;
  gap: 8px;
}

.ticket-reply {
  padding: 10px 12px;
  border: 1px solid var(--c-border);
  border-radius: 8px;
  font-size: 14px;
}

.ticket-reply p {
  margin: 0;
}

.ticket-reply-head {
  margin-bottom: 4px !important;
  color: var(--c-muted);
  font-size: 11px;
}

.ticket-refresh {
  margin-top: 16px;
}

.ticket-state {
  margin: 16px 0 0;
  color: var(--c-muted);
  font-size: 14px;
}

.ticket-list {
  display: grid;
  gap: 10px;
  margin-top: 20px;
}

.ticket-item {
  display: block;
  width: 100%;
  padding: 14px;
  border: 1px solid var(--c-border);
  border-radius: 10px;
  background: var(--c-surface);
  color: var(--c-text);
  text-align: left;
  cursor: pointer;
}

.ticket-item-head strong {
  min-width: 0;
  overflow: hidden;
  font-size: 15px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ticket-item-foot {
  display: block;
  margin-top: 8px;
  color: var(--c-muted);
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 11px;
}
</style>
