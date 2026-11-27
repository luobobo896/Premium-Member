<script lang="ts" setup>
import type { TableColumnsType } from 'ant-design-vue';
import type { Dayjs } from 'dayjs';

import type { TicketApi } from '#/api';

import { computed, onMounted, reactive, ref } from 'vue';

import {
  Button,
  DatePicker,
  Drawer,
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
  acceptTicketApi,
  getTicketApi,
  getTicketsApi,
  replyTicketApi,
  updateTicketApi,
} from '#/api';

const TextArea = Input.TextArea;
const RangePicker = DatePicker.RangePicker;

const statusOptions = [
  { label: '待受理', value: 'OPEN' },
  { label: '处理中', value: 'PROCESSING' },
  { label: '已解决', value: 'RESOLVED' },
  { label: '已关闭', value: 'CLOSED' },
];

const categoryOptions = [
  { label: '订单', value: 'ORDER' },
  { label: '会员', value: 'MEMBER' },
  { label: '权益', value: 'BENEFIT' },
  { label: 'AI 创作', value: 'AI' },
  { label: '其他', value: 'OTHER' },
];

const priorityOptions = [
  { label: '低', value: 'LOW' },
  { label: '普通', value: 'NORMAL' },
  { label: '高', value: 'HIGH' },
];

const statusNames: Record<string, string> = {
  CLOSED: '已关闭',
  OPEN: '待受理',
  PROCESSING: '处理中',
  RESOLVED: '已解决',
};

const statusColors: Record<string, string> = {
  CLOSED: 'default',
  OPEN: 'red',
  PROCESSING: 'processing',
  RESOLVED: 'green',
};

const categoryNames = Object.fromEntries(
  categoryOptions.map((item) => [item.value, item.label]),
);
const priorityNames = Object.fromEntries(
  priorityOptions.map((item) => [item.value, item.label]),
);

const query = reactive({
  category: undefined as string | undefined,
  keyword: '',
  page: 1,
  pageSize: 20,
  status: undefined as string | undefined,
});
const range = ref<[Dayjs, Dayjs] | undefined>();
const rows = ref<TicketApi.Row[]>([]);
const loading = ref(false);
const pagination = reactive({
  current: 1,
  pageSize: 20,
  showSizeChanger: false,
  total: 0,
});

const drawerOpen = ref(false);
const detail = ref<TicketApi.Detail | null>(null);
const detailLoading = ref(false);
const replyOpen = ref(false);
const replyContent = ref('');
const replySaving = ref(false);
const editForm = reactive({
  priority: 'NORMAL',
  remark: '',
  status: 'OPEN',
});

const columns: TableColumnsType = [
  { dataIndex: 'ticketNo', key: 'ticketNo', title: '工单号', width: 180 },
  { dataIndex: 'status', key: 'status', title: '状态', width: 85 },
  { dataIndex: 'priority', key: 'priority', title: '优先级', width: 70 },
  { dataIndex: 'category', key: 'category', title: '分类', width: 80 },
  { dataIndex: 'title', key: 'title', title: '标题', minWidth: 170 },
  { dataIndex: 'memberName', key: 'memberName', title: '会员', width: 100 },
  { dataIndex: 'assigneeName', key: 'assigneeName', title: '处理人', width: 100 },
  { dataIndex: 'createdAt', key: 'createdAt', title: '提交时间', width: 150 },
  { key: 'action', title: '操作', width: 150, fixed: 'right' },
];

function formatDate(value: null | string | undefined) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-';
}

async function load() {
  loading.value = true;
  try {
    const [from, to] = range.value ?? [];
    const result = await getTicketsApi({
      category: query.category,
      from: from?.format('YYYY-MM-DD'),
      keyword: query.keyword || undefined,
      page: query.page,
      pageSize: query.pageSize,
      status: query.status,
      to: to?.format('YYYY-MM-DD'),
    });
    rows.value = result.items;
    pagination.total = result.total;
  } finally {
    loading.value = false;
  }
}

function search() {
  query.page = 1;
  pagination.current = 1;
  load();
}

function reset() {
  query.category = undefined;
  query.keyword = '';
  query.status = undefined;
  range.value = undefined;
  search();
}

async function openDetail(row: any) {
  drawerOpen.value = true;
  detailLoading.value = true;
  try {
    detail.value = await getTicketApi(row.ticketNo);
    syncEditForm();
  } finally {
    detailLoading.value = false;
  }
}

function syncEditForm() {
  if (!detail.value) return;
  editForm.status = detail.value.ticket.status;
  editForm.priority = detail.value.ticket.priority;
  editForm.remark = detail.value.ticket.remark;
}

async function accept() {
  if (!detail.value) return;
  detail.value = await acceptTicketApi(detail.value.ticket.ticketNo);
  syncEditForm();
  message.success('已受理');
  load();
}

async function saveHandling() {
  if (!detail.value) return;
  detail.value = await updateTicketApi(detail.value.ticket.ticketNo, {
    priority: editForm.priority,
    remark: editForm.remark,
    status: editForm.status,
  });
  syncEditForm();
  message.success('已更新');
  load();
}

async function submitReply() {
  if (!detail.value) return;
  if (!replyContent.value.trim()) {
    message.warning('请输入回复内容');
    return;
  }
  replySaving.value = true;
  try {
    detail.value = await replyTicketApi(
      detail.value.ticket.ticketNo,
      replyContent.value.trim(),
    );
    replyContent.value = '';
    replyOpen.value = false;
    syncEditForm();
    message.success('回复已发送');
    load();
  } finally {
    replySaving.value = false;
  }
}

