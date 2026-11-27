const API_BASE = 'http://127.0.0.1:8081';
const TOKEN_KEY = 'c_access_token';

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

function request<T>(
  path: string,
  method: 'GET' | 'POST',
  data?: Record<string, string>,
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

export async function logout(): Promise<void> {
  try {
    await request<void>('/c/auth/logout', 'POST');
  } finally {
    clearToken();
  }
}
