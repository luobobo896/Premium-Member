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

export async function logout(): Promise<void> {
  try {
    await request<void>('/c/auth/logout', { method: 'POST' });
  } finally {
    clearAccessToken();
  }
}
