<script setup lang="ts">
import { computed, ref } from 'vue';

import AppIcon from './components/AppIcon.vue';
import BenefitView from './views/BenefitView.vue';
import HomeView from './views/HomeView.vue';
import LoginView from './views/LoginView.vue';
import MemberView from './views/MemberView.vue';
import OrderView from './views/OrderView.vue';
import PayResultView from './views/PayResultView.vue';
import PlanView from './views/PlanView.vue';
import AiView from './views/AiView.vue';
import TicketView from './views/TicketView.vue';
import { consumeLoginRedirect, getAccessToken, setLoginIntent } from './api';
import type { OrderInfo } from './api';

const loggedIn = ref(Boolean(getAccessToken()));
type View =
  | 'ai'
  | 'benefit'
  | 'home'
  | 'login'
  | 'member'
  | 'orders'
  | 'payResult'
  | 'plans'
  | 'tickets';
const activeView = ref<View>('home');
const currentOrder = ref<OrderInfo | null>(null);

const tabs = [
  { key: 'home', label: '首页', icon: 'home' },
  { key: 'ai', label: '创作', icon: 'sparkles' },
  { key: 'benefit', label: '权益', icon: 'gift' },
  { key: 'member', label: '我的', icon: 'user' },
] as const;

const showTabs = computed(() => !['login', 'payResult'].includes(activeView.value));
const activeTab = computed(() => {
  if (activeView.value === 'orders' || activeView.value === 'tickets') return 'member';
  return activeView.value;
});

function requestLogin(redirect: View, action?: { claimSku?: string; planId?: number }) {
  setLoginIntent(redirect, action);
  activeView.value = 'login';
}

function navigate(view: View) {
  if (!loggedIn.value && (view === 'orders' || view === 'tickets')) {
    requestLogin(view);
    return;
  }
  activeView.value = view;
}

function handleLoggedIn() {
  loggedIn.value = true;
  const redirect = (consumeLoginRedirect() || 'member') as View;
  activeView.value = redirect === 'payResult' ? 'member' : redirect;
}

function handleLogout() {
  loggedIn.value = false;
  activeView.value = 'home';
}

function handleOrdered(order: OrderInfo) {
  currentOrder.value = order;
  activeView.value = 'payResult';
}
</script>

<template>
  <main class="app-shell" :class="{ 'app-shell--plain': !showTabs }">
    <HomeView
      v-if="activeView === 'home'"
      :logged-in="loggedIn"
      @login="requestLogin('member')"
      @navigate="navigate"
    />
    <MemberView
      v-else-if="activeView === 'member'"
      @login="requestLogin('member')"
      @logout="handleLogout"
      @navigate="navigate"
    />
    <BenefitView
      v-else-if="activeView === 'benefit'"
      @login="requestLogin('benefit', $event)"
      @logout="handleLogout"
    />
    <AiView
      v-else-if="activeView === 'ai'"
      @login="requestLogin('ai')"
      @logout="handleLogout"
    />
    <PlanView
      v-else-if="activeView === 'plans'"
      @login="requestLogin('plans', $event)"
      @logout="handleLogout"
      @ordered="handleOrdered"
    />
    <PayResultView
      v-else-if="loggedIn && activeView === 'payResult' && currentOrder"
      :order="currentOrder"
      @logout="handleLogout"
      @orders="navigate('orders')"
      @plans="navigate('plans')"
    />
    <OrderView v-else-if="activeView === 'orders' && loggedIn" @logout="handleLogout" />
    <TicketView v-else-if="activeView === 'tickets' && loggedIn" @logout="handleLogout" />
    <LoginView v-else-if="activeView === 'login'" @logged-in="handleLoggedIn" />
    <HomeView v-else :logged-in="loggedIn" @login="requestLogin('member')" @navigate="navigate" />

    <nav v-if="showTabs" class="tabbar" aria-label="主导航">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        :class="{ 'tabbar__item--active': activeTab === tab.key }"
        class="tabbar__item"
        type="button"
        :aria-label="tab.label"
        :aria-current="activeTab === tab.key ? 'page' : undefined"
        @click="navigate(tab.key)"
      >
        <span
          class="tabbar__icon"
          :class="{ 'tabbar__icon--fab': tab.key === 'ai' }"
        >
          <AppIcon :name="tab.icon" :size="tab.key === 'ai' ? 24 : 22" />
        </span>
        <span class="tabbar__label">{{ tab.label }}</span>
      </button>
    </nav>
  </main>
</template>
