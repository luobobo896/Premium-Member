<script lang="ts" setup>
import type { TableColumnsType } from 'ant-design-vue';
import type { Dayjs } from 'dayjs';

import type { AiApi } from '#/api';

import { computed, onMounted, reactive, ref } from 'vue';

import {
  Button,
  DatePicker,
  Input,
  InputNumber,
  message,
  Modal,
  Select,
  Space,
  Table,
  Tabs,
  Tag,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import {
  createAiTemplateApi,
  deleteAiTemplateApi,
  getAiTasksApi,
  getAiTemplatesApi,
  getAiUsageApi,
  updateAiTemplateApi,
} from '#/api';

const TabPane = Tabs.TabPane;
const TextArea = Input.TextArea;
const RangePicker = DatePicker.RangePicker;

const activeTab = ref('templates');
const templates = ref<AiApi.Template[]>([]);
const templatesLoading = ref(false);
const editorOpen = ref(false);
const saving = ref(false);
const editing = ref<AiApi.Template | null>(null);
const templateForm = reactive({
  category: '',
  code: '',
  coverUrl: '',
  name: '',
  params: '{}',
  sort: 0,
  status: 1,
  type: 'PHOTO' as 'DRESSUP' | 'PHOTO' | 'VIDEO',
});

const tasks = ref<AiApi.TaskRow[]>([]);
const tasksLoading = ref(false);
const taskQuery = reactive({
  memberId: undefined as number | undefined,
  page: 1,
  pageSize: 10,
  status: '',
  taskNo: '',
  type: '',
});
const taskPagination = reactive({
  current: 1,
  pageSize: 10,
  showSizeChanger: false,
  total: 0,
});

const usage = ref<AiApi.UsageRow[]>([]);
const usageLoading = ref(false);
const usageRange = ref<[Dayjs, Dayjs]>([dayjs().subtract(6, 'day'), dayjs()]);

const typeOptions = [
  { label: '照片', value: 'PHOTO' },
  { label: '换装', value: 'DRESSUP' },
  { label: '视频', value: 'VIDEO' },
];

const statusOptions = [
  { label: '排队中', value: 'QUEUED' },
  { label: '生成中', value: 'RUNNING' },
  { label: '成功', value: 'SUCCESS' },
  { label: '失败', value: 'FAILED' },
  { label: '已拒绝', value: 'REJECTED' },
  { label: '未知', value: 'UNKNOWN' },
];

const typeNames: Record<string, string> = {
  DRESSUP: '换装',
  PHOTO: '照片',
  VIDEO: '视频',
};

const statusColors: Record<string, string> = {
  FAILED: 'red',
  QUEUED: 'default',
  REJECTED: 'red',
  RUNNING: 'processing',
  SUCCESS: 'green',
  UNKNOWN: 'orange',
};

const templateColumns: TableColumnsType = [
  { dataIndex: 'code', key: 'code', title: '编码', width: 150 },
  { dataIndex: 'name', key: 'name', title: '名称', minWidth: 150 },
  { dataIndex: 'type', key: 'type', title: '类型', width: 90 },
  { dataIndex: 'category', key: 'category', title: '分类', width: 100 },
  { dataIndex: 'versionNo', key: 'versionNo', title: '版本', width: 80 },
  { dataIndex: 'sort', key: 'sort', title: '排序', width: 80 },
  { dataIndex: 'status', key: 'status', title: '状态', width: 90 },
  { dataIndex: 'updatedAt', key: 'updatedAt', title: '更新时间', width: 160 },
  { key: 'action', title: '操作', width: 210, fixed: 'right' },
];

const taskColumns: TableColumnsType = [
  { dataIndex: 'taskNo', key: 'taskNo', title: '任务号', width: 230 },
  { dataIndex: 'memberNo', key: 'memberNo', title: '会员', width: 150 },
  { dataIndex: 'templateName', key: 'templateName', title: '模板', minWidth: 130 },
  { dataIndex: 'type', key: 'type', title: '类型', width: 80 },
  { dataIndex: 'status', key: 'status', title: '状态', width: 90 },
  { dataIndex: 'failReason', key: 'failReason', title: '失败原因', minWidth: 180 },
  { dataIndex: 'createdAt', key: 'createdAt', title: '提交时间', width: 160 },
  { dataIndex: 'finishedAt', key: 'finishedAt', title: '完成时间', width: 160 },
];

const usageColumns: TableColumnsType = [
  { dataIndex: 'day', key: 'day', title: '日期', width: 120 },
  { dataIndex: 'total', key: 'total', title: '总任务', width: 90 },
  { dataIndex: 'imageTasks', key: 'imageTasks', title: '图片任务', width: 100 },
  { dataIndex: 'videoTasks', key: 'videoTasks', title: '视频任务', width: 100 },
  { dataIndex: 'success', key: 'success', title: '成功', width: 80 },
  { dataIndex: 'failed', key: 'failed', title: '失败', width: 80 },
  { dataIndex: 'rejected', key: 'rejected', title: '拒绝', width: 80 },
  { dataIndex: 'unknown', key: 'unknown', title: '未知', width: 80 },
  { dataIndex: 'costCents', key: 'costCents', title: '供应商成本', width: 110 },
];

const activeCount = computed(() => templates.value.filter((item) => item.status === 1).length);

function formatDate(value: null | string | undefined) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-';
}

