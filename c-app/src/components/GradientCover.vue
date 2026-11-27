<script setup lang="ts">
withDefaults(
  defineProps<{
    label: string;
    tone?: string;
    height?: number;
    caption?: string;
  }>(),
  { tone: 'photo', height: 180 },
);
</script>

<template>
  <view class="cover" :class="`cover--${tone}`" :style="{ height: `${height}px` }">
    <text class="cover-label" :class="{ 'cover-label--captioned': caption }">{{ label }}</text>
    <view v-if="caption" class="caption">
      <text class="caption-text">{{ caption }}</text>
    </view>
  </view>
</template>

<style scoped>
.cover {
  position: relative;
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: var(--radius-card);
}

.cover::after {
  position: absolute;
  top: -30%;
  right: -20%;
  width: 80%;
  height: 80%;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.16), transparent 68%);
  content: '';
}

.cover-label {
  position: relative;
  z-index: 1;
  color: var(--cover-ink, var(--text));
  font-size: 30px;
  font-weight: 700;
  letter-spacing: 2px;
  line-height: 1.1;
  text-align: center;
}

.cover-label--captioned {
  margin-bottom: 12px;
  font-size: 22px;
  letter-spacing: 1px;
  opacity: 0.55;
}

.caption {
  position: absolute;
  z-index: 2;
  right: var(--space-1);
  bottom: var(--space-1);
  left: var(--space-1);
}

.caption-text {
  display: block;
  overflow: hidden;
  padding: 3px;
  border-radius: var(--radius-pill);
  background: rgba(0, 0, 0, 0.45);
  color: var(--text);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: -0.5px;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cover--photo {
  --cover-ink: var(--cover-photo-ink);
  background: linear-gradient(160deg, var(--cover-photo-a), var(--cover-photo-b));
}

.cover--dress {
  --cover-ink: var(--cover-dress-ink);
  background: linear-gradient(160deg, var(--cover-dress-a), var(--cover-dress-b));
}

.cover--video {
  --cover-ink: var(--cover-video-ink);
  background: linear-gradient(160deg, var(--cover-video-a), var(--cover-video-b));
}

.cover--benefit {
  --cover-ink: var(--cover-benefit-ink);
  background: linear-gradient(160deg, var(--cover-benefit-a), var(--cover-benefit-b));
}

.cover--benefit2 {
  --cover-ink: var(--cover-benefit2-ink);
  background: linear-gradient(160deg, var(--cover-benefit2-a), var(--cover-benefit2-b));
}

.cover--benefit3 {
  --cover-ink: var(--cover-benefit3-ink);
  background: linear-gradient(160deg, var(--cover-benefit3-a), var(--cover-benefit3-b));
}
</style>
