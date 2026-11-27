<script setup lang="ts">
import { onLaunch, onShow } from '@dcloudio/uni-app';

onLaunch(() => {
  console.log('Premium Member c-app launch');
});

onShow(() => {
  console.log('Premium Member c-app show');
});
</script>

<style>
page {
  --bg: #0b0b0d;
  --surface: #141416;
  --overlay: #1c1c1f;
  --line: rgba(255, 255, 255, 0.08);
  --border: rgba(255, 255, 255, 0.06);
  --text: #f5f5f7;
  --text-muted: #9a9aa2;
  --text-disabled: #5a5a60;
  --accent: #3ddc97;
  --accent-deep: #04140d;
  --accent-soft: rgba(61, 220, 151, 0.14);
  --accent-gradient: linear-gradient(135deg, #2bd6b0, #3ddc97);
  --danger: #ff6b6b;
  --danger-soft: rgba(255, 107, 107, 0.14);
  --warning: #f5c451;
  --warning-soft: rgba(245, 196, 81, 0.14);
  --hero-a: #0d2a25;
  --hero-b: #101826;
  --member-a: #12352c;
  --member-b: #0f1b1a;
  --cover-photo-a: #123b3a;
  --cover-photo-b: #0e1e2a;
  --cover-photo-ink: #7fe3c0;
  --cover-dress-a: #2a1733;
  --cover-dress-b: #16111f;
  --cover-dress-ink: #e3b6ff;
  --cover-video-a: #332414;
  --cover-video-b: #181310;
  --cover-video-ink: #f5c451;
  --cover-benefit-a: #14332c;
  --cover-benefit-b: #101a19;
  --cover-benefit-ink: #8fe7c8;
  --cover-benefit2-a: #2b2440;
  --cover-benefit2-b: #15131c;
  --cover-benefit2-ink: #c9b8ff;
  --cover-benefit3-a: #332614;
  --cover-benefit3-b: #181410;
  --cover-benefit3-ink: #f0c98a;
  --font-page: 28px;
  --font-hero: 24px;
  --font-section: 18px;
  --font-card: 16px;
  --font-body: 14px;
  --font-sub: 12px;
  --font-number: 30px;
  --space-1: 4px;
  --space-2: 8px;
  --space-3: 12px;
  --space-4: 16px;
  --space-6: 24px;
  --space-8: 32px;
  --space-12: 48px;
  --radius-card: 16px;
  --radius-control: 12px;
  --radius-pill: 999px;
  --shadow-float: 0 12px 32px rgba(0, 0, 0, 0.45);
  --shadow-accent: 0 8px 28px rgba(61, 220, 151, 0.28);
  --ease: cubic-bezier(0.2, 0.8, 0.2, 1);
  background: var(--bg);
  color: var(--text);
  font-family: -apple-system, 'PingFang SC', 'HarmonyOS Sans SC', 'MiSans',
    'Noto Sans SC', 'Source Han Sans SC', sans-serif;
  font-size: var(--font-body);
  line-height: 1.6;
}

view,
text,
button,
input,
textarea,
image,
scroll-view,
swiper,
swiper-item {
  box-sizing: border-box;
}

button {
  margin: 0;
  padding: 0;
  border: 0;
  background: transparent;
  font-family: inherit;
  line-height: normal;
}

button::after {
  border: 0;
}

::selection {
  background: rgba(61, 220, 151, 0.3);
  color: var(--text);
}

::-webkit-scrollbar {
  width: 0;
  height: 0;
}

button:focus-visible,
input:focus-visible,
textarea:focus-visible,
.pm-tap:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}

.pm-page {
  min-height: 100vh;
  padding: calc(env(safe-area-inset-top) + var(--space-3)) var(--space-4)
    calc(92px + env(safe-area-inset-bottom));
  background: var(--bg);
  color: var(--text);
}

.pm-page--plain {
  padding-bottom: calc(var(--space-6) + env(safe-area-inset-bottom));
}

.pm-card {
  padding: var(--space-4);
  border: 1px solid var(--border);
  border-radius: var(--radius-card);
  background: var(--surface);
}

