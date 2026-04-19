import { request } from "@/api/http";
import type { PageResult } from "@/types/api";
import type { Product, ProductPayload, ProductQuery } from "@/types/product";

export function listProductApi(params: ProductQuery) {
  return request<PageResult<Product>>({
    url: "/demo/product/list",
    method: "get",
    params
  });
}

export function createProductApi(data: ProductPayload) {
  return request<string | number>({
    url: "/demo/product/create",
    method: "post",
    data
  });
}

export function updateProductApi(data: ProductPayload) {
  return request<string>({
    url: "/demo/product/update",
    method: "put",
    data
  });
}

export function detailProductApi(id: string | number) {
  return request<Product>({
    url: "/demo/product/detail",
    method: "get",
    params: { id }
  });
}

export function updateProductPublishStatusApi(id: string | number, publishStatus: number) {
  return request<string>({
    url: "/demo/product/publish/status",
    method: "post",
    params: { id, publishStatus }
  });
}

export function batchUpdateProductPublishStatusApi(ids: Array<string | number>, publishStatus: number) {
  return request<number>({
    url: "/demo/product/publish/status/batch",
    method: "post",
    data: { ids, publishStatus }
  });
}

export function deleteProductApi(id: string | number) {
  return request<string>({
    url: "/demo/product/delete",
    method: "post",
    params: { id }
  });
}

export function batchDeleteProductApi(ids: Array<string | number>) {
  return request<number>({
    url: "/demo/product/delete/batch",
    method: "post",
    data: { ids }
  });
}
