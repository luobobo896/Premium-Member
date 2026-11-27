<script setup lang="ts">
import { computed, ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';

import {
  ApiError,
  clearToken,
  getAiQuota,
  getAiTemplates,
  getBenefits,
  getContents,
  getToken,
  type AiQuota,
  type AiTemplate,
  type AiType,
  type BenefitCenter,
} from '../../api';
import AppIcon from '../../components/AppIcon.vue';
import AppTabbar from '../../components/AppTabbar.vue';
import GradientCover from '../../components/GradientCover.vue';

interface BannerItem {
  imageUrl?: string;
  title?: string;
}

const featureTiles: Array<{
  label: string;
  type: AiType;
  icon: 'image' | 'shirt' | 'video';
}> = [
  { label: 'AI 写真', type: 'PHOTO', icon: 'image' },
  { label: '一键换装', type: 'DRESSUP', icon: 'shirt' },
  { label: '图生视频', type: 'VIDEO', icon: 'video' },
];

const loggedIn = ref(false);
const promo = ref<BannerItem>({ title: '会员专属权益' });
const quota = ref<AiQuota | null>(null);
const templates = ref<AiTemplate[]>([]);
const benefitCenter = ref<BenefitCenter | null>(null);
const loading = ref(true);

const featured = computed(() => templates.value.slice(0, 8));
const availableBenefits = computed(() => benefitCenter.value?.quotaRemaining ?? 0);

function coverLabel(template: AiTemplate): string {
  return template.category.length <= 4 ? template.category : template.name.slice(0, 2);
}

function coverTone(type: AiType): string {
  if (type === 'DRESSUP') return 'dress';
  if (type === 'VIDEO') return 'video';
  return 'photo';
}

function openType(type: AiType): void {
  uni.setStorageSync('pm_ai_type', type);
  uni.removeStorageSync('pm_ai_template_id');
  uni.reLaunch({ url: '/pages/ai/ai' });
}

function openTemplate(template: AiTemplate): void {
  uni.setStorageSync('pm_ai_type', template.type);
  uni.setStorageSync('pm_ai_template_id', template.id);
  uni.reLaunch({ url: '/pages/ai/ai' });
}

function openBenefits(): void {
  uni.reLaunch({ url: '/pages/benefit/benefit' });
}

function openStatus(): void {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/member/member' });
    return;
  }
  uni.reLaunch({ url: '/pages/member/member' });
}

async function load(): Promise<void> {
  loggedIn.value = Boolean(getToken());
  loading.value = true;

  try {
    const contents = await getContents(['BANNER']);
    const item = contents.find((row) => row.key === 'BANNER');
    if (item) {
      const config = JSON.parse(item.contentJson) as { items?: BannerItem[] };
      if (config.items?.length) promo.value = config.items[0];
    }
  } catch {
    // 内容未配置时保留默认卡位。
  }

  templates.value = await getAiTemplates().catch(() => [] as AiTemplate[]);

  if (!loggedIn.value) {
    quota.value = null;
    benefitCenter.value = null;
    loading.value = false;
    return;
  }

  const [quotaData, benefitData] = await Promise.all([
    getAiQuota().catch((ex) => {
      if (ex instanceof ApiError && ex.statusCode === 401) {
        clearToken();
        loggedIn.value = false;
      }
      return null;
    }),
    getBenefits().catch(() => null),
  ]);
  quota.value = quotaData;
  benefitCenter.value = benefitData;
  loading.value = false;
}

onShow(load);
</script>

