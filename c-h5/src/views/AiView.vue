<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { Button } from 'vant';

import {
  ApiError,
  assetUrl,
  clearAccessToken,
  createAiTask,
  deleteAiWork,
  getAccessToken,
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

const emit = defineEmits<{ login: []; logout: []; plans: [] }>();

const typeTabs: Array<{ label: string; value: AiType }> = [
  { label: '照片', value: 'PHOTO' },
  { label: '换装', value: 'DRESSUP' },
  { label: '视频', value: 'VIDEO' },
];

const STATUS_LABELS: Record<string, string> = {
  FAILED: '生成失败，积分已返还',
  QUEUED: '排队中',
  REJECTED: '已拒绝，积分已返还',
  RUNNING: '正在生成',
  SUCCESS: '生成完成',
  UNKNOWN: '结果未知，积分保留',
};

const quota = ref<AiQuota | null>(null);
const loggedIn = ref(Boolean(getAccessToken()));
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
const taskPrice = computed(() => {
  const type = selectedTemplate.value?.type ?? activeType.value;
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

function coverTone(type: AiType): string {
  if (type === 'DRESSUP') return 'cover--dress';
  if (type === 'VIDEO') return 'cover--video';
  return 'cover--photo';
}

function formatDate(value: null | string): string {
  if (!value) return '-';
  return new Intl.DateTimeFormat('zh-CN', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value));
}

function formatMonthDay(value: null | string): string {
  if (!value) return '-';
  const date = new Date(value);
  return `${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

function selectType(type: AiType) {
  activeType.value = type;
  activeCategory.value = '';
  selectedTemplate.value = null;
  inputAsset.value = null;
}

function pickFile() {
  if (!loggedIn.value) {
    emit('login');
    return;
  }
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
    loggedIn.value = false;
    quota.value = null;
    tasks.value = [];
    return;
  }
  error.value = ex instanceof Error ? ex.message : fallback;
}

async function load() {
  loggedIn.value = Boolean(getAccessToken());
  loading.value = true;
  error.value = '';
  try {
    templates.value = await getAiTemplates();
    if (!loggedIn.value) {
      quota.value = null;
      tasks.value = [];
      return;
    }
    const [quotaData, taskData] = await Promise.all([getAiQuota(), getAiTasks()]);
    quota.value = quotaData;
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
  if (!loggedIn.value) {
    emit('login');
    return;
  }
  if (!selectedTemplate.value || !inputAsset.value || submitting.value) return;
  if (insufficient.value) {
    error.value = '积分不足，去开通会员';
    return;
  }
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
  if (!loggedIn.value) {
    emit('login');
    return;
  }
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
  <section class="page" aria-labelledby="ai-title">
    <header class="page-head">
      <div>
        <p class="eyebrow">AI 创作</p>
        <h1 id="ai-title" class="page-title">把照片变成作品</h1>
        <p class="lede">选模板、上传一张照片，Mock 供应商约 1 秒生成占位作品。</p>
      </div>
      <span v-if="quota?.subscription" class="pill pill--accent">
        积分 {{ quota.balancePoints }}
      </span>
    </header>

    <p v-if="loading" class="state-text">正在加载 AI 创作…</p>
    <p v-else-if="error" class="form-error" role="alert">{{ error }}</p>

    <template v-if="!loading && !error">
      <article v-if="loggedIn && quota?.subscription" class="media-card">
        <div class="cover cover--photo quota-cover">
          <span class="cover__eyebrow">本月积分</span>
          <span class="cover__title quota-number">{{ quota.balancePoints }}</span>
          <span class="cover__sub">
            {{ formatMonthDay(quota.resetAt) }} 重置 · 每月
            {{ quota.monthlyPoints }} 积分
          </span>
        </div>
      </article>

      <p v-else-if="!loggedIn" class="empty-state">
        游客可浏览全部 AI 模板，登录后上传照片并生成作品。
      </p>

      <p v-else class="empty-state">
        当前没有订阅，购买套餐后每月发放 AI 积分。
      </p>

      <div class="chips" role="tablist" aria-label="创作类型">
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

      <div class="chips chips--sub">
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

      <section aria-labelledby="template-title">
        <div class="section-head">
          <h2 id="template-title" class="section-title">选择模板</h2>
          <p class="section-sub">{{ visibleTemplates.length }} 个</p>
        </div>
        <div class="template-grid">
          <button
            v-for="template in visibleTemplates"
            :key="template.id"
            :class="{ selected: selectedTemplate?.id === template.id }"
            class="media-card template-card"
            type="button"
            @click="selectedTemplate = template"
          >
            <span class="cover template-cover" :class="coverTone(template.type)">
              <span class="cover__eyebrow">{{ template.category }}</span>
              <span class="cover__title">{{ template.name }}</span>
              <span class="template-price">{{ template.pointPrice }} 积分</span>
            </span>
          </button>
        </div>
      </section>

      <article v-if="!loggedIn" class="media-card">
        <div class="cover cover--hero cover--compact">
          <span class="cover__eyebrow">登录后生成</span>
          <span class="cover__title">{{ selectedTemplate?.name || '先选一个模板' }}</span>
          <span class="cover__sub">登录后上传素材并提交创作</span>
        </div>
        <div class="card-body">
          <Button block type="primary" class="submit-button" @click="emit('login')">
            登录后生成
          </Button>
        </div>
      </article>

      <article v-else class="media-card">
        <div class="cover cover--hero cover--compact">
          <span class="cover__eyebrow">开始创作</span>
          <span class="cover__title">{{ selectedTemplate?.name || '尚未选择模板' }}</span>
          <span class="cover__sub">上传一张照片，确认模板后提交</span>
        </div>
        <div class="card-body">
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
          <p v-if="inputAsset" class="card-sub">
            已上传 {{ (inputAsset.size / 1024).toFixed(1) }} KB · {{ inputAsset.mime }}
          </p>
          <p class="card-sub">已选模板：{{ selectedTemplate?.name || '请选择一个模板' }}</p>
          <p class="card-sub">{{ consumeText }}</p>
          <Button
            block
            type="primary"
            class="submit-button"
            :disabled="!selectedTemplate || !inputAsset || insufficient"
            :loading="submitting"
            @click="submit"
          >
            {{ insufficient ? '积分不足，去开通会员' : `提交创作（消耗 ${taskPrice} 积分）` }}
          </Button>
          <Button
            v-if="insufficient"
            block
            plain
            class="secondary-button"
            @click="emit('plans')"
          >
            去开通会员
          </Button>
        </div>
      </article>

      <article v-if="loggedIn && currentTask" class="media-card" aria-live="polite">
        <div class="cover cover--benefit cover--compact">
          <span class="cover__eyebrow">当前任务</span>
          <span class="cover__title">{{ currentTask.templateName }}</span>
          <span class="cover__sub">{{ STATUS_LABELS[currentTask.status] ?? currentTask.status }}</span>
          <span
            class="pill task-status"
            :class="
              currentTask.status === 'SUCCESS'
                ? 'pill--accent'
                : currentTask.status === 'UNKNOWN'
                  ? 'pill--warning'
                  : currentTask.status === 'FAILED' || currentTask.status === 'REJECTED'
                    ? 'pill--danger'
                    : ''
            "
          >
            {{ currentTask.status }}
          </span>
        </div>
        <div class="card-body">
          <p class="card-sub">{{ currentTask.taskNo }}</p>
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
            <img :alt="currentTask.templateName" :src="assetUrl(currentTask.outputUrl)" />
          </div>
          <p v-if="currentTask.status === 'UNKNOWN'" class="card-sub">
            供应商未返回明确结果，本次积分继续占用，只能查询，不会自动重试。
          </p>
        </div>
      </article>

      <section v-if="loggedIn" aria-labelledby="works-title">
        <div class="section-head">
          <h2 id="works-title" class="section-title">我的作品</h2>
          <p class="section-sub">{{ works.length }} 个</p>
        </div>
        <p v-if="works.length === 0" class="state-text">还没有生成成功的作品。</p>
        <div v-else class="card-list">
          <article v-for="task in works" :key="task.id" class="media-card work-card">
            <div class="work-cover">
              <video
                v-if="task.outputMime?.startsWith('video')"
                :src="assetUrl(task.outputUrl)"
                autoplay
                loop
                muted
                playsinline
              />
              <img v-else :alt="task.templateName" :src="assetUrl(task.outputUrl)" />
            </div>
            <div class="card-body work-body">
              <div>
                <p class="card-title">{{ task.templateName }}</p>
                <p class="card-sub">{{ formatDate(task.finishedAt) }}</p>
              </div>
              <Button plain size="small" type="danger" @click="removeWork(task)">
                删除
              </Button>
            </div>
          </article>
        </div>
      </section>
    </template>
  </section>
</template>

<style scoped>
.quota-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-2);
}

.quota-cover {
  min-height: 132px;
}

.quota-number {
  font-size: var(--font-number);
  font-variant-numeric: tabular-nums;
}

.quota-cover .cover__sub {
  white-space: normal;
}

.chips {
  display: flex;
  gap: var(--space-2);
  margin: calc(var(--space-2) * -1) 0 0;
  overflow-x: auto;
  padding-bottom: 2px;
}

.chips--sub {
  margin-top: calc(var(--space-3) * -1);
}

.chips button {
  height: 34px;
  flex: none;
  padding: 0 var(--space-3);
  border: 1px solid var(--border);
  border-radius: var(--radius-pill);
  background: var(--surface);
  color: var(--text-muted);
  font-size: var(--font-sub);
  cursor: pointer;
}

.chips button.active {
  border-color: var(--accent);
  background: var(--accent-soft);
  color: var(--accent);
  font-weight: 600;
}

.template-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-2);
}

.template-card {
  border-color: var(--border);
  transition: border-color 180ms var(--ease);
}

.template-card.selected {
  border-color: var(--accent);
}

.template-cover {
  aspect-ratio: 3 / 4;
  min-height: 0;
  padding: var(--space-2);
}

.template-cover .cover__title {
  padding-bottom: 18px;
  font-size: var(--font-body);
  line-height: 1.35;
  white-space: normal;
}

.template-price {
  position: absolute;
  right: var(--space-2);
  bottom: var(--space-2);
  padding: 2px 8px;
  border-radius: var(--radius-pill);
  background: rgba(5, 10, 9, 0.72);
  color: var(--accent);
  font-size: 11px;
  font-variant-numeric: tabular-nums;
}

.file-input {
  display: none;
}

.task-status {
  position: absolute;
  top: var(--space-3);
  right: var(--space-3);
  z-index: 2;
}

.ai-output {
  overflow: hidden;
  border-radius: var(--radius-control);
  background: var(--bg);
}

.ai-output img,
.ai-output video {
  display: block;
  width: 100%;
  max-height: 420px;
  object-fit: contain;
}

.work-cover {
  overflow: hidden;
  aspect-ratio: 16 / 9;
  background: var(--overlay);
}

.work-cover img,
.work-cover video {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.work-body {
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
}
</style>
