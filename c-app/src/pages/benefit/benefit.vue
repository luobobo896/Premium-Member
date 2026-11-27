<script setup lang="ts">
import { computed, ref } from 'vue';
import { onLoad, onShow } from '@dcloudio/uni-app';

import {
  ApiError,
  claimBenefit,
  clearToken,
  consumePendingClaim,
  getBenefitClaims,
  getBenefitCatalog,
  getBenefits,
  getToken,
  requireLogin,
  type BenefitCatalogProduct,
  type BenefitCenter,
  type BenefitClaimHistory,
  type BenefitProduct,
} from '../../api';
import AppTabbar from '../../components/AppTabbar.vue';
import GradientCover from '../../components/GradientCover.vue';

const center = ref<BenefitCenter | null>(null);
const catalog = ref<BenefitCatalogProduct[]>([]);
const history = ref<BenefitClaimHistory[]>([]);
const loggedIn = ref(false);
const loading = ref(true);
const error = ref('');
const claimingSku = ref('');
const autoClaimSku = ref('');
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

const quotaPercent = computed(() => {
  if (!center.value?.quotaTotal) return 0;
  return Math.min(100, Math.round((center.value.quotaRemaining / center.value.quotaTotal) * 100));
});

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

function coverLabel(name: string): string {
  return name.replace(/\s/g, '').slice(0, 2);
}

function coverTone(index: number): string {
  return ['benefit', 'benefit2', 'benefit3'][index % 3];
}

function statusClass(product: BenefitProduct): string {
  if (product.claimed) return 'pm-pill--accent';
  if (product.claimStatus === 'FAILED') return 'pm-pill--danger';
  return 'pm-pill--warning';
}

function goLogin(): void {
  requireLogin('/pages/benefit/benefit');
}

function claimCatalog(product: BenefitCatalogProduct): void {
  requireLogin('/pages/benefit/benefit', product.skuCode);
}

