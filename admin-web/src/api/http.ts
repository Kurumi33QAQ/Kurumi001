import axios, { type AxiosError, type AxiosInstance, type AxiosRequestConfig, type InternalAxiosRequestConfig } from "axios";
import { ElMessage } from "element-plus";
import type { ApiResponse } from "@/types/api";
import { clearAccessToken, getAccessToken, setAccessToken } from "@/utils/auth";

type UnauthorizedHandler = () => void;

const rawBaseUrl = (import.meta.env.VITE_API_BASE_URL || "").trim();
const BASE_URL = !rawBaseUrl || rawBaseUrl.includes("/demo/file/upload") ? "/api" : rawBaseUrl;

const service: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: 12000
});

let unauthorizedHandler: UnauthorizedHandler | null = null;
let refreshPromise: Promise<string> | null = null;

type RetryRequestConfig = InternalAxiosRequestConfig & {
  _retry?: boolean;
};

function isWrappedApiResponse(data: unknown): data is ApiResponse<unknown> {
  return Boolean(data && typeof data === "object" && "code" in data && "message" in data);
}

function isRefreshRequest(url?: string) {
  return Boolean(url?.includes("/demo/token/refresh"));
}

function normalizeBizCode(code: unknown) {
  if (typeof code === "number") return code;
  if (typeof code === "string" && code.trim() !== "") {
    const parsed = Number(code);
    return Number.isNaN(parsed) ? -1 : parsed;
  }
  return -1;
}

function triggerUnauthorized() {
  clearAccessToken();
  unauthorizedHandler?.();
}

function getErrorMessage(error: AxiosError) {
  const data = error.response?.data;
  if (isWrappedApiResponse(data)) {
    return data.message || "请求失败";
  }
  return error.message || "请求失败，请稍后重试";
}

async function requestTokenRefresh() {
  const currentToken = getAccessToken();
  if (!currentToken) {
    throw new Error("当前未登录");
  }

  const response = await axios.post<ApiResponse<{ token: string }>>(
    `${BASE_URL}/demo/token/refresh`,
    null,
    { headers: { Authorization: `Bearer ${currentToken}` } }
  );

  const data = response.data;
  const code = normalizeBizCode(data?.code);
  if (!isWrappedApiResponse(data) || code !== 200 || !data.data?.token) {
    throw new Error(data?.message || "刷新 token 失败");
  }

  setAccessToken(data.data.token);
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
  return service(config);
}

service.interceptors.request.use((config) => {
  const token = getAccessToken();
  if (token) {
    config.headers = config.headers || {};
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

service.interceptors.response.use(
  async (response) => {
    if (response.config.responseType === "blob") {
      return response;
    }

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

    if (bizCode === 401 && !config._retry && !isRefreshRequest(config.url) && getAccessToken()) {
      try {
        return await refreshTokenAndRetry(config);
      } catch {
        ElMessage.error("登录状态已失效，请重新登录");
        triggerUnauthorized();
        return Promise.reject(new Error(message));
      }
    }

    if (bizCode === 403) {
      ElMessage.error("无权限访问该功能");
    } else if (bizCode === 401) {
      ElMessage.error("登录状态已失效，请重新登录");
      triggerUnauthorized();
    } else {
      ElMessage.error(message);
    }

    return Promise.reject(new Error(message));
  },
  async (error: AxiosError) => {
    const config = (error.config || {}) as RetryRequestConfig;
    const status = error.response?.status;
    const unauthorized = status === 401;

    if (unauthorized && !config._retry && !isRefreshRequest(config.url) && getAccessToken()) {
      try {
        return await refreshTokenAndRetry(config);
      } catch {
        ElMessage.error("登录状态已失效，请重新登录");
        triggerUnauthorized();
        return Promise.reject(error);
      }
    }

    if (status === 403) {
      ElMessage.error("无权限访问该功能");
    } else if (unauthorized) {
      ElMessage.error("登录状态已失效，请重新登录");
      triggerUnauthorized();
    } else {
      ElMessage.error(getErrorMessage(error));
    }

    return Promise.reject(error);
  }
);

export function setUnauthorizedHandler(handler: UnauthorizedHandler) {
  unauthorizedHandler = handler;
}

export function request<T = unknown>(config: AxiosRequestConfig) {
  return service.request<unknown, T>(config);
}

export function download(config: AxiosRequestConfig) {
  return service.request({
    ...config,
    responseType: "blob"
  });
}
