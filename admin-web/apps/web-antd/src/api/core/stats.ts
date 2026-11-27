import { requestClient } from '#/api/request';

export namespace StatsApi {
  export interface DailyRow {
    day: string;
    orderCount: number;
    paidAmountCents: number;
    activeMembers: number;
    benefitApplied: number;
    benefitSuccess: number;
    benefitException: number;
    aiImageTasks: number;
    aiVideoTasks: number;
  }

  export interface BlobResponse {
    data: Blob;
  }
}

export function getDailyStatsApi(params: {
  from?: string;
  source?: string;
  to?: string;
}) {
  return requestClient.get<StatsApi.DailyRow[]>('/system/stats/daily', {
    params,
  });
}

export function exportStatsApi(params: {
  from?: string;
  source?: string;
  to?: string;
}) {
  return requestClient.get<StatsApi.BlobResponse>('/system/stats/export', {
    params,
    responseReturn: 'raw',
    responseType: 'blob',
  });
}
