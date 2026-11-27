const API_BASE = 'http://127.0.0.1:8081';
const TOKEN_KEY = 'c_access_token';
const LOGIN_REDIRECT_KEY = 'pm_login_redirect';
const PENDING_CLAIM_KEY = 'pm_pending_claim';

export interface LoginData {
  accessToken: string;
  user: {
    id: number;
    phone: string;
    nickname: string;
    avatar: string;
  };
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

export interface BenefitCatalogProduct {
  cover: string;
  description: string;
  name: string;
  rules: string;
  skuCode: string;
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

export type AiType = 'DRESSUP' | 'PHOTO' | 'VIDEO';

export interface AiTemplate {
  category: string;
  code: string;
  coverUrl: string;
  id: number;
  name: string;
  params: string;
  sort: number;
  type: AiType;
}

export interface AiAsset {
  id: number;
  mime: string;
  purpose: string;
  size: number;
  url: string;
}

export interface AiQuotaPart {
  limit: number;
  remaining: number;
  used: number;
}

export interface AiQuota {
  image: AiQuotaPart;
  period: BenefitPeriod | null;
  video: AiQuotaPart;
}

export interface AiTask {
  createdAt: string;
  failReason: string;
  finishedAt: string | null;
  id: number;
  inputUrl: string | null;
  outputMime: string | null;
  outputUrl: string | null;
  status: string;
  taskNo: string;
  templateName: string;
  type: AiType;
}

export interface ContentItem {
  key: string;
  locale: string;
  contentJson: string;
  versionNo: number;
  publishAt: string;
}

export interface TicketReply {
  senderType: string;
  senderName: string;
  content: string;
  createdAt: string;
}

export interface TicketInfo {
  id: number;
  ticketNo: string;
  orderNo: string;
  category: string;
  title: string;
  content: string;
  status: string;
  priority: string;
  replyCount: number;
  createdAt: string;
  updatedAt: string;
  resolvedAt: string | null;
  replies?: TicketReply[] | null;
}

export interface TicketCreatePayload {
  title: string;
  content: string;
  category: string;
  orderNo?: string;
  priority?: string;
}

interface ApiResponse<T> {
  code: number;
  data: T;
  error: string | null;
  message: string;
  bizCode?: string;
}

export class ApiError extends Error {
  readonly statusCode: number;
  readonly bizCode?: string;

