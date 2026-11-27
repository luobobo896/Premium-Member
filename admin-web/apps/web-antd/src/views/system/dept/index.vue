<script lang="ts" setup>
import type { SystemApi } from '#/api';

import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridColumns } from '#/adapter/vxe-table';

import { nextTick, ref } from 'vue';

import { Button, Drawer, message, Modal, Tag } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  createDeptApi,
  deleteDeptApi,
  getDeptsApi,
  updateDeptApi,
} from '#/api';

const deptRows = ref<SystemApi.Dept[]>([]);
const editing = ref<SystemApi.Dept | null>(null);
const drawerOpen = ref(false);
const saving = ref(false);

const statusOptions = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 },
];

function flattenDepts(
  nodes: SystemApi.Dept[],
  depth = 0,
  excludeId?: number,
): { label: string; value: number }[] {
  return nodes.flatMap((node) =>
    node.id === excludeId
      ? []
      : [
          { label: `${'　'.repeat(depth)}${node.name}`, value: node.id },
          ...flattenDepts(node.children ?? [], depth + 1, excludeId),
        ],
  );
}

const searchSchema: VbenFormSchema[] = [
  {
    fieldName: 'name',
    label: '部门名称',
    component: 'Input',
    componentProps: { allowClear: true, placeholder: '模糊搜索' },
  },
  {
    fieldName: 'status',
    label: '状态',
    component: 'Select',
    componentProps: { allowClear: true, options: statusOptions },
  },
];

const columns: VxeTableGridColumns<SystemApi.Dept> = [
  { field: 'name', title: '部门名称', minWidth: 220, treeNode: true },
  { field: 'leader', title: '负责人', width: 120 },
  { field: 'phone', title: '联系电话', width: 140 },
  { field: 'sort', title: '排序', width: 70 },
  { field: 'status', title: '状态', width: 80, slots: { default: 'status' } },
  {
    field: 'action',
    title: '操作',
    width: 150,
    fixed: 'right',
    slots: { default: 'action' },
  },
];

const [Grid, gridApi] = useVbenVxeGrid<SystemApi.Dept>({
  formOptions: {
    schema: searchSchema,
    showCollapseButton: false,
    submitOnChange: false,
  },
  gridOptions: {
    columns,
    height: 600,
    pagerConfig: { enabled: false },
    treeConfig: {
      childrenField: 'children',
      expandAll: true,
      rowField: 'id',
      transform: false,
    },
    proxyConfig: {
      autoLoad: true,
      enabled: true,
      ajax: {
        query: async (_params: any, formValues: any) => {
          const items = await getDeptsApi(formValues);
          deptRows.value = items;
          return { items, total: items.length };
        },
      },
    },
    toolbarConfig: { refresh: true, zoom: true },
  },
});

function buildSchema(dept: SystemApi.Dept | null): VbenFormSchema[] {
  const parentOptions = [
    { label: '顶级', value: 0 },
    ...flattenDepts(deptRows.value, 0, dept?.id),
  ];
  return [
    {
      fieldName: 'parentId',
      label: '父级',
      component: 'Select',
      componentProps: {
        optionFilterProp: 'label',
        options: parentOptions,
        showSearch: true,
      },
    },
    {
      fieldName: 'name',
      label: '部门名称',
      component: 'Input',
      rules: 'required',
    },
    { fieldName: 'leader', label: '负责人', component: 'Input' },
    { fieldName: 'phone', label: '联系电话', component: 'Input' },
    {
      fieldName: 'sort',
      label: '排序',
      component: 'InputNumber',
      componentProps: { min: 0 },
    },
    {
      fieldName: 'status',
      label: '状态',
      component: 'Select',
      componentProps: { options: statusOptions },
    },
  ];
}

const [Form, formApi] = useVbenForm({
  schema: buildSchema(null),
  showDefaultActions: false,
  wrapperClass: 'grid-cols-1 md:grid-cols-2',
});

async function openCreate() {
  editing.value = null;
  drawerOpen.value = true;
  formApi.setState({ schema: buildSchema(null) });
  await nextTick();
  await formApi.resetForm();
  await formApi.setValues({ parentId: 0, sort: 0, status: 1 });
}

async function openEdit(row: SystemApi.Dept) {
  editing.value = row;
  drawerOpen.value = true;
  formApi.setState({ schema: buildSchema(row) });
  await nextTick();
  await formApi.resetForm();
  await formApi.setValues(row);
}

async function save() {
  const { valid } = await formApi.validate();
  if (!valid) {
    return;
  }
  const values = await formApi.getValues();
  const payload: SystemApi.DeptPayload = {
    leader: values.leader ?? '',
    name: values.name,
    parentId: values.parentId ?? 0,
    phone: values.phone ?? '',
    sort: values.sort ?? 0,
    status: values.status,
  };
  saving.value = true;
  try {
    if (editing.value) {
      await updateDeptApi(editing.value.id, payload);
    } else {
      await createDeptApi(payload);
    }
    message.success('保存成功');
    drawerOpen.value = false;
    await gridApi.reload();
  } catch {
    // 请求层已统一提示错误
  } finally {
    saving.value = false;
  }
}

function remove(row: SystemApi.Dept) {
  Modal.confirm({
    content: `确定删除部门「${row.name}」？`,
    okType: 'danger',
    title: '删除部门',
    async onOk() {
      await deleteDeptApi(row.id);
      message.success('已删除');
      await gridApi.reload();
    },
  });
}
</script>

<template>
  <div class="h-full p-4">
    <Grid>
      <template #toolbar-actions>
        <Button
          v-access:code="'system:dept:create'"
          type="primary"
          @click="openCreate"
        >
          新增部门
        </Button>
      </template>
      <template #status="{ row }">
        <Tag :color="row.status === 1 ? 'green' : 'red'">
          {{ row.status === 1 ? '启用' : '停用' }}
        </Tag>
      </template>
      <template #action="{ row }">
        <Button
          v-access:code="'system:dept:update'"
          size="small"
          type="link"
          @click="openEdit(row)"
        >
          编辑
        </Button>
        <Button
          v-access:code="'system:dept:delete'"
          danger
          size="small"
          type="link"
          @click="remove(row)"
        >
          删除
        </Button>
      </template>
    </Grid>

    <Drawer
      v-model:open="drawerOpen"
      :title="editing ? '编辑部门' : '新增部门'"
      width="640"
    >
      <Form />
      <template #footer>
        <div class="flex justify-end gap-2">
          <Button @click="drawerOpen = false">取消</Button>
          <Button :loading="saving" type="primary" @click="save">保存</Button>
        </div>
      </template>
    </Drawer>
  </div>
</template>
