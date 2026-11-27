<script setup lang="ts">
import { computed, ref } from 'vue';
import { onShow, onUnload } from '@dcloudio/uni-app';

import {
  ApiError,
  assetUrl,
  clearToken,
  createAiTask,
  deleteAiWork,
  getAiQuota,
  getAiTask,
  getAiTasks,
  getAiTemplates,
  getToken,
  uploadAiAsset,
  type AiQuota,
  type AiTask,
  type AiTemplate,
  type AiType,
} from '../../api';

const typeTabs: Array<{ label: string; value: AiType }> = [
  { label: '照片', value: 'PHOTO' },
  { label: '换装', value: 'DRESSUP' },
  { label: '视频', value: 'VIDEO' },
];
const terminal = new Set(['SUCCESS', 'FAILED', 'REJECTED', 'UNKNOWN']);

const quota = ref<AiQuota | null>(null);
const templates = ref<AiTemplate[]>([]);
const tasks = ref<AiTask[]>([]);
const activeType = ref<AiType>('PHOTO');
const selected = ref<AiTemplate | null>(null);
const inputPreview = ref('');
const currentTask = ref<AiTask | null>(null);
const loading = ref(true);
const uploading = ref(false);
const submitting = ref(false);
const error = ref('');
const uploadedAssetId = ref(0);
let pollTimer: number | undefined;

const visibleTemplates = computed(() =>
  templates.value.filter((item) => item.type === activeType.value),
);
const works = computed(() => tasks.value.filter((item) => item.outputUrl));

function handleError(ex: unknown) {
  if (ex instanceof ApiError && ex.statusCode === 401) {
    clearToken();
    uni.reLaunch({ url: '/pages/login/login' });
    return;
  }
  error.value = ex instanceof Error ? ex.message : '操作失败';
}

async function load() {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/login/login' });
    return;
  }
  loading.value = true;
  error.value = '';
  try {
    const [quotaData, templateData, taskData] = await Promise.all([
      getAiQuota(),
      getAiTemplates(),
      getAiTasks(),
    ]);
    quota.value = quotaData;
    templates.value = templateData;
    tasks.value = taskData;
  } catch (ex) {
    handleError(ex);
  } finally {
    loading.value = false;
  }
}

async function refreshQuotaAndTasks() {
  const [quotaData, taskData] = await Promise.all([getAiQuota(), getAiTasks()]);
  quota.value = quotaData;
  tasks.value = taskData;
}

function selectType(type: AiType) {
  activeType.value = type;
  selected.value = null;
  inputPreview.value = '';
  uploadedAssetId.value = 0;
}

async function onPicked(path: string) {
  inputPreview.value = path;
  uploadedAssetId.value = 0;
  uploading.value = true;
  error.value = '';
  try {
    const asset = await uploadAiAsset(path);
    uploadedAssetId.value = asset.id;
  } catch (ex) {
    handleError(ex);
    inputPreview.value = '';
  } finally {
    uploading.value = false;
  }
}

function chooseFile() {
  if (activeType.value === 'VIDEO') {
    uni.chooseVideo({
      sourceType: ['album', 'camera'],
      success: (result) => onPicked(result.tempFilePath),
    });
    return;
  }
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: (result) => onPicked(result.tempFilePaths[0]),
  });
}

async function submit() {
  if (!selected.value || !inputPreview.value || submitting.value) return;
  submitting.value = true;
  error.value = '';
  try {
    if (!uploadedAssetId.value) {
      const asset = await uploadAiAsset(inputPreview.value);
      uploadedAssetId.value = asset.id;
    }
    const key = `app-${Date.now()}-${Math.random().toString(16).slice(2)}`;
    currentTask.value = await createAiTask({
      idempotencyKey: key,
      inputAssetId: uploadedAssetId.value,
      templateId: selected.value.id,
    });
    await refreshQuotaAndTasks();
    poll(currentTask.value.taskNo);
  } catch (ex) {
    handleError(ex);
  } finally {
    submitting.value = false;
  }
}

