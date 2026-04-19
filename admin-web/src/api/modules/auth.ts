import { request } from "@/api/http";
import type { AdminInfo, LoginRequest, LoginResponse, PermissionSummary } from "@/types/auth";

export function loginApi(data: LoginRequest) {
  return request<LoginResponse>({
    url: "/demo/login/simple",
    method: "post",
    data
  });
}

export function registerApi(data: LoginRequest) {
  return request<number>({
    url: "/demo/register/simple",
    method: "post",
    data
  });
}

export function getMeApi() {
  return request<AdminInfo>({
    url: "/demo/admin/me",
    method: "get"
  });
}

export function logoutApi() {
  return request<string>({
    url: "/demo/logout",
    method: "post"
  });
}

export function permissionSummaryApi(adminId: number) {
  return request<PermissionSummary>({
    url: "/demo/admin/permission/summary",
    method: "get",
    params: { adminId }
  });
}

export function authoritiesApi() {
  return request<string[]>({
    url: "/demo/admin/authorities",
    method: "get"
  });
}
