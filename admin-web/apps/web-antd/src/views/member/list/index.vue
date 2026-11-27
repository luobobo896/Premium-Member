<script lang="ts" setup>
import type { TableColumnsType } from 'ant-design-vue';

import type { MemberApi } from '#/api';
import type { VbenFormSchema } from '#/adapter/form';

import { computed, nextTick, onMounted, reactive, ref } from 'vue';

import {
  Button,
  Descriptions,
  Drawer,
  message,
  Modal,
  Table,
  Tag,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import { useVbenForm } from '#/adapter/form';
import {
  createMemberApi,
  deleteMemberApi,
  getMemberApi,
  getMembersApi,
  updateMemberApi,
} from '#/api';

const DescriptionsItem = Descriptions.Item;

const loading = ref(false);
const rows = ref<MemberApi.Member[]>([]);
const total = ref(0);
const query = reactive({ page: 1, pageSize: 2 });

const editing = ref<MemberApi.Member | null>(null);
const formOpen = ref(false);
const saving = ref(false);

const detailOpen = ref(false);
const detail = ref<MemberApi.Member | null>(null);
const detailLoading = ref(false);

const levelOptions = [
  { label: 'BASIC', value: 'BASIC' },
  { label: 'SILVER', value: 'SILVER' },
  { label: 'GOLD', value: 'GOLD' },
  { label: 'PLATINUM', value: 'PLATINUM' },
];
const statusOptions = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 },
];

const columns: TableColumnsType = [
  { key: 'memberNo', title: '会员编号', dataIndex: 'memberNo' },
  { key: 'nickname', title: '昵称', dataIndex: 'nickname' },
  { key: 'phone', title: '手机号', dataIndex: 'phone' },
  { key: 'level', title: '等级', dataIndex: 'level' },
  { key: 'points', title: '积分', dataIndex: 'points' },
  { key: 'balance', title: '余额', dataIndex: 'balance' },
  { key: 'status', title: '状态', dataIndex: 'status' },
  { key: 'joinedAt', title: '入会时间', dataIndex: 'joinedAt' },
  {
    key: 'action',
    title: '操作',
    width: 180,
    fixed: 'right',
  },
];

const pagination = computed(() => ({
  current: query.page,
  pageSize: query.pageSize,
  showSizeChanger: false,
  showTotal: (value: number) => `共 ${value} 条`,
  total: total.value,
}));

function buildSchema(isEdit: boolean): VbenFormSchema[] {
  return [
    ...(isEdit
      ? [
          {
            fieldName: 'memberNo',
            label: '会员编号',
            component: 'Input' as const,
            componentProps: { disabled: true },
          },
        ]
      : []),
    {
      fieldName: 'username',
      label: '登录名',
      component: 'Input',
      rules: 'required',
    },
    { fieldName: 'nickname', label: '昵称', component: 'Input' },
    {
      fieldName: 'phone',
      label: '手机号',
      component: 'Input',
      componentProps: { placeholder: '11 位手机号，可空' },
    },
    {
      fieldName: 'level',
      label: '等级',
      component: 'Select',
      componentProps: { options: levelOptions },
    },
    {
      fieldName: 'points',
      label: '积分',
      component: 'InputNumber',
      componentProps: { min: 0, precision: 0, class: 'w-full' },
    },
    {
      fieldName: 'balance',
      label: '余额',
      component: 'InputNumber',
      componentProps: { min: 0, precision: 2, class: 'w-full' },
    },
    {
      fieldName: 'status',
      label: '状态',
      component: 'Select',
      componentProps: { options: statusOptions },
    },
    {
      fieldName: 'joinedAt',
      label: '入会时间',
      component: 'DatePicker',
      componentProps: { class: 'w-full', showTime: true },
    },
    {
      fieldName: 'remark',
      label: '备注',
      component: 'Textarea',
      formItemClass: 'col-span-2',
    },
  ];
}

const [Form, formApi] = useVbenForm({
  schema: buildSchema(false),
  showDefaultActions: false,
  wrapperClass: 'grid-cols-1 md:grid-cols-2',
});

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

async function openCreate() {
  editing.value = null;
  formOpen.value = true;
  formApi.setState({ schema: buildSchema(false) });
  await nextTick();
  await formApi.resetForm();
  await formApi.setValues({
    level: 'BASIC',
    points: 0,
    balance: 0,
    status: 1,
  });
}

async function openEdit(record: Record<string, any>) {
  const row = record as MemberApi.Member;
  editing.value = row;
  formOpen.value = true;
  formApi.setState({ schema: buildSchema(true) });
  await nextTick();
  await formApi.resetForm();
  await formApi.setValues({
    ...row,
    joinedAt: row.joinedAt ? dayjs(row.joinedAt) : undefined,
  });
}

