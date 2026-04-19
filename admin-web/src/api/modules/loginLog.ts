import { download, request } from "@/api/http";
import type { PageResult } from "@/types/api";
import type { LoginLog, LoginLogQuery } from "@/types/log";

export function listLoginLogApi(params: LoginLogQuery) {
  return request<PageResult<LoginLog>>({
    url: "/demo/admin/login-log/list",
    method: "get",
    params
  });
}

export function exportLoginLogApi(params: Omit<LoginLogQuery, "pageNum" | "pageSize">) {
  return download({
    url: "/demo/admin/login-log/export",
    method: "get",
    params
  });
}

export function cleanLoginLogApi(beforeTime: string) {
  return request<number>({
    url: "/demo/admin/login-log/clean",
    method: "post",
    data: { beforeTime }
  });
}
