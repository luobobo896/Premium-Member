<script setup lang="ts">
import { computed, ref } from 'vue';
import { onLoad, onShow, onUnload } from '@dcloudio/uni-app';

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
  requireLogin,
  uploadAiAsset,
  type AiQuota,
  type AiTask,
  type AiTemplate,
  type AiType,
} from '../../api';
import AppIcon from '../../components/AppIcon.vue';
import AppTabbar from '../../components/AppTabbar.vue';
import GradientCover from '../../components/GradientCover.vue';

const TYPE_TITLES: Record<AiType, string> = {
  PHOTO: 'AI 写真',
  DRESSUP: '一键换装',
  VIDEO: '图生视频',
};
const terminal = new Set(['SUCCESS', 'FAILED', 'REJECTED', 'UNKNOWN']);
const TASK_STATUS: Record<string, string> = {
  QUEUED: '排队中',
  RUNNING: '生成中',
  SUCCESS: '已完成',
  FAILED: '失败',
  REJECTED: '已拒绝',
  UNKNOWN: '未知',
};

const loggedIn = ref(false);
const activeType = ref<AiType>('PHOTO');
const quota = ref<AiQuota | null>(null);
const templates = ref<AiTemplate[]>([]);
const tasks = ref<AiTask[]>([]);
const selected = ref<AiTemplate | null>(null);
const inputPreview = ref('');
const currentTask = ref<AiTask | null>(null);
const loading = ref(true);
const uploading = ref(false);
const submitting = ref(false);
const error = ref('');
const showWorks = ref(false);
const uploadedAssetId = ref(0);
const presetTemplateId = ref(0);
let pollTimer: number | undefined;

const typeTitle = computed(() => TYPE_TITLES[activeType.value]);
const visibleTemplates = computed(() =>
  templates.value.filter((item) => item.type === activeType.value),
);
const works = computed(() => tasks.value.filter((item) => item.outputUrl));
const taskParams = computed<Record<string, unknown>>(() => {
  if (!selected.value?.params) return {};
  try {
    return JSON.parse(selected.value.params) as Record<string, unknown>;
  } catch {
    return {};
  }
});
const videoDuration = computed(() => {
  const value = Number(taskParams.value.duration);
  return Number.isFinite(value) && value > 0 ? value : 5;
});
const aspectRatio = computed(() =>
  typeof taskParams.value.aspectRatio === 'string' ? taskParams.value.aspectRatio : '3:4',
);
const taskPrice = computed(() => {
  const type = selected.value?.type ?? activeType.value;
  if (type === 'VIDEO') return quota.value?.prices.video ?? 500;
  if (type === 'DRESSUP') return quota.value?.prices.dressup ?? 100;
  return quota.value?.prices.photo ?? 100;
});
const insufficient = computed(
  () => loggedIn.value && Boolean(quota.value) && quota.value!.balancePoints < taskPrice.value,
);
const consumeText = computed(() => {
  if (!loggedIn.value) return '登录后按积分生成';
  if (!quota.value?.subscription) return '当前没有订阅，开通会员后获得积分';
  return `本次消耗 ${taskPrice.value} 积分 · 剩余 ${quota.value.balancePoints}`;
});
const ctaText = computed(() => {
  if (!loggedIn.value) return '登录后生成';
  if (insufficient.value) return '积分不足，去开通会员';
  return submitting.value ? '提交中' : '开始生成';
});
const ctaDisabled = computed(
  () => submitting.value || insufficient.value,
);

function coverLabel(template: AiTemplate): string {
  return template.category.length <= 4 ? template.category : template.name.slice(0, 2);
}

function coverTone(type: AiType): string {
  if (type === 'DRESSUP') return 'dress';
  if (type === 'VIDEO') return 'video';
  return 'photo';
}

function statusLabel(status: string): string {
  return TASK_STATUS[status] ?? status;
}

function statusClass(status: string): string {
  if (status === 'SUCCESS') return 'pm-pill--accent';
  if (status === 'FAILED' || status === 'REJECTED') return 'pm-pill--danger';
  return 'pm-pill--warning';
}

function goLogin(): void {
  uni.setStorageSync('pm_ai_type', activeType.value);
  requireLogin('/pages/ai/ai');
}

function openPlans(): void {
  uni.reLaunch({ url: '/pages/index/index' });
}

function handleError(ex: unknown) {
  if (ex instanceof ApiError && ex.statusCode === 401) {
    clearToken();
    loggedIn.value = false;
    return;
  }
  error.value = ex instanceof Error ? ex.message : '操作失败';
}

