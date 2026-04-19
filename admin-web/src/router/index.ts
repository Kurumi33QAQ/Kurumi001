import { createRouter, createWebHistory } from "vue-router";
import type { RouteRecordRaw } from "vue-router";
import { useAuthStore } from "@/store/auth";

const routes: RouteRecordRaw[] = [
  {
    path: "/login",
    name: "Login",
    component: () => import("@/views/login/LoginView.vue"),
    meta: { public: true }
  },
  {
    path: "/403",
    name: "Forbidden",
    component: () => import("@/views/error/ForbiddenView.vue"),
    meta: { public: true }
  },
  {
    path: "/",
    component: () => import("@/layout/MainLayout.vue"),
    redirect: "/dashboard",
    children: [
      {
        path: "dashboard",
        name: "Dashboard",
        component: () => import("@/views/dashboard/DashboardView.vue")
      },
      {
        path: "product",
        name: "Product",
        component: () => import("@/views/product/ProductManageView.vue"),
        meta: { permission: "pms:product:read" }
      },
      {
        path: "order",
        name: "Order",
        component: () => import("@/views/order/OrderManageView.vue"),
        meta: { permission: "oms:order:read" }
      },
      {
        path: "rbac",
        name: "RBAC",
        component: () => import("@/views/rbac/RbacManageView.vue"),
        meta: { permission: "admin:read" }
      },
      {
        path: "login-log",
        name: "LoginLog",
        component: () => import("@/views/log/LoginLogView.vue"),
        meta: { permission: "admin:read" }
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

export default router;
