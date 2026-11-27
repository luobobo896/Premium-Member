import { requestClient } from '#/api/request';

export namespace TradeApi {
  export interface Plan {
    aiImageQuota: number;
    aiVideoQuota: number;
    benefitQuota: number;
    code: string;
    createdAt: string;
    durationDays: number;
    id: number;
    name: string;
    priceCents: number;
    status: number;
    versionNo: number;
  }

  export interface PlanPayload {
    aiImageQuota?: number;
    aiVideoQuota?: number;
    benefitQuota?: number;
    code: string;
    durationDays?: number;
    name: string;
    priceCents: number;
    status?: number;
  }

  export interface OrderRow {
    aiImageQuota: number;
    aiVideoQuota: number;
    amountCents: number;
    benefitQuota: number;
    channel: string;
    createdAt: string;
    durationDays: number;
    id: number;
    memberId: number;
    memberName: string;
    memberNo: string;
    orderNo: string;
    paidAt: null | string;
    periodEndAt: null | string;
    periodId: null | number;
    periodNo: null | string;
    periodStartAt: null | string;
    periodStatus: null | string;
    phone: string;
    planCode: string;
    planName: string;
    planVersionId: number;
    planVersionNo: number;
    purpose: string;
    remark: string;
    source: string;
    status: string;
    userId: number;
  }

  export interface OrderDetail {
    order: OrderRow;
    payments: PaymentTransaction[];
    refunds: Refund[];
  }

  export interface PaymentTransaction {
    amountCents: number;
    channel: string;
    createdAt: string;
    id: number;
    orderId: number;
    paidAt: null | string;
    rawPayload: string;
    status: string;
    txnNo: string;
  }

  export interface Refund {
    amountCents: number;
    createdAt: string;
    id: number;
    operator: string;
    orderId: number;
    processedAt: null | string;
    reason: string;
    refundNo: string;
    status: string;
    updatedAt: string;
  }

  export interface PaymentRow {
    amountCents: number;
    channel: string;
    createdAt: string;
    id: number;
    memberName: string;
    memberNo: string;
    orderId: number;
    orderNo: string;
    paidAt: null | string;
    status: string;
    txnNo: string;
  }

  export interface Page<T> {
    items: T[];
    total: number;
  }
}

export function getPlansApi() {
  return requestClient.get<TradeApi.Plan[]>('/system/plans');
}

export function createPlanApi(data: TradeApi.PlanPayload) {
  return requestClient.post<TradeApi.Plan>('/system/plans', data);
}

export function updatePlanApi(id: number, data: TradeApi.PlanPayload) {
  return requestClient.put<TradeApi.Plan>(`/system/plans/${id}`, data);
}

export function deletePlanApi(id: number) {
  return requestClient.delete(`/system/plans/${id}`);
}

export function getOrdersApi(params: {
  memberId?: number;
  orderNo?: string;
  page: number;
  pageSize: number;
  status?: string;
}) {
  return requestClient.get<TradeApi.Page<TradeApi.OrderRow>>(
    '/system/orders',
    { params },
  );
}

export function getOrderApi(orderNo: string) {
  return requestClient.get<TradeApi.OrderDetail>(
    `/system/orders/${encodeURIComponent(orderNo)}`,
  );
}

export function refundOrderApi(
  orderNo: string,
  data: { amountCents: number; reason: string; result: 'APPROVE' | 'REJECT' },
) {
  return requestClient.post(
    `/system/orders/${encodeURIComponent(orderNo)}/refund`,
    data,
  );
}

export function getPaymentsApi(params: {
  channel?: string;
  orderNo?: string;
  page: number;
  pageSize: number;
  txnNo?: string;
}) {
  return requestClient.get<TradeApi.Page<TradeApi.PaymentRow>>(
    '/system/payments',
    { params },
  );
}
