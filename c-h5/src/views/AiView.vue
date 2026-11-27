<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { Button, Tag } from 'vant';

import {
  ApiError,
  assetUrl,
  clearAccessToken,
  createAiTask,
  deleteAiWork,
  getAiQuota,
  getAiTask,
  getAiTasks,
  getAiTemplates,
  uploadAiAsset,
  type AiAsset,
  type AiQuota,
  type AiTask,
  type AiTemplate,
  type AiType,
} from '../api';

const emit = defineEmits<{ logout: [] }>();

const typeTabs: Array<{ label: string; value: AiType }> = [
  { label: '照片', value: 'PHOTO' },
  { label: '换装', value: 'DRESSUP' },
  { label: '视频', value: 'VIDEO' },
];

const STATUS_LABELS: Record<string, string> = {
  FAILED: '生成失败，次数已返还',
  QUEUED: '排队中',
  REJECTED: '已拒绝，次数已返还',
  RUNNING: '正在生成',
  SUCCESS: '生成完成',
  UNKNOWN: '结果未知，次数保留',
};

const quota = ref<AiQuota | null>(null);
const templates = ref<AiTemplate[]>([]);
const tasks = ref<AiTask[]>([]);
const activeType = ref<AiType>('PHOTO');
const activeCategory = ref('');
const selectedTemplate = ref<AiTemplate | null>(null);
const inputAsset = ref<AiAsset | null>(null);
const currentTask = ref<AiTask | null>(null);
const fileInput = ref<HTMLInputElement | null>(null);
const loading = ref(true);
const uploading = ref(false);
const submitting = ref(false);
const error = ref('');
let pollTimer: number | undefined;

const visibleTemplates = computed(() =>
  templates.value.filter(
    (item) =>
      item.type === activeType.value &&
      (!activeCategory.value || item.category === activeCategory.value),
  ),
);

const categories = computed(() => {
  const values = templates.value
    .filter((item) => item.type === activeType.value)
    .map((item) => item.category)
    .filter(Boolean);
  return [...new Set(values)];
});

const works = computed(() => tasks.value.filter((item) => item.outputUrl));
const terminal = new Set(['SUCCESS', 'FAILED', 'REJECTED', 'UNKNOWN']);

function formatDate(value: null | string): string {
  if (!value) return '-';
  return new Intl.DateTimeFormat('zh-CN', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value));
}

function selectType(type: AiType) {
  activeType.value = type;
  activeCategory.value = '';
  selectedTemplate.value = null;
  inputAsset.value = null;
}

function pickFile() {
  fileInput.value?.click();
}

async function onFile(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = '';
  if (!file) return;
  uploading.value = true;
  error.value = '';
  try {
    inputAsset.value = await uploadAiAsset(file);
  } catch (ex) {
    handleError(ex, '上传失败');
  } finally {
    uploading.value = false;
  }
}

function handleError(ex: unknown, fallback: string) {
  if (ex instanceof ApiError && ex.status === 401) {
    clearAccessToken();
    emit('logout');
    return;
  }
  error.value = ex instanceof Error ? ex.message : fallback;
}

async function load() {
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
    handleError(ex, 'AI 创作加载失败');
  } finally {
    loading.value = false;
  }
}

async function refreshQuotaAndTasks() {
  const [quotaData, taskData] = await Promise.all([getAiQuota(), getAiTasks()]);
  quota.value = quotaData;
  tasks.value = taskData;
}

async function submit() {
  if (!selectedTemplate.value || !inputAsset.value || submitting.value) return;
  submitting.value = true;
  error.value = '';
  try {
    const key = crypto.randomUUID
      ? crypto.randomUUID()
      : `h5-${Date.now()}-${Math.random().toString(16).slice(2)}`;
    currentTask.value = await createAiTask({
      idempotencyKey: key,
      inputAssetId: inputAsset.value.id,
      templateId: selectedTemplate.value.id,
    });
    await refreshQuotaAndTasks();
    poll(currentTask.value.taskNo);
  } catch (ex) {
    handleError(ex, '提交失败');
  } finally {
    submitting.value = false;
  }
}

function poll(taskNo: string) {
  if (pollTimer) window.clearTimeout(pollTimer);
  pollTimer = window.setTimeout(async () => {
    try {
      const task = await getAiTask(taskNo);
      currentTask.value = task;
      if (!terminal.has(task.status)) {
        poll(taskNo);
      } else {
        await refreshQuotaAndTasks();
      }
    } catch (ex) {
      handleError(ex, '查询任务失败');
    }
  }, 1200);
}

async function removeWork(task: AiTask) {
  if (!window.confirm('删除这个作品？生成记录仍保留在后台。')) return;
  try {
    await deleteAiWork(task.id);
    await refreshQuotaAndTasks();
    if (currentTask.value?.id === task.id) currentTask.value = null;
  } catch (ex) {
    handleError(ex, '删除失败');
  }
}

onMounted(load);
onBeforeUnmount(() => {
  if (pollTimer) window.clearTimeout(pollTimer);
});
</script>

