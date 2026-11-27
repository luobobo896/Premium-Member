<script lang="ts" setup>
import type { TableColumnsType } from 'ant-design-vue';

import type { MemberApi } from '#/api';

import { computed, onMounted, reactive, ref } from 'vue';

import { Table, Tag } from 'ant-design-vue';
import dayjs from 'dayjs';

import { getMembersApi } from '#/api';

const loading = ref(false);
const rows = ref<MemberApi.Member[]>([]);
const total = ref(0);
const query = reactive({ page: 1, pageSize: 2 });

const columns: TableColumnsType = [
  { key: 'memberNo', title: '会员编号', dataIndex: 'memberNo' },
  { key: 'nickname', title: '昵称', dataIndex: 'nickname' },
  { key: 'phone', title: '手机号', dataIndex: 'phone' },
  { key: 'level', title: '等级', dataIndex: 'level' },
  { key: 'points', title: '积分', dataIndex: 'points' },
  { key: 'balance', title: '余额', dataIndex: 'balance' },
  { key: 'status', title: '状态', dataIndex: 'status' },
  { key: 'joinedAt', title: '入会时间', dataIndex: 'joinedAt' },
];

const pagination = computed(() => ({
  current: query.page,
  pageSize: query.pageSize,
  showSizeChanger: false,
  showTotal: (value: number) => `共 ${value} 条`,
  total: total.value,
}));

async function load() {
  loading.value = true;
  try {
    const result = await getMembersApi(query);
    rows.value = result.items;
    total.value = result.total;
  } finally {
    loading.value = false;
  }
}

function handleTableChange(page: { current?: number; pageSize?: number }) {
  query.page = page.current ?? 1;
  query.pageSize = page.pageSize ?? query.pageSize;
  load();
}

onMounted(load);
</script>

<template>
  <div class="p-4">
    <h2 class="mb-4 text-lg font-medium">会员列表</h2>
    <Table
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      :pagination="pagination"
      row-key="id"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <Tag v-if="column.key === 'level'" color="blue">{{ record.level }}</Tag>
        <Tag
          v-else-if="column.key === 'status'"
          :color="record.status === 1 ? 'green' : 'red'"
        >
          {{ record.status === 1 ? '启用' : '停用' }}
        </Tag>
        <span v-else-if="column.key === 'balance'">
          ¥{{ Number(record.balance).toFixed(2) }}
        </span>
        <span v-else-if="column.key === 'joinedAt'">
          {{ dayjs(record.joinedAt).format('YYYY-MM-DD') }}
        </span>
      </template>
    </Table>
  </div>
</template>
