<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { Button } from 'vant';

import {
  ApiError,
  claimBenefit,
  clearAccessToken,
  consumePendingClaim,
  getBenefitClaims,
  getBenefitCatalog,
  getBenefits,
  getAccessToken,
  type BenefitCatalogProduct,
  type BenefitCenter,
  type BenefitClaimHistory,
  type BenefitProduct,
} from '../api';

const emit = defineEmits<{
  login: [action: { claimSku: string }];
  logout: [];
}>();

const center = ref<BenefitCenter | null>(null);
const catalog = ref<BenefitCatalogProduct[]>([]);
const history = ref<BenefitClaimHistory[]>([]);
const loggedIn = ref(Boolean(getAccessToken()));
const loading = ref(true);
const error = ref('');
const claimingSku = ref('');
const reveal = ref<{
  cardCode: string;
  productName: string;
  redemption: string | null;
} | null>(null);

const TONES = ['cover--benefit', 'cover--benefit2', 'cover--benefit3'];
const CLAIM_LABELS: Record<string, string> = {
  AVAILABLE: '待发放',
  PROCESSING: '处理中',
  SUCCESS: '已发放',
  FAILED: '已失败',
  UNKNOWN: '状态未知',
};

function toneClass(index: number): string {
  return TONES[index % TONES.length];
}

function quotaPercent(): number {
  if (!center.value?.quotaTotal) return 0;
  return Math.round((center.value.quotaRemaining / center.value.quotaTotal) * 100);
}

function claimLabel(status: null | string): string {
  return status ? (CLAIM_LABELS[status] ?? status) : '未领取';
}

function statusPill(status: string): string {
  if (status === 'SUCCESS') return 'pill pill--accent';
  if (status === 'FAILED') return 'pill pill--danger';
  if (status === 'PROCESSING' || status === 'AVAILABLE') return 'pill pill--warning';
  return 'pill';
}

function formatDate(value: null | string): string {
  if (!value) return '-';
  return new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium' }).format(new Date(value));
}