function changePage(page: number) {
  query.page = page;
  pagination.current = page;
  load();
}

function handleTableChange(page: any) {
  changePage(page.current);
}

const paginationConfig = computed(() => ({
  current: pagination.current,
  pageSize: pagination.pageSize,
  showTotal: (total: number) => `共 ${total} 条`,
  total: pagination.total,
}));

onMounted(load);
</script>

<template>
  <div class="flex h-full flex-col gap-3 p-4">
    <div class="flex flex-wrap items-center gap-2">
      <Select
        v-model:value="query.status"
        allow-clear
        class="w-32"
        :options="statusOptions"
        placeholder="状态"
        @change="search"
      />
      <Select
        v-model:value="query.category"
        allow-clear
        class="w-32"
        :options="categoryOptions"
        placeholder="分类"
        @change="search"
      />
      <Input
        v-model:value="query.keyword"
        allow-clear
        class="w-60"
        placeholder="工单号 / 订单号 / 标题 / 手机号"
        @press-enter="search"
      />
      <RangePicker v-model:value="range" class="w-64" />
      <Button type="primary" @click="search">查询</Button>
      <Button @click="reset">重置</Button>
    </div>

    <Table
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      :pagination="paginationConfig"
      :scroll="{ x: 1500 }"
      row-key="id"
      size="middle"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <Tag :color="statusColors[record.status]">
            {{ statusNames[record.status] ?? record.status }}
          </Tag>
        </template>
        <template v-else-if="column.key === 'priority'">
          {{ priorityNames[record.priority] ?? record.priority }}
        </template>
        <template v-else-if="column.key === 'category'">
          {{ categoryNames[record.category] ?? record.category }}
        </template>
        <template v-else-if="column.key === 'memberName'">
          {{ record.memberName || '-' }}
        </template>
        <template v-else-if="column.key === 'createdAt'">
          {{ formatDate(record.createdAt) }}
        </template>
        <template v-else-if="column.key === 'action'">
          <Space size="small">
            <Button size="small" type="link" @click="openDetail(record)">
              详情
            </Button>
            <Button
              v-access:code="'ticket:handle'"
              size="small"
              type="link"
              @click="openDetail(record)"
            >
              处理
            </Button>
          </Space>
        </template>
      </template>
    </Table>

    <Drawer
      v-model:open="drawerOpen"
      destroy-on-close
      :title="detail ? `工单 ${detail.ticket.ticketNo}` : '工单详情'"
      width="720"
    >
      <div v-if="detailLoading" class="py-10 text-center text-gray-500">
        加载中…
      </div>
      <div v-else-if="detail" class="flex flex-col gap-4">
        <div class="grid grid-cols-2 gap-3 text-sm">
          <div>会员：{{ detail.ticket.memberName || '-' }} {{ detail.ticket.phone || '' }}</div>
          <div>分类：{{ categoryNames[detail.ticket.category] ?? detail.ticket.category }}</div>
          <div>订单号：{{ detail.ticket.orderNo || '-' }}</div>
          <div>提交时间：{{ formatDate(detail.ticket.createdAt) }}</div>
          <div>
            状态：
            <Tag :color="statusColors[detail.ticket.status]">
              {{ statusNames[detail.ticket.status] ?? detail.ticket.status }}
            </Tag>
          </div>
          <div>处理人：{{ detail.ticket.assigneeName || '-' }}</div>
        </div>

        <div class="rounded border border-gray-200 p-3">
          <div class="mb-1 font-medium">{{ detail.ticket.title }}</div>
          <div class="whitespace-pre-wrap text-sm">{{ detail.ticket.content }}</div>
        </div>

        <div>
          <div class="mb-2 font-medium">回复记录</div>
          <div v-if="detail.replies.length === 0" class="text-sm text-gray-500">
            暂无回复
          </div>
          <div
            v-for="reply in detail.replies"
            :key="reply.id"
            class="mb-2 rounded border border-gray-200 p-2 text-sm"
          >
            <div class="mb-1 text-xs text-gray-500">
              {{ reply.senderName || reply.senderType }} ·
              {{ formatDate(reply.createdAt) }}
            </div>
            <div class="whitespace-pre-wrap">{{ reply.content }}</div>
          </div>
        </div>

        <div class="flex flex-col gap-3 border-t pt-3">
          <div class="flex flex-wrap items-center gap-2">
            <Select v-model:value="editForm.status" class="w-32" :options="statusOptions" />
            <Select
              v-model:value="editForm.priority"
              class="w-32"
              :options="priorityOptions"
            />
            <Button
              v-access:code="'ticket:handle'"
              :disabled="['CLOSED', 'RESOLVED'].includes(detail.ticket.status)"
              @click="accept"
            >
              受理
            </Button>
            <Button
              v-access:code="'ticket:handle'"
              type="primary"
              @click="saveHandling"
            >
              保存状态/备注
            </Button>
            <Button
              v-access:code="'ticket:handle'"
              @click="replyOpen = true"
            >
              回复
            </Button>
          </div>
          <TextArea
            v-model:value="editForm.remark"
            :rows="2"
            placeholder="内部备注（不对用户展示）"
          />
        </div>
      </div>
    </Drawer>

    <Modal
      v-model:open="replyOpen"
      :confirm-loading="replySaving"
      title="回复用户"
      @ok="submitReply"
    >
      <TextArea v-model:value="replyContent" :rows="5" placeholder="输入回复内容" />
    </Modal>
  </div>
</template>
