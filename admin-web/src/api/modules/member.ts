import { memberRequest } from "@/api/memberHttp";
import type { PageResult } from "@/types/api";
import type { Order } from "@/types/order";
import type { Product } from "@/types/product";

export interface MemberInfo {
  id: string | number;
  username: string;
  nickName?: string;
  phone?: string;
  email?: string;
  status: number;
}

export interface MemberLoginResponse {
  token: string;
  tokenHead: string;
  memberInfo: MemberInfo;
}

export interface SeckillActivity {
  id: string | number;
  productId: string | number;
  name: string;
  seckillPrice: number;
  seckillStock: number;
  soldCount: number;
  perLimit: number;
  startTime: string;
  endTime: string;
  status: number;
  activityStatus?: number;
  activityStatusText?: string;
}

export interface MemberNotification {
  id: string | number;
  memberId?: string | number;
  memberUsername: string;
  title: string;
  content: string;
  type: number;
  businessId?: string | number;
  readStatus: number;
  createTime?: string;
  readTime?: string;
  deleteStatus?: number;
}

export function memberRegisterApi(data: { username: string; password: string }) {
  return memberRequest<string | number>({
    url: "/member/auth/register",
    method: "post",
    data
  });
}

export function memberLoginApi(data: { username: string; password: string }) {
  return memberRequest<MemberLoginResponse>({
    url: "/member/auth/login",
    method: "post",
    data
  });
}

export function memberMeApi() {
  return memberRequest<MemberInfo>({
    url: "/member/auth/me",
    method: "get"
  });
}

export function memberLogoutApi() {
  return memberRequest<string>({
    url: "/member/auth/logout",
    method: "post"
  });
}

export function memberProductListApi(params: {
  pageNum: number;
  pageSize: number;
  name?: string;
  minPrice?: number;
  maxPrice?: number;
}) {
  return memberRequest<PageResult<Product>>({
    url: "/member/product/list",
    method: "get",
    params
  });
}

export function memberProductDetailApi(id: string | number) {
  return memberRequest<Product>({
    url: "/member/product/detail",
    method: "get",
    params: { id }
  });
}

export function memberCreateOrderApi(data: { productId: string | number; quantity: number; note?: string }) {
  return memberRequest<string | number>({
    url: "/member/order/create",
    method: "post",
    data,
    headers: {
      "X-Idempotency-Key": `order_${Date.now()}_${Math.random().toString(16).slice(2)}`
    }
  });
}

export function memberOrderListApi(params: { pageNum: number; pageSize: number; status?: number }) {
  return memberRequest<PageResult<Order>>({
    url: "/member/order/list",
    method: "get",
    params
  });
}

export function memberCancelOrderApi(orderId: string | number) {
  return memberRequest<string>({
    url: "/member/order/cancel",
    method: "post",
    params: { orderId }
  });
}

export function memberSeckillListApi(params: { pageNum: number; pageSize: number; name?: string }) {
  return memberRequest<PageResult<SeckillActivity>>({
    url: "/member/seckill/list",
    method: "get",
    params
  });
}

export function memberSubmitSeckillApi(data: { activityId: string | number; quantity: number }) {
  return memberRequest<string | number>({
    url: "/member/seckill/submit",
    method: "post",
    data
  });
}

export function memberNotificationListApi(params: { pageNum: number; pageSize: number }) {
  return memberRequest<PageResult<MemberNotification>>({
    url: "/member/notification/list",
    method: "get",
    params
  });
}

export function memberUnreadCountApi() {
  return memberRequest<number>({
    url: "/member/notification/unread-count",
    method: "get"
  });
}

export function memberReadNotificationApi(id: string | number) {
  return memberRequest<string>({
    url: "/member/notification/read",
    method: "post",
    params: { id }
  });
}

export function memberReadAllNotificationApi() {
  return memberRequest<string>({
    url: "/member/notification/read-all",
    method: "post"
  });
}
