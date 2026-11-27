<script setup lang="ts">
import { onLaunch, onShow } from '@dcloudio/uni-app';

onLaunch(() => {
  console.log('Premium Member c-app launch');
});

onShow(() => {
  console.log('Premium Member c-app show');
});
</script>

<style>
page {
  background: #f7f8fa;
}
</style>
