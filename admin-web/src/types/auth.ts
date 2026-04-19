export interface AdminInfo {
  id: number;
  username: string;
  nickName?: string;
  email?: string;
  status?: number;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  tokenHead?: string;
  adminInfo: AdminInfo;
}

export interface PermissionSummary {
  adminId: number;
  username: string;
  roleNames: string[];
  authorities: string[];
}
