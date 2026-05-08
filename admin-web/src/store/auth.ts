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
        // 开发阶段兜底：权限汇总接口不可用时，仍尝试读取当前用户权限点。
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
        throw new Error("登录失败，未返回访问凭证");
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
        // 即使退出接口临时失败，也优先清理本地登录状态，避免继续误操作。
      } finally {
        this.clearAuth();
      }
    }
  }
});
