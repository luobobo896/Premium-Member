<script setup lang="ts">
import { Button } from 'vant';

defineProps<{ loggedIn: boolean }>();
const emit = defineEmits<{
  login: [];
  navigate: [view: 'ai' | 'benefit' | 'member' | 'plans'];
}>();
</script>

<template>
  <section class="surface home-surface" aria-labelledby="home-title">
    <p class="eyebrow">Premium Member</p>
    <h1 id="home-title">先看目录，再决定要不要开通</h1>
    <p class="lede">
      套餐、权益目录与 AI 模板都可以游客浏览；购买、领取和我的记录需要登录。
    </p>

    <ul class="home-points">
      <li>AI 写真、换装与图生视频，按会员周期发放次数</li>
      <li>每个有效周期可选 2 项权益，领取后卡密仅展示一次</li>
      <li>手机号 + 密码登录，登录后回到刚才的页面继续</li>
    </ul>

    <div class="home-actions">
      <Button type="primary" block class="submit-button" @click="emit('navigate', 'plans')">
        查看会员套餐
      </Button>
      <Button plain block class="secondary-button" @click="emit('navigate', 'benefit')">
        浏览权益目录
      </Button>
      <Button plain block class="secondary-button" @click="emit('navigate', 'ai')">
        查看 AI 模板
      </Button>
      <Button v-if="!loggedIn" plain block class="secondary-button" @click="emit('login')">
        登录 / 注册
      </Button>
      <Button v-else plain block class="secondary-button" @click="emit('navigate', 'member')">
        进入会员中心
      </Button>
    </div>
  </section>
</template>

<style scoped>
.home-surface {
  max-width: 480px;
  padding-top: 40px;
}

.home-surface h1 {
  margin: 0;
  font-size: 30px;
  font-weight: 600;
  line-height: 1.3;
}

.home-points {
  margin: 24px 0 0;
  padding: 0;
  list-style: none;
}

.home-points li {
  padding: 12px 0;
  border-bottom: 1px solid var(--c-border);
  color: var(--c-muted);
  font-size: 14px;
}

.home-points li:last-child {
  border-bottom: 0;
}

.home-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 24px;
}

.home-actions .submit-button,
.home-actions .secondary-button {
  margin-top: 0;
}
</style>