<template>
  <section class="surface ai-surface" aria-labelledby="ai-title">
    <header class="member-header">
      <div>
        <p class="eyebrow">AI 创作</p>
        <h1 id="ai-title">把照片变成作品</h1>
        <p class="lede">选模板、上传一张照片，Mock 供应商约 1 秒生成占位作品。</p>
      </div>
      <span v-if="quota?.period" class="status-pill">
        图 {{ quota.image.remaining }} · 视频 {{ quota.video.remaining }}
      </span>
    </header>

    <p v-if="loading" class="state-text">正在加载 AI 创作…</p>
    <p v-else-if="error" class="form-error" role="alert">{{ error }}</p>

    <template v-if="!loading">
      <div v-if="!quota?.period" class="state-block">
        <p class="state-text">当前没有生效中的会员周期，购买套餐后才会获得 AI 次数。</p>
      </div>

      <template v-else>
        <div class="ai-quota-grid">
          <div>
            <span class="ai-quota-label">图片次数</span>
            <strong>{{ quota.image.remaining }}/{{ quota.image.limit }}</strong>
            <span class="ai-quota-meta">照片与换装共用</span>
          </div>
          <div>
            <span class="ai-quota-label">视频次数</span>
            <strong>{{ quota.video.remaining }}/{{ quota.video.limit }}</strong>
            <span class="ai-quota-meta">单独计次</span>
          </div>
        </div>

        <div class="ai-type-tabs" role="tablist">
          <button
            v-for="tab in typeTabs"
            :key="tab.value"
            :class="{ active: activeType === tab.value }"
            type="button"
            @click="selectType(tab.value)"
          >
            {{ tab.label }}
          </button>
        </div>

        <div class="ai-category-row">
          <button
            :class="{ active: !activeCategory }"
            type="button"
            @click="activeCategory = ''"
          >
            全部
          </button>
          <button
            v-for="category in categories"
            :key="category"
            :class="{ active: activeCategory === category }"
            type="button"
            @click="activeCategory = category"
          >
            {{ category }}
          </button>
        </div>

        <div class="ai-template-grid">
          <button
            v-for="template in visibleTemplates"
            :key="template.id"
            :class="{ selected: selectedTemplate?.id === template.id }"
            class="ai-template"
            type="button"
            @click="selectedTemplate = template"
          >
            <span class="ai-template-cover">{{ template.name.slice(0, 1) }}</span>
            <span class="ai-template-name">{{ template.name }}</span>
            <span class="ai-template-category">{{ template.category }}</span>
          </button>
        </div>

        <div class="ai-submit-card">
          <p class="ai-step-title">1. 上传一张照片</p>
          <input
            ref="fileInput"
            accept="image/jpeg,image/png,image/webp,image/gif,video/mp4,video/quicktime,video/webm"
            class="file-input"
            type="file"
            @change="onFile"
          />
          <Button
            block
            plain
            class="secondary-button"
            :loading="uploading"
            @click="pickFile"
          >
            {{ inputAsset ? '更换素材' : '选择图片 / 视频' }}
          </Button>
          <p v-if="inputAsset" class="ai-file-name">
            已上传 {{ (inputAsset.size / 1024).toFixed(1) }} KB · {{ inputAsset.mime }}
          </p>

          <p class="ai-step-title">2. 确认模板</p>
          <p class="ai-selected-name">{{ selectedTemplate?.name || '请选择一个模板' }}</p>

          <Button
            block
            type="primary"
            class="submit-button"
            :disabled="!selectedTemplate || !inputAsset"
            :loading="submitting"
            @click="submit"
          >
            提交创作（消耗 1 次）
          </Button>
        </div>

        <section v-if="currentTask" class="ai-current" aria-live="polite">
          <div class="benefit-card-head">
            <div>
              <p class="eyebrow">当前任务</p>
              <h2>{{ currentTask.templateName }}</h2>
            </div>
            <Tag
              :type="
                currentTask.status === 'SUCCESS'
                  ? 'success'
                  : currentTask.status === 'UNKNOWN'
                    ? 'warning'
                    : currentTask.status === 'FAILED' || currentTask.status === 'REJECTED'
                      ? 'danger'
                      : 'primary'
              "
            >
              {{ currentTask.status }}
            </Tag>
          </div>
          <p class="benefit-meta">
            <span>{{ currentTask.taskNo }}</span>
            <span>{{ STATUS_LABELS[currentTask.status] ?? currentTask.status }}</span>
          </p>
          <p v-if="currentTask.failReason" class="form-error">{{ currentTask.failReason }}</p>
          <div v-if="currentTask.outputUrl" class="ai-output">
            <video
              v-if="currentTask.outputMime?.startsWith('video')"
              :src="assetUrl(currentTask.outputUrl)"
              autoplay
              controls
              loop
              muted
              playsinline
            />
            <img
              v-else
              :alt="currentTask.templateName"
              :src="assetUrl(currentTask.outputUrl)"
            />
          </div>
          <p v-if="currentTask.status === 'UNKNOWN'" class="ai-note">
            供应商未返回明确结果，本次次数继续占用，只能查询，不会自动重试。
          </p>
        </section>

        <section class="history-section">
          <h2>我的作品</h2>
          <p v-if="works.length === 0" class="state-text">还没有生成成功的作品。</p>
          <div v-else class="ai-works">
            <article v-for="task in works" :key="task.id" class="ai-work">
              <video
                v-if="task.outputMime?.startsWith('video')"
                :src="assetUrl(task.outputUrl)"
                autoplay
                loop
                muted
                playsinline
              />
              <img v-else :alt="task.templateName" :src="assetUrl(task.outputUrl)" />
              <div>
                <strong>{{ task.templateName }}</strong>
                <span class="history-meta">{{ formatDate(task.finishedAt) }}</span>
              </div>
              <Button plain size="small" type="danger" @click="removeWork(task)">
                删除
              </Button>
            </article>
          </div>
        </section>
      </template>
    </template>
  </section>
</template>
