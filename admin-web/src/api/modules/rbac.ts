import { request } from "@/api/http";
import type { OptionItem } from "@/types/api";
import type { AdminUser } from "@/types/rbac";

export function listAdminApi() {
  return request<AdminUser[]>({
    url: "/demo/admin/list",
    method: "get"
  });
}

export function listRoleOptionsApi() {
  return request<OptionItem[]>({
    url: "/demo/admin/role/options",
    method: "get"
  });
}

export function listResourceOptionsApi() {
  return request<OptionItem[]>({
    url: "/demo/admin/resource/options",
    method: "get"
  });
}

export function listAdminRoleIdsApi(adminId: number) {
  return request<number[]>({
    url: "/demo/admin/role/list",
    method: "get",
    params: { adminId }
  });
}

export function assignAdminRolesApi(adminId: number, roleIds: number[]) {
  return request<string>({
    url: "/demo/admin/role/assign",
    method: "post",
    data: { adminId, roleIds }
  });
}

export function listRoleResourceIdsApi(roleId: number) {
  return request<number[]>({
    url: "/demo/admin/role/resource/list",
    method: "get",
    params: { roleId }
  });
}

export function assignRoleResourcesApi(roleId: number, resourceIds: number[]) {
  return request<string>({
    url: "/demo/admin/role/resource/assign",
    method: "post",
    data: { roleId, resourceIds }
  });
}
