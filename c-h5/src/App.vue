<script setup lang="ts">
import { ref } from 'vue';

const count = ref(0);
</script>

<template>
  <main class="page">
    <img class="logo" src="https://vant-ui.github.io/vant/logo.png" alt="Vant" />
    <h1>Premium Member</h1>
    <p>移动端 H5 骨架已运行</p>
    <van-button type="primary" @click="count += 1">
      点我 +1（{{ count }}）
    </van-button>
  </main>
</template>

<style scoped>
.page {
  min-height: 100vh;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  align-items: center;
  justify-content: center;
  background: #f7f8fa;
}

.logo {
  width: 96px;
}

h1 {
  margin: 0;
  font-size: 28px;
}

p {
  margin: 0;
  color: #646566;
}
</style>
