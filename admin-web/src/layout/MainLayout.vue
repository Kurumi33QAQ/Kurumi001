<template>
  <el-container class="layout-root">
    <el-aside width="220px" class="layout-aside">
      <div class="brand">MyMall 管理后台</div>
      <el-menu :default-active="activeMenu" router class="menu">
        <el-menu-item v-for="item in visibleMenus" :key="item.key" :index="item.path">
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="layout-header">
        <div class="header-left">{{ currentTitle }}</div>
        <div class="header-right">
          <span class="username">{{ authStore.displayName }}</span>
          <el-button size="small" type="danger" plain @click="handleLogout">退出登录</el-button>
        </div>
      </el-header>
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessageBox } from "element-plus";
import { MENU_ITEMS } from "@/constants/menu";
import { useAuthStore } from "@/store/auth";

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const visibleMenus = computed(() =>
  MENU_ITEMS.filter((item) => authStore.hasPermission(item.permission))
);

const activeMenu = computed(() => route.path);

const currentTitle = computed(() => {
  const current = MENU_ITEMS.find((item) => route.path.startsWith(item.path));
  return current?.title || "工作台";
});

async function handleLogout() {
  try {
    await ElMessageBox.confirm("确定退出当前账号吗？", "提示", { type: "warning" });
    await authStore.logout();
    router.replace("/login");
  } catch {
    // 用户取消时不处理
  }
}
</script>

<style scoped>
.layout-root {
  min-height: 100vh;
}

.layout-aside {
  background: #0f172a;
  color: #ffffff;
}

.brand {
  height: 58px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.3px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.22);
}

.menu {
  border-right: none;
  background: transparent;
}

:deep(.el-menu-item) {
  color: #e2e8f0;
}

:deep(.el-menu-item:hover) {
  background: rgba(30, 41, 59, 0.75);
}

:deep(.el-menu-item.is-active) {
  color: #ffffff;
  background: #1e293b;
}

.layout-header {
  height: 58px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #ffffff;
  border-bottom: 1px solid #e2e8f0;
}

.header-left {
  font-size: 16px;
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.username {
  font-size: 14px;
  color: #334155;
}

.layout-main {
  padding: 16px;
}

@media (max-width: 960px) {
  .layout-aside {
    width: 176px !important;
  }
}
</style>
