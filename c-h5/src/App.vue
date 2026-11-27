<script setup lang="ts">
import { ref } from 'vue';

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
  <main class="app-shell">
    <nav
      v-if="!['login', 'payResult'].includes(activeView)"
      class="view-switch"
      aria-label="会员导航"
    >
      <button
        :class="{ active: activeView === 'home' }"
        type="button"
        @click="navigate('home')"
      >
        首页
      </button>
      <button
        :class="{ active: activeView === 'member' }"
        type="button"
        @click="navigate('member')"
      >
        我的
      </button>
      <button
        :class="{ active: activeView === 'benefit' }"
        type="button"
        @click="navigate('benefit')"
      >
        权益
      </button>
      <button
        :class="{ active: activeView === 'ai' }"
        type="button"
        @click="navigate('ai')"
      >
        AI 模板
      </button>
      <button
        :class="{ active: activeView === 'plans' || activeView === 'payResult' }"
        type="button"
        @click="navigate('plans')"
      >
        套餐
      </button>
      <button
        :class="{ active: activeView === 'orders' }"
        type="button"
        @click="navigate('orders')"
      >
        订单
      </button>
      <button
        :class="{ active: activeView === 'tickets' }"
        type="button"
        @click="navigate('tickets')"
      >
        工单
      </button>
    </nav>
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
  </main>
</template>

<style scoped>
.view-switch button {
  padding: 0 2px;
  font-size: 13px;
  white-space: nowrap;
}
</style>
