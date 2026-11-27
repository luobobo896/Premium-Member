<script setup lang="ts">
import { ref } from 'vue';

import BenefitView from './views/BenefitView.vue';
import LoginView from './views/LoginView.vue';
import MemberView from './views/MemberView.vue';
import { getAccessToken } from './api';

const loggedIn = ref(Boolean(getAccessToken()));
const activeView = ref<'benefit' | 'member'>('member');

function handleLoggedIn() {
  activeView.value = 'member';
  loggedIn.value = true;
}

function handleLogout() {
  activeView.value = 'member';
  loggedIn.value = false;
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
    </nav>
    <MemberView v-if="loggedIn && activeView === 'member'" @logout="handleLogout" />
    <BenefitView v-else-if="loggedIn" @logout="handleLogout" />
    <LoginView v-else @logged-in="handleLoggedIn" />
  </main>
</template>
