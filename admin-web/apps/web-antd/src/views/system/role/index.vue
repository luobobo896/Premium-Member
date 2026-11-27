<script lang="ts" setup>
import type { SystemApi } from '#/api';

import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridColumns } from '#/adapter/vxe-table';

import { computed, nextTick, ref } from 'vue';

import {
  Button,
  Checkbox,
  Drawer,
  message,
  Modal,
  Tag,
  Tree,
} from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  assignRoleMenusApi,
  assignRolePermissionsApi,
  createRoleApi,
  deleteRoleApi,
  getMenusApi,
  getPermissionsApi,
  getRoleMenuIdsApi,
  getRolePermissionIdsApi,
  getRolesApi,
  updateRoleApi,
} from '#/api';

const editing = ref<SystemApi.Role | null>(null);
const drawerOpen = ref(false);
const saving = ref(false);

const menuOpen = ref(false);
const menuTarget = ref<SystemApi.Role | null>(null);
const menuRows = ref<SystemApi.Menu[]>([]);
const checkedMenuIds = ref<number[]>([]);
const halfCheckedMenuIds = ref<number[]>([]);

const permissionOpen = ref(false);
const permissionTarget = ref<SystemApi.Role | null>(null);
const permissionRows = ref<SystemApi.Permission[]>([]);
const checkedPermissionIds = ref<number[]>([]);

const statusOptions = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 },
];

function toTreeData(nodes: SystemApi.Menu[]): any[] {
  return nodes.map((node) => ({
    children: toTreeData(node.children ?? []),
    key: node.id,
    title: node.title,
  }));
}

const menuTreeData = computed(() => toTreeData(menuRows.value));

const permissionOptions = computed(() =>
  permissionRows.value.map((item) => ({
    label: `${item.name}（${item.code}）`,
    value: item.id,
  })),
);

const searchSchema: VbenFormSchema[] = [
  {
    fieldName: 'code',
    label: '角色码',
    component: 'Input',
    componentProps: { allowClear: true, placeholder: '模糊搜索' },
  },
  {
    fieldName: 'name',
    label: '角色名',
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

const columns: VxeTableGridColumns<SystemApi.Role> = [
  { field: 'code', title: '角色码', width: 150 },
  { field: 'name', title: '角色名', minWidth: 140 },
  { field: 'status', title: '状态', width: 80, slots: { default: 'status' } },
  { field: 'sort', title: '排序', width: 80 },
  { field: 'remark', title: '备注', minWidth: 200 },
  {
    field: 'action',
    title: '操作',
    width: 300,
    fixed: 'right',
    slots: { default: 'action' },
  },
];

const [Grid, gridApi] = useVbenVxeGrid<SystemApi.Role>({
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
          return getRolesApi({
            ...formValues,
            page: page.currentPage,
            pageSize: page.pageSize,
          });
        },
      },
    },
    toolbarConfig: { refresh: true, zoom: true },
  },
});

