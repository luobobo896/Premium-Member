<script lang="ts" setup>
import { useRoute } from 'vue-router';

const route = useRoute();
</script>

<template>
  <div class="p-6">
    <h2 class="text-lg font-medium">{{ route.meta.title }}</h2>
    <p class="mt-2 text-sm text-gray-500">
      该菜单已在后端配置，页面功能待实现；当前占位以保持菜单结构完整。
    </p>
  </div>
</template>
