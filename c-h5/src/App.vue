<script setup lang="ts">
import { ref } from 'vue';

import BenefitView from './views/BenefitView.vue';
import LoginView from './views/LoginView.vue';
import MemberView from './views/MemberView.vue';
import OrderView from './views/OrderView.vue';
import PayResultView from './views/PayResultView.vue';
import PlanView from './views/PlanView.vue';
import AiView from './views/AiView.vue';
import { getAccessToken } from './api';
import type { OrderInfo } from './api';

const loggedIn = ref(Boolean(getAccessToken()));
const activeView = ref<'ai' | 'benefit' | 'member' | 'orders' | 'payResult' | 'plans'>('member');
const currentOrder = ref<OrderInfo | null>(null);

function handleLoggedIn() {
  activeView.value = 'member';
  loggedIn.value = true;
}

function handleLogout() {
  activeView.value = 'member';
  loggedIn.value = false;
}

function handleOrdered(order: OrderInfo) {
  currentOrder.value = order;
  activeView.value = 'payResult';
}
</script>

<template>
  <main class="app-shell">
    <nav v-if="loggedIn" class="view-switch" aria-label="会员导航">
      <button
        :class="{ active: activeView === 'member' }"
        type="button"
        @click="activeView = 'member'"
      >
        会员中心
      </button>
      <button
        :class="{ active: activeView === 'benefit' }"
        type="button"
        @click="activeView = 'benefit'"
      >
        权益中心
      </button>
      <button
        :class="{ active: activeView === 'ai' }"
        type="button"
        @click="activeView = 'ai'"
      >
        AI 创作
      </button>
      <button
        :class="{ active: activeView === 'plans' || activeView === 'payResult' }"
        type="button"
        @click="activeView = 'plans'"
      >
        购买
      </button>
      <button
        :class="{ active: activeView === 'orders' }"
        type="button"
        @click="activeView = 'orders'"
      >
        订单
      </button>
    </nav>
    <MemberView v-if="loggedIn && activeView === 'member'" @logout="handleLogout" />
    <BenefitView v-else-if="loggedIn && activeView === 'benefit'" @logout="handleLogout" />
    <AiView v-else-if="loggedIn && activeView === 'ai'" @logout="handleLogout" />
    <PlanView v-else-if="loggedIn && activeView === 'plans'" @logout="handleLogout" @ordered="handleOrdered" />
    <PayResultView
      v-else-if="loggedIn && activeView === 'payResult' && currentOrder"
      :order="currentOrder"
      @logout="handleLogout"
      @orders="activeView = 'orders'"
      @plans="activeView = 'plans'"
    />
    <OrderView v-else-if="loggedIn && activeView === 'orders'" @logout="handleLogout" />
    <LoginView v-else @logged-in="handleLoggedIn" />
  </main>
</template>
