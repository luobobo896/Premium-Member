<script setup lang="ts">
import { ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';

import {
  ApiError,
  claimBenefit,
  clearToken,
  getBenefitClaims,
  getBenefits,
  getToken,
  type BenefitCenter,
  type BenefitClaimHistory,
  type BenefitProduct,
} from '../../api';

const center = ref<BenefitCenter | null>(null);
const history = ref<BenefitClaimHistory[]>([]);
const loading = ref(true);
const error = ref('');
const claimingSku = ref('');
const reveal = ref<{
  cardCode: string;
  productName: string;
  redemption: string | null;
} | null>(null);

const CLAIM_LABELS: Record<string, string> = {
  AVAILABLE: '待发放',
  PROCESSING: '处理中',
  SUCCESS: '已发放',
  FAILED: '已失败',
  UNKNOWN: '状态未知',
};

function claimLabel(status: null | string): string {
  return status ? (CLAIM_LABELS[status] ?? status) : '未领取';
}

function formatDate(value: null | string): string {
  if (!value) return '-';
  const date = new Date(value);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(
    date.getDate(),
  ).padStart(2, '0')}`;
}

async function load(): Promise<void> {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/login/login' });
    return;
  }
  loading.value = true;
  error.value = '';
  try {
    const [centerData, claimData] = await Promise.all([getBenefits(), getBenefitClaims()]);
    center.value = centerData;
    history.value = claimData;
  } catch (ex) {
    if (ex instanceof ApiError && ex.statusCode === 401) {
      clearToken();
      uni.reLaunch({ url: '/pages/login/login' });
      return;
    }
    if (ex instanceof ApiError && ex.bizCode === 'MEMBER_NOT_FOUND') {
      error.value = '该手机号还没有会员档案，请联系运营人员开通。';
      return;
    }
    error.value = ex instanceof Error ? ex.message : '加载失败，请稍后重试';
  } finally {
    loading.value = false;
  }
}

async function claim(product: BenefitProduct): Promise<void> {
  if (claimingSku.value) return;
  claimingSku.value = product.skuCode;
  error.value = '';
  try {
    const result = await claimBenefit(product.skuCode);
    if (result.cardCode) {
      reveal.value = {
        cardCode: result.cardCode,
        productName: result.productName,
        redemption: result.redemption,
      };
    }
    await load();
  } catch (ex) {
    error.value = ex instanceof Error ? ex.message : '领取失败，请稍后重试';
  } finally {
    claimingSku.value = '';
  }
}

function back(): void {
  uni.navigateBack({ delta: 1 });
}

onShow(load);
</script>

<template>
  <view class="screen">
    <view class="card">
      <view class="header">
        <text class="eyebrow">权益中心</text>
        <text class="title">本期权益</text>
        <text v-if="center?.period" class="lede">
          周期 {{ center.period.periodNo }} · 至 {{ formatDate(center.period.endAt) }}
        </text>
      </view>

      <text v-if="center?.period" class="pill">
        剩余 {{ center.quotaRemaining }}/{{ center.quotaTotal }}
      </text>

      <text v-if="loading" class="state">正在加载权益…</text>
      <view v-else-if="error && !center" class="state-block">
        <text class="error">{{ error }}</text>
        <button class="secondary-button" @click="load">重新加载</button>
      </view>
      <view v-else-if="!center?.period" class="state-block">
        <text class="state">当前没有生效中的权益周期。</text>
      </view>

      <template v-else>
        <text v-if="error" class="error">{{ error }}</text>

        <view v-for="product in center.products" :key="product.skuCode" class="product">
          <view class="product-head">
            <text class="product-name">{{ product.name }}</text>
            <text v-if="product.claimed" class="tag success">已领取</text>
            <text v-else-if="product.claimStatus === 'FAILED'" class="tag danger">可重试</text>
          </view>
          <text class="product-desc">{{ product.description }}</text>
          <text class="product-rules">{{ product.rules }}</text>
          <text class="product-meta">
            {{ product.cardMasked ? `卡密 ${product.cardMasked}` : `状态：${claimLabel(product.claimStatus)}` }}
          </text>
          <button
            class="primary-button"
            :disabled="product.claimed || !product.inStock || center.quotaRemaining === 0"
            :loading="claimingSku === product.skuCode"
            @click="claim(product)"
          >
            {{
              product.claimed
                ? '本期已领取'
                : center.quotaRemaining === 0
                  ? '本期名额已用尽'
                : product.inStock
                  ? '立即领取'
                  : '库存不足'
            }}
          </button>
        </view>

        <view v-if="reveal" class="reveal">
          <text class="eyebrow">领取成功 · 仅展示一次</text>
          <text class="code">{{ reveal.cardCode }}</text>
          <text class="product-desc">
            {{ reveal.productName }}：{{ reveal.redemption || '请按兑换说明尽快使用。' }}
          </text>
          <button class="secondary-button" @click="reveal = null">我已保存</button>
        </view>

        <view class="history">
          <text class="history-title">领取历史</text>
          <text v-if="history.length === 0" class="state">还没有领取记录。</text>
          <view v-for="item in history" :key="item.claimId" class="history-row">
            <view>
              <text class="history-name">{{ item.productName }}</text>
              <text class="product-meta">
                {{ item.cardMasked || '-' }} · {{ formatDate(item.claimedAt) }}
              </text>
            </view>
            <text class="tag" :class="{ success: item.status === 'SUCCESS' }">
              {{ claimLabel(item.status) }}
            </text>
          </view>
        </view>
      </template>

      <button class="secondary-button" @click="back">返回会员中心</button>
    </view>
  </view>
</template>

<style scoped>
.screen {
  min-height: 100vh;
  padding: 48rpx 32rpx;
  display: flex;
  align-items: flex-start;
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

.state {
  display: block;
  margin-top: 32rpx;
  color: #5c645f;
  font-size: 28rpx;
}

.state-block {
  margin-top: 32rpx;
}

.error {
  display: block;
  margin-top: 24rpx;
  color: #a83b32;
  font-size: 28rpx;
}

.product {
  margin-top: 28rpx;
  padding: 28rpx;
  border: 1rpx solid #ddd9d1;
  border-radius: 20rpx;
}

.product-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.product-name {
  color: #1f2421;
  font-size: 34rpx;
  font-weight: 600;
}

.product-desc {
  display: block;
  margin-top: 12rpx;
  color: #5c645f;
  font-size: 26rpx;
}

.product-rules {
  display: block;
  margin-top: 16rpx;
  padding: 16rpx 20rpx;
  border-radius: 12rpx;
  background: #f7f5f0;
  color: #5c645f;
  font-size: 24rpx;
}

.product-meta {
  display: block;
  margin-top: 12rpx;
  color: #5c645f;
  font-size: 24rpx;
}

.tag {
  padding: 4rpx 16rpx;
  border-radius: 999rpx;
  background: #eeeae3;
  color: #5c645f;
  font-size: 22rpx;
}

.tag.success {
  background: #e7efec;
  color: #2f6b5f;
}

.tag.danger {
  background: #f4e5e3;
  color: #a83b32;
}

.primary-button {
  height: 80rpx;
  margin-top: 24rpx;
  border-radius: 14rpx;
  background: #2f6b5f;
  color: #ffffff;
  font-size: 30rpx;
  line-height: 80rpx;
}

.primary-button[disabled] {
  background: #c9c4bb;
  color: #ffffff;
}

.secondary-button {
  height: 80rpx;
  margin-top: 24rpx;
  border: 1rpx solid #ddd9d1;
  border-radius: 14rpx;
  background: transparent;
  color: #1f2421;
  font-size: 30rpx;
  line-height: 80rpx;
}

.reveal {
  margin-top: 28rpx;
  padding: 24rpx;
  border: 1rpx solid #2f6b5f;
  border-radius: 20rpx;
  background: #e7efec;
}

.code {
  display: block;
  margin-top: 12rpx;
  color: #2f6b5f;
  font-family: monospace;
  font-size: 40rpx;
  font-weight: 700;
  word-break: break-all;
}

.history {
  margin-top: 36rpx;
  padding-top: 24rpx;
  border-top: 1rpx solid #ddd9d1;
}

.history-title {
  display: block;
  color: #1f2421;
  font-size: 30rpx;
  font-weight: 600;
}

.history-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #eeeae3;
}

.history-name {
  display: block;
  color: #1f2421;
  font-size: 28rpx;
}
</style>
