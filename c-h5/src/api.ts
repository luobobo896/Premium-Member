const API_BASE = import.meta.env.VITE_C_API_BASE ?? 'http://127.0.0.1:8081';
const TOKEN_KEY = 'c_access_token';

export interface CUserInfo {
  id: number;
  phone: string;
  nickname: string;
  avatar: string;
}

export interface LoginData {
  accessToken: string;
  user: CUserInfo;
}

export interface MemberInfo {
  id: number;
  memberNo: string;
  username: string;
  nickname: string;
  phone: string;
  level: string;
  points: number;
  balance: number;
  status: number;
  joinedAt: string;
}

export interface BenefitPeriod {
  endAt: string;
  id: number;
  periodNo: string;
  startAt: string;
}

export interface BenefitProduct {
  cardMasked: string | null;
  claimed: boolean;
  claimStatus: string | null;
  claimedAt: string | null;
  description: string;
  inStock: boolean;
  name: string;
  rules: string;
  skuCode: string;
}

export interface BenefitCenter {
  period: BenefitPeriod | null;
  products: BenefitProduct[];
  quotaRemaining: number;
  quotaTotal: number;
  quotaUsed: number;
}

export interface BenefitClaimResult {
  alreadyClaimed: boolean;
  cardCode: string | null;
  cardMasked: string | null;
  claimId: number;
  claimedAt: string | null;
  productName: string;
  productSku: string;
  redemption: string | null;
  status: string;
}

export interface BenefitClaimHistory {
  cardMasked: string | null;
  claimId: number;
  claimedAt: string | null;
  productName: string;
  productSku: string;
  redemption: string | null;
  remark: string;
  status: string;
}

export interface PlanInfo {
  aiImageQuota: number;
  aiVideoQuota: number;
  benefitQuota: number;
  code: string;
  durationDays: number;
  id: number;
  name: string;
  priceCents: number;
  versionNo: number;
}

export interface OrderPeriod {
  endAt: string;
  id: number;
  periodNo: string;
  startAt: string;
  status: string;
}

export interface OrderInfo {
  amountCents: number;
  channel: string;
  createdAt: string;
  id: number;
  orderNo: string;
  paidAt: string | null;
  payMode: string | null;
  period: OrderPeriod | null;
  plan: PlanInfo | null;
  remark: string;
  source: string;
  status: string;
}

interface ApiResponse<T> {
  code: number;
  data: T;
  error: string | null;
  message: string;
  bizCode?: string;
}

export class ApiError extends Error {
  readonly status: number;
  readonly bizCode?: string;

  constructor(message: string, status: number, bizCode?: string) {
    super(message);
    this.status = status;
    this.bizCode = bizCode;
  }
}

export function getAccessToken(): string {
  return sessionStorage.getItem(TOKEN_KEY) ?? '';
}

function setAccessToken(token: string): void {
  sessionStorage.setItem(TOKEN_KEY, token);
}

export function clearAccessToken(): void {
  sessionStorage.removeItem(TOKEN_KEY);
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = getAccessToken();
  const headers = new Headers(init.headers);
  headers.set('Accept', 'application/json');
  if (init.body) {
    headers.set('Content-Type', 'application/json');
  }
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  let response: Response;
  try {
    response = await fetch(`${API_BASE}${path}`, {
      ...init,
      headers,
      credentials: 'include',
      signal: AbortSignal.timeout(10_000),
    });
  } catch {
    throw new ApiError('网络连接失败，请稍后重试', 0);
  }

  const body = (await response.json().catch(() => null)) as ApiResponse<T> | null;
  if (!response.ok || !body || body.code !== 0) {
    throw new ApiError(body?.error || body?.message || '请求失败', response.status, body?.bizCode);
  }
  return body.data;
}

export async function login(phone: string, password: string): Promise<LoginData> {
  const data = await request<LoginData>('/c/auth/login', {
    method: 'POST',
    body: JSON.stringify({ phone, password }),
  });
  setAccessToken(data.accessToken);
  return data;
}

export function getInfo(): Promise<CUserInfo> {
  return request<CUserInfo>('/c/user/info');
}

export function getMember(): Promise<MemberInfo> {
  return request<MemberInfo>('/c/members/me');
}

export function getBenefits(): Promise<BenefitCenter> {
  return request<BenefitCenter>('/c/benefits');
}

export function claimBenefit(sku: string): Promise<BenefitClaimResult> {
  return request<BenefitClaimResult>(`/c/benefits/${encodeURIComponent(sku)}/claim`, {
    method: 'POST',
  });
}

export function getBenefitClaims(): Promise<BenefitClaimHistory[]> {
  return request<BenefitClaimHistory[]>('/c/benefits/claims');
}

export function getPlans(): Promise<PlanInfo[]> {
  return request<PlanInfo[]>('/c/plans');
}

export function createOrder(planVersionId: number): Promise<OrderInfo> {
  return request<OrderInfo>('/c/orders', {
    method: 'POST',
    body: JSON.stringify({ planVersionId, source: 'H5' }),
  });
}

export function mockPay(orderNo: string): Promise<OrderInfo> {
  return request<OrderInfo>('/c/payments/mock/notify', {
    method: 'POST',
    body: JSON.stringify({ orderNo }),
  });
}

export function getOrders(): Promise<OrderInfo[]> {
  return request<OrderInfo[]>('/c/orders');
}

export function getOrder(orderNo: string): Promise<OrderInfo> {
  return request<OrderInfo>(`/c/orders/${encodeURIComponent(orderNo)}`);
}

export async function logout(): Promise<void> {
  try {
    await request<void>('/c/auth/logout', { method: 'POST' });
  } finally {
    clearAccessToken();
  }
}