.pm-title {
  display: block;
  color: var(--text);
  font-size: var(--font-page);
  font-weight: 700;
  line-height: 1.25;
}

.pm-section {
  display: block;
  color: var(--text);
  font-size: var(--font-section);
  font-weight: 600;
  line-height: 1.4;
}

.pm-card-title {
  display: block;
  color: var(--text);
  font-size: var(--font-card);
  font-weight: 600;
  line-height: 1.4;
}

.pm-body {
  color: var(--text);
  font-size: var(--font-body);
  line-height: 1.6;
}

.pm-sub {
  display: block;
  color: var(--text-muted);
  font-size: var(--font-sub);
  line-height: 1.5;
}

.pm-number {
  color: var(--text);
  font-size: var(--font-number);
  font-variant-numeric: tabular-nums;
  font-weight: 700;
  line-height: 1.1;
}

.pm-btn {
  display: flex;
  width: 100%;
  height: 48px;
  align-items: center;
  justify-content: center;
  padding: 0 var(--space-4);
  border: 1px solid transparent;
  border-radius: var(--radius-control);
  font-size: var(--font-body);
  font-weight: 600;
  line-height: 1;
  white-space: nowrap;
  transition: opacity 180ms var(--ease), background-color 180ms var(--ease),
    border-color 180ms var(--ease);
}

.pm-btn:active {
  opacity: 0.85;
}

.pm-btn[disabled] {
  background: var(--overlay);
  color: var(--text-disabled);
  opacity: 1;
}

.pm-btn--primary {
  background: var(--accent-gradient);
  color: var(--accent-deep);
}

.pm-btn--primary[disabled] {
  background: var(--accent-gradient);
  color: var(--accent-deep);
  opacity: 0.45;
}

.pm-btn--secondary {
  border-color: var(--border);
  background: transparent;
  color: var(--text);
}

.pm-btn--ghost {
  height: 44px;
  color: var(--text-muted);
  font-weight: 400;
}

.pm-input {
  display: block;
  width: 100%;
  height: 48px;
  padding: 0 14px;
  border: 1px solid var(--border);
  border-radius: var(--radius-control);
  background: var(--surface);
  color: var(--text);
  font-family: inherit;
  font-size: var(--font-body);
  line-height: 1.2;
}

.pm-textarea {
  display: block;
  width: 100%;
  height: 120px;
  padding: var(--space-3) 14px;
  border: 1px solid var(--border);
  border-radius: var(--radius-control);
  background: var(--surface);
  color: var(--text);
  font-family: inherit;
  font-size: var(--font-body);
  line-height: 1.6;
}

.pm-placeholder {
  color: var(--text-muted);
  opacity: 1;
}

.pm-pill {
  display: inline-flex;
  height: 26px;
  align-items: center;
  padding: 0 10px;
  border-radius: var(--radius-pill);
  background: var(--overlay);
  color: var(--text-muted);
  font-size: var(--font-sub);
  font-weight: 600;
  line-height: 1;
}

.pm-pill--accent {
  background: var(--accent-soft);
  color: var(--accent);
}

.pm-pill--warning {
  background: var(--warning-soft);
  color: var(--warning);
}

.pm-pill--danger {
  background: var(--danger-soft);
  color: var(--danger);
}

.pm-skeleton {
  border-radius: var(--radius-card);
  background: var(--overlay);
  animation: pm-pulse 1.6s ease-in-out infinite;
}

.pm-notice {
  display: block;
  padding: var(--space-3);
  border-radius: var(--radius-control);
  background: var(--danger-soft);
  color: var(--danger);
  font-size: var(--font-sub);
  line-height: 1.5;
}

.pm-empty {
  padding: var(--space-8) var(--space-4);
  color: var(--text-muted);
  font-size: var(--font-body);
  line-height: 1.6;
  text-align: center;
}

@keyframes pm-pulse {
  0%,
  100% {
    opacity: 0.55;
  }
  50% {
    opacity: 1;
  }
}

@media (prefers-reduced-motion: reduce) {
  .pm-skeleton {
    animation: none;
  }

  .pm-btn {
    transition: none;
  }
}
</style>