  constructor(message: string, statusCode: number, bizCode?: string) {
    super(message);
    this.statusCode = statusCode;
    this.bizCode = bizCode;
  }
}

export function getToken(): string {
  return uni.getStorageSync(TOKEN_KEY) || '';
}

export function setToken(token: string): void {
  uni.setStorageSync(TOKEN_KEY, token);
}

export function clearToken(): void {
  uni.removeStorageSync(TOKEN_KEY);
}

export function requireLogin(redirect: string, pendingClaimSku = ''): void {
  uni.setStorageSync(LOGIN_REDIRECT_KEY, redirect);
  if (pendingClaimSku) {
    uni.setStorageSync(PENDING_CLAIM_KEY, pendingClaimSku);
  }
  uni.navigateTo({ url: '/pages/login/login' });
}

export function consumeLoginRedirect(): string {
  const redirect = uni.getStorageSync(LOGIN_REDIRECT_KEY) || '';
  uni.removeStorageSync(LOGIN_REDIRECT_KEY);
  return redirect;
}

export function consumePendingClaim(): string {
  const sku = uni.getStorageSync(PENDING_CLAIM_KEY) || '';
  uni.removeStorageSync(PENDING_CLAIM_KEY);
  return sku;
}

function request<T>(
  path: string,
  method: 'DELETE' | 'GET' | 'POST',
  data?: object,
): Promise<T> {
  return new Promise((resolve, reject) => {
    uni.request({
      url: `${API_BASE}${path}`,
      method,
      data,
      timeout: 10000,
      withCredentials: true,
      header: {
        Accept: 'application/json',
        ...(getToken() ? { Authorization: `Bearer ${getToken()}` } : {}),
      },
      success: (response) => {
        const body = response.data as ApiResponse<T>;
        if (response.statusCode >= 200 && response.statusCode < 300 && body?.code === 0) {
          resolve(body.data);
          return;
        }
        reject(
          new ApiError(
            body?.error || body?.message || '请求失败',
            response.statusCode,
            body?.bizCode,
          ),
        );
      },
      fail: () => reject(new ApiError('网络连接失败，请稍后重试', 0)),
    });
  });
}

export async function login(phone: string, password: string): Promise<LoginData> {
  const data = await request<LoginData>('/c/auth/login', 'POST', { phone, password });
  setToken(data.accessToken);
  return data;
}

export function getInfo(): Promise<{ id: number; phone: string; nickname: string }> {
  return request('/c/user/info', 'GET');
}

export function getMember(): Promise<MemberInfo> {
  return request('/c/members/me', 'GET');
}

export function getBenefits(): Promise<BenefitCenter> {
  return request('/c/benefits', 'GET');
}

export function getBenefitCatalog(): Promise<BenefitCatalogProduct[]> {
  return request('/c/benefits/products', 'GET');
}

export function claimBenefit(sku: string): Promise<BenefitClaimResult> {
  return request(`/c/benefits/${encodeURIComponent(sku)}/claim`, 'POST');
}

export function getBenefitClaims(): Promise<BenefitClaimHistory[]> {
  return request('/c/benefits/claims', 'GET');
}

export function assetUrl(path: null | string): string {
  if (!path) return '';
  return path.startsWith('http') ? path : `${API_BASE}${path}`;
}

export function getAiTemplates(type?: AiType): Promise<AiTemplate[]> {
  return request(`/c/ai/templates${type ? `?type=${type}` : ''}`, 'GET');
}

export function getAiQuota(): Promise<AiQuota> {
  return request('/c/quota', 'GET');
}

export function getAiTasks(): Promise<AiTask[]> {
  return request('/c/ai/tasks', 'GET');
}

export function getAiTask(taskNo: string): Promise<AiTask> {
  return request(`/c/ai/tasks/${encodeURIComponent(taskNo)}`, 'GET');
}

export function createAiTask(data: {
  idempotencyKey: string;
  inputAssetId: number;
  templateId: number;
}): Promise<AiTask> {
  return request('/c/ai/tasks', 'POST', data);
}

export function deleteAiWork(id: number): Promise<void> {
  return request(`/c/ai/works/${id}`, 'DELETE');
}

export function getContents(keys?: string[], locale = 'zh-CN'): Promise<ContentItem[]> {
  const search = [`locale=${encodeURIComponent(locale)}`];
  if (keys?.length) {
    search.push(`keys=${encodeURIComponent(keys.join(','))}`);
  }
  return request(`/c/contents?${search.join('&')}`, 'GET');
}

export function getTickets(): Promise<TicketInfo[]> {
  return request('/c/tickets', 'GET');
}

export function getTicket(ticketNo: string): Promise<TicketInfo> {
  return request(`/c/tickets/${encodeURIComponent(ticketNo)}`, 'GET');
}

export function createTicket(payload: TicketCreatePayload): Promise<TicketInfo> {
  return request('/c/tickets', 'POST', payload);
}

export function uploadAiAsset(filePath: string): Promise<AiAsset> {
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: `${API_BASE}/c/ai/assets`,
      filePath,
      name: 'file',
      header: getToken() ? { Authorization: `Bearer ${getToken()}` } : {},
      success: (response) => {
        let body: ApiResponse<AiAsset> | null = null;
        try {
          body = JSON.parse(response.data) as ApiResponse<AiAsset>;
        } catch {
          reject(new ApiError('上传响应无法解析', response.statusCode));
          return;
        }
        if (response.statusCode >= 200 && response.statusCode < 300 && body.code === 0) {
          resolve(body.data);
          return;
        }
        reject(new ApiError(body.error || body.message || '上传失败', response.statusCode, body.bizCode));
      },
      fail: () => reject(new ApiError('上传失败，请检查网络', 0)),
    });
  });
}

export async function logout(): Promise<void> {
  try {
    await request<void>('/c/auth/logout', 'POST');
  } finally {
    clearToken();
  }
}
