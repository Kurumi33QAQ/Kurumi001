import { request } from "@/api/http";
import type { PageResult } from "@/types/api";
import type { Order, OrderQuery, OrderStatusPayload } from "@/types/order";

export function listOrderApi(params: OrderQuery) {
  return request<PageResult<Order>>({
    url: "/demo/order/list",
    method: "get",
    params
  });
}

export function detailOrderApi(id: number) {
  return request<Order>({
    url: "/demo/order/detail",
    method: "get",
    params: { id }
  });
}

export function updateOrderStatusApi(data: OrderStatusPayload) {
  return request<string>({
    url: "/demo/order/status/update",
    method: "post",
    data
  });
}
