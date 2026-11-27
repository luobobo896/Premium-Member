<script lang="ts" setup>
import type { TableColumnsType } from 'ant-design-vue';

import type { ContentApi } from '#/api';

import { onMounted, reactive, ref } from 'vue';

import {
  Button,
  Input,
  message,
  Modal,
  Select,
  Space,
  Table,
  Tag,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import {
  getContentApi,
  getContentsApi,
  publishContentApi,
  rollbackContentApi,
  saveContentDraftApi,
  updateContentDraftApi,
} from '#/api';

const TextArea = Input.TextArea;

const keyOptions = [
  { label: '套餐页文案', value: 'PLAN_COPY' },
  { label: '首页轮播', value: 'BANNER' },
  { label: '公告', value: 'ANNOUNCEMENT' },
  { label: '下载', value: 'DOWNLOAD' },
  { label: '联系方式', value: 'CONTACT' },
  { label: '服务协议', value: 'AGREEMENT' },
];

const hint: Record<string, string> = {
  PLAN_COPY: '示例：{"title":"选择购买","lede":"支付成功后立即开通","rules":["规则一"]}',
  BANNER: '示例：{"items":[{"title":"会员权益","imageUrl":"https://...","link":""}]}',
  ANNOUNCEMENT: '示例：{"title":"公告","content":"内容"}',
  DOWNLOAD: '示例：{"ios":"https://...","android":"https://..."}',
  CONTACT: '示例：{"serviceWechat":"pm-service","phone":"400-000-0000"}',
  AGREEMENT: '示例：{"title":"会员服务协议","content":"全文"}',
};

const query = reactive({ key: undefined as string | undefined, locale: 'zh-CN' });
const rows = ref<ContentApi.Item[]>([]);
const loading = ref(false);

const editOpen = ref(false);
const editSaving = ref(false);
const editingId = ref<null | number>(null);
const editForm = reactive({
  contentJson: '{}',
  key: 'PLAN_COPY',
  locale: 'zh-CN',
});

const previewOpen = ref(false);
const previewItem = ref<ContentApi.Item | null>(null);

const columns: TableColumnsType = [
  { dataIndex: 'key', key: 'key', title: '配置项', width: 150 },
  { dataIndex: 'locale', key: 'locale', title: '语言', width: 90 },
  { dataIndex: 'versionNo', key: 'versionNo', title: '版本', width: 80 },
  { dataIndex: 'status', key: 'status', title: '状态', width: 100 },
  { dataIndex: 'publishAt', key: 'publishAt', title: '发布时间', width: 160 },
  { dataIndex: 'updatedBy', key: 'updatedBy', title: '更新人', width: 100 },
  { dataIndex: 'updatedAt', key: 'updatedAt', title: '更新时间', width: 160 },
  { key: 'action', title: '操作', width: 270, fixed: 'right' },
];

function keyName(key: string) {
  return keyOptions.find((item) => item.value === key)?.label ?? key;
}

function formatDate(value: null | string | undefined) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-';
}

async function load() {
  loading.value = true;
  try {
    rows.value = await getContentsApi({
      key: query.key,
      locale: query.locale || undefined,
    });
  } finally {
    loading.value = false;
  }
}

async function openCreate() {
  editingId.value = null;
  editForm.key = query.key ?? 'PLAN_COPY';
  editForm.locale = query.locale || 'zh-CN';
  editForm.contentJson = await latestJson(editForm.key, editForm.locale);
  editOpen.value = true;
}

async function latestJson(key: string, locale: string) {
  try {
    const versions = await getContentsApi({ key, locale });
    const draft = versions.find((item) => item.status === 'DRAFT');
    const published = versions.find((item) => item.status === 'PUBLISHED');
    return (draft ?? published)?.contentJson ?? '{}';
  } catch {
    return '{}';
  }
}

async function openEdit(row: any) {
  if (row.status === 'DRAFT') {
    editingId.value = row.id;
    editForm.key = row.key;
    editForm.locale = row.locale;
    editForm.contentJson = pretty(row.contentJson);
    editOpen.value = true;
    return;
  }
  editingId.value = null;
  editForm.key = row.key;
  editForm.locale = row.locale;
  editForm.contentJson = pretty(row.contentJson);
  editOpen.value = true;
}

