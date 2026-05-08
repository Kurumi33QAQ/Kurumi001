<template>
  <el-container class="layout-root">
    <el-aside width="238px" class="layout-aside">
      <div class="brand-block">
        <div class="brand-mark">M</div>
        <div>
          <strong>MyMall</strong>
          <span>运营管理后台</span>
        </div>
      </div>

      <el-menu :default-active="activeMenu" router class="menu">
        <el-menu-item v-for="item in visibleMenus" :key="item.key" :index="item.path">
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>

      <div class="aside-card">
        <span>买家商城入口</span>
        <button @click="openMall">查看前台</button>
      </div>
    </el-aside>

    <el-container class="layout-content">
      <el-header class="layout-header">
        <div>
          <p>当前位置</p>
          <h1>{{ currentTitle }}</h1>
        </div>
        <div class="header-right">
          <div class="admin-chip">
            <span>{{ authStore.displayName.slice(0, 1).toUpperCase() }}</span>
            <div>
              <strong>{{ authStore.displayName }}</strong>
              <small>后台管理员</small>
            </div>
          </div>
          <el-button type="danger" plain @click="handleLogout">退出登录</el-button>
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

const visibleMenus = computed(() => MENU_ITEMS.filter((item) => authStore.hasPermission(item.permission)));
const activeMenu = computed(() => route.path);

const currentTitle = computed(() => {
  const current = MENU_ITEMS.find((item) => route.path.startsWith(item.path));
  return current?.title || "工作台";
});

function openMall() {
  window.open("/mall", "_blank");
}

async function handleLogout() {
  try {
    await ElMessageBox.confirm("确定退出当前管理员账号吗？", "退出登录", { type: "warning" });
    await authStore.logout();
    router.replace("/login");
  } catch {
    // 用户取消时不处理。
  }
}
</script>

<style scoped>
.layout-root {
  min-height: 100vh;
  background:
    radial-gradient(circle at 88% 8%, rgba(104, 151, 132, 0.16), transparent 28%),
    linear-gradient(135deg, #f6f3ed 0%, #eef3ef 100%);
}

.layout-aside {
  display: flex;
  flex-direction: column;
  padding: 18px;
  color: #f8faf7;
  background: linear-gradient(180deg, #22362d 0%, #17251f 100%);
  box-shadow: 18px 0 50px rgba(38, 48, 39, 0.14);
}

.brand-block {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 6px 24px;
}

.brand-mark {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  border-radius: 16px;
  color: #22362d;
  background: #f3d7a4;
  font-weight: 900;
  box-shadow: 0 12px 28px rgba(243, 215, 164, 0.18);
}

.brand-block strong,
.brand-block span {
  display: block;
}

.brand-block strong {
  font-size: 18px;
  letter-spacing: 0.2px;
}

.brand-block span {
  margin-top: 3px;
  color: rgba(248, 250, 247, 0.68);
  font-size: 12px;
}

.menu {
  flex: 1;
  border-right: none;
  background: transparent;
}

:deep(.el-menu-item) {
  height: 48px;
  margin: 6px 0;
  border-radius: 16px;
  color: rgba(248, 250, 247, 0.74);
  font-weight: 700;
}

:deep(.el-menu-item:hover) {
  color: #ffffff;
  background: rgba(255, 255, 255, 0.08);
}

:deep(.el-menu-item.is-active) {
  color: #263027;
  background: #f3d7a4;
}

.aside-card {
  margin-top: 18px;
  padding: 16px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.aside-card span {
  display: block;
  margin-bottom: 12px;
  color: rgba(248, 250, 247, 0.72);
  font-size: 13px;
}

.aside-card button {
  width: 100%;
  border: 0;
  border-radius: 14px;
  padding: 10px;
  color: #22362d;
  background: #fff8e8;
  cursor: pointer;
  font-weight: 800;
}

.layout-content {
  min-width: 0;
}

.layout-header {
  height: 78px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  background: rgba(255, 252, 246, 0.78);
  border-bottom: 1px solid rgba(123, 103, 72, 0.1);
  backdrop-filter: blur(16px);
}

.layout-header p,
.layout-header h1 {
  margin: 0;
}

.layout-header p {
  color: #81867b;
  font-size: 12px;
}

.layout-header h1 {
  margin-top: 4px;
  color: #263027;
  font-size: 24px;
  letter-spacing: -0.04em;
}

.header-right,
.admin-chip {
  display: flex;
  align-items: center;
  gap: 12px;
}

.admin-chip {
  padding: 7px 12px 7px 7px;
  border-radius: 18px;
  background: #fffaf2;
  border: 1px solid rgba(123, 103, 72, 0.1);
}

.admin-chip > span {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border-radius: 13px;
  color: #ffffff;
  background: #3d8064;
  font-weight: 900;
}

.admin-chip strong,
.admin-chip small {
  display: block;
}

.admin-chip strong {
  color: #263027;
  font-size: 14px;
}

.admin-chip small {
  color: #7d8378;
  font-size: 12px;
}

.layout-main {
  padding: 24px;
}

@media (max-width: 960px) {
  .layout-aside {
    width: 188px !important;
    padding: 12px;
  }

  .layout-header {
    padding: 0 16px;
  }

  .admin-chip small {
    display: none;
  }
}
</style>