async function load(): Promise<void> {
  loggedIn.value = Boolean(getToken());
  loading.value = true;
  error.value = '';
  try {
    catalog.value = await getBenefitCatalog();
    if (!loggedIn.value) {
      center.value = null;
      history.value = [];
      return;
    }
    const [centerData, claimData] = await Promise.all([getBenefits(), getBenefitClaims()]);
    center.value = centerData;
    history.value = claimData;
    await resumePendingClaim();
    await resumeAutoClaim();
  } catch (ex) {
    if (ex instanceof ApiError && ex.statusCode === 401) {
      clearToken();
      loggedIn.value = false;
      center.value = null;
      history.value = [];
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
  if (!loggedIn.value) {
    requireLogin('/pages/benefit/benefit', product.skuCode);
    return;
  }
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

async function resumePendingClaim(): Promise<void> {
  const sku = consumePendingClaim();
  const product = center.value?.products.find((item) => item.skuCode === sku);
  if (product && !product.claimed) {
    await claim(product);
  }
}

async function resumeAutoClaim(): Promise<void> {
  const sku = autoClaimSku.value;
  autoClaimSku.value = '';
  const product = center.value?.products.find((item) => item.skuCode === sku);
  if (product && !product.claimed) {
    await claim(product);
  }
}

onLoad((options) => {
  autoClaimSku.value = typeof options?.claim === 'string' ? options.claim : '';
});
onShow(load);
</script>

<template>
  <view class="pm-page">
    <text class="pm-title">{{ loggedIn ? '权益中心' : '会员权益目录' }}</text>
    <text v-if="loggedIn && center?.period" class="pm-sub period">
      周期 {{ center.period.periodNo }} · 至 {{ formatDate(center.period.endAt) }}
    </text>

    <text v-if="loading" class="pm-skeleton quota-skeleton"></text>

    <view v-else-if="error && !center" class="pm-card state-card">
      <text class="pm-notice">{{ error }}</text>
      <button class="pm-btn pm-btn--secondary" @click="load">重新加载</button>
    </view>

    <template v-else-if="!loggedIn">
      <view class="pm-card catalog-note">
        <view>
          <text class="pm-card-title">登录后领取本期权益</text>
          <text class="pm-sub">会员每个周期可选择 2 项权益，卡密仅领取时展示。</text>
        </view>
        <button class="pm-btn pm-btn--primary" @click="goLogin">登录 / 注册</button>
      </view>

      <view class="product-list">
        <view v-for="product in catalog" :key="product.skuCode" class="pm-card product">
          <view class="product-top">
            <view class="product-cover">
              <GradientCover
                :label="coverLabel(product.name)"
                :tone="product.cover"
                :height="72"
              />
            </view>
            <view class="product-main">
              <text class="pm-card-title">{{ product.name }}</text>
              <text class="pm-sub product-desc">{{ product.description }}</text>
            </view>
          </view>
          <text class="product-rules">{{ product.rules }}</text>
          <button class="pm-btn pm-btn--secondary claim" @click="claimCatalog(product)">
            登录后领取
          </button>
        </view>
      </view>
    </template>

    <view v-else-if="!center?.period" class="pm-card state-card">
      <text class="pm-body">当前没有生效中的权益周期。</text>
    </view>

    <template v-else>
      <view v-if="error" class="pm-notice notice">{{ error }}</view>

      <view class="pm-card quota">
        <view class="quota-row">
          <view>
            <text class="pm-sub">剩余名额</text>
            <view class="quota-number">
              <text class="pm-number">{{ center.quotaRemaining }}</text>
              <text class="quota-total">/{{ center.quotaTotal }}</text>
            </view>
          </view>
          <text class="pm-pill pm-pill--accent">本期</text>
        </view>
        <view class="quota-bar">
          <view class="quota-fill" :style="{ width: `${quotaPercent}%` }"></view>
        </view>
      </view>

      <view class="product-list">
        <view
          v-for="(product, index) in center.products"
          :key="product.skuCode"
          class="pm-card product"
        >
          <view class="product-top">
            <view class="product-cover">
              <GradientCover
                :label="coverLabel(product.name)"
                :tone="coverTone(index)"
                :height="72"
              />
            </view>
            <view class="product-main">
              <view class="product-head">
                <text class="pm-card-title">{{ product.name }}</text>
                <text class="pm-pill" :class="statusClass(product)">
                  {{ product.claimed ? '已领取' : claimLabel(product.claimStatus) }}
                </text>
              </view>
              <text class="pm-sub product-desc">{{ product.description }}</text>
            </view>
          </view>

          <text class="product-rules">{{ product.rules }}</text>
          <text class="pm-sub product-meta">
            {{ product.cardMasked ? `卡密 ${product.cardMasked}` : `状态：${claimLabel(product.claimStatus)}` }}
          </text>

          <button
            class="pm-btn pm-btn--primary claim"
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
      </view>

      <view class="pm-card history">
        <text class="pm-section">领取历史</text>
        <text v-if="history.length === 0" class="pm-empty">还没有领取记录。</text>
        <view v-for="item in history" :key="item.claimId" class="history-row">
          <view class="history-main">
            <text class="pm-card-title history-name">{{ item.productName }}</text>
            <text class="pm-sub">
              {{ item.cardMasked || '-' }} · {{ formatDate(item.claimedAt) }}
            </text>
          </view>
          <text class="pm-pill" :class="item.status === 'SUCCESS' ? 'pm-pill--accent' : ''">
            {{ claimLabel(item.status) }}
          </text>
        </view>
      </view>
    </template>

    <view v-if="reveal" class="modal-mask">
      <view class="pm-card modal">
        <text class="pm-sub">领取成功 · 仅展示一次</text>
        <text class="code">{{ reveal.cardCode }}</text>
        <text class="pm-body modal-desc">
          {{ reveal.productName }}：{{ reveal.redemption || '请按兑换说明尽快使用。' }}
        </text>
        <button class="pm-btn pm-btn--secondary" @click="reveal = null">我已保存</button>
      </view>
    </view>

    <AppTabbar active="benefit" />
  </view>
</template>

<style scoped>
.period {
  margin-top: var(--space-2);
}

.quota-skeleton {
  height: 110px;
  margin-top: var(--space-6);
}

.state-card {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  margin-top: var(--space-6);
}

.notice {
  margin-top: var(--space-4);
}

.quota {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  margin-top: var(--space-6);
}

.quota-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.quota-number {
  display: flex;
  align-items: baseline;
  gap: var(--space-1);
  margin-top: var(--space-1);
}

.quota-total {
  color: var(--text-muted);
  font-size: var(--font-body);
}

.quota-bar {
  width: 100%;
  height: 4px;
  overflow: hidden;
  border-radius: var(--radius-pill);
  background: var(--overlay);
}

.quota-fill {
  height: 100%;
  border-radius: var(--radius-pill);
  background: var(--accent-gradient);
}

.product-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  margin-top: var(--space-4);
}

.catalog-note {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  margin-top: var(--space-4);
}

.product-cover {
  flex: none;
  width: 72px;
}

.product-top {
  display: flex;
  gap: var(--space-3);
}

.product-main {
  flex: 1;
  min-width: 0;
}

.product-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-2);
}

.product-desc {
  margin-top: var(--space-1);
}

.pm-card-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-rules {
  display: block;
  margin-top: var(--space-3);
  padding-top: var(--space-3);
  border-top: 1px solid var(--line);
  color: var(--text-muted);
  font-size: var(--font-sub);
  line-height: 1.6;
}

.product-meta {
  margin-top: var(--space-1);
}

.claim {
  margin-top: var(--space-3);
}

.history {
  margin-top: var(--space-6);
}

.history-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  padding: var(--space-3) 0;
  border-bottom: 1px solid var(--line);
}

.history-row:last-child {
  padding-bottom: 0;
  border-bottom: 0;
}

.history-main {
  flex: 1;
  min-width: 0;
}

.history-name {
  margin-bottom: var(--space-1);
}

.modal-mask {
  position: fixed;
  z-index: 50;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-6);
  background: rgba(0, 0, 0, 0.72);
}

.modal {
  display: flex;
  width: 100%;
  max-width: 340px;
  flex-direction: column;
  gap: var(--space-3);
}

.code {
  color: var(--accent);
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 24px;
  font-weight: 700;
  line-height: 1.3;
  word-break: break-all;
}

.modal-desc {
  line-height: 1.6;
}
</style>
