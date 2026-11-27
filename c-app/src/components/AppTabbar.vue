<script setup lang="ts">
import AppIcon from './AppIcon.vue';

type TabKey = 'index' | 'ai' | 'benefit' | 'member';

const props = defineProps<{ active: TabKey }>();

const items: Array<{ key: TabKey; label: string; icon: 'home' | 'gift' | 'user' | 'sparkles'; url: string }> = [
  { key: 'index', label: '首页', icon: 'home', url: '/pages/index/index' },
  { key: 'ai', label: '创作', icon: 'sparkles', url: '/pages/ai/ai' },
  { key: 'benefit', label: '权益', icon: 'gift', url: '/pages/benefit/benefit' },
  { key: 'member', label: '我的', icon: 'user', url: '/pages/member/member' },
];

function go(item: (typeof items)[number]): void {
  if (item.key === props.active) return;
  uni.reLaunch({ url: item.url });
}
</script>

<template>
  <view class="tabbar">
    <view
      v-for="item in items"
      :key="item.key"
      class="tab"
      :class="{ 'tab--active': item.key === active, 'tab--center': item.key === 'ai' }"
      :aria-label="item.label"
      @click="go(item)"
    >
      <view v-if="item.key === 'ai'" class="fab">
        <AppIcon name="plus" :size="26" />
      </view>
      <view v-else class="tab-icon"><AppIcon :name="item.icon" :size="24" /></view>
      <text class="tab-label">{{ item.label }}</text>
    </view>
  </view>
</template>

<style scoped>
.tabbar {
  position: fixed;
  z-index: 30;
  right: 0;
  bottom: 0;
  left: 0;
  display: flex;
  height: calc(64px + env(safe-area-inset-bottom));
  align-items: flex-start;
  padding: var(--space-2) var(--space-4) env(safe-area-inset-bottom);
  border-top: 1px solid var(--line);
  background: rgba(20, 20, 22, 0.94);
  box-shadow: var(--shadow-float);
  backdrop-filter: blur(18px);
}

.tab {
  display: flex;
  flex: 1;
  min-height: 48px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  color: var(--text-muted);
}

.tab-icon {
  display: flex;
  height: 26px;
  align-items: center;
  justify-content: center;
}

.tab-label {
  font-size: var(--font-sub);
  line-height: 1.2;
}

.tab--active {
  color: var(--accent);
}

.tab--center {
  position: relative;
}

.fab {
  display: flex;
  width: 56px;
  height: 56px;
  align-items: center;
  justify-content: center;
  margin-top: -26px;
  border-radius: var(--radius-pill);
  background: var(--accent-gradient);
  box-shadow: var(--shadow-accent);
  color: var(--accent-deep);
}

.tab--center .tab-label {
  margin-top: 1px;
}
</style>
