import { requestClient } from '#/api/request';

export namespace SubscriptionApi {
  export interface SubscriptionRow {
    balancePoints: number;
    failureReason: string;
    graceUntil: null | string;
    id: number;
    lastChargeAt: null | string;
    lastChargeReason: null | string;
    lastChargeStatus: null | string;
    lastChargeTime: null | string;
    memberId: number;
    memberName: string;
    memberNo: string;
    monthlyPoints: number;
    nextChargeAt: null | string;
    phone: string;
    planName: string;
    priceCents: number;
    status: string;
    version: number;
  }

  export interface ChargeRow {
    amountCents: number;
    attemptNo: number;
    channelTxnNo: string;
    chargeNo: string;
    createdAt: string;
    failReason: string;
    finishedAt: null | string;
    id: number;
    memberId: number;
    memberName: string;
    memberNo: string;
    period: string;
    phone: string;
    provider: string;
    status: string;
    subscriptionId: number;
  }

  export interface BillingRun {
    failed: number;
    granted: number;
    period: string;
    processed: number;
    suspended: number;
  }

  export interface Page<T> {
    items: T[];
    total: number;
  }
}

export function getSubscriptionsApi(params: {
  memberNo?: string;
  page: number;
  pageSize: number;
  status?: string;
}) {
  return requestClient.get<SubscriptionApi.Page<SubscriptionApi.SubscriptionRow>>(
    '/system/subscriptions',
    { params },
  );
}

export function getSubscriptionChargesApi(params: {
  memberNo?: string;
  page: number;
  pageSize: number;
  period?: string;
  status?: string;
  subscriptionId?: number;
}) {
  return requestClient.get<SubscriptionApi.Page<SubscriptionApi.ChargeRow>>(
    '/system/subscriptions/charges',
    { params },
  );
}

export function grantSubscriptionPointsApi(
  id: number,
  data: { points: number; reason: string },
) {
  return requestClient.post(`/system/subscriptions/${id}/grant-points`, data);
}

export function runSubscriptionBillingApi(data: {
  graceDays?: number;
  mockResult?: string;
  period?: string;
  subscriptionId?: number;
}) {
  return requestClient.post<SubscriptionApi.BillingRun>(
    '/system/subscriptions/run-billing',
    data,
  );
}

export function runSubscriptionDunningApi(data: {
  forceSuspend?: boolean;
  mockResult?: string;
  subscriptionId?: number;
}) {
  return requestClient.post<SubscriptionApi.BillingRun>(
    '/system/subscriptions/run-dunning',
    data,
  );
}
