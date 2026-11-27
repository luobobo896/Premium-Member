<script lang="ts" setup>
import type { SystemApi } from '#/api';

import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridColumns } from '#/adapter/vxe-table';

import { Tag } from 'ant-design-vue';
import dayjs from 'dayjs';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getOperationLogsApi } from '#/api';

const moduleOptions = [
  { label: '认证', value: '认证' },
  { label: '会员', value: '会员' },
  { label: '用户', value: '用户' },
  { label: '角色', value: '角色' },
  { label: '菜单', value: '菜单' },
  { label: '部门', value: '部门' },
];

const searchSchema: VbenFormSchema[] = [
  {
    fieldName: 'username',
    label: '操作人',
    component: 'Input',
    componentProps: { allowClear: true, placeholder: '登录名模糊搜索' },
  },
  {
    fieldName: 'module',
    label: '模块',
    component: 'Select',
    componentProps: { allowClear: true, options: moduleOptions },
  },
  {
    fieldName: 'timeRange',
    label: '时间',
    component: 'RangePicker',
    componentProps: { class: 'w-full', showTime: true },
  },
];

const columns: VxeTableGridColumns<SystemApi.OperationLog> = [
  { field: 'username', title: '操作人', width: 110 },
  { field: 'module', title: '模块', width: 80 },
  { field: 'action', title: '操作', width: 110 },
  { field: 'method', title: '方法', width: 80 },
  { field: 'path', title: '路径', minWidth: 190 },
  { field: 'params', title: '参数', minWidth: 220, showOverflow: 'tooltip' },
  { field: 'ip', title: 'IP', width: 130 },
  {
    field: 'success',
    title: '结果',
    width: 90,
    slots: { default: 'success' },
  },
  { field: 'errorCode', title: '错误码', width: 140 },
  { field: 'costMs', title: '耗时(ms)', width: 100 },
  {
    field: 'createdAt',
    title: '时间',
    width: 170,
    slots: { default: 'createdAt' },
  },
];

const [Grid] = useVbenVxeGrid<SystemApi.OperationLog>({
  formOptions: {
    schema: searchSchema,
    showCollapseButton: false,
    submitOnChange: false,
  },
  gridOptions: {
    columns,
    height: 560,
    proxyConfig: {
      autoLoad: true,
      enabled: true,
      ajax: {
        query: async ({ page }: any, formValues: any) => {
          const { timeRange, ...rest } = formValues;
          return getOperationLogsApi({
            ...rest,
            page: page.currentPage,
            pageSize: page.pageSize,
            startTime: timeRange?.[0]?.toISOString(),
            endTime: timeRange?.[1]?.toISOString(),
          });
        },
      },
    },
    toolbarConfig: { refresh: true, zoom: true },
  },
});
</script>

<template>
  <div class="h-full p-4">
    <Grid>
      <template #success="{ row }">
        <Tag :color="row.success === 1 ? 'green' : 'red'">
          {{ row.success === 1 ? '成功' : '失败' }}
        </Tag>
      </template>
      <template #createdAt="{ row }">
        {{ dayjs(row.createdAt).format('YYYY-MM-DD HH:mm:ss') }}
      </template>
    </Grid>
  </div>
</template>
