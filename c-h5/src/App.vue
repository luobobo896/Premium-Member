<script setup lang="ts">
import { ref } from 'vue';

import LoginView from './views/LoginView.vue';
import MemberView from './views/MemberView.vue';
import { getAccessToken } from './api';

const loggedIn = ref(Boolean(getAccessToken()));
</script>

<template>
  <main class="app-shell">
    <MemberView v-if="loggedIn" @logout="loggedIn = false" />
    <LoginView v-else @logged-in="loggedIn = true" />
  </main>
</template>
