<script lang="ts" setup>
import type { SystemApi } from '#/api';

import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridColumns } from '#/adapter/vxe-table';

import { computed, nextTick, onMounted, ref } from 'vue';

import {
  Button,
  Checkbox,
  Drawer,
  Input,
  message,
  Modal,
  Tag,
} from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  assignUserRolesApi,
  createUserApi,
  deleteUserApi,
  getDeptsApi,
  getRoleOptionsApi,
  getUsersApi,
  resetUserPasswordApi,
  updateUserApi,
  updateUserStatusApi,
} from '#/api';

const deptOptions = ref<{ label: string; value: number }[]>([]);
const roleOptions = ref<SystemApi.Role[]>([]);
const editing = ref<SystemApi.User | null>(null);
const drawerOpen = ref(false);
const saving = ref(false);

const passwordOpen = ref(false);
const passwordValue = ref('');
const passwordTarget = ref<SystemApi.User | null>(null);

const roleOpen = ref(false);
const roleTarget = ref<SystemApi.User | null>(null);
const checkedRoleIds = ref<number[]>([]);

const statusOptions = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 },
];

function flattenDepts(
  nodes: SystemApi.Dept[],
  depth = 0,
): { label: string; value: number }[] {
  return nodes.flatMap((node) => [
    { label: `${'　'.repeat(depth)}${node.name}`, value: node.id },
    ...flattenDepts(node.children ?? [], depth + 1),
  ]);
}

function roleName(id: number) {
  return roleOptions.value.find((role) => role.id === id)?.name ?? id;
}

const searchSchema: VbenFormSchema[] = [
  {
    fieldName: 'username',
    label: '登录名',
    component: 'Input',
    componentProps: { allowClear: true, placeholder: '模糊搜索' },
  },
  {
    fieldName: 'realName',
    label: '昵称',
    component: 'Input',
    componentProps: { allowClear: true, placeholder: '模糊搜索' },
  },
  {
    fieldName: 'status',
    label: '状态',
    component: 'Select',
    componentProps: { allowClear: true, options: statusOptions },
  },
  {
    fieldName: 'deptId',
    label: '部门',
    component: 'Select',
    componentProps: {
      allowClear: true,
      optionFilterProp: 'label',
      options: [],
      showSearch: true,
    },
  },
];

const columns: VxeTableGridColumns<SystemApi.User> = [
  { field: 'username', title: '登录名', width: 100 },
  { field: 'realName', title: '昵称', width: 110 },
  { field: 'deptName', title: '部门', width: 100 },
  {
    field: 'roleIds',
    title: '角色',
    width: 140,
    slots: { default: 'roles' },
  },
  { field: 'phone', title: '手机号', width: 120 },
  { field: 'email', title: '邮箱', width: 170 },
  { field: 'status', title: '状态', width: 70, slots: { default: 'status' } },
  {
    field: 'action',
    title: '操作',
    width: 300,
    fixed: 'right',
    slots: { default: 'action' },
  },
];

