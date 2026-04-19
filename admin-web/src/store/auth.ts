import { defineStore } from "pinia";
import { ElMessage } from "element-plus";
import { authoritiesApi, getMeApi, loginApi, logoutApi, permissionSummaryApi, registerApi } from "@/api/modules/auth";
import type { AdminInfo, LoginRequest } from "@/types/auth";
import { clearAccessToken, getAccessToken, setAccessToken } from "@/utils/auth";

interface AuthState {
  token: string;
  adminInfo: AdminInfo | null;
  authorities: string[];
  initialized: boolean;
}

export const useAuthStore = defineStore("auth", {
  state: (): AuthState => ({
    token: getAccessToken(),
    adminInfo: null,
    authorities: [],
    initialized: false
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token),
    displayName: (state) => state.adminInfo?.nickName || state.adminInfo?.username || "管理员"
  },
  actions: {
    hasPermission(required?: string | string[]) {
      if (!required) return true;
      const list = Array.isArray(required) ? required : [required];
      return list.some((permission) => this.authorities.includes(permission));
    },
    clearAuth() {
      this.token = "";
      this.adminInfo = null;
      this.authorities = [];
      this.initialized = false;
      clearAccessToken();
    },
    async bootstrap() {
      if (!this.token) {
        this.initialized = false;
        return;
      }

      const me = await getMeApi();
      this.adminInfo = me;

      try {
        const summary = await permissionSummaryApi(me.id);
        this.authorities = summary.authorities || [];
      } catch {
        // dev 模式兜底接口，避免权限列表为空导致菜单全隐藏
        try {
          this.authorities = await authoritiesApi();
        } catch {
          this.authorities = [];
        }
      }

      this.initialized = true;
    },
    async register(payload: LoginRequest) {
      const id = await registerApi(payload);
      ElMessage.success("注册成功，请登录");
      return id;
    },
    async login(payload: LoginRequest) {
      const data = await loginApi(payload);
      if (!data?.token) {
        throw new Error("登录失败，未返回 token");
      }
      this.token = data.token;
      setAccessToken(data.token);
      this.adminInfo = data.adminInfo;
      await this.bootstrap();
      ElMessage.success("登录成功");
    },
    async logout() {
      try {
        await logoutApi();
      } catch {
        // 忽略退出接口失败，确保本地状态已清空
      } finally {
        this.clearAuth();
      }
    }
  }
});