async function loadTemplates() {
  templatesLoading.value = true;
  try {
    templates.value = await getAiTemplatesApi();
  } finally {
    templatesLoading.value = false;
  }
}

async function loadTasks() {
  tasksLoading.value = true;
  try {
    const result = await getAiTasksApi({
      memberId: taskQuery.memberId || undefined,
      page: taskQuery.page,
      pageSize: taskQuery.pageSize,
      status: taskQuery.status || undefined,
      taskNo: taskQuery.taskNo || undefined,
      type: taskQuery.type || undefined,
    });
    tasks.value = result.items;
    taskPagination.total = result.total;
  } finally {
    tasksLoading.value = false;
  }
}

async function loadUsage() {
  usageLoading.value = true;
  try {
    const [from, to] = usageRange.value ?? [];
    usage.value = await getAiUsageApi({
      from: from?.format('YYYY-MM-DD'),
      to: to?.format('YYYY-MM-DD'),
    });
  } finally {
    usageLoading.value = false;
  }
}

function openCreate() {
  editing.value = null;
  Object.assign(templateForm, {
    category: '',
    code: '',
    coverUrl: '',
    name: '',
    params: '{}',
    sort: templates.value.length + 1,
    status: 1,
    type: 'PHOTO',
  });
  editorOpen.value = true;
}

function openEdit(record: AiApi.Template) {
  editing.value = record;
  Object.assign(templateForm, {
    category: record.category,
    code: record.code,
    coverUrl: record.coverUrl,
    name: record.name,
    params: record.params || '{}',
    sort: record.sort,
    status: record.status,
    type: record.type,
  });
  editorOpen.value = true;
}

async function saveTemplate() {
  if (!templateForm.code.trim() || !templateForm.name.trim()) {
    message.warning('编码和名称必填');
    return;
  }
  saving.value = true;
  try {
    const payload: AiApi.TemplatePayload = {
      category: templateForm.category,
      code: templateForm.code.trim(),
      coverUrl: templateForm.coverUrl,
      name: templateForm.name.trim(),
      params: templateForm.params,
      sort: templateForm.sort,
      status: templateForm.status,
      type: templateForm.type,
    };
    if (editing.value) {
      await updateAiTemplateApi(editing.value.id, payload);
      message.success('模板已更新');
    } else {
      await createAiTemplateApi(payload);
      message.success('模板已创建');
    }
    editorOpen.value = false;
    await loadTemplates();
  } catch (error) {
    message.error(error instanceof Error ? error.message : '保存失败');
  } finally {
    saving.value = false;
  }
}

async function toggleStatus(record: AiApi.Template) {
  try {
    await updateAiTemplateApi(record.id, {
      category: record.category,
      code: record.code,
      coverUrl: record.coverUrl,
      name: record.name,
      params: record.params,
      sort: record.sort,
      status: record.status === 1 ? 0 : 1,
      type: record.type,
    });
    message.success(record.status === 1 ? '已下架' : '已上架');
    await loadTemplates();
  } catch (error) {
    message.error(error instanceof Error ? error.message : '操作失败');
  }
}

function removeTemplate(record: AiApi.Template) {
  Modal.confirm({
    content: `删除模板 ${record.name}？历史任务仍保留快照名称。`,
    onOk: async () => {
      await deleteAiTemplateApi(record.id);
      message.success('已删除');
      await loadTemplates();
    },
    title: '确认删除',
  });
}

function searchTasks() {
  taskQuery.page = 1;
  loadTasks();
}

function onTaskTableChange(page: any) {
  taskQuery.page = page.current;
  taskQuery.pageSize = page.pageSize;
  loadTasks();
}

onMounted(() => {
  loadTemplates();
  loadTasks();
  loadUsage();
});
</script>