async function saveDraft() {
  try {
    JSON.parse(editForm.contentJson);
  } catch {
    message.error('内容必须是合法 JSON');
    return;
  }
  editSaving.value = true;
  try {
    if (editingId.value) {
      await updateContentDraftApi(editingId.value, editForm.contentJson);
    } else {
      await saveContentDraftApi({
        contentJson: editForm.contentJson,
        key: editForm.key,
        locale: editForm.locale,
      });
    }
    message.success('草稿已保存');
    editOpen.value = false;
    await load();
  } finally {
    editSaving.value = false;
  }
}

async function publish(row: any) {
  await publishContentApi(row.id);
  message.success(`v${row.versionNo} 已发布`);
  await load();
}

async function rollback(row: any) {
  Modal.confirm({
    content: `把 v${row.versionNo} 的内容复制为新版本并立即发布？`,
    onOk: async () => {
      const restored = await rollbackContentApi(row.id);
      message.success(`已回滚并生成 v${restored.versionNo}`);
      await load();
    },
    title: '回滚内容',
  });
}

async function preview(row: any) {
  previewItem.value = await getContentApi(row.id);
  previewOpen.value = true;
}

function pretty(json: string) {
  try {
    return JSON.stringify(JSON.parse(json), null, 2);
  } catch {
    return json;
  }
}

onMounted(load);
</script>

<template>
  <div class="flex h-full flex-col gap-3 p-4">
    <div class="flex flex-wrap items-center gap-2">
      <Select
        v-model:value="query.key"
        allow-clear
        class="w-44"
        :options="keyOptions"
        placeholder="全部配置项"
        @change="load"
      />
      <Input
        v-model:value="query.locale"
        class="w-32"
        placeholder="zh-CN"
        @press-enter="load"
      />
      <Button type="primary" @click="load">查询</Button>
      <Button v-access:code="'content:update'" @click="openCreate">
        新建/编辑草稿
      </Button>
      <span class="text-xs text-gray-500">
        选择具体配置项可查看全部历史版本；不选则展示每项最新版本。
      </span>
    </div>

    <Table
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      :pagination="false"
      :scroll="{ x: 1200 }"
      row-key="id"
      size="middle"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'key'">
          {{ keyName(record.key) }}
          <span class="text-xs text-gray-400">{{ record.key }}</span>
        </template>
        <template v-else-if="column.key === 'status'">
          <Tag :color="record.status === 'PUBLISHED' ? 'green' : 'orange'">
            {{ record.status === 'PUBLISHED' ? '已发布' : '草稿' }}
          </Tag>
        </template>
        <template v-else-if="column.key === 'publishAt'">
          {{ formatDate(record.publishAt) }}
        </template>
        <template v-else-if="column.key === 'updatedAt'">
          {{ formatDate(record.updatedAt) }}
        </template>
        <template v-else-if="column.key === 'action'">
          <Space size="small">
            <Button size="small" type="link" @click="preview(record)">预览</Button>
            <Button
              v-access:code="'content:update'"
              size="small"
              type="link"
              @click="openEdit(record)"
            >
              {{ record.status === 'DRAFT' ? '编辑草稿' : '基于此版本改草稿' }}
            </Button>
            <Button
              v-if="record.status === 'DRAFT'"
              v-access:code="'content:publish'"
              size="small"
              type="link"
              @click="publish(record)"
            >
              发布
            </Button>
            <Button
              v-if="record.status === 'PUBLISHED'"
              v-access:code="'content:rollback'"
              size="small"
              type="link"
              @click="rollback(record)"
            >
              回滚此版
            </Button>
          </Space>
        </template>
      </template>
    </Table>

    <Modal
      v-model:open="editOpen"
      :confirm-loading="editSaving"
      title="内容草稿"
      width="720px"
      @ok="saveDraft"
    >
      <div class="flex flex-col gap-3">
        <div class="flex items-center gap-2">
          <Select
            v-model:value="editForm.key"
            class="w-44"
            :disabled="Boolean(editingId)"
            :options="keyOptions"
          />
          <Input
            v-model:value="editForm.locale"
            class="w-32"
            :disabled="Boolean(editingId)"
          />
        </div>
        <div class="text-xs text-gray-500">{{ hint[editForm.key] }}</div>
        <TextArea v-model:value="editForm.contentJson" :rows="16" spellcheck="false" />
      </div>
    </Modal>

    <Modal
      v-model:open="previewOpen"
      :footer="null"
      :title="previewItem ? `预览 ${previewItem.key} v${previewItem.versionNo}` : '预览'"
      width="680px"
    >
      <pre class="max-h-96 overflow-auto rounded bg-gray-50 p-3 text-xs">{{
        previewItem ? pretty(previewItem.contentJson) : ''
      }}</pre>
    </Modal>
  </div>
</template>
