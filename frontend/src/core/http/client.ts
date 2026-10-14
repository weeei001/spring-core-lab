import axios, { type AxiosResponse } from 'axios';
import type { Result } from './types';

/** 開始登入的網址；使用者按登入按鈕時整頁跳轉，由 web-server 導向 authentik */
export const LOGIN_URL = '/auth/login/authentik';

/** 後端回傳的錯誤；message 可以直接給使用者看 */
export class ApiError extends Error {
  readonly status: number | undefined;

  constructor(message: string, status?: number) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

/**
 * 所有 API 共用的 axios。同網域請求時 axios 會自動把 XSRF-TOKEN cookie 放進 X-XSRF-TOKEN header，
 * 後端的 CSRF 檢查才會通過。
 */
export const http = axios.create({ headers: { 'Content-Type': 'application/json' } });

function isResult(value: unknown): value is Result<unknown> {
  return typeof value === 'object' && value !== null && 'result' in value && 'message' in value;
}

/**
 * 送出請求並取出 Result 的 data。失敗時丟 ApiError（含 HTTP 狀態，401 由 query-client 處理）。
 * 用法：send(http.get<Result<Product>>(url))，回應型別由 axios 的泛型宣告。
 */
export async function send<T>(request: Promise<AxiosResponse<Result<T>>>): Promise<T> {
  try {
    const response = await request;
    return response.data.data;
  } catch (error: unknown) {
    if (!axios.isAxiosError(error)) {
      throw error;
    }
    const status = error.response?.status;
    // 回應不一定是 Result（例如 web-server 沒啟動時是 proxy 的錯誤頁）
    const body: unknown = error.response?.data;
    const fallback = status === undefined ? '無法連線到伺服器' : `請求失敗（HTTP ${String(status)}）`;
    throw new ApiError(isResult(body) && body.message !== null ? body.message : fallback, status);
  }
}
