export interface Order {
  id: number;
  orderSn: string;
  memberUsername: string;
  totalAmount: number;
  payAmount: number;
  status: number;
  note?: string;
  deleteStatus: number;
  createTime?: string;
  updateTime?: string;
}

export interface OrderQuery {
  pageNum: number;
  pageSize: number;
  orderSn?: string;
  memberUsername?: string;
  status?: number;
}

export interface OrderStatusPayload {
  id: number;
  status: number;
  note?: string;
}
