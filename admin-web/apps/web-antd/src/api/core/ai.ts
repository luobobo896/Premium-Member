import { requestClient } from '#/api/request';

export namespace AiApi {
  export interface Template {
    category: string;
    code: string;
    coverUrl: string;
    createdAt: string;
    id: number;
    name: string;
    params: string;
    sort: number;
    status: number;
    type: 'DRESSUP' | 'PHOTO' | 'VIDEO';
    updatedAt: string;
    versionNo: number;
  }

  export interface TemplatePayload {
    category?: string;
    code: string;
    coverUrl?: string;
    name: string;
    params?: string;
    sort?: number;
    status?: number;
    type: 'DRESSUP' | 'PHOTO' | 'VIDEO';
  }

  export interface TaskRow {
    costCents: number;
    createdAt: string;
    failReason: string;
    finishedAt: null | string;
    id: number;
    memberId: number;
    memberName: string;
    memberNo: string;
    periodId: number;
    periodNo: string;
    phone: string;
    status: string;
    supplierTaskNo: string;
    taskNo: string;
    templateCode: string;
    templateId: number;
    templateName: string;
    type: string;
  }

  export interface UsageRow {
    costCents: number;
    day: string;
    failed: number;
    imageTasks: number;
    rejected: number;
    success: number;
    total: number;
    unknown: number;
    videoTasks: number;
  }

  export interface Page<T> {
    items: T[];
    total: number;
  }
}

export function getAiTemplatesApi() {
  return requestClient.get<AiApi.Template[]>('/system/ai/templates');
}

export function createAiTemplateApi(data: AiApi.TemplatePayload) {
  return requestClient.post<AiApi.Template>('/system/ai/templates', data);
}

export function updateAiTemplateApi(id: number, data: AiApi.TemplatePayload) {
  return requestClient.put<AiApi.Template>(`/system/ai/templates/${id}`, data);
}

export function deleteAiTemplateApi(id: number) {
  return requestClient.delete(`/system/ai/templates/${id}`);
}

export function getAiTasksApi(params: {
  memberId?: number;
  page: number;
  pageSize: number;
  status?: string;
  taskNo?: string;
  type?: string;
}) {
  return requestClient.get<AiApi.Page<AiApi.TaskRow>>('/system/ai/tasks', {
    params,
  });
}

export function getAiUsageApi(params: { from?: string; to?: string }) {
  return requestClient.get<AiApi.UsageRow[]>('/system/ai/usage', { params });
}
