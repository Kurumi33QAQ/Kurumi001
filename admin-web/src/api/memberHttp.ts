import axios, { type AxiosError, type AxiosInstance, type AxiosRequestConfig, type InternalAxiosRequestConfig } from "axios";
import { ElMessage } from "element-plus";
import type { ApiResponse } from "@/types/api";
import { clearMemberAccessToken, getMemberAccessToken, setMemberAccessToken } from "@/utils/memberAuth";

const rawBaseUrl = (import.meta.env.VITE_API_BASE_URL || "").trim();
const BASE_URL = !rawBaseUrl || rawBaseUrl.includes("/demo/file/upload") ? "/api" : rawBaseUrl;

const memberService: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: 12000
});

type RetryRequestConfig = InternalAxiosRequestConfig & {
  _retry?: boolean;
};

function isWrappedApiResponse(data: unknown): data is ApiResponse<unknown> {
  return Boolean(data && typeof data === "object" && "code" in data && "message" in data);
}

function normalizeBizCode(code: unknown) {
  if (typeof code === "number") return code;
  if (typeof code === "string" && code.trim() !== "") {
    const parsed = Number(code);
    return Number.isNaN(parsed) ? -1 : parsed;
  }
  return -1;
}

function isRefreshRequest(url?: string) {
  return Boolean(url?.includes("/member/auth/token/refresh"));
}

let refreshPromise: Promise<string> | null = null;

async function requestTokenRefresh() {
  const currentToken = getMemberAccessToken();
  if (!currentToken) {
    throw new Error("买家未登录");
  }

  const response = await axios.post<ApiResponse<{ token: string }>>(
    `${BASE_URL}/member/auth/token/refresh`,
    null,
    { headers: { Authorization: `Bearer ${currentToken}` } }
  );

  const data = response.data;
  const code = normalizeBizCode(data?.code);
  if (!isWrappedApiResponse(data) || code !== 200 || !data.data?.token) {
    throw new Error(data?.message || "买家登录状态刷新失败");
  }

  setMemberAccessToken(data.data.token);
  return data.data.token;
}

async function refreshTokenAndRetry(config: RetryRequestConfig) {
  if (!refreshPromise) {
    refreshPromise = requestTokenRefresh().finally(() => {
      refreshPromise = null;
    });
  }
  const newToken = await refreshPromise;
  config.headers = config.headers || {};
  config.headers.Authorization = `Bearer ${newToken}`;
  config._retry = true;
  return memberService(config);
}

memberService.interceptors.request.use((config) => {
  const token = getMemberAccessToken();
  if (token) {
    config.headers = config.headers || {};
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

memberService.interceptors.response.use(
  async (response) => {
    const responseData = response.data;
    if (!isWrappedApiResponse(responseData)) {
      return responseData;
    }

    const bizCode = normalizeBizCode(responseData.code);
    if (bizCode === 200) {
      return responseData.data;
    }

    const config = response.config as RetryRequestConfig;
    const message = responseData.message || "请求失败";
    if (bizCode === 401 && !config._retry && !isRefreshRequest(config.url) && getMemberAccessToken()) {
      try {
        return await refreshTokenAndRetry(config);
      } catch {
        clearMemberAccessToken();
        ElMessage.error("买家登录状态已失效，请重新登录");
        return Promise.reject(new Error(message));
      }
    }

    ElMessage.error(message);
    return Promise.reject(new Error(message));
  },
  async (error: AxiosError) => {
    const config = (error.config || {}) as RetryRequestConfig;
    const status = error.response?.status;
    if (status === 401 && !config._retry && !isRefreshRequest(config.url) && getMemberAccessToken()) {
      try {
        return await refreshTokenAndRetry(config);
      } catch {
        clearMemberAccessToken();
        ElMessage.error("买家登录状态已失效，请重新登录");
        return Promise.reject(error);
      }
    }

    ElMessage.error(error.message || "请求失败，请稍后重试");
    return Promise.reject(error);
  }
);

export function memberRequest<T = unknown>(config: AxiosRequestConfig) {
  return memberService.request<unknown, T>(config);
}


