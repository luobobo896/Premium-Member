<script lang="ts" setup>
import type { SystemApi } from '#/api';

import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridColumns } from '#/adapter/vxe-table';

import { nextTick, ref } from 'vue';

import { Button, Drawer, message, Modal, Tag } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  createMenuApi,
  deleteMenuApi,
  getMenusApi,
  updateMenuApi,
} from '#/api';

const menuRows = ref<SystemApi.Menu[]>([]);
const editing = ref<SystemApi.Menu | null>(null);
const drawerOpen = ref(false);
const saving = ref(false);

const statusOptions = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 },
];
const visibleOptions = [
  { label: '显示', value: 1 },
  { label: '隐藏', value: 0 },
];
const typeOptions = [
  { label: '目录', value: 'catalog' },
  { label: '菜单', value: 'menu' },
  { label: '按钮', value: 'button' },
  { label: '内嵌', value: 'embedded' },
  { label: '外链', value: 'link' },
];

function flattenMenus(
  nodes: SystemApi.Menu[],
  depth = 0,
  excludeId?: number,
): { label: string; value: number }[] {
  return nodes.flatMap((node) =>
    node.id === excludeId
      ? []
      : [
          { label: `${'　'.repeat(depth)}${node.title}`, value: node.id },
          ...flattenMenus(node.children ?? [], depth + 1, excludeId),
        ],
  );
}

const searchSchema: VbenFormSchema[] = [
  {
    fieldName: 'title',
    label: '标题',
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

const columns: VxeTableGridColumns<SystemApi.Menu> = [
  { field: 'title', title: '标题', minWidth: 180, treeNode: true },
  { field: 'type', title: '类型', width: 90, slots: { default: 'type' } },
  { field: 'path', title: '路由', minWidth: 140 },
  { field: 'component', title: '组件', minWidth: 180 },
  { field: 'permissionCode', title: '权限标识', minWidth: 160 },
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

const [Grid, gridApi] = useVbenVxeGrid<SystemApi.Menu>({
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
          const items = await getMenusApi(formValues);
          menuRows.value = items;
          return { items, total: items.length };
        },
      },
    },
    toolbarConfig: { refresh: true, zoom: true },
  },
});

function buildSchema(menu: SystemApi.Menu | null): VbenFormSchema[] {
  const parentOptions = [
    { label: '顶级', value: 0 },
    ...flattenMenus(menuRows.value, 0, menu?.id),
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
      fieldName: 'type',
      label: '类型',
      component: 'Select',
      componentProps: { options: typeOptions },
    },
    {
      fieldName: 'name',
      label: '路由 name',
      component: 'Input',
      rules: 'required',
    },
    {
      fieldName: 'title',
      label: '标题',
      component: 'Input',
      rules: 'required',
    },
    { fieldName: 'path', label: '路由 path', component: 'Input' },
    { fieldName: 'component', label: '组件路径', component: 'Input' },
    { fieldName: 'redirect', label: '重定向', component: 'Input' },
    {
      fieldName: 'permissionCode',
      label: '权限标识',
      component: 'Input',
      componentProps: { placeholder: '如 system:user:list' },
    },
    { fieldName: 'icon', label: '图标', component: 'Input' },
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
    {
      fieldName: 'visible',
      label: '可见',
      component: 'Select',
      componentProps: { options: visibleOptions },
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
  await formApi.setValues({
    affixTab: 0,
    hideInMenu: 0,
    keepAlive: 0,
    parentId: 0,
    sort: 0,
    status: 1,
    type: 'menu',
    visible: 1,
  });
}

async function openEdit(row: SystemApi.Menu) {
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
  const payload: SystemApi.MenuPayload = {
    affixTab: editing.value?.affixTab ?? 0,
    badge: editing.value?.badge,
    badgeType: editing.value?.badgeType,
    badgeVariants: editing.value?.badgeVariants,
    component: values.component ?? '',
    hideInMenu: editing.value?.hideInMenu ?? 0,
    icon: values.icon ?? '',
    iframeSrc: editing.value?.iframeSrc,
    keepAlive: editing.value?.keepAlive ?? 0,
    link: editing.value?.link,
    name: values.name,
    parentId: values.parentId ?? 0,
    path: values.path ?? '',
    permissionCode: values.permissionCode ?? '',
    redirect: values.redirect ?? '',
    sort: values.sort ?? 0,
    status: values.status,
    title: values.title,
    type: values.type,
    visible: values.visible,
  };
  saving.value = true;
  try {
    if (editing.value) {
      await updateMenuApi(editing.value.id, payload);
    } else {
      await createMenuApi(payload);
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

function remove(row: SystemApi.Menu) {
  Modal.confirm({
    content: `确定删除菜单「${row.title}」？`,
    okType: 'danger',
    title: '删除菜单',
    async onOk() {
      await deleteMenuApi(row.id);
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
          v-access:code="'system:menu:create'"
          type="primary"
          @click="openCreate"
        >
          新增菜单
        </Button>
      </template>
      <template #type="{ row }">
        <Tag color="blue">{{ row.type }}</Tag>
      </template>
      <template #status="{ row }">
        <Tag :color="row.status === 1 ? 'green' : 'red'">
          {{ row.status === 1 ? '启用' : '停用' }}
        </Tag>
      </template>
      <template #action="{ row }">
        <Button
          v-access:code="'system:menu:update'"
          size="small"
          type="link"
          @click="openEdit(row)"
        >
          编辑
        </Button>
        <Button
          v-access:code="'system:menu:delete'"
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
      :title="editing ? '编辑菜单' : '新增菜单'"
      width="680"
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
