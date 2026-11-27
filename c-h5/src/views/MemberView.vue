<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { Button } from 'vant';

import AppIcon from '../components/AppIcon.vue';
import {
  ApiError,
  clearAccessToken,
  getAccessToken,
  getInfo,
  getMember,
  logout,
  type CUserInfo,
  type MemberInfo,
} from '../api';

const emit = defineEmits<{
  login: [];
  logout: [];
  navigate: ['benefit' | 'ai' | 'tickets' | 'orders'];
}>();

const LEVELS: Record<string, string> = {
  BASIC: '基础会员',
  SILVER: '白银会员',
  GOLD: '黄金会员',
  PLATINUM: '铂金会员',
};

const user = ref<CUserInfo | null>(null);
const member = ref<MemberInfo | null>(null);
const loggedIn = ref(Boolean(getAccessToken()));
const loading = ref(true);
const error = ref('');
const loggingOut = ref(false);

function levelName(level: string): string {
  return LEVELS[level] ?? level;
}

function formatDate(value: string): string {
  return new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium' }).format(new Date(value));
}

function formatBalance(value: number): string {
  return `¥${value.toFixed(2)}`;
}

function initial(): string {
  return (user.value?.nickname || user.value?.phone || 'P').slice(0, 1);
}

async function load(): Promise<void> {
  loggedIn.value = Boolean(getAccessToken());
  if (!loggedIn.value) {
    loading.value = false;
    return;
  }
  loading.value = true;
  error.value = '';
  try {
    const [info, memberInfo] = await Promise.all([getInfo(), getMember()]);
    user.value = info;
    member.value = memberInfo;
  } catch (ex) {
    if (ex instanceof ApiError && ex.status === 401) {
      clearAccessToken();
      emit('logout');
      return;
    }
    if (ex instanceof ApiError && ex.bizCode === 'MEMBER_NOT_FOUND') {
      user.value = await getInfo();
      member.value = null;
      return;
    }
    error.value = ex instanceof Error ? ex.message : '加载失败，请稍后重试';
  } finally {
    loading.value = false;
  }
}

async function signOut(): Promise<void> {
  loggingOut.value = true;
  try {
    await logout();
  } finally {
    emit('logout');
    loggingOut.value = false;
  }
}

onMounted(load);
</script>