function buildSchema(role: SystemApi.Role | null): VbenFormSchema[] {
  return [
    {
      fieldName: 'code',
      label: '角色码',
      component: 'Input',
      rules: 'required',
      componentProps: {
        disabled: role?.code === 'super_admin',
        placeholder: '小写字母、数字、下划线',
      },
    },
    {
      fieldName: 'name',
      label: '角色名',
      component: 'Input',
      rules: 'required',
    },
    {
      fieldName: 'status',
      label: '状态',
      component: 'Select',
      componentProps: {
        disabled: role?.code === 'super_admin',
        options: statusOptions,
      },
    },
    {
      fieldName: 'sort',
      label: '排序',
      component: 'InputNumber',
      componentProps: { min: 0 },
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
  await formApi.setValues({ sort: 0, status: 1 });
}

async function openEdit(row: SystemApi.Role) {
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
  const payload = {
    code: values.code,
    name: values.name,
    remark: values.remark ?? '',
    sort: values.sort ?? 0,
    status: values.status,
  };
  saving.value = true;
  try {
    if (editing.value) {
      await updateRoleApi(editing.value.id, payload);
    } else {
      await createRoleApi(payload);
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

function remove(row: SystemApi.Role) {
  Modal.confirm({
    content: `确定删除角色「${row.name}」？`,
    okType: 'danger',
    title: '删除角色',
    async onOk() {
      await deleteRoleApi(row.id);
      message.success('已删除');
      await gridApi.reload();
    },
  });
}

async function openMenus(row: SystemApi.Role) {
  menuTarget.value = row;
  if (menuRows.value.length === 0) {
    menuRows.value = await getMenusApi();
  }
  checkedMenuIds.value = await getRoleMenuIdsApi(row.id);
  halfCheckedMenuIds.value = [];
  menuOpen.value = true;
}

function handleMenuCheck(_keys: any, info: any) {
  halfCheckedMenuIds.value = info?.halfCheckedKeys ?? [];
}

async function submitMenus() {
  if (!menuTarget.value) {
    return;
  }
  const ids = [
    ...new Set([...checkedMenuIds.value, ...halfCheckedMenuIds.value]),
  ];
  try {
    await assignRoleMenusApi(menuTarget.value.id, ids);
    message.success('菜单已更新');
    menuOpen.value = false;
    await gridApi.reload();
  } catch {
    // 请求层已统一提示错误
  }
}

async function openPermissions(row: SystemApi.Role) {
  permissionTarget.value = row;
  if (permissionRows.value.length === 0) {
    permissionRows.value = await getPermissionsApi();
  }
  checkedPermissionIds.value = await getRolePermissionIdsApi(row.id);
  permissionOpen.value = true;
}

async function submitPermissions() {
  if (!permissionTarget.value) {
    return;
  }
  try {
    await assignRolePermissionsApi(
      permissionTarget.value.id,
      checkedPermissionIds.value,
    );
    message.success('权限点已更新');
    permissionOpen.value = false;
    await gridApi.reload();
  } catch {
    // 请求层已统一提示错误
  }
}
</script>

<template>
  <div class="h-full p-4">
    <Grid>
      <template #toolbar-actions>
        <Button
          v-access:code="'system:role:create'"
          type="primary"
          @click="openCreate"
        >
          新增角色
        </Button>
      </template>
      <template #status="{ row }">
        <Tag :color="row.status === 1 ? 'green' : 'red'">
          {{ row.status === 1 ? '启用' : '停用' }}
        </Tag>
      </template>
      <template #action="{ row }">
        <Button
          v-if="row.code !== 'super_admin'"
          v-access:code="'system:role:update'"
          size="small"
          type="link"
          @click="openEdit(row)"
        >
          编辑
        </Button>
        <Button
          v-if="row.code !== 'super_admin'"
          v-access:code="'system:role:update'"
          size="small"
          type="link"
          @click="openMenus(row)"
        >
          分配菜单
        </Button>
        <Button
          v-access:code="'system:role:update'"
          size="small"
          type="link"
          @click="openPermissions(row)"
        >
          分配权限
        </Button>
        <Button
          v-if="row.code !== 'super_admin'"
          v-access:code="'system:role:delete'"
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
      :title="editing ? '编辑角色' : '新增角色'"
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

    <Modal
      v-model:open="menuOpen"
      :title="`分配菜单：${menuTarget?.name ?? ''}`"
      width="560"
      @ok="submitMenus"
    >
      <div class="max-h-96 overflow-auto py-2">
        <Tree
          v-model:checked-keys="checkedMenuIds"
          :default-expand-all="true"
          :tree-data="menuTreeData"
          checkable
          @check="handleMenuCheck"
        />
      </div>
    </Modal>

    <Modal
      v-model:open="permissionOpen"
      :title="`分配权限：${permissionTarget?.name ?? ''}`"
      width="640"
      @ok="submitPermissions"
    >
      <div class="max-h-96 overflow-auto py-2">
        <Checkbox.Group
          v-model:value="checkedPermissionIds"
          :options="permissionOptions"
          class="flex flex-col gap-2"
        />
      </div>
    </Modal>
  </div>
</template>
