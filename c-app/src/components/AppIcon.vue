<script setup lang="ts">
const PATHS = {
  home: ['M3 10.5 12 3l9 7.5V21a1 1 0 0 1-1 1h-5v-7H9v7H4a1 1 0 0 1-1-1z'],
  sparkles: [
    'M12 3l1.9 5.1L19 10l-5.1 1.9L12 17l-1.9-5.1L5 10l5.1-1.9z',
    'M19 16l.7 1.8L21.5 18.5l-1.8.7L19 21l-.7-1.8-1.8-.7 1.8-.7z',
  ],
  gift: [
    'M20 12v9H4v-9',
    'M2 7h20v5H2z',
    'M12 7v14',
    'M12 7H7.5a2.5 2.5 0 0 1 0-5C11 2 12 7 12 7z',
    'M12 7h4.5a2.5 2.5 0 0 0 0-5C13 2 12 7 12 7z',
  ],
  user: ['M20 21a8 8 0 0 0-16 0', 'M12 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8'],
  plus: ['M12 5v14', 'M5 12h14'],
  'arrow-left': ['M19 12H5', 'M12 19l-7-7 7-7'],
  'chevron-right': ['M9 18l6-6-6-6'],
  ticket: [
    'M3 8a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v2a2 2 0 0 0 0 4v2a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-2a2 2 0 0 0 0-4z',
    'M13 6v2',
    'M13 11v2',
    'M13 16v2',
  ],
  logout: ['M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4', 'M16 17l5-5-5-5', 'M21 12H9'],
  image: ['M3 5h18v14H3z', 'M8.5 10a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3', 'M21 15l-5-5L5 21'],
  video: ['M3 6h13v12H3z', 'M16 10l5-3v10l-5-3z'],
  shirt: [
    'M20.4 6.4 16 3a4 4 0 0 1-8 0L3.6 6.4a2 2 0 0 0-.6 1.4V10l4 1v10h10V11l4-1V7.8a2 2 0 0 0-.6-1.4z',
  ],
  crown: ['M2 4l3 12h14l3-12-6 7-4-7-4 7z', 'M5 20h14'],
  check: ['M20 6 9 17l-5-5'],
  close: ['M18 6 6 18', 'M6 6l12 12'],
} as const;

withDefaults(
  defineProps<{
    name: keyof typeof PATHS;
    size?: number;
  }>(),
  { size: 24 },
);
</script>

<template>
  <svg
    :width="size"
    :height="size"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    stroke-width="1.5"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
  >
    <path v-for="(path, index) in PATHS[name]" :key="index" :d="path" />
  </svg>
</template>
