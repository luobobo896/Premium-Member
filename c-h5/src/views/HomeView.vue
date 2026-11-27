<script setup lang="ts">
import { Button } from 'vant';

import AppIcon from '../components/AppIcon.vue';

defineProps<{ loggedIn: boolean }>();
const emit = defineEmits<{
  login: [];
  navigate: [view: 'ai' | 'benefit' | 'member' | 'plans'];
}>();

const features = [
  { label: 'AI 写真', sub: '照片生成', tone: 'cover--photo', icon: 'image' },
  { label: '一键换装', sub: '服装替换', tone: 'cover--dress', icon: 'shirt' },
  { label: '图生视频', sub: '静态转动态', tone: 'cover--video', icon: 'video' },
] as const;
</script>

<template>
  <section class="page" aria-labelledby="home-title">
    <header class="page-head">
      <div>
        <p class="eyebrow">Premium Member</p>
        <h1 id="home-title" class="page-title">AI 创作</h1>
        <p class="lede">先看目录，再决定要不要开通。套餐、权益与模板都可以游客浏览。</p>
      </div>
    </header>

    <button class="media-card home-hero" type="button" @click="emit('navigate', 'plans')">
      <span class="cover cover--hero">
        <span class="cover__eyebrow">会员套餐</span>
        <span class="cover__title">支付成功后立即开通</span>
        <span class="cover__sub">周期按服务端时间计算 · 到期不自动续费</span>
        <span class="pill pill--accent home-hero__pill">查看套餐</span>
      </span>
    </button>

    <section aria-labelledby="feature-title">
      <div class="section-head">
        <h2 id="feature-title" class="section-title">创作能力</h2>
        <p class="section-sub">按会员周期发放次数</p>
      </div>
      <div class="feature-grid">
        <button
          v-for="feature in features"
          :key="feature.label"
          class="media-card feature-card"
          type="button"
          @click="emit('navigate', 'ai')"
        >
          <span class="cover feature-cover" :class="feature.tone">
            <span class="cover__eyebrow"><AppIcon :name="feature.icon" :size="18" /></span>
            <span class="cover__title">{{ feature.label }}</span>
            <span class="cover__sub">{{ feature.sub }}</span>
          </span>
        </button>
      </div>
    </section>

    <section aria-labelledby="entry-title">
      <div class="section-head">
        <h2 id="entry-title" class="section-title">继续了解</h2>
      </div>
      <div class="action-stack">
        <Button plain block class="secondary-button" @click="emit('navigate', 'benefit')">
          浏览权益目录
        </Button>
        <Button v-if="!loggedIn" plain block class="secondary-button" @click="emit('login')">
          登录 / 注册
        </Button>
        <Button v-else plain block class="secondary-button" @click="emit('navigate', 'member')">
          进入会员中心
        </Button>
      </div>
    </section>
  </section>
</template>

<style scoped>
.home-hero {
  text-align: left;
}

.home-hero__pill {
  margin-top: var(--space-2);
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-2);
}

.feature-card {
  min-width: 0;
}

.feature-cover {
  min-height: 148px;
  padding: var(--space-3);
}

.feature-cover .cover__title {
  font-size: var(--font-card);
}

.feature-cover .cover__sub {
  white-space: normal;
}
</style>
