<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { Button, Tag } from 'vant';

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
  <section class="surface benefit-surface" aria-labelledby="benefit-title">
    <header class="member-header">
      <div>
        <p class="eyebrow">{{ loggedIn ? '权益中心' : '会员权益' }}</p>
        <h1 id="benefit-title">{{ loggedIn ? '本期权益' : '权益目录' }}</h1>
        <p v-if="center?.period" class="lede">
          周期 {{ center.period.periodNo }} · {{ formatDate(center.period.startAt) }} 至
          {{ formatDate(center.period.endAt) }}
        </p>
        <p v-else-if="loggedIn" class="lede">领取后请在有效期内使用，过期不结转。</p>
        <p v-else class="lede">游客可先浏览目录，登录后查看名额并领取。</p>
      </div>
      <span v-if="loggedIn && center?.period" class="status-pill">
        剩余 {{ center.quotaRemaining }}/{{ center.quotaTotal }}
      </span>
    </header>

    <p v-if="loading" class="state-text">正在加载权益…</p>

    <div v-else-if="!loggedIn" class="benefit-list">
      <article v-for="product in catalog" :key="product.skuCode" class="benefit-card">
        <span class="catalog-cover" :class="product.cover">
          {{ product.name.slice(0, 1) }}
        </span>
        <div class="benefit-card-head">
          <div>
            <h2>{{ product.name }}</h2>
            <p class="benefit-desc">{{ product.description }}</p>
          </div>
        </div>
        <p class="benefit-rules">{{ product.rules }}</p>
        <Button block plain class="secondary-button" @click="claimCatalog(product)">
          登录后领取
        </Button>
      </article>
    </div>

    <div v-else-if="error && !center" class="state-block">
      <p class="form-error" role="alert">{{ error }}</p>
      <Button plain block class="secondary-button" @click="load">重新加载</Button>
    </div>

    <div v-else-if="!center?.period" class="state-block">
      <p class="state-text">当前没有生效中的权益周期。</p>
    </div>

    <template v-else>
      <p v-if="error" class="form-error" role="alert">{{ error }}</p>

      <div class="benefit-list">
        <article v-for="product in center.products" :key="product.skuCode" class="benefit-card">
          <div class="benefit-card-head">
            <div>
              <h2>{{ product.name }}</h2>
              <p class="benefit-desc">{{ product.description }}</p>
            </div>
            <Tag v-if="product.claimed" type="success">已领取</Tag>
            <Tag v-else-if="product.claimStatus === 'FAILED'" type="danger">可重试</Tag>
          </div>
          <p class="benefit-rules">{{ product.rules }}</p>
          <div class="benefit-meta">
            <span v-if="product.cardMasked">卡密 {{ product.cardMasked }}</span>
            <span v-if="product.claimedAt">领取于 {{ formatDate(product.claimedAt) }}</span>
            <span v-else>当前状态：{{ claimLabel(product.claimStatus) }}</span>
          </div>
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
        </article>
      </div>

      <section v-if="reveal" class="code-reveal" aria-live="polite">
        <p class="eyebrow">领取成功 · 仅展示一次</p>
        <p class="code-value">{{ reveal.cardCode }}</p>
        <p class="code-note">
          {{ reveal.productName }}：{{ reveal.redemption || '请按兑换说明尽快使用。' }}
        </p>
        <Button plain block class="secondary-button" @click="reveal = null">我已保存</Button>
      </section>

      <section class="history-section">
        <h2>领取历史</h2>
        <p v-if="history.length === 0" class="state-text">还没有领取记录。</p>
        <ul v-else class="history-list">
          <li v-for="item in history" :key="item.claimId">
            <div>
              <strong>{{ item.productName }}</strong>
              <span class="history-meta">
                {{ item.cardMasked || '-' }} · {{ formatDate(item.claimedAt) }}
              </span>
            </div>
            <Tag :type="item.status === 'SUCCESS' ? 'success' : 'default'">
              {{ claimLabel(item.status) }}
            </Tag>
          </li>
        </ul>
      </section>
    </template>
  </section>
</template>

<style scoped>
.catalog-cover {
  display: grid;
  width: 100%;
  height: 96px;
  margin-bottom: 12px;
  place-items: center;
  border-radius: 10px;
  color: var(--c-surface);
  font-size: 30px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.catalog-cover.benefit {
  background: linear-gradient(160deg, var(--c-cover-1-a), var(--c-cover-1-b));
}

.catalog-cover.benefit2 {
  background: linear-gradient(160deg, var(--c-cover-2-a), var(--c-cover-2-b));
}

.catalog-cover.benefit3 {
  background: linear-gradient(160deg, var(--c-cover-3-a), var(--c-cover-3-b));
}

.benefit-card .secondary-button {
  margin-top: 14px;
}
</style>