async function save() {
  const { valid } = await formApi.validate();
  if (!valid) {
    return;
  }
  const values = await formApi.getValues();
  const payload: MemberApi.MemberPayload = {
    balance: values.balance ?? 0,
    joinedAt: values.joinedAt
      ? dayjs(values.joinedAt).toISOString()
      : undefined,
    level: values.level,
    nickname: values.nickname ?? '',
    phone: values.phone ?? '',
    points: values.points ?? 0,
    remark: values.remark ?? '',
    status: values.status,
    username: values.username,
  };
  saving.value = true;
  try {
    if (editing.value) {
      await updateMemberApi(editing.value.id, payload);
    } else {
      await createMemberApi(payload);
    }
    message.success('保存成功');
    formOpen.value = false;
    await load();
  } catch {
    // 请求层已按后端错误码提示
  } finally {
    saving.value = false;
  }
}

function remove(record: Record<string, any>) {
  const row = record as MemberApi.Member;
  Modal.confirm({
    content: `确定删除会员「${row.nickname || row.username}」？`,
    okType: 'danger',
    title: '删除会员',
    async onOk() {
      await deleteMemberApi(row.id);
      message.success('已删除');
      if (rows.value.length === 1 && query.page > 1) {
        query.page -= 1;
      }
      await load();
    },
  });
}

async function openDetail(record: Record<string, any>) {
  const row = record as MemberApi.Member;
  detailOpen.value = true;
  detail.value = row;
  detailLoading.value = true;
  try {
    detail.value = await getMemberApi(row.id);
  } catch {
    // 请求层已按后端错误码提示
  } finally {
    detailLoading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <div class="p-4">
    <div class="mb-4 flex items-center justify-between">
      <h2 class="text-lg font-medium">会员列表</h2>
      <Button
        v-access:code="'member:create'"
        type="primary"
        @click="openCreate"
      >
        新增会员
      </Button>
    </div>
    <Table
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      :pagination="pagination"
      row-key="id"
      :scroll="{ x: 1100 }"
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
        <span v-else-if="column.key === 'action'" class="flex gap-1">
          <Button
            v-access:code="'member:read'"
            size="small"
            type="link"
            @click="openDetail(record)"
          >
            详情
          </Button>
          <Button
            v-access:code="'member:update'"
            size="small"
            type="link"
            @click="openEdit(record)"
          >
            编辑
          </Button>
          <Button
            v-access:code="'member:delete'"
            danger
            size="small"
            type="link"
            @click="remove(record)"
          >
            删除
          </Button>
        </span>
      </template>
    </Table>

    <Drawer
      v-model:open="formOpen"
      :title="editing ? '编辑会员' : '新增会员'"
      width="640"
    >
      <Form />
      <template #footer>
        <div class="flex justify-end gap-2">
          <Button @click="formOpen = false">取消</Button>
          <Button :loading="saving" type="primary" @click="save">保存</Button>
        </div>
      </template>
    </Drawer>

    <Drawer v-model:open="detailOpen" title="会员详情" width="480">
      <Descriptions :column="1" bordered size="small">
        <DescriptionsItem label="会员编号">
          {{ detail?.memberNo }}
        </DescriptionsItem>
        <DescriptionsItem label="登录名">
          {{ detail?.username }}
        </DescriptionsItem>
        <DescriptionsItem label="昵称">
          {{ detail?.nickname }}
        </DescriptionsItem>
        <DescriptionsItem label="手机号">
          {{ detail?.phone || '-' }}
        </DescriptionsItem>
        <DescriptionsItem label="等级">{{ detail?.level }}</DescriptionsItem>
        <DescriptionsItem label="积分">{{ detail?.points }}</DescriptionsItem>
        <DescriptionsItem label="余额">
          ¥{{ Number(detail?.balance ?? 0).toFixed(2) }}
        </DescriptionsItem>
        <DescriptionsItem label="状态">
          {{ detail?.status === 1 ? '启用' : '停用' }}
        </DescriptionsItem>
        <DescriptionsItem label="入会时间">
          {{
            detail?.joinedAt
              ? dayjs(detail.joinedAt).format('YYYY-MM-DD HH:mm:ss')
              : '-'
          }}
        </DescriptionsItem>
        <DescriptionsItem label="备注">
          {{ detail?.remark || '-' }}
        </DescriptionsItem>
      </Descriptions>
      <div v-if="detailLoading" class="mt-2 text-xs text-gray-400">
        正在刷新详情...
      </div>
    </Drawer>
  </div>
</template>
