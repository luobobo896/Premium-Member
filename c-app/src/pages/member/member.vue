<script setup lang="ts">
import { computed, ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';

import {
  ApiError,
  clearToken,
  getBenefits,
  getInfo,
  getMember,
  getToken,
  logout,
  requireLogin,
  type MemberInfo,
} from '../../api';
import AppIcon from '../../components/AppIcon.vue';
import AppTabbar from '../../components/AppTabbar.vue';

const LEVELS: Record<string, string> = {
  BASIC: '基础会员',
  SILVER: '白银会员',
  GOLD: '黄金会员',
  PLATINUM: '铂金会员',
};

const nickname = ref('');
const phone = ref('');
const member = ref<MemberInfo | null>(null);
const benefitEnd = ref('');
const loading = ref(true);
const error = ref('');
const loggingOut = ref(false);
const loggedIn = ref(Boolean(getToken()));

const initial = computed(() => (nickname.value || '会').slice(0, 1));

function levelName(level: string): string {
  return LEVELS[level] ?? level;
}

function formatDate(value: null | string): string {
  if (!value) return '-';
  const date = new Date(value);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(
    date.getDate(),
  ).padStart(2, '0')}`;
}

function formatBalance(value: number): string {
  return `¥${value.toFixed(2)}`;
}

async function load(): Promise<void> {
  loggedIn.value = Boolean(getToken());
  if (!loggedIn.value) {
    loading.value = false;
    return;
  }
  loading.value = true;
  error.value = '';
  try {
    const info = await getInfo();
    nickname.value = info.nickname;
    phone.value = info.phone;
    try {
      member.value = await getMember();
    } catch (ex) {
      if (ex instanceof ApiError && ex.bizCode === 'MEMBER_NOT_FOUND') {
        member.value = null;
        benefitEnd.value = '';
        return;
      }
      throw ex;
    }
    const center = await getBenefits().catch(() => null);
    benefitEnd.value = center?.period?.endAt ?? '';
  } catch (ex) {
    if (ex instanceof ApiError && ex.statusCode === 401) {
      clearToken();
      loggedIn.value = false;
      member.value = null;
      return;
    }
    error.value = ex instanceof Error ? ex.message : '加载失败，请稍后重试';
  } finally {
    loading.value = false;
  }
}

async function signOut(): Promise<void> {
  loggingOut.value = true;
  try {
    await logout();
  } finally {
    loggingOut.value = false;
    loggedIn.value = false;
    member.value = null;
    uni.reLaunch({ url: '/pages/member/member' });
  }
}

function goLogin(): void {
  requireLogin('/pages/member/member');
}

function openBenefit(): void {
  uni.reLaunch({ url: '/pages/benefit/benefit' });
}

function openAi(): void {
  uni.reLaunch({ url: '/pages/ai/ai' });
}

function openTickets(): void {
  uni.navigateTo({ url: '/pages/ticket/ticket' });
}

onShow(load);
</script>

<template>
  <view class="pm-page">
    <template v-if="!loggedIn">
      <view class="guest-hero">
        <view class="avatar guest-avatar">
          <AppIcon name="user" :size="28" />
        </view>
        <text class="pm-title guest-title">登录 / 注册</text>
        <text class="pm-sub">登录后查看会员卡、订单、权益记录与 AI 作品</text>
      </view>

      <view class="pm-card guest-points">
        <view class="guest-point">
          <AppIcon name="sparkles" :size="18" />
          <text>AI 写真、换装与图生视频</text>
        </view>
        <view class="guest-point">
          <AppIcon name="gift" :size="18" />
          <text>每个会员周期领取 2 项权益</text>
        </view>
        <view class="guest-point">
          <AppIcon name="ticket" :size="18" />
          <text>专属工单与客服处理进度</text>
        </view>
      </view>

      <button class="pm-btn pm-btn--primary guest-login" @click="goLogin">登录 / 注册</button>
      <button class="pm-btn pm-btn--secondary guest-browse" @click="openBenefit">
        先看看权益目录
      </button>
    </template>

    <template v-else>
      <view class="profile">
        <view class="avatar">
          <text class="avatar-text">{{ initial }}</text>
        </view>
        <view class="profile-copy">
          <text class="pm-title name">{{ nickname || '会员账号' }}</text>
          <text class="pm-sub">{{ phone }}</text>
        </view>
      </view>

      <view v-if="loading" class="pm-skeleton member-skeleton"></view>

      <view v-else-if="error" class="pm-card state-card">
        <text class="pm-notice">{{ error }}</text>
        <button class="pm-btn pm-btn--secondary retry" @click="load">重新加载</button>
      </view>

      <view v-else-if="!member" class="pm-card state-card">
        <text class="pm-body">该手机号还没有会员档案，请联系运营人员开通。</text>
      </view>

      <view v-else class="member-card">
        <view class="card-head">
          <text class="card-label">会员卡</text>
          <text class="pm-pill" :class="member.status === 1 ? 'pm-pill--accent' : 'pm-pill--danger'">
            {{ member.status === 1 ? '正常' : '停用' }}
          </text>
        </view>
        <text class="card-level">{{ levelName(member.level) }}</text>
        <text class="card-no">{{ member.memberNo }}</text>
        <view class="card-grid">
          <view class="card-cell cell-date">
            <text class="cell-label">到期</text>
            <text class="cell-value">{{ formatDate(benefitEnd) }}</text>
          </view>
          <view class="card-cell">
            <text class="cell-label">余额</text>
            <text class="cell-value">{{ formatBalance(member.balance) }}</text>
          </view>
          <view class="card-cell">
            <text class="cell-label">积分</text>
            <text class="cell-value">{{ member.points }}</text>
          </view>
        </view>
      </view>

      <view class="pm-card menu">
        <view class="menu-row pm-tap" @click="openBenefit">
          <view class="menu-icon"><AppIcon name="gift" :size="20" /></view>
          <text class="menu-label">权益中心</text>
          <AppIcon name="chevron-right" :size="18" />
        </view>
        <view class="menu-line"></view>
        <view class="menu-row pm-tap" @click="openAi">
          <view class="menu-icon"><AppIcon name="sparkles" :size="20" /></view>
          <text class="menu-label">AI 创作</text>
          <AppIcon name="chevron-right" :size="18" />
        </view>
        <view class="menu-line"></view>
        <view class="menu-row pm-tap" @click="openTickets">
          <view class="menu-icon"><AppIcon name="ticket" :size="20" /></view>
          <text class="menu-label">我的工单</text>
          <AppIcon name="chevron-right" :size="18" />
        </view>
      </view>

      <view class="pm-card menu">
        <view class="menu-row pm-tap" @click="signOut">
          <view class="menu-icon danger"><AppIcon name="logout" :size="20" /></view>
          <text class="menu-label danger">{{ loggingOut ? '退出中' : '退出登录' }}</text>
        </view>
      </view>
    </template>

    <AppTabbar active="member" />
  </view>
</template>

<style scoped>
.guest-hero {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: var(--space-2);
  margin-top: var(--space-6);
}

.guest-avatar {
  margin-bottom: var(--space-2);
  color: var(--accent);
}

.guest-title {
  margin-top: var(--space-1);
}

.guest-points {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  margin-top: var(--space-6);
}

.guest-point {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  color: var(--text-muted);
  font-size: var(--font-body);
}

.guest-point :deep(svg) {
  color: var(--accent);
}

.guest-login {
  margin-top: var(--space-6);
}

.guest-browse {
  margin-top: var(--space-3);
}

.profile {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-6);
}

.avatar {
  display: flex;
  width: 64px;
  height: 64px;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: var(--radius-pill);
  background: linear-gradient(150deg, var(--member-a), var(--surface));
}

.avatar-text {
  color: var(--accent);
  font-size: var(--font-hero);
  font-weight: 600;
}

.profile-copy {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: var(--space-1);
  min-width: 0;
}

.name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.member-skeleton {
  height: 196px;
}

.state-card {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.retry {
  margin-top: var(--space-1);
}

.member-card {
  position: relative;
  padding: var(--space-4);
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: var(--radius-card);
  background: linear-gradient(135deg, var(--member-a), var(--member-b));
}

.member-card::after {
  position: absolute;
  top: -90px;
  right: -60px;
  width: 220px;
  height: 220px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(61, 220, 151, 0.2), transparent 66%);
  content: '';
}

.card-head {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  z-index: 1;
}

.card-label {
  color: var(--text-muted);
  font-size: var(--font-sub);
}

.card-level {
  position: relative;
  display: block;
  margin-top: var(--space-4);
  color: var(--text);
  font-size: var(--font-hero);
  font-weight: 600;
  line-height: 1.3;
  z-index: 1;
}

.card-no {
  position: relative;
  display: block;
  margin-top: var(--space-1);
  color: var(--text-muted);
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: var(--font-sub);
  z-index: 1;
}

.card-grid {
  position: relative;
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-top: var(--space-6);
  z-index: 1;
}

.card-cell {
  flex: 1 1 calc(50% - 4px);
  min-width: 0;
}

.cell-date {
  flex-basis: 100%;
}

.cell-label {
  display: block;
  color: var(--text-muted);
  font-size: var(--font-sub);
}

.cell-value {
  display: block;
  margin-top: var(--space-1);
  overflow: hidden;
  color: var(--text);
  font-size: var(--font-body);
  font-variant-numeric: tabular-nums;
  font-weight: 600;
  line-height: 1.3;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.menu {
  margin-top: var(--space-4);
  padding: 0 var(--space-4);
}

.menu-row {
  display: flex;
  height: 56px;
  align-items: center;
  gap: var(--space-3);
  color: var(--text-muted);
}

.menu-icon {
  display: flex;
  width: 32px;
  height: 32px;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-pill);
  background: var(--overlay);
  color: var(--text-muted);
}

.menu-label {
  flex: 1;
  color: var(--text);
  font-size: var(--font-body);
}

.menu-line {
  height: 1px;
  margin-left: var(--space-12);
  background: var(--line);
}

.danger {
  color: var(--danger);
}
</style>
