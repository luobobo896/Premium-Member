import { requestClient } from '#/api/request';

export namespace ContentApi {
  export interface Item {
    id: number;
    key: string;
    locale: string;
    contentJson: string;
    versionNo: number;
    status: 'DRAFT' | 'PUBLISHED';
    publishAt: null | string;
    updatedBy: string;
    createdAt: string;
    updatedAt: string;
  }
}

export function getContentsApi(params?: { key?: string; locale?: string }) {
  return requestClient.get<ContentApi.Item[]>('/system/contents', { params });
}

export function getContentApi(id: number) {
  return requestClient.get<ContentApi.Item>(`/system/contents/${id}`);
}

export function saveContentDraftApi(data: {
  contentJson: string;
  key: string;
  locale?: string;
}) {
  return requestClient.post<ContentApi.Item>('/system/contents', data);
}

export function updateContentDraftApi(id: number, contentJson: string) {
  return requestClient.put<ContentApi.Item>(`/system/contents/${id}`, {
    contentJson,
  });
}

export function publishContentApi(id: number) {
  return requestClient.post<ContentApi.Item>(`/system/contents/${id}/publish`);
}

export function rollbackContentApi(id: number) {
  return requestClient.post<ContentApi.Item>(
    `/system/contents/${id}/rollback`,
  );
}