function poll(taskNo: string) {
  if (pollTimer) clearTimeout(pollTimer);
  pollTimer = setTimeout(async () => {
    try {
      const task = await getAiTask(taskNo);
      currentTask.value = task;
      if (!terminal.has(task.status)) {
        poll(taskNo);
      } else {
        await refreshQuotaAndTasks();
      }
    } catch (ex) {
      handleError(ex);
    }
  }, 1500) as unknown as number;
}

function removeWork(task: AiTask) {
  uni.showModal({
    content: '删除这个作品？生成记录仍保留在后台。',
    success: async (result) => {
      if (!result.confirm) return;
      try {
        await deleteAiWork(task.id);
        await refreshQuotaAndTasks();
        if (currentTask.value?.id === task.id) currentTask.value = null;
      } catch (ex) {
        handleError(ex);
      }
    },
  });
}

function back() {
  uni.navigateBack({ delta: 1 });
}

onShow(load);
onUnload(() => {
  if (pollTimer) clearTimeout(pollTimer);
});
</script>

<template>
  <view class="screen">
    <view class="card">
      <view class="header">
        <text class="eyebrow">AI 创作</text>
        <text class="title">把照片变成作品</text>
        <text v-if="quota?.period" class="pill">
          图 {{ quota.image.remaining }} · 视频 {{ quota.video.remaining }}
        </text>
      </view>

      <text v-if="loading" class="state">正在加载…</text>
      <view v-else-if="!quota?.period" class="state-block">
        <text v-if="error" class="error">{{ error }}</text>
        <text class="state">当前没有生效中的会员周期，购买套餐后才会获得 AI 次数。</text>
      </view>

      <template v-else>
        <text v-if="error" class="error">{{ error }}</text>
        <view class="tabs">
          <text
            v-for="tab in typeTabs"
            :key="tab.value"
            :class="{ active: activeType === tab.value }"
            class="tab"
            @click="selectType(tab.value)"
          >
            {{ tab.label }}
          </text>
        </view>

        <view class="template-grid">
          <view
            v-for="template in visibleTemplates"
            :key="template.id"
            :class="{ selected: selected?.id === template.id }"
            class="template"
            @click="selected = template"
          >
            <text class="template-cover">{{ template.name.slice(0, 1) }}</text>
            <text class="template-name">{{ template.name }}</text>
            <text class="template-category">{{ template.category }}</text>
          </view>
        </view>

        <view class="submit-card">
          <text class="step">1. 上传素材（H5 模拟文件选择）</text>
          <button class="secondary-button" :loading="uploading" @click="chooseFile">
            {{ inputPreview ? '更换素材' : '选择图片 / 视频' }}
          </button>
          <image v-if="inputPreview && activeType !== 'VIDEO'" :src="inputPreview" class="preview" />
          <text v-if="inputPreview && activeType === 'VIDEO'" class="state">已选择视频素材</text>

          <text class="step">2. 确认模板</text>
          <text class="selected-name">{{ selected?.name || '请选择一个模板' }}</text>
          <button
            class="primary-button"
            :disabled="!selected || !inputPreview"
            :loading="submitting"
            @click="submit"
          >
            提交创作（消耗 1 次）
          </button>
        </view>

        <view v-if="currentTask" class="current">
          <view class="current-head">
            <text class="current-title">{{ currentTask.templateName }}</text>
            <text class="tag">{{ currentTask.status }}</text>
          </view>
          <text class="meta">{{ currentTask.taskNo }}</text>
          <text v-if="currentTask.failReason" class="error">{{ currentTask.failReason }}</text>
          <video
            v-if="currentTask.outputUrl && currentTask.outputMime?.startsWith('video')"
            :src="assetUrl(currentTask.outputUrl)"
            autoplay
            class="output"
            controls
            loop
            muted
          />
          <image
            v-else-if="currentTask.outputUrl"
            :src="assetUrl(currentTask.outputUrl)"
            class="output"
            mode="widthFix"
          />
        </view>

        <view class="history">
          <text class="history-title">我的作品</text>
          <text v-if="works.length === 0" class="state">还没有生成成功的作品。</text>
          <view v-for="task in works" :key="task.id" class="work">
            <image
              v-if="!task.outputMime?.startsWith('video')"
              :src="assetUrl(task.outputUrl)"
              class="work-thumb"
            />
            <text v-else class="work-thumb work-video">视频</text>
            <text class="work-name">{{ task.templateName }}</text>
            <text class="work-delete" @click="removeWork(task)">删除</text>
          </view>
        </view>
      </template>

      <button class="secondary-button" @click="back">返回会员中心</button>
    </view>
  </view>