async function load() {
  loggedIn.value = Boolean(getToken());
  loading.value = true;
  error.value = '';
  templates.value = await getAiTemplates().catch(() => [] as AiTemplate[]);
  if (!loggedIn.value) {
    quota.value = null;
    tasks.value = [];
    loading.value = false;
    return;
  }
  try {
    const [quotaData, taskData] = await Promise.all([
      getAiQuota(),
      getAiTasks(),
    ]);
    quota.value = quotaData;
    tasks.value = taskData;
    if (presetTemplateId.value) {
      const preset = templates.value.find((item) => item.id === presetTemplateId.value);
      if (preset) {
        activeType.value = preset.type;
        selected.value = preset;
      }
      presetTemplateId.value = 0;
    }
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

function choosePhoto() {
  if (!loggedIn.value) {
    goLogin();
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
  if (!loggedIn.value) {
    goLogin();
    return;
  }
  if (!selected.value) {
    error.value = '请选择一个模板';
    return;
  }
  if (insufficient.value) {
    error.value = '积分不足，去开通会员';
    return;
  }
  if (!inputPreview.value) {
    error.value = '请上传照片';
    return;
  }
  if (submitting.value) return;
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

function toggleWorks(): void {
  if (!loggedIn.value) {
    goLogin();
    return;
  }
  showWorks.value = !showWorks.value;
}

function back(): void {
  uni.reLaunch({ url: '/pages/index/index' });
}

onLoad(() => {
  const preset = uni.getStorageSync('pm_ai_type') as AiType | '';
  if (preset === 'PHOTO' || preset === 'DRESSUP' || preset === 'VIDEO') {
    activeType.value = preset;
  }
  presetTemplateId.value = Number(uni.getStorageSync('pm_ai_template_id')) || 0;
  uni.removeStorageSync('pm_ai_type');
  uni.removeStorageSync('pm_ai_template_id');
});

onShow(load);
onUnload(() => {
  if (pollTimer) clearTimeout(pollTimer);
});
</script>

<template>
  <view class="pm-page">
    <view class="nav">
      <view class="back pm-tap" aria-label="返回" @click="back">
        <AppIcon name="arrow-left" :size="22" />
      </view>
      <text class="nav-title">{{ typeTitle }}</text>
      <view class="nav-space"></view>
    </view>

    <view v-if="loading" class="pm-skeleton page-skeleton"></view>

    <template v-else>
      <text v-if="error" class="pm-notice notice">{{ error }}</text>

      <view class="upload-card pm-card">
        <view class="upload" :class="{ 'upload--filled': inputPreview }" @click="choosePhoto">
          <image v-if="inputPreview" :src="inputPreview" class="upload-image" mode="aspectFill" />
          <view v-else class="upload-empty">
            <AppIcon name="image" :size="30" />
            <text class="upload-text">上传照片</text>
          </view>
          <view v-if="inputPreview" class="upload-again">
            <text>{{ uploading ? '上传中…' : '重新选择' }}</text>
          </view>
        </view>
      </view>

      <view class="section-head">
        <text class="pm-section">选择模板</text>
      </view>

      <scroll-view v-if="visibleTemplates.length" class="scroll" scroll-x>
        <view class="tpl-row">
          <view
            v-for="template in visibleTemplates"
            :key="template.id"
            class="tpl pm-tap"
            :class="{ 'tpl--selected': selected?.id === template.id }"
            @click="selected = template"
          >
            <GradientCover
              :label="coverLabel(template)"
              :caption="template.name"
              :tone="coverTone(template.type)"
              :height="110"
            />
            <text class="tpl-price">{{ template.pointPrice }} 积分</text>
            <view v-if="selected?.id === template.id" class="check">
              <AppIcon name="check" :size="14" />
            </view>
          </view>
        </view>
      </scroll-view>
      <view v-else class="pm-empty">暂无模板</view>

      <view v-if="selected" class="params">
        <template v-if="activeType === 'VIDEO'">
          <view class="param">
            <text class="pm-sub">时长</text>
            <text class="pm-pill">{{ videoDuration }} 秒</text>
          </view>
          <view class="param">
            <text class="pm-sub">分辨率</text>
            <text class="pm-pill">720P</text>
          </view>
        </template>
        <template v-else>
          <view class="param">
            <text class="pm-sub">风格</text>
            <text class="pm-pill">{{ selected.category }}</text>
          </view>
          <view class="param">
            <text class="pm-sub">比例</text>
            <text class="pm-pill">{{ aspectRatio }}</text>
          </view>
        </template>
      </view>

      <view class="consume">
        <AppIcon name="sparkles" :size="16" />
        <text class="consume-text">{{ consumeText }}</text>
      </view>

      <button class="pm-btn pm-btn--primary generate" :disabled="ctaDisabled" @click="submit">
        {{ ctaText }}
      </button>
      <button v-if="insufficient" class="pm-btn pm-btn--secondary plan-link" @click="openPlans">
        去开通会员
      </button>

      <view class="works-link pm-tap" @click="toggleWorks">
        <text>我的作品</text>
        <AppIcon name="chevron-right" :size="16" />
      </view>

      <view v-if="currentTask" class="pm-card current">
        <view class="current-head">
          <text class="pm-card-title">{{ currentTask.templateName }}</text>
          <text class="pm-pill" :class="statusClass(currentTask.status)">
            {{ statusLabel(currentTask.status) }}
          </text>
        </view>
        <text class="pm-sub task-no">{{ currentTask.taskNo }}</text>
        <text v-if="currentTask.failReason" class="pm-notice fail">
          {{ currentTask.failReason }}
        </text>
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

      <view v-if="showWorks" class="works">
        <text v-if="works.length === 0" class="pm-empty">还没有生成成功的作品。</text>
        <view v-for="task in works" :key="task.id" class="pm-card work">
          <image
            v-if="!task.outputMime?.startsWith('video')"
            :src="assetUrl(task.outputUrl)"
            class="work-thumb"
            mode="aspectFill"
          />
          <view v-else class="work-thumb work-video">
            <AppIcon name="video" :size="20" />
          </view>
          <text class="work-name">{{ task.templateName }}</text>
          <button class="work-delete" @click="removeWork(task)">删除</button>
        </view>
      </view>
    </template>

    <AppTabbar active="ai" />
  </view>
</template>

<style scoped>
.nav {
  display: flex;
  height: 48px;
  align-items: center;
  justify-content: space-between;
}

.back {
  display: flex;
  width: 44px;
  height: 44px;
  align-items: center;
  justify-content: center;
  color: var(--text);
}

.nav-title {
  color: var(--text);
  font-size: var(--font-hero);
  font-weight: 600;
}

.nav-space {
  width: 44px;
}

.page-skeleton {
  height: 520px;
  margin-top: var(--space-4);
}

.notice {
  margin-top: var(--space-3);
}

.upload-card {
  display: flex;
  justify-content: center;
  margin-top: var(--space-4);
}

.upload {
  position: relative;
  display: flex;
  width: 220px;
  height: 220px;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border: 1px dashed var(--line);
  border-radius: var(--radius-card);
  background: var(--overlay);
}

.upload--filled {
  border-style: solid;
  border-color: var(--border);
}

.upload-image {
  width: 100%;
  height: 100%;
}

.upload-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
  color: var(--text-muted);
}

.upload-text {
  color: var(--text);
  font-size: var(--font-body);
  font-weight: 600;
}

.upload-again {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  padding: var(--space-2);
  background: rgba(0, 0, 0, 0.55);
  color: var(--text);
  font-size: var(--font-sub);
  text-align: center;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: var(--space-6) 0 var(--space-3);
}

.scroll {
  width: 100%;
  white-space: nowrap;
}

.tpl-row {
  display: flex;
  gap: var(--space-3);
}

.tpl {
  position: relative;
  flex: none;
  width: 104px;
  padding: var(--space-1);
  border: 1px solid var(--border);
  border-radius: var(--radius-control);
  background: var(--surface);
}

.tpl--selected {
  border-color: var(--accent);
}

.check {
  position: absolute;
  top: var(--space-2);
  right: var(--space-2);
  display: flex;
  width: 22px;
  height: 22px;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-pill);
  background: var(--accent);
  color: var(--accent-deep);
}

.tpl-price {
  position: absolute;
  right: var(--space-1);
  bottom: 22px;
  padding: 1px 6px;
  border-radius: var(--radius-pill);
  background: rgba(5, 10, 9, 0.72);
  color: var(--accent);
  font-size: 10px;
  font-variant-numeric: tabular-nums;
}

.params {
  display: flex;
  gap: var(--space-4);
  margin-top: var(--space-4);
}

.param {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.consume {
  display: flex;
  height: 44px;
  align-items: center;
  gap: var(--space-2);
  margin-top: var(--space-4);
  padding: 0 var(--space-3);
  border-radius: var(--radius-control);
  background: var(--accent-soft);
  color: var(--accent);
}

.consume-text {
  color: var(--text);
  font-size: var(--font-sub);
}

.generate {
  margin-top: var(--space-4);
}

.plan-link {
  margin-top: var(--space-2);
}

.works-link {
  display: flex;
  min-height: 44px;
  align-items: center;
  justify-content: center;
  gap: var(--space-1);
  color: var(--text-muted);
  font-size: var(--font-sub);
}

.current {
  margin-top: var(--space-3);
  border-color: var(--accent);
}

.current-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
}

.task-no {
  margin-top: var(--space-1);
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
}

.fail {
  margin-top: var(--space-3);
}

.output {
  display: block;
  width: 100%;
  margin-top: var(--space-3);
  border-radius: var(--radius-control);
}

.works {
  margin-top: var(--space-3);
}

.work {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-top: var(--space-3);
}

.work-thumb {
  flex: none;
  width: 64px;
  height: 64px;
  overflow: hidden;
  border-radius: var(--radius-control);
  background: var(--overlay);
}

.work-video {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
}

.work-name {
  flex: 1;
  overflow: hidden;
  color: var(--text);
  font-size: var(--font-body);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.work-delete {
  display: flex;
  min-width: 44px;
  height: 44px;
  align-items: center;
  justify-content: center;
  color: var(--danger);
  font-size: var(--font-sub);
}
</style>