async function load(): Promise<void> {
  loggedIn.value = Boolean(getAccessToken());
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
  } catch (ex) {
    if (ex instanceof ApiError && ex.status === 401) {
      clearAccessToken();
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
    emit('login', { claimSku: product.skuCode });
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

function claimCatalog(product: BenefitCatalogProduct): void {
  emit('login', { claimSku: product.skuCode });
}

onMounted(load);
</script>

<template>
  <section class="page" aria-labelledby="benefit-title">
    <header class="page-head">
      <div>
        <p class="eyebrow">{{ loggedIn ? '权益中心' : '会员权益' }}</p>
        <h1 id="benefit-title" class="page-title">
          {{ loggedIn ? '本期权益' : '权益目录' }}
        </h1>
        <p v-if="center?.period" class="lede">
          周期 {{ center.period.periodNo }} · {{ formatDate(center.period.startAt) }} 至
          {{ formatDate(center.period.endAt) }}
        </p>
        <p v-else-if="loggedIn" class="lede">领取后请在有效期内使用，过期不结转。</p>
        <p v-else class="lede">游客可先浏览目录，登录后查看名额并领取。</p>
      </div>
    </header>

    <p v-if="loading" class="state-text">正在加载权益…</p>

    <div v-else-if="!loggedIn" class="card-list">
      <article
        v-for="(product, index) in catalog"
        :key="product.skuCode"
        class="media-card"
      >
        <div class="cover cover--compact" :class="toneClass(index)">
          <span class="cover__eyebrow">会员权益</span>
          <span class="cover__title">{{ product.name }}</span>
          <span class="cover__sub">{{ product.description }}</span>
        </div>
        <div class="card-body">
          <p class="rule-text">{{ product.rules }}</p>
          <Button block plain class="secondary-button" @click="claimCatalog(product)">
            登录后领取
          </Button>
        </div>
      </article>
    </div>

    <div v-else-if="error && !center" class="empty-state">
      <p class="form-error" role="alert">{{ error }}</p>
      <Button plain block class="secondary-button" @click="load">重新加载</Button>
    </div>

    <p v-else-if="!center?.period" class="empty-state">当前没有生效中的权益周期。</p>

    <template v-else>
      <div class="cover cover--member quota-hero">
        <span class="cover__eyebrow">本期剩余名额</span>
        <span class="cover__title quota-title">
          {{ center.quotaRemaining }} / {{ center.quotaTotal }} 可选
        </span>
        <span class="cover__sub">
          {{ formatDate(center.period.startAt) }} 至 {{ formatDate(center.period.endAt) }}
        </span>
        <span class="quota-bar" aria-hidden="true">
          <span :style="{ width: `${quotaPercent()}%` }"></span>
        </span>
      </div>

      <p v-if="error" class="form-error" role="alert">{{ error }}</p>

      <div class="card-list">
        <article
          v-for="(product, index) in center.products"
          :key="product.skuCode"
          class="media-card"
        >
          <div class="cover cover--compact" :class="toneClass(index)">
            <span class="cover__eyebrow">
              {{ product.claimed ? '已领取' : claimLabel(product.claimStatus) }}
            </span>
            <span class="cover__title">{{ product.name }}</span>
            <span class="cover__sub">{{ product.description }}</span>
            <span
              v-if="product.claimed"
              class="pill pill--accent benefit-state"
            >
              已领取
            </span>
            <span
              v-else-if="product.claimStatus === 'FAILED'"
              class="pill pill--danger benefit-state"
            >
              可重试
            </span>
          </div>
          <div class="card-body">
            <p class="rule-text">{{ product.rules }}</p>
            <p class="meta-row">
              <span v-if="product.cardMasked">卡密 {{ product.cardMasked }}</span>
              <span v-if="product.claimedAt">领取于 {{ formatDate(product.claimedAt) }}</span>
              <span v-else>当前状态：{{ claimLabel(product.claimStatus) }}</span>
            </p>
            <Button
              block
              type="primary"
              class="submit-button"
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
            </Button>
          </div>
        </article>
      </div>

      <article v-if="reveal" class="media-card" aria-live="polite">
        <div class="cover cover--benefit">
          <span class="cover__eyebrow">领取成功 · 仅展示一次</span>
          <span class="cover__title code-value">{{ reveal.cardCode }}</span>
          <span class="cover__sub">{{ reveal.productName }}</span>
        </div>
        <div class="card-body">
          <p class="card-sub">
            {{ reveal.redemption || '请按兑换说明尽快使用。' }}
          </p>
          <Button plain block class="secondary-button" @click="reveal = null">
            我已保存
          </Button>
        </div>
      </article>

      <section aria-labelledby="history-title">
        <div class="section-head">
          <h2 id="history-title" class="section-title">领取历史</h2>
        </div>
        <p v-if="history.length === 0" class="state-text">还没有领取记录。</p>
        <ul v-else class="history-list">
          <li v-for="item in history" :key="item.claimId">
            <div>
              <strong>{{ item.productName }}</strong>
              <span class="history-meta">
                {{ item.cardMasked || '-' }} · {{ formatDate(item.claimedAt) }}
              </span>
            </div>
            <span :class="statusPill(item.status)">{{ claimLabel(item.status) }}</span>
          </li>
        </ul>
      </section>
    </template>
  </section>
</template>

<style scoped>
.quota-hero {
  min-height: 168px;
}

.quota-title {
  font-size: var(--font-page);
}

.quota-bar {
  display: block;
  width: 100%;
  height: 6px;
  margin-top: var(--space-2);
  overflow: hidden;
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.1);
}

.quota-bar > span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--accent-gradient);
}

.benefit-state {
  position: absolute;
  top: var(--space-3);
  right: var(--space-3);
  z-index: 2;
}

.rule-text {
  margin: 0;
  color: var(--text-muted);
  font-size: var(--font-sub);
  line-height: 1.6;
}

.code-value {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: var(--font-hero);
  word-break: break-all;
}

.history-list {
  margin: 0;
  padding: 0;
  border-top: 1px solid var(--line);
  list-style: none;
}

.history-list li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  padding: var(--space-3) 0;
  border-bottom: 1px solid var(--line);
}

.history-list strong {
  display: block;
  font-size: var(--font-body);
  font-weight: 600;
}

.history-meta {
  display: block;
  color: var(--text-muted);
  font-size: var(--font-sub);
}
</style>
