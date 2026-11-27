<script setup lang="ts">
import { ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';

import {
  ApiError,
  clearToken,
  getInfo,
  getMember,
  getToken,
  logout,
  type MemberInfo,
} from '../../api';

const LEVELS: Record<string, string> = {
  BASIC: '基础会员',
  SILVER: '白银会员',
  GOLD: '黄金会员',
  PLATINUM: '铂金会员',
};

const nickname = ref('');
const phone = ref('');
const member = ref<MemberInfo | null>(null);
const loading = ref(true);
const error = ref('');
const loggingOut = ref(false);

function levelName(level: string): string {
  return LEVELS[level] ?? level;
}

function formatDate(value: string): string {
  const date = new Date(value);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(
    date.getDate(),
  ).padStart(2, '0')}`;
}

function formatBalance(value: number): string {
  return `¥${value.toFixed(2)}`;
}

async function load(): Promise<void> {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/login/login' });
    return;
  }
  loading.value = true;
  error.value = '';
  try {
    const [info, memberInfo] = await Promise.all([getInfo(), getMember()]);
    nickname.value = info.nickname;
    phone.value = info.phone;
    member.value = memberInfo;
  } catch (ex) {
    if (ex instanceof ApiError && ex.statusCode === 401) {
      clearToken();
      uni.reLaunch({ url: '/pages/login/login' });
      return;
    }
    if (ex instanceof ApiError && ex.bizCode === 'MEMBER_NOT_FOUND') {
      const info = await getInfo();
      nickname.value = info.nickname;
      phone.value = info.phone;
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
    uni.reLaunch({ url: '/pages/login/login' });
  }
}

function openBenefit(): void {
  uni.navigateTo({ url: '/pages/benefit/benefit' });
}

function openAi(): void {
  uni.navigateTo({ url: '/pages/ai/ai' });
}

onShow(load);
</script>

<template>
  <view class="screen">
    <view class="card">
      <view class="header">
        <view>
          <text class="eyebrow">会员中心</text>
          <text class="title">{{ nickname || '会员账号' }}</text>
          <text class="lede">{{ phone }}</text>
          <text v-if="member" class="pill" :class="{ inactive: member.status !== 1 }">
            {{ member.status === 1 ? '正常' : '停用' }}
          </text>
        </view>
      </view>

      <text v-if="loading" class="state">正在加载会员信息…</text>

      <view v-else-if="error" class="state-block">
        <text class="error">{{ error }}</text>
        <button class="secondary-button" @click="load">重新加载</button>
      </view>

      <view v-else-if="!member" class="state-block">
        <text class="state">该手机号还没有会员档案，请联系运营人员开通。</text>
      </view>

      <view v-else class="member-body">
        <text class="member-no">{{ member.memberNo }}</text>
        <view class="grid">
          <view class="item">
            <text class="item-label">等级</text>
            <text class="item-value">{{ levelName(member.level) }}</text>
          </view>
          <view class="item">
            <text class="item-label">积分</text>
            <text class="item-value">{{ member.points }}</text>
          </view>
          <view class="item">
            <text class="item-label">余额</text>
            <text class="item-value">{{ formatBalance(member.balance) }}</text>
          </view>
          <view class="item">
            <text class="item-label">入会时间</text>
            <text class="item-value">{{ formatDate(member.joinedAt) }}</text>
          </view>
        </view>
      </view>

      <button class="secondary-button benefit-entry" @click="openBenefit">权益中心</button>
      <button class="secondary-button benefit-entry" @click="openAi">AI 创作</button>
      <button class="secondary-button logout" :loading="loggingOut" @click="signOut">
        {{ loggingOut ? '退出中' : '退出登录' }}
      </button>
    </view>
  </view>
</template>

<style scoped>
.screen {
  min-height: 100vh;
  padding: 48rpx 32rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f4f2ed;
}

.card {
  width: 100%;
  max-width: 680rpx;
  padding: 56rpx 40rpx 40rpx;
  border: 1rpx solid #ddd9d1;
  border-radius: 32rpx;
  background: #fffdf9;
}

.header {
  display: block;
}

.eyebrow {
  display: block;
  color: #2f6b5f;
  font-size: 24rpx;
  font-weight: 600;
}

.title {
  display: block;
  margin-top: 16rpx;
  color: #1f2421;
  font-size: 56rpx;
  font-weight: 600;
  line-height: 1.25;
}

.lede {
  display: block;
  margin-top: 12rpx;
  color: #5c645f;
  font-size: 28rpx;
}

.pill {
  display: inline-block;
  margin-top: 20rpx;
  padding: 8rpx 24rpx;
  border-radius: 999rpx;
  background: #e7efec;
  color: #2f6b5f;
  font-size: 24rpx;
  font-weight: 600;
}

.pill.inactive {
  background: #f4e5e3;
  color: #a83b32;
}

.state {
  display: block;
  margin-top: 48rpx;
  color: #5c645f;
  font-size: 28rpx;
}

.state-block {
  margin-top: 32rpx;
}

.error {
  display: block;
  color: #a83b32;
  font-size: 28rpx;
}

.member-body {
  margin-top: 40rpx;
}

.member-no {
  display: block;
  padding: 24rpx 32rpx;
  border: 1rpx solid #ddd9d1;
  border-radius: 16rpx;
  color: #1f2421;
  font-size: 32rpx;
  font-weight: 600;
}

.grid {
  display: flex;
  flex-wrap: wrap;
  margin-top: 24rpx;
}

.item {
  width: 50%;
  margin-top: 24rpx;
}

.item-label {
  display: block;
  color: #5c645f;
  font-size: 28rpx;
}

.item-value {
  display: block;
  margin-top: 8rpx;
  color: #1f2421;
  font-size: 36rpx;
  font-weight: 600;
}

.secondary-button {
  height: 88rpx;
  margin-top: 40rpx;
  border: 1rpx solid #ddd9d1;
  border-radius: 16rpx;
  background: transparent;
  color: #1f2421;
  font-size: 32rpx;
  line-height: 88rpx;
}

.logout {
  margin-top: 48rpx;
}

.benefit-entry {
  margin-top: 32rpx;
  background: #2f6b5f;
  color: #ffffff;
}
</style>
