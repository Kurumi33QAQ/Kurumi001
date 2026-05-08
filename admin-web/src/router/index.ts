import { createRouter, createWebHistory } from "vue-router";
import type { RouteRecordRaw } from "vue-router";
import { useAuthStore } from "@/store/auth";

const routes: RouteRecordRaw[] = [
  {
    path: "/login",
    name: "Login",
    component: () => import("@/views/login/LoginView.vue"),
    meta: { public: true, title: "MyMall 管理后台登录" }
  },
  {
    path: "/403",
    name: "Forbidden",
    component: () => import("@/views/error/ForbiddenView.vue"),
    meta: { public: true, title: "无权限访问 - MyMall 管理后台" }
  },
  {
    path: "/mall/login",
    name: "MemberLogin",
    component: () => import("@/views/member/MemberLoginView.vue"),
    meta: { public: true, title: "会员登录 - MyMall 精选商城" }
  },
  {
    path: "/mall",
    name: "MemberMall",
    component: () => import("@/views/member/MemberMallView.vue"),
    meta: { public: true, title: "MyMall 精选商城" }
  },
  {
    path: "/",
    component: () => import("@/layout/MainLayout.vue"),
    redirect: "/dashboard",
    children: [
      {
        path: "dashboard",
        name: "Dashboard",
        component: () => import("@/views/dashboard/DashboardView.vue"),
        meta: { title: "工作台 - MyMall 管理后台" }
      },
      {
        path: "product",
        name: "Product",
        component: () => import("@/views/product/ProductManageView.vue"),
        meta: { permission: "pms:product:read", title: "商品管理 - MyMall 管理后台" }
      },
      {
        path: "order",
        name: "Order",
        component: () => import("@/views/order/OrderManageView.vue"),
        meta: { permission: "oms:order:read", title: "订单管理 - MyMall 管理后台" }
      },
      {
        path: "rbac",
        name: "RBAC",
        component: () => import("@/views/rbac/RbacManageView.vue"),
        meta: { permission: "admin:read", title: "权限管理 - MyMall 管理后台" }
      },
      {
        path: "login-log",
        name: "LoginLog",
        component: () => import("@/views/log/LoginLogView.vue"),
        meta: { permission: "admin:read", title: "登录日志 - MyMall 管理后台" }
      }
    ]
  },
  {
    path: "/:pathMatch(.*)*",
    redirect: "/dashboard"
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach(async (to) => {
  const authStore = useAuthStore();
  const isPublic = Boolean(to.meta.public);

  if (to.path === "/login" && authStore.isLoggedIn) {
    return "/dashboard";
  }

  if (!isPublic && !authStore.isLoggedIn) {
    return { path: "/login", query: { redirect: to.fullPath } };
  }

  if (!isPublic && authStore.isLoggedIn && !authStore.initialized) {
    try {
      await authStore.bootstrap();
    } catch {
      authStore.clearAuth();
      return { path: "/login", query: { redirect: to.fullPath } };
    }
  }

  const requiredPermission = to.meta.permission as string | undefined;
  if (requiredPermission && !authStore.hasPermission(requiredPermission)) {
    return "/403";
  }

  return true;
});

router.afterEach((to) => {
  document.title = (to.meta.title as string | undefined) || "MyMall";
});

export default router;
