<script lang="ts" setup>
import type { TableColumnsType } from 'ant-design-vue';
import type { Dayjs } from 'dayjs';

import type { StatsApi } from '#/api';

import { onMounted, ref } from 'vue';

import { Button, DatePicker, message, Select, Table } from 'ant-design-vue';
import dayjs from 'dayjs';

import { exportStatsApi, getDailyStatsApi } from '#/api';

const RangePicker = DatePicker.RangePicker;

const sourceOptions = [
  { label: '全部来源', value: '' },
  { label: 'H5', value: 'H5' },
  { label: 'APP', value: 'APP' },
  { label: 'MOCK', value: 'MOCK' },
];

const range = ref<[Dayjs, Dayjs]>([dayjs().subtract(6, 'day'), dayjs()]);
const source = ref('');
const rows = ref<StatsApi.DailyRow[]>([]);
const loading = ref(false);
const exporting = ref(false);

const columns: TableColumnsType = [
  { dataIndex: 'day', key: 'day', title: '日期', width: 110 },
  { dataIndex: 'orderCount', key: 'orderCount', title: '订单数', width: 90 },
  {
    dataIndex: 'paidAmountCents',
    key: 'paidAmountCents',
    title: '实付金额(分)',
    width: 120,
  },
  { dataIndex: 'activeMembers', key: 'activeMembers', title: '有效会员', width: 100 },
  { dataIndex: 'benefitApplied', key: 'benefitApplied', title: '权益申请', width: 100 },
  { dataIndex: 'benefitSuccess', key: 'benefitSuccess', title: '权益成功', width: 100 },
  {
    dataIndex: 'benefitException',
    key: 'benefitException',
    title: '权益异常',
    width: 100,
  },
  { dataIndex: 'aiImageTasks', key: 'aiImageTasks', title: 'AI 图片', width: 90 },
  { dataIndex: 'aiVideoTasks', key: 'aiVideoTasks', title: 'AI 视频', width: 90 },
];

function params() {
  const [from, to] = range.value ?? [];
  return {
    from: from?.format('YYYY-MM-DD'),
    source: source.value || undefined,
    to: to?.format('YYYY-MM-DD'),
  };
}

async function load() {
  loading.value = true;
  try {
    rows.value = await getDailyStatsApi(params());
  } finally {
    loading.value = false;
  }
}

async function exportCsv() {
  exporting.value = true;
  try {
    const response = await exportStatsApi(params());
    const blob = response.data;
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `stats-${params().from}-${params().to}.csv`;
    link.click();
    URL.revokeObjectURL(url);
    message.success('导出已开始，审计已记录');
  } finally {
    exporting.value = false;
  }
}

onMounted(load);
</script>

<template>
  <div class="flex h-full flex-col gap-3 p-4">
    <div class="flex flex-wrap items-center gap-2">
      <RangePicker v-model:value="range" class="w-64" />
      <Select v-model:value="source" class="w-36" :options="sourceOptions" />
      <Button type="primary" @click="load">查询</Button>
      <Button
        v-access:code="'stats:export'"
        :loading="exporting"
        @click="exportCsv"
      >
        导出 CSV
      </Button>
      <span class="text-xs text-gray-500">
        实付金额按支付日归集；有效会员按当日有生效周期的去重会员数。
      </span>
    </div>

    <Table
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      :pagination="false"
      row-key="day"
      size="middle"
    />
  </div>
</template>
