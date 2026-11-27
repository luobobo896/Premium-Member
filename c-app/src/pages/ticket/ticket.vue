<script setup lang="ts">
import { computed, ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';

import {
  ApiError,
  clearToken,
  createTicket,
  getTicket,
  getTickets,
  getToken,
  type TicketInfo,
} from '../../api';
import AppIcon from '../../components/AppIcon.vue';

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
const showForm = ref(false);
const submitting = ref(false);
const error = ref('');
const formError = ref('');
const categoryIndex = ref(4);
const form = ref({
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
  if (status === 'OPEN') return 'pm-pill--danger';
  if (status === 'PROCESSING') return 'pm-pill--warning';
  if (status === 'RESOLVED') return 'pm-pill--accent';
  return '';
}

function formatTime(value: null | string | undefined): string {
  if (!value) return '-';
  const date = new Date(value);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(
    date.getDate(),
  ).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(
    date.getMinutes(),
  ).padStart(2, '0')}`;
}

function stepTime(step: string): string {
  if (!selected.value) return '';
  if (step === 'OPEN') return formatTime(selected.value.createdAt);
  if (step === 'PROCESSING') {
    return selected.value.status === 'PROCESSING' ? formatTime(selected.value.updatedAt) : '';
  }
  if (step === 'RESOLVED' || step === 'CLOSED') {
    return selected.value.resolvedAt ? formatTime(selected.value.resolvedAt) : '';
  }
  return '';
}

function handleError(ex: unknown, fallback: string) {
  if (ex instanceof ApiError && ex.statusCode === 401) {
    clearToken();
    uni.reLaunch({ url: '/pages/login/login' });
    return;
  }
  error.value = ex instanceof Error ? ex.message : fallback;
}

async function load(): Promise<void> {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/login/login' });
    return;
  }
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
  if (selected.value) {
    await openDetail(selected.value);
  }
}

function onCategoryChange(event: { detail: { value: number } }): void {
  categoryIndex.value = Number(event.detail.value);
}

function startCreate(): void {
  showForm.value = true;
  selected.value = null;
  formError.value = '';
}

function cancelCreate(): void {
  showForm.value = false;
  formError.value = '';
}

async function submit(): Promise<void> {
  if (!form.value.title.trim()) {
    formError.value = '请填写工单标题';
    return;
  }
  if (!form.value.content.trim()) {
    formError.value = '请描述遇到的问题';
    return;
  }
  submitting.value = true;
  formError.value = '';
  try {
    const created = await createTicket({
      category: CATEGORY_OPTIONS[categoryIndex.value]?.value ?? 'OTHER',
      content: form.value.content.trim(),
      orderNo: form.value.orderNo.trim() || undefined,
      title: form.value.title.trim(),
    });
    form.value = { content: '', orderNo: '', title: '' };
    showForm.value = false;
    await load();
    selected.value = created;
  } catch (ex) {
    formError.value = ex instanceof Error ? ex.message : '提交失败，请稍后重试';
  } finally {
    submitting.value = false;
  }
}

function back(): void {
  const pages = getCurrentPages();
  if (pages.length > 1) {
    uni.navigateBack({ delta: 1 });
    return;
  }
  uni.reLaunch({ url: '/pages/member/member' });
}

onShow(load);
</script>

<template>
  <view class="pm-page pm-page--plain">
    <view class="nav">
      <view class="back pm-tap" aria-label="返回" @click="back">
        <AppIcon name="arrow-left" :size="22" />
      </view>
      <text class="pm-title">我的工单</text>
    </view>
    <text class="pm-sub lede">提交问题后由客服受理，处理进度与回复会显示在这里。</text>

    <button v-if="!showForm && !selected" class="pm-btn pm-btn--primary new" @click="startCreate">
      新建工单
    </button>

    <view v-if="loading" class="skeleton-list">
      <view class="pm-skeleton skeleton-row"></view>
      <view class="pm-skeleton skeleton-row"></view>
    </view>

    <view v-else-if="error && !selected && !showForm" class="pm-card state-card">
      <text class="pm-notice">{{ error }}</text>
      <button class="pm-btn pm-btn--secondary" @click="load">重新加载</button>
    </view>

    <view v-if="showForm" class="pm-card form-card">
      <text class="pm-section">新建工单</text>
      <text class="field-label">分类</text>
      <picker
        mode="selector"
        :range="CATEGORY_OPTIONS"
        range-key="label"
        :value="categoryIndex"
        @change="onCategoryChange"
      >
        <view class="pm-input picker">{{ CATEGORY_OPTIONS[categoryIndex]?.label }}</view>
      </picker>
      <text class="field-label">标题</text>
      <input
        v-model="form.title"
        class="pm-input field"
        maxlength="128"
        placeholder="一句话说明问题"
        placeholder-class="pm-placeholder"
      />
      <text class="field-label">订单号（可选）</text>
      <input
        v-model="form.orderNo"
        class="pm-input field"
        maxlength="40"
        placeholder="如 PM202611270001"
        placeholder-class="pm-placeholder"
      />
      <text class="field-label">问题描述</text>
      <textarea
        v-model="form.content"
        class="pm-textarea field"
        maxlength="5000"
        placeholder="请补充发生时间、操作步骤和期望结果"
        placeholder-class="pm-placeholder"
      />
      <text v-if="formError" class="pm-notice form-error">{{ formError }}</text>
      <view class="form-actions">
        <button class="pm-btn pm-btn--secondary" @click="cancelCreate">取消</button>
        <button class="pm-btn pm-btn--primary" :loading="submitting" @click="submit">
          提交工单
        </button>
      </view>
    </view>

    <view v-if="selected" class="pm-card detail-card">
      <view class="detail-head">
        <button class="back-list pm-tap" @click="selected = null">← 返回列表</button>
        <text class="pm-pill" :class="statusPill(selected.status)">
          {{ STATUS_LABELS[selected.status] ?? selected.status }}
        </text>
      </view>
      <text class="ticket-no">{{ selected.ticketNo }}</text>
      <text class="pm-section detail-title">{{ selected.title }}</text>
      <text class="pm-sub detail-meta">
        {{ categoryLabel(selected.category) }}
        <template v-if="selected.orderNo"> · 订单 {{ selected.orderNo }}</template>
      </text>
      <text class="content">{{ selected.content }}</text>

      <text class="pm-section block-title">处理进度</text>
      <view class="timeline">
        <view
          v-for="(step, index) in STATUS_FLOW"
          :key="step.value"
          class="timeline-row"
          :class="{ current: index === currentStep, done: index <= currentStep }"
        >
          <view class="dot"></view>
          <view class="timeline-body">
            <text class="timeline-label">{{ step.label }}</text>
            <text v-if="stepTime(step.value)" class="pm-sub">{{ stepTime(step.value) }}</text>
          </view>
        </view>
      </view>

      <text class="pm-section block-title">客服回复</text>
      <text v-if="!selected.replies?.length" class="pm-empty reply-empty">
        暂无回复，客服受理后会在这里展示。
      </text>
      <view v-else class="replies">
        <view v-for="reply in selected.replies" :key="reply.createdAt + reply.content" class="reply">
          <text class="pm-sub">{{ reply.senderName || '客服' }} · {{ formatTime(reply.createdAt) }}</text>
          <text class="pm-body reply-content">{{ reply.content }}</text>
        </view>
      </view>
      <button class="pm-btn pm-btn--secondary refresh" @click="refreshDetail">刷新进度</button>
    </view>

    <text v-if="detailLoading" class="pm-sub detail-loading">正在加载详情…</text>

    <template v-if="!showForm && !selected && !loading && !error">
      <text v-if="tickets.length === 0" class="pm-empty">
        还没有工单，遇到问题可以新建一条。
      </text>
      <view v-else class="ticket-list">
        <view
          v-for="ticket in tickets"
          :key="ticket.id"
          class="pm-card ticket-item pm-tap"
          @click="openDetail(ticket)"
        >
          <view class="ticket-head">
            <text class="pm-card-title ticket-title">{{ ticket.title }}</text>
            <text class="pm-pill" :class="statusPill(ticket.status)">
              {{ STATUS_LABELS[ticket.status] ?? ticket.status }}
            </text>
          </view>
          <text class="pm-sub ticket-meta">
            {{ categoryLabel(ticket.category) }} · {{ formatTime(ticket.createdAt) }}
          </text>
          <text class="ticket-foot">
            {{ ticket.ticketNo }}
            <template v-if="ticket.replyCount"> · {{ ticket.replyCount }} 条回复</template>
          </text>
        </view>
      </view>
    </template>
  </view>
</template>

<style scoped>
.nav {
  display: flex;
  height: 48px;
  align-items: center;
  gap: var(--space-2);
}

.back {
  display: flex;
  width: 44px;
  height: 44px;
  align-items: center;
  justify-content: center;
  margin-left: calc(var(--space-3) * -1);
  color: var(--text);
}

.lede {
  margin-top: var(--space-2);
}

.new {
  margin-top: var(--space-6);
}

.skeleton-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  margin-top: var(--space-6);
}

.skeleton-row {
  height: 112px;
}

.state-card {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  margin-top: var(--space-6);
}

.form-card {
  display: flex;
  flex-direction: column;
  margin-top: var(--space-6);
}

.field-label {
  margin-top: var(--space-4);
  color: var(--text-muted);
  font-size: var(--font-sub);
}

.field {
  margin-top: var(--space-2);
}

.picker {
  display: flex;
  align-items: center;
}

.form-error {
  margin-top: var(--space-3);
}

.form-actions {
  display: flex;
  gap: var(--space-3);
  margin-top: var(--space-4);
}

.form-actions .pm-btn {
  flex: 1;
}

.detail-card {
  display: flex;
  flex-direction: column;
  margin-top: var(--space-6);
}

.detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
}

.back-list {
  min-height: 44px;
  color: var(--accent);
  font-size: var(--font-sub);
}

.ticket-no {
  margin-top: var(--space-2);
  color: var(--text-muted);
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: var(--font-sub);
}

.detail-title {
  margin-top: var(--space-2);
}

.detail-meta {
  margin-top: var(--space-1);
}

.content {
  margin-top: var(--space-3);
  padding-top: var(--space-3);
  border-top: 1px solid var(--line);
  color: var(--text);
  font-size: var(--font-body);
  line-height: 1.7;
  white-space: pre-wrap;
}

.block-title {
  margin-top: var(--space-6);
}

.timeline {
  display: flex;
  flex-direction: column;
  margin-top: var(--space-3);
}

.timeline-row {
  position: relative;
  display: flex;
  gap: var(--space-3);
  padding-bottom: var(--space-4);
  color: var(--text-disabled);
}

.timeline-row:last-child {
  padding-bottom: 0;
}

.timeline-row::before {
  position: absolute;
  top: 14px;
  bottom: 0;
  left: 5px;
  width: 1px;
  background: var(--line);
  content: '';
}

.timeline-row:last-child::before {
  display: none;
}

.dot {
  flex: none;
  width: 11px;
  height: 11px;
  margin-top: 5px;
  border: 2px solid var(--line);
  border-radius: var(--radius-pill);
  background: var(--surface);
}

.timeline-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.timeline-row.done {
  color: var(--text);
}

.timeline-row.done .dot {
  border-color: var(--accent);
  background: var(--accent);
}

.timeline-row.current .timeline-label {
  color: var(--accent);
}

.timeline-label {
  font-size: var(--font-body);
  line-height: 1.4;
}

.reply-empty {
  padding: var(--space-6) 0;
}

.replies {
  margin-top: var(--space-3);
}

.reply {
  padding: var(--space-3) 0;
  border-top: 1px solid var(--line);
}

.reply-content {
  display: block;
  margin-top: var(--space-1);
}

.refresh {
  margin-top: var(--space-4);
}

.detail-loading {
  margin-top: var(--space-3);
}

.ticket-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  margin-top: var(--space-4);
}

.ticket-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
}

.ticket-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ticket-meta {
  margin-top: var(--space-1);
}

.ticket-foot {
  display: block;
  margin-top: var(--space-2);
  color: var(--text-muted);
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: var(--font-sub);
}
</style>