const [Grid, gridApi] = useVbenVxeGrid<SystemApi.User>({
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
          return getUsersApi({
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

const baseFields = computed<VbenFormSchema[]>(() => [
  {
    fieldName: 'realName',
    label: '昵称',
    component: 'Input',
    rules: 'required',
  },
  {
    fieldName: 'deptId',
    label: '部门',
    component: 'Select',
    componentProps: {
      allowClear: true,
      options: deptOptions.value,
      placeholder: '未分配',
    },
  },
  { fieldName: 'email', label: '邮箱', component: 'Input' },
  { fieldName: 'phone', label: '手机号', component: 'Input' },
  {
    fieldName: 'status',
    label: '状态',
    component: 'Select',
    componentProps: { options: statusOptions },
  },
  {
    fieldName: 'description',
    label: '描述',
    component: 'Textarea',
    formItemClass: 'col-span-2',
  },
]);

const createSchema = computed<VbenFormSchema[]>(() => [
  {
    fieldName: 'username',
    label: '登录名',
    component: 'Input',
    rules: 'required',
    componentProps: { placeholder: '3-32 位字母、数字或下划线' },
  },
  {
    fieldName: 'password',
    label: '初始密码',
    component: 'InputPassword',
    rules: 'required',
    componentProps: { placeholder: '至少 6 位' },
  },
  ...baseFields.value,
]);

const editSchema = computed<VbenFormSchema[]>(() => [
  {
    fieldName: 'username',
    label: '登录名',
    component: 'Input',
    componentProps: { disabled: true },
  },
  ...baseFields.value,
]);

const [Form, formApi] = useVbenForm({
  schema: createSchema.value,
  showDefaultActions: false,
  wrapperClass: 'grid-cols-1 md:grid-cols-2',
});

async function openCreate() {
  editing.value = null;
  drawerOpen.value = true;
  formApi.setState({ schema: createSchema.value });
  await nextTick();
  await formApi.resetForm();
  await formApi.setValues({ status: 1 });
}

async function openEdit(row: SystemApi.User) {
  editing.value = row;
  drawerOpen.value = true;
  formApi.setState({ schema: editSchema.value });
  await nextTick();
  await formApi.resetForm();
  await formApi.setValues({ ...row, email: row.email ?? '' });
}

async function save() {
  const { valid } = await formApi.validate();
  if (!valid) {
    return;
  }
  const values = await formApi.getValues();
  saving.value = true;
  try {
    if (editing.value) {
      await updateUserApi(editing.value.id, {
        deptId: values.deptId ?? null,
        description: values.description ?? '',
        email: values.email ?? '',
        phone: values.phone ?? '',
        realName: values.realName,
        status: values.status,
      });
    } else {
      await createUserApi({
        deptId: values.deptId ?? null,
        description: values.description ?? '',
        email: values.email ?? '',
        password: values.password,
        phone: values.phone ?? '',
        realName: values.realName,
        status: values.status,
        username: values.username,
      });
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

function remove(row: SystemApi.User) {
  Modal.confirm({
    content: `确定删除用户「${row.username}」？`,
    okType: 'danger',
    title: '删除用户',
    async onOk() {
      await deleteUserApi(row.id);
      message.success('已删除');
      await gridApi.reload();
    },
  });
}

function toggleStatus(row: SystemApi.User) {
  const next = row.status === 1 ? 0 : 1;
  Modal.confirm({
    content: `确定${next === 1 ? '启用' : '停用'}用户「${row.username}」？`,
    title: `${next === 1 ? '启用' : '停用'}用户`,
    async onOk() {
      await updateUserStatusApi(row.id, next);
      message.success('状态已更新');
      await gridApi.reload();
    },
  });
}

function openPassword(row: SystemApi.User) {
  passwordTarget.value = row;
  passwordValue.value = '';
  passwordOpen.value = true;
}

async function submitPassword() {
  if (passwordValue.value.length < 6) {
    message.warning('密码至少 6 位');
    return;
  }
  if (!passwordTarget.value) {
    return;
  }
  try {
    await resetUserPasswordApi(passwordTarget.value.id, passwordValue.value);
    message.success('密码已重置');
    passwordOpen.value = false;
  } catch {
    // 请求层已统一提示错误
  }
}

function openRoles(row: SystemApi.User) {
  roleTarget.value = row;
  checkedRoleIds.value = [...row.roleIds];
  roleOpen.value = true;
}

async function submitRoles() {
  if (!roleTarget.value) {
    return;
  }
  try {
    await assignUserRolesApi(roleTarget.value.id, checkedRoleIds.value);
    message.success('角色已更新');
    roleOpen.value = false;
    await gridApi.reload();
  } catch {
    // 请求层已统一提示错误
  }
}

onMounted(async () => {
  deptOptions.value = flattenDepts(await getDeptsApi());
  roleOptions.value = await getRoleOptionsApi();
  await nextTick();
  gridApi.formApi?.updateSchema?.([
    {
      fieldName: 'deptId',
      componentProps: {
        allowClear: true,
        optionFilterProp: 'label',
        options: deptOptions.value,
        showSearch: true,
      },
    },
  ]);
});
</script>

<template>
  <div class="h-full p-4">
    <Grid>
      <template #toolbar-actions>
        <Button
          v-access:code="'system:user:create'"
          type="primary"
          @click="openCreate"
        >
          新增用户
        </Button>
      </template>
      <template #roles="{ row }">
        <Tag v-for="id in row.roleIds" :key="id" color="blue">
          {{ roleName(id) }}
        </Tag>
      </template>
      <template #status="{ row }">
        <Tag :color="row.status === 1 ? 'green' : 'red'">
          {{ row.status === 1 ? '启用' : '停用' }}
        </Tag>
      </template>
      <template #action="{ row }">
        <Button
          v-access:code="'system:user:update'"
          size="small"
          type="link"
          @click="openEdit(row)"
        >
          编辑
        </Button>
        <Button
          v-access:code="'system:user:update'"
          size="small"
          type="link"
          @click="openRoles(row)"
        >
          分配角色
        </Button>
        <Button
          v-access:code="'system:user:update'"
          size="small"
          type="link"
          @click="openPassword(row)"
        >
          重置密码
        </Button>
        <Button
          v-access:code="'system:user:update'"
          size="small"
          type="link"
          @click="toggleStatus(row)"
        >
          {{ row.status === 1 ? '停用' : '启用' }}
        </Button>
        <Button
          v-access:code="'system:user:delete'"
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
      :title="editing ? '编辑用户' : '新增用户'"
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
      v-model:open="passwordOpen"
      :title="`重置密码：${passwordTarget?.username ?? ''}`"
      @ok="submitPassword"
    >
      <Input.Password
        v-model:value="passwordValue"
        class="mt-4"
        placeholder="新密码，至少 6 位"
      />
    </Modal>

    <Modal
      v-model:open="roleOpen"
      :title="`分配角色：${roleTarget?.username ?? ''}`"
      @ok="submitRoles"
    >
      <Checkbox.Group v-model:value="checkedRoleIds" class="mt-4">
        <div class="flex flex-col gap-2">
          <Checkbox
            v-for="role in roleOptions"
            :key="role.id"
            :value="role.id"
          >
            {{ role.name }}（{{ role.code }}）
          </Checkbox>
        </div>
      </Checkbox.Group>
    </Modal>
  </div>
</template>
