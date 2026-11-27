import { requestClient } from '#/api/request';

export namespace SystemApi {
  export interface PageQuery {
    page: number;
    pageSize: number;
  }

  export interface PageResult<T> {
    items: T[];
    total: number;
  }

  export interface User {
    createdAt: string;
    deptId: null | number;
    deptName: null | string;
    description: string;
    email: null | string;
    id: number;
    phone: null | string;
    realName: string;
    roleIds: number[];
    status: number;
    updatedAt: string;
    username: string;
  }

  export interface UserQuery extends PageQuery {
    deptId?: number;
    realName?: string;
    status?: number;
    username?: string;
  }

  export interface UserCreate {
    deptId?: null | number;
    description?: string;
    email?: string;
    password: string;
    phone?: string;
    realName: string;
    status: number;
    username: string;
  }

  export interface UserUpdate {
    deptId?: null | number;
    description?: string;
    email?: string;
    phone?: string;
    realName: string;
    status: number;
  }

  export interface Role {
    code: string;
    createdAt: string;
    dataScope: string;
    id: number;
    name: string;
    remark: string;
    sort: number;
    status: number;
    updatedAt: string;
  }

  export interface RoleQuery extends PageQuery {
    code?: string;
    name?: string;
    status?: number;
  }

  export interface RoleCreate {
    code: string;
    dataScope: string;
    name: string;
    remark?: string;
    sort: number;
    status: number;
  }

  export interface Permission {
    code: string;
    description: string;
    id: number;
    name: string;
    type: string;
  }

  export interface Menu {
    affixTab: number;
    badge: null | string;
    badgeType: null | string;
    badgeVariants: null | string;
    children?: Menu[];
    component: null | string;
    hideInMenu: number;
    icon: null | string;
    id: number;
    iframeSrc: null | string;
    keepAlive: number;
    link: null | string;
    name: string;
    parentId: number;
    path: string;
    permissionCode: null | string;
    redirect: null | string;
    sort: number;
    status: number;
    title: string;
    type: string;
    visible: number;
  }

  export interface MenuPayload {
    affixTab: number;
    badge?: null | string;
    badgeType?: null | string;
    badgeVariants?: null | string;
    component?: null | string;
    hideInMenu: number;
    icon?: null | string;
    iframeSrc?: null | string;
    keepAlive: number;
    link?: null | string;
    name: string;
    parentId: number;
    path?: string;
    permissionCode?: null | string;
    redirect?: null | string;
    sort: number;
    status: number;
    title: string;
    type: string;
    visible: number;
  }

  export interface Dept {
    children?: Dept[];
    id: number;
    leader: string;
    name: string;
    parentId: number;
    phone: string;
    sort: number;
    status: number;
  }

  export interface DeptPayload {
    leader?: string;
    name: string;
    parentId: number;
    phone?: string;
    sort: number;
    status: number;
  }

  export interface OperationLog {
    action: string;
    costMs: number;
    createdAt: string;
    errorCode: null | string;
    id: number;
    ip: string;
    method: string;
    module: string;
    params: string;
    path: string;
    success: number;
    userAgent: string;
    userId: null | number;
    username: string;
  }

  export interface OperationLogQuery extends PageQuery {
    endTime?: string;
    module?: string;
    startTime?: string;
    username?: string;
  }
}

export async function getUsersApi(params: SystemApi.UserQuery) {
  return requestClient.get<SystemApi.PageResult<SystemApi.User>>(
    '/system/users',
    { params },
  );
}

export async function createUserApi(data: SystemApi.UserCreate) {
  return requestClient.post<SystemApi.User>('/system/users', data);
}

export async function updateUserApi(
  id: number,
  data: SystemApi.UserUpdate,
) {
  return requestClient.put<SystemApi.User>(`/system/users/${id}`, data);
}

export async function deleteUserApi(id: number) {
  return requestClient.delete(`/system/users/${id}`);
}

export async function resetUserPasswordApi(id: number, password: string) {
  return requestClient.put(`/system/users/${id}/password`, { password });
}

export async function updateUserStatusApi(id: number, status: number) {
  return requestClient.put(`/system/users/${id}/status`, { status });
}

export async function assignUserRolesApi(id: number, roleIds: number[]) {
  return requestClient.put(`/system/users/${id}/roles`, { roleIds });
}

export async function getRolesApi(params: SystemApi.RoleQuery) {
  return requestClient.get<SystemApi.PageResult<SystemApi.Role>>(
    '/system/roles',
    { params },
  );
}

export async function getRoleOptionsApi() {
  return requestClient.get<SystemApi.Role[]>('/system/roles/options');
}

export async function createRoleApi(data: SystemApi.RoleCreate) {
  return requestClient.post<SystemApi.Role>('/system/roles', data);
}

export async function updateRoleApi(
  id: number,
  data: SystemApi.RoleCreate,
) {
  return requestClient.put<SystemApi.Role>(`/system/roles/${id}`, data);
}

export async function deleteRoleApi(id: number) {
  return requestClient.delete(`/system/roles/${id}`);
}

export async function getRoleMenuIdsApi(id: number) {
  return requestClient.get<number[]>(`/system/roles/${id}/menus`);
}

export async function assignRoleMenusApi(id: number, menuIds: number[]) {
  return requestClient.put(`/system/roles/${id}/menus`, { menuIds });
}

export async function getRolePermissionIdsApi(id: number) {
  return requestClient.get<number[]>(`/system/roles/${id}/permissions`);
}

export async function assignRolePermissionsApi(
  id: number,
  permissionIds: number[],
) {
  return requestClient.put(`/system/roles/${id}/permissions`, {
    permissionIds,
  });
}

export async function getPermissionsApi() {
  return requestClient.get<SystemApi.Permission[]>(
    '/system/roles/permissions',
  );
}

export async function getMenusApi(params?: {
  status?: number;
  title?: string;
}) {
  return requestClient.get<SystemApi.Menu[]>('/system/menus', { params });
}

export async function createMenuApi(data: SystemApi.MenuPayload) {
  return requestClient.post<SystemApi.Menu>('/system/menus', data);
}

export async function updateMenuApi(
  id: number,
  data: SystemApi.MenuPayload,
) {
  return requestClient.put<SystemApi.Menu>(`/system/menus/${id}`, data);
}

export async function deleteMenuApi(id: number) {
  return requestClient.delete(`/system/menus/${id}`);
}

export async function getDeptsApi(params?: {
  name?: string;
  status?: number;
}) {
  return requestClient.get<SystemApi.Dept[]>('/system/depts', { params });
}

export async function createDeptApi(data: SystemApi.DeptPayload) {
  return requestClient.post<SystemApi.Dept>('/system/depts', data);
}

export async function updateDeptApi(
  id: number,
  data: SystemApi.DeptPayload,
) {
  return requestClient.put<SystemApi.Dept>(`/system/depts/${id}`, data);
}

export async function deleteDeptApi(id: number) {
  return requestClient.delete(`/system/depts/${id}`);
}

export async function getOperationLogsApi(params: SystemApi.OperationLogQuery) {
  return requestClient.get<SystemApi.PageResult<SystemApi.OperationLog>>(
    '/system/logs',
    { params },
  );
}