<template>
  <view class="pm-page">
    <text class="pm-title">AI 创作</text>

    <view v-if="loading" class="pm-skeleton status-skeleton"></view>
    <view v-else class="status pm-card pm-tap" @click="openStatus">
      <view class="status-left">
        <AppIcon name="crown" :size="18" />
        <text class="status-title">{{ loggedIn ? '创作会员' : 'AI 创作' }}</text>
      </view>
      <view class="status-right">
        <text v-if="loggedIn && quota?.period" class="pm-sub">
          图片剩余 <text class="status-num">{{ quota.image.remaining }}</text> · 视频剩余
          <text class="status-num">{{ quota.video.remaining }}</text>
        </text>
        <text v-else-if="loggedIn" class="pm-sub">未开通本期权益</text>
        <text v-else class="pm-sub">登录后查看</text>
        <AppIcon name="chevron-right" :size="16" />
      </view>
    </view>

    <view class="promo pm-tap" @click="openBenefits">
      <image v-if="promo.imageUrl" class="promo-image" mode="aspectFill" :src="promo.imageUrl" />
      <view class="promo-shade"></view>
      <view class="promo-copy">
        <text class="promo-title">{{ promo.title || '会员专属权益' }}</text>
        <text class="promo-sub">Premium Member</text>
      </view>
    </view>

    <view class="features">
      <view
        v-for="tile in featureTiles"
        :key="tile.type"
        class="feature pm-tap"
        @click="openType(tile.type)"
      >
        <view class="feature-icon"><AppIcon :name="tile.icon" :size="22" /></view>
        <text class="feature-label">{{ tile.label }}</text>
      </view>
    </view>

    <view class="section-head">
      <text class="pm-section">精选模板</text>
      <text v-if="featured.length" class="pm-sub">{{ featured.length }} 个</text>
    </view>

    <view v-if="loading" class="template-grid">
      <view v-for="index in 8" :key="index" class="pm-skeleton tpl-skeleton"></view>
    </view>

    <view v-else-if="featured.length" class="template-grid">
      <view
        v-for="template in featured"
        :key="template.id"
        class="tpl pm-tap"
        @click="openTemplate(template)"
      >
        <GradientCover
          :label="coverLabel(template)"
          :caption="template.name"
          :tone="coverTone(template.type)"
          :height="104"
        />
      </view>
    </view>

    <view v-else class="pm-empty">暂无模板</view>

    <view class="pm-card benefit-row pm-tap" @click="openBenefits">
      <AppIcon name="crown" :size="20" />
      <text class="benefit-title">会员权益</text>
      <text class="pm-sub benefit-count">
        {{
          !loggedIn
            ? '登录后领取'
            : benefitCenter?.period
              ? `本期可选 ${availableBenefits} 项`
              : '暂无可用权益'
        }}
      </text>
      <AppIcon name="chevron-right" :size="16" />
    </view>

    <AppTabbar active="index" />
  </view>
</template>

<style scoped>
.status-skeleton {
  height: 48px;
  margin-top: var(--space-4);
}

.status {
  display: flex;
  height: 48px;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  margin-top: var(--space-4);
}

.status-left {
  display: flex;
  flex: none;
  align-items: center;
  gap: var(--space-2);
  color: var(--accent);
}

.status-title {
  color: var(--text);
  font-size: var(--font-body);
  font-weight: 600;
}

.status-right {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: var(--space-1);
  color: var(--text-muted);
}

.status-right .pm-sub {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-num {
  color: var(--accent);
  font-weight: 600;
}

.promo {
  position: relative;
  height: 148px;
  margin-top: var(--space-3);
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: var(--radius-card);
  background: linear-gradient(135deg, var(--hero-a), var(--hero-b));
}

.promo::before {
  position: absolute;
  top: -80px;
  right: -40px;
  width: 220px;
  height: 220px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(61, 220, 151, 0.28), transparent 66%);
  content: '';
}

.promo-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

.promo-shade {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  background: linear-gradient(90deg, rgba(5, 10, 9, 0.8), rgba(5, 10, 9, 0.15));
}

.promo-copy {
  position: absolute;
  right: var(--space-4);
  bottom: var(--space-4);
  left: var(--space-4);
  z-index: 1;
}

.promo-title {
  display: block;
  color: var(--text);
  font-size: 22px;
  font-weight: 600;
  line-height: 1.3;
}

.promo-sub {
  display: block;
  margin-top: var(--space-1);
  color: rgba(245, 245, 247, 0.7);
  font-size: var(--font-sub);
}

.features {
  display: flex;
  gap: var(--space-3);
  margin-top: var(--space-3);
}

.feature {
  display: flex;
  flex: 1;
  min-height: 92px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  border: 1px solid var(--border);
  border-radius: var(--radius-card);
  background: var(--surface);
}

.feature-icon {
  display: flex;
  width: 38px;
  height: 38px;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-pill);
  background: var(--accent-soft);
  color: var(--accent);
}

.feature-label {
  color: var(--text);
  font-size: var(--font-body);
  font-weight: 600;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: var(--space-6) 0 var(--space-3);
}

.template-grid {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.tpl {
  width: calc(25% - 6px);
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: var(--radius-control);
  background: var(--surface);
}

.tpl-skeleton {
  width: calc(25% - 6px);
  height: 112px;
  border-radius: var(--radius-control);
}

.benefit-row {
  display: flex;
  height: 56px;
  align-items: center;
  gap: var(--space-2);
  margin-top: var(--space-6);
  color: var(--accent);
}

.benefit-title {
  color: var(--text);
  font-size: var(--font-body);
  font-weight: 600;
}

.benefit-count {
  flex: 1;
  text-align: right;
}

.empty-login {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  align-items: stretch;
  text-align: center;
}
</style>
