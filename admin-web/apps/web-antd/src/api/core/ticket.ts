import { requestClient } from '#/api/request';

export namespace TicketApi {
  export interface Row {
    id: number;
    ticketNo: string;
    userType: string;
    memberId: null | number;
    memberNo: null | string;
    memberName: null | string;
    phone: null | string;
    userId: null | number;
    orderNo: string;
    category: string;
    title: string;
    content: string;
    status: string;
    priority: string;
    assigneeAdminId: null | number;
    assigneeName: null | string;
    remark: string;
    replyCount: number;
    createdAt: string;
    updatedAt: string;
    resolvedAt: null | string;
  }

  export interface Reply {
    id: number;
    senderType: string;
    senderId: null | number;
    senderName: string;
    content: string;
    createdAt: string;
  }

  export interface Detail {
    ticket: Row;
    replies: Reply[];
  }

  export interface Page<T> {
    items: T[];
    total: number;
  }
}

export function getTicketsApi(params: {
  category?: string;
  from?: string;
  keyword?: string;
  page: number;
  pageSize: number;
  status?: string;
  to?: string;
}) {
  return requestClient.get<TicketApi.Page<TicketApi.Row>>('/system/tickets', {
    params,
  });
}

export function getTicketApi(ticketNo: string) {
  return requestClient.get<TicketApi.Detail>(
    `/system/tickets/${encodeURIComponent(ticketNo)}`,
  );
}

export function acceptTicketApi(ticketNo: string) {
  return requestClient.post<TicketApi.Detail>(
    `/system/tickets/${encodeURIComponent(ticketNo)}/accept`,
  );
}

export function replyTicketApi(ticketNo: string, content: string) {
  return requestClient.post<TicketApi.Detail>(
    `/system/tickets/${encodeURIComponent(ticketNo)}/replies`,
    { content },
  );
}

export function updateTicketApi(
  ticketNo: string,
  data: {
    assigneeAdminId?: number;
    priority?: string;
    remark?: string;
    status?: string;
  },
) {
  return requestClient.put<TicketApi.Detail>(
    `/system/tickets/${encodeURIComponent(ticketNo)}`,
    data,
  );
}
