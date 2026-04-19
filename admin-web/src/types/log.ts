export interface LoginLog {
  id: number;
  adminId?: number;
  username: string;
  ip?: string;
  userAgent?: string;
  status: number;
  message?: string;
  createTime?: string;
}

export interface LoginLogQuery {
  pageNum: number;
  pageSize: number;
  username?: string;
  status?: number;
  startTime?: string;
  endTime?: string;
}