<template>
  <div class="p-4">
    <Tabs v-model:active-key="activeTab">
      <TabPane key="templates" tab="模板管理">
        <div class="mb-3 flex flex-wrap items-center justify-between gap-2">
          <span class="text-xs text-gray-500">
            共 {{ templates.length }} 个模板，{{ activeCount }} 个上架；首期照片 8 / 换装 8 / 视频 4。
          </span>
          <Button type="primary" @click="openCreate">新增模板</Button>
        </div>
        <Table
          :columns="templateColumns"
          :data-source="templates"
          :loading="templatesLoading"
          :pagination="false"
          :scroll="{ x: 1150 }"
          row-key="id"
          size="middle"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'type'">
              <Tag color="blue">{{ typeNames[record.type] ?? record.type }}</Tag>
            </template>
            <template v-else-if="column.key === 'status'">
              <Tag :color="record.status === 1 ? 'green' : 'default'">
                {{ record.status === 1 ? '已上架' : '已下架' }}
              </Tag>
            </template>
            <template v-else-if="column.key === 'versionNo'">
              v{{ record.versionNo }}
            </template>
            <template v-else-if="column.key === 'updatedAt'">
              {{ formatDate(record.updatedAt) }}
            </template>
            <template v-else-if="column.key === 'action'">
              <Space>
                <Button size="small" type="link" @click="openEdit(record)">编辑</Button>
                <Button size="small" type="link" @click="toggleStatus(record)">
                  {{ record.status === 1 ? '下架' : '上架' }}
                </Button>
                <Button danger size="small" type="link" @click="removeTemplate(record)">
                  删除
                </Button>
              </Space>
            </template>
          </template>
        </Table>
      </TabPane>

      <TabPane key="tasks" tab="任务查询">
        <div class="mb-3 flex flex-wrap items-center gap-2">
          <Input
            v-model:value="taskQuery.taskNo"
            allow-clear
            class="w-60"
            placeholder="任务号"
          />
          <Select
            v-model:value="taskQuery.status"
            :options="statusOptions"
            allow-clear
            class="w-32"
            placeholder="状态"
          />
          <Select
            v-model:value="taskQuery.type"
            :options="typeOptions"
            allow-clear
            class="w-32"
            placeholder="类型"
          />
          <InputNumber
            v-model:value="taskQuery.memberId"
            class="w-36"
            placeholder="会员 ID"
          />
          <Button @click="searchTasks">查询</Button>
        </div>
        <Table
          :columns="taskColumns"
          :data-source="tasks"
          :loading="tasksLoading"
          :pagination="taskPagination"
          :scroll="{ x: 1250 }"
          row-key="id"
          size="middle"
          @change="onTaskTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'type'">
              {{ typeNames[record.type] ?? record.type }}
            </template>
            <template v-else-if="column.key === 'status'">
              <Tag :color="statusColors[record.status] ?? 'default'">
                {{ record.status }}
              </Tag>
            </template>
            <template v-else-if="column.key === 'failReason'">
              <span :class="{ 'text-red-500': record.failReason }">
                {{ record.failReason || '-' }}
              </span>
            </template>
            <template v-else-if="column.key === 'createdAt'">
              {{ formatDate(record.createdAt) }}
            </template>
            <template v-else-if="column.key === 'finishedAt'">
              {{ formatDate(record.finishedAt) }}
            </template>
          </template>
        </Table>
      </TabPane>

      <TabPane key="usage" tab="用量日报">
        <div class="mb-3 flex flex-wrap items-center gap-2">
          <RangePicker v-model:value="usageRange" @change="loadUsage" />
          <Button @click="loadUsage">刷新</Button>
          <span class="text-xs text-gray-500">按北京时间自然日统计，成本来自 supplier 回传。</span>
        </div>
        <Table
          :columns="usageColumns"
          :data-source="usage"
          :loading="usageLoading"
          :pagination="false"
          row-key="day"
          size="middle"
        />
      </TabPane>
    </Tabs>

    <Modal
      v-model:open="editorOpen"
      :confirm-loading="saving"
      :title="editing ? '编辑模板' : '新增模板'"
      width="620px"
      @ok="saveTemplate"
    >
      <div class="grid gap-3 py-2">
        <div class="flex gap-3">
          <Input
            v-model:value="templateForm.code"
            :disabled="Boolean(editing)"
            addon-before="编码"
            placeholder="AI_PHOTO_01"
          />
          <Select
            v-model:value="templateForm.type"
            :options="typeOptions"
            class="w-32"
          />
        </div>
        <Input v-model:value="templateForm.name" addon-before="名称" />
        <div class="flex gap-3">
          <Input v-model:value="templateForm.category" addon-before="分类" />
          <InputNumber v-model:value="templateForm.sort" :min="0" addon-before="排序" />
        </div>
        <Input v-model:value="templateForm.coverUrl" addon-before="封面" placeholder="可留空" />
        <TextArea
          v-model:value="templateForm.params"
          :rows="6"
          placeholder='{"prompt":"...","aspectRatio":"3:4"}'
        />
        <Select
          v-model:value="templateForm.status"
          :options="[
            { label: '上架', value: 1 },
            { label: '下架', value: 0 },
          ]"
          class="w-32"
        />
      </div>
    </Modal>
  </div>
</template>
