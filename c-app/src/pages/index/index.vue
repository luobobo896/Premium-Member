<script setup lang="ts">
import { ref } from 'vue';

const count = ref(0);
</script>

<template>
  <view class="page">
    <text class="title">Premium Member</text>
    <text class="desc">uni-app 骨架已运行</text>
    <button type="primary" @click="count += 1">点我 +1（{{ count }}）</button>
  </view>
</template>

<style>
.page {
  min-height: 100vh;
  padding: 48rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 32rpx;
}

.title {
  font-size: 48rpx;
  font-weight: 600;
}

.desc {
  font-size: 28rpx;
  color: #8f8f94;
}
</style>
