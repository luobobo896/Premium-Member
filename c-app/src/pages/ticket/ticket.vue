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

function statusClass(status: string): string {
  return `status-${status.toLowerCase()}`;
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

onShow(load);
</script>

<template>
  <view class="screen">
    <view class="card">
      <view class="header">
        <text class="eyebrow">客服支持</text>
        <text class="title">我的工单</text>
        <text class="lede">提交问题后由客服受理，处理进度与回复会显示在这里。</text>
      </view>

      <button v-if="!showForm" class="primary-button" @click="startCreate">新建工单</button>

      <text v-if="loading" class="state">正在加载工单…</text>
      <text v-else-if="error" class="error">{{ error }}</text>

      <view v-if="showForm" class="form-card">
        <text class="section-title">新建工单</text>
        <text class="field-label">分类</text>
        <picker
          mode="selector"
          :range="CATEGORY_OPTIONS"
          range-key="label"
          :value="categoryIndex"
          @change="onCategoryChange"
        >
          <view class="field-input">{{ CATEGORY_OPTIONS[categoryIndex]?.label }}</view>
        </picker>
        <text class="field-label">标题</text>
        <input v-model="form.title" class="field-input" maxlength="128" placeholder="一句话说明问题" />
        <text class="field-label">订单号（可选）</text>
        <input
          v-model="form.orderNo"
          class="field-input"
          maxlength="40"
          placeholder="如 PM202611270001"
        />
        <text class="field-label">问题描述</text>
        <textarea
          v-model="form.content"
          class="field-textarea"
          maxlength="5000"
          placeholder="请补充发生时间、操作步骤和期望结果"
        />
        <text v-if="formError" class="error">{{ formError }}</text>
        <view class="form-actions">
          <button class="secondary-button" @click="cancelCreate">取消</button>
          <button class="primary-button" :loading="submitting" @click="submit">提交工单</button>
        </view>
      </view>

      <view v-if="selected" class="detail-card">
        <view class="detail-head">
          <button class="link-button" @click="selected = null">← 返回列表</button>
          <text class="tag" :class="statusClass(selected.status)">
            {{ STATUS_LABELS[selected.status] ?? selected.status }}
          </text>
        </view>
        <text class="ticket-no">{{ selected.ticketNo }}</text>
        <text class="detail-title">{{ selected.title }}</text>
        <text class="meta">
          {{ categoryLabel(selected.category) }}
          <template v-if="selected.orderNo"> · 订单 {{ selected.orderNo }}</template>
        </text>
        <text class="content">{{ selected.content }}</text>

        <text class="section-title">处理进度</text>
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
              <text v-if="stepTime(step.value)" class="timeline-time">{{ stepTime(step.value) }}</text>
            </view>
          </view>
        </view>

        <text class="section-title">客服回复</text>
        <text v-if="!selected.replies?.length" class="state">暂无回复，客服受理后会在这里展示。</text>
        <view v-else class="replies">
          <view v-for="reply in selected.replies" :key="reply.createdAt + reply.content" class="reply">
            <text class="reply-head">{{ reply.senderName || '客服' }} · {{ formatTime(reply.createdAt) }}</text>
            <text class="reply-content">{{ reply.content }}</text>
          </view>
        </view>
        <button class="secondary-button" @click="refreshDetail">刷新进度</button>
      </view>

      <text v-if="detailLoading" class="state">正在加载详情…</text>

      <template v-if="!showForm && !selected">
        <text v-if="!loading && !error && tickets.length === 0" class="state">
          还没有工单，遇到问题可以新建一条。
        </text>
        <view v-else class="ticket-list">
          <view
            v-for="ticket in tickets"
            :key="ticket.id"
            class="ticket-item"
            @click="openDetail(ticket)"
          >
            <view class="ticket-item-head">
              <text class="ticket-title">{{ ticket.title }}</text>
              <text class="tag" :class="statusClass(ticket.status)">
                {{ STATUS_LABELS[ticket.status] ?? ticket.status }}
              </text>
            </view>
            <text class="meta">{{ categoryLabel(ticket.category) }} · {{ formatTime(ticket.createdAt) }}</text>
            <text class="ticket-foot">
              {{ ticket.ticketNo }}
              <template v-if="ticket.replyCount"> · {{ ticket.replyCount }} 条回复</template>
            </text>
          </view>
        </view>
      </template>
    </view>
  </view>
</template>

<style scoped>
.screen {
  min-height: 100vh;
  padding: 32rpx;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  background: #f4f2ed;
}

.card {
  width: 100%;
  max-width: 680rpx;
  padding: 48rpx 36rpx 40rpx;
  border: 1rpx solid #ddd9d1;
  border-radius: 32rpx;
  background: #fffdf9;
}

.eyebrow {
  display: block;
  color: #2f6b5f;
  font-size: 24rpx;
  font-weight: 600;
}

.title {
  display: block;
  margin-top: 12rpx;
  color: #1f2421;
  font-size: 52rpx;
  font-weight: 600;
  line-height: 1.25;
}

.lede {
  display: block;
  margin-top: 12rpx;
  color: #5c645f;
  font-size: 26rpx;
}

.primary-button,
.secondary-button {
  height: 88rpx;
  margin-top: 32rpx;
  border-radius: 16rpx;
  font-size: 30rpx;
  line-height: 88rpx;
}

.primary-button {
  background: #2f6b5f;
  color: #ffffff;
}

.secondary-button {
  border: 1rpx solid #ddd9d1;
  background: transparent;
  color: #1f2421;
}

.state,
.error,
.meta {
  display: block;
  margin-top: 20rpx;
  color: #5c645f;
  font-size: 26rpx;
}

.error {
  color: #a83b32;
}

.form-card,
.detail-card {
  margin-top: 32rpx;
  padding: 28rpx;
  border: 1rpx solid #ddd9d1;
  border-radius: 20rpx;
}

.section-title {
  display: block;
  margin: 28rpx 0 12rpx;
  color: #1f2421;
  font-size: 30rpx;
  font-weight: 600;
}

.form-card .section-title {
  margin-top: 0;
}

.field-label {
  display: block;
  margin-top: 20rpx;
  color: #5c645f;
  font-size: 24rpx;
}

.field-input,
.field-textarea {
  width: 100%;
  margin-top: 10rpx;
  padding: 18rpx 20rpx;
  box-sizing: border-box;
  border: 1rpx solid #ddd9d1;
  border-radius: 12rpx;
  background: #fffdf9;
  color: #1f2421;
  font-size: 28rpx;
}

.field-textarea {
  height: 220rpx;
}

.form-actions {
  display: flex;
  gap: 16rpx;
}

.form-actions button {
  flex: 1;
}

.detail-head,
.ticket-item-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.link-button {
  padding: 0;
  border: 0;
  background: transparent;
  color: #2f6b5f;
  font-size: 24rpx;
  line-height: 1.4;
}

.tag {
  flex: none;
  padding: 4rpx 18rpx;
  border-radius: 999rpx;
  background: #f1eee8;
  color: #5c645f;
  font-size: 22rpx;
  font-weight: 600;
}

.status-open {
  background: #f4e5e3;
  color: #a83b32;
}

.status-processing {
  background: #e7efec;
  color: #2f6b5f;
}

.status-resolved {
  background: #e4f1e7;
  color: #2d6b3d;
}

.ticket-no {
  display: block;
  margin-top: 20rpx;
  color: #5c645f;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 22rpx;
}

.detail-title {
  display: block;
  margin-top: 8rpx;
  color: #1f2421;
  font-size: 36rpx;
  font-weight: 600;
}

.content {
  display: block;
  margin-top: 20rpx;
  padding: 20rpx;
  border-radius: 12rpx;
  background: #f7f5f0;
  color: #1f2421;
  font-size: 28rpx;
  line-height: 1.6;
  white-space: pre-wrap;
}

.timeline {
  display: flex;
  flex-direction: column;
}

.timeline-row {
  position: relative;
  display: flex;
  gap: 16rpx;
  padding-bottom: 24rpx;
  color: #9a9f9c;
}

.timeline-row:last-child {
  padding-bottom: 0;
}

.timeline-row::before {
  position: absolute;
  top: 20rpx;
  bottom: 0;
  left: 7rpx;
  width: 1rpx;
  background: #ddd9d1;
  content: '';
}

.timeline-row:last-child::before {
  display: none;
}

.dot {
  flex: none;
  width: 16rpx;
  height: 16rpx;
  margin-top: 10rpx;
  border: 3rpx solid #ddd9d1;
  border-radius: 50%;
  background: #fffdf9;
}

.timeline-row.done {
  color: #1f2421;
}

.timeline-row.done .dot {
  border-color: #2f6b5f;
  background: #2f6b5f;
}

.timeline-row.current .timeline-label {
  color: #2f6b5f;
}

.timeline-label,
.timeline-time {
  display: block;
  font-size: 26rpx;
}

.timeline-time {
  margin-top: 2rpx;
  color: #5c645f;
  font-size: 22rpx;
}

.replies {
  display: flex;
  flex-direction: column;
  gap: 14rpx;
}

.reply {
  padding: 18rpx;
  border: 1rpx solid #ddd9d1;
  border-radius: 12rpx;
}

.reply-head,
.reply-content {
  display: block;
  font-size: 26rpx;
}

.reply-head {
  margin-bottom: 6rpx;
  color: #5c645f;
  font-size: 22rpx;
}

.reply-content {
  color: #1f2421;
}

.ticket-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
  margin-top: 28rpx;
}

.ticket-item {
  padding: 24rpx;
  border: 1rpx solid #ddd9d1;
  border-radius: 16rpx;
}

.ticket-title {
  flex: 1;
  overflow: hidden;
  color: #1f2421;
  font-size: 30rpx;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ticket-foot {
  display: block;
  margin-top: 12rpx;
  color: #5c645f;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 22rpx;
}
</style>