<template>
  <section class="page" aria-labelledby="member-title">
    <template v-if="!loggedIn">
      <header class="page-head">
        <div>
          <p class="eyebrow">我的</p>
          <h1 id="member-title" class="page-title">会员中心</h1>
          <p class="lede">登录后查看会员卡、订单、权益记录与 AI 作品。</p>
        </div>
      </header>

      <article class="media-card">
        <div class="cover cover--hero">
          <span class="cover__eyebrow">Premium Member</span>
          <span class="cover__title">把照片变成作品</span>
          <span class="cover__sub">AI 写真 · 一键换装 · 图生视频</span>
        </div>
        <div class="card-body">
          <p class="card-title">会员周期内按次使用</p>
          <p class="card-sub">AI 次数按会员周期发放，权益名额每期可领取。</p>
          <ul class="selling-list">
            <li>AI 写真、换装与图生视频，按周期发放次数</li>
            <li>每个有效周期可选择 2 项权益，卡密仅领取时展示</li>
            <li>手机号 + 密码登录，登录后回到原页面继续</li>
          </ul>
        </div>
      </article>

      <Button block type="primary" class="submit-button" @click="emit('login')">
        登录 / 注册
      </Button>
    </template>

    <template v-else>
      <header class="page-head">
        <div>
          <p class="eyebrow">我的</p>
          <h1 id="member-title" class="page-title">会员中心</h1>
        </div>
      </header>

      <article class="media-card">
        <div class="cover cover--member profile-cover">
          <span class="avatar" aria-hidden="true">{{ initial() }}</span>
          <span class="cover__eyebrow">会员中心</span>
          <h2 class="cover__title">{{ user?.nickname || '会员账号' }}</h2>
          <span class="cover__sub">{{ user?.phone || '正在读取账号' }}</span>
          <span
            v-if="member"
            class="pill profile-status"
            :class="member.status === 1 ? 'pill--accent' : 'pill--danger'"
          >
            {{ member.status === 1 ? '正常' : '停用' }}
          </span>
        </div>

        <div class="card-body">
          <p v-if="loading" class="state-text">正在加载会员信息…</p>

          <template v-else-if="error">
            <p class="form-error" role="alert">{{ error }}</p>
            <Button plain block class="secondary-button" @click="load">重新加载</Button>
          </template>

          <template v-else-if="!member">
            <p class="card-title">还没有会员档案</p>
            <p class="card-sub">该手机号还没有会员档案，请联系运营人员开通。</p>
          </template>

          <template v-else>
            <p class="member-no">会员号 {{ member.memberNo }}</p>
            <dl class="stat-grid">
              <div>
                <dt>等级</dt>
                <dd>{{ levelName(member.level) }}</dd>
              </div>
              <div>
                <dt>积分</dt>
                <dd>{{ member.points.toLocaleString('zh-CN') }}</dd>
              </div>
              <div>
                <dt>余额</dt>
                <dd>{{ formatBalance(member.balance) }}</dd>
              </div>
              <div>
                <dt>入会时间</dt>
                <dd>{{ formatDate(member.joinedAt) }}</dd>
              </div>
            </dl>
          </template>
        </div>
      </article>

      <section aria-labelledby="member-entry-title">
        <div class="section-head">
          <h2 id="member-entry-title" class="section-title">会员服务</h2>
        </div>
        <div class="list">
          <button class="list-item" type="button" @click="emit('navigate', 'benefit')">
            <span class="list-item__icon"><AppIcon name="gift" :size="20" /></span>
            <span class="list-item__body">
              <span class="list-item__title">权益中心</span>
              <span class="list-item__sub">查看本期名额与领取记录</span>
            </span>
            <span class="list-item__arrow"><AppIcon name="chevron-right" :size="18" /></span>
          </button>
          <button class="list-item" type="button" @click="emit('navigate', 'ai')">
            <span class="list-item__icon"><AppIcon name="sparkles" :size="20" /></span>
            <span class="list-item__body">
              <span class="list-item__title">AI 创作</span>
              <span class="list-item__sub">选择模板与查看作品</span>
            </span>
            <span class="list-item__arrow"><AppIcon name="chevron-right" :size="18" /></span>
          </button>
          <button class="list-item" type="button" @click="emit('navigate', 'orders')">
            <span class="list-item__icon"><AppIcon name="ticket" :size="20" /></span>
            <span class="list-item__body">
              <span class="list-item__title">我的订单</span>
              <span class="list-item__sub">查看购买记录与会员周期</span>
            </span>
            <span class="list-item__arrow"><AppIcon name="chevron-right" :size="18" /></span>
          </button>
          <button class="list-item" type="button" @click="emit('navigate', 'tickets')">
            <span class="list-item__icon"><AppIcon name="ticket" :size="20" /></span>
            <span class="list-item__body">
              <span class="list-item__title">我的工单</span>
              <span class="list-item__sub">提交问题与查看处理进度</span>
            </span>
            <span class="list-item__arrow"><AppIcon name="chevron-right" :size="18" /></span>
          </button>
        </div>
      </section>

      <Button
        plain
        block
        class="secondary-button"
        :loading="loggingOut"
        @click="signOut"
      >
        退出登录
      </Button>
    </template>
  </section>
</template>

<style scoped>
.selling-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.selling-list li {
  padding: var(--space-2) 0;
  border-bottom: 1px solid var(--line);
  color: var(--text-muted);
  font-size: var(--font-body);
}

.selling-list li:last-child {
  border-bottom: 0;
}

.profile-cover {
  padding-top: var(--space-6);
}

.avatar {
  display: grid;
  width: 56px;
  height: 56px;
  margin-bottom: var(--space-2);
  place-items: center;
  border-radius: var(--radius-pill);
  background: var(--accent-gradient);
  color: var(--accent-deep);
  font-size: var(--font-hero);
  font-weight: 700;
}

.profile-status {
  position: absolute;
  top: var(--space-4);
  right: var(--space-4);
  z-index: 2;
}

.member-no {
  margin: 0;
  color: var(--text-muted);
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: var(--font-sub);
}
</style>