</template>

<style scoped>
.screen {
  min-height: 100vh;
  padding: 40rpx 32rpx;
  background: #f4f2ed;
}

.card {
  padding: 48rpx 36rpx;
  border: 1rpx solid #ddd9d1;
  border-radius: 32rpx;
  background: #fffdf9;
}

.eyebrow {
  display: block;
  color: #2f6b5f;
  font-size: 24rpx;
  font-weight: 600;
}

.title {
  display: block;
  margin-top: 12rpx;
  color: #1f2421;
  font-size: 52rpx;
  font-weight: 600;
}

.pill {
  display: inline-block;
  margin-top: 16rpx;
  padding: 8rpx 24rpx;
  border-radius: 999rpx;
  background: #e7efec;
  color: #2f6b5f;
  font-size: 24rpx;
  font-weight: 600;
}

.state,
.error,
.meta {
  display: block;
  margin-top: 24rpx;
  color: #5c645f;
  font-size: 26rpx;
}

.error {
  color: #a83b32;
}

.tabs {
  display: flex;
  gap: 12rpx;
  margin-top: 28rpx;
}

.tab {
  padding: 10rpx 28rpx;
  border: 1rpx solid #ddd9d1;
  border-radius: 999rpx;
  color: #5c645f;
  font-size: 26rpx;
}

.tab.active {
  border-color: #2f6b5f;
  background: #e7efec;
  color: #2f6b5f;
  font-weight: 600;
}

.template-grid {
  display: flex;
  flex-wrap: wrap;
  margin-top: 20rpx;
}

.template {
  width: calc(33.33% - 12rpx);
  margin: 0 12rpx 12rpx 0;
  padding: 12rpx;
  border: 1rpx solid #ddd9d1;
  border-radius: 16rpx;
  box-sizing: border-box;
}

.template.selected {
  border-color: #2f6b5f;
  background: #e7efec;
}

.template-cover {
  display: flex;
  height: 150rpx;
  align-items: center;
  justify-content: center;
  border-radius: 12rpx;
  background: linear-gradient(160deg, #dce9e4, #a9c7bc);
  color: #2f6b5f;
  font-size: 52rpx;
  font-weight: 700;
}

.template-name {
  display: block;
  margin-top: 10rpx;
  color: #1f2421;
  font-size: 24rpx;
  font-weight: 600;
}

.template-category {
  display: block;
  color: #5c645f;
  font-size: 20rpx;
}

.submit-card {
  margin-top: 20rpx;
  padding: 24rpx;
  border: 1rpx solid #ddd9d1;
  border-radius: 16rpx;
}

.step {
  display: block;
  margin: 20rpx 0 12rpx;
  color: #5c645f;
  font-size: 24rpx;
}

.step:first-child {
  margin-top: 0;
}

.selected-name {
  display: block;
  color: #1f2421;
  font-size: 28rpx;
  font-weight: 600;
}

.preview,
.output {
  display: block;
  width: 100%;
  margin-top: 16rpx;
  border-radius: 12rpx;
}

.current {
  margin-top: 24rpx;
  padding: 24rpx;
  border: 1rpx solid #2f6b5f;
  border-radius: 16rpx;
  background: #e7efec;
}

.current-head {
  display: flex;
  justify-content: space-between;
}

.current-title {
  font-size: 30rpx;
  font-weight: 600;
}

.history {
  margin-top: 32rpx;
}

.history-title {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
}

.work {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #ddd9d1;
}

.work-thumb {
  width: 88rpx;
  height: 88rpx;
  border-radius: 12rpx;
  background: #e7efec;
}

.work-video {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #2f6b5f;
  font-size: 22rpx;
}

.work-name {
  flex: 1;
  font-size: 26rpx;
}

.work-delete {
  color: #a83b32;
  font-size: 24rpx;
}
</style>
