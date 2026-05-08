<template>
  <div class="dashboard">
    <section class="welcome-panel">
      <div>
        <p class="section-kicker">运营总览</p>
        <h2>{{ greeting }}，{{ authStore.displayName }}</h2>
        <p class="welcome-desc">这里汇总商品、订单和权限状态，帮助管理员快速判断今天需要先处理什么。</p>
      </div>
      <div class="system-card">
        <span class="status-dot"></span>
        <div>
          <strong>系统运行正常</strong>
          <small>接口、权限与页面已接入</small>
        </div>
      </div>
    </section>

    <section class="metric-grid" v-loading="loading">
      <article v-for="item in metricCards" :key="item.label" class="metric-card">
        <span>{{ item.badge }}</span>
        <p>{{ item.label }}</p>
        <strong>{{ item.value }}</strong>
        <small>{{ item.hint }}</small>
      </article>
    </section>

    <section class="dashboard-grid">
      <div class="panel-card">
        <div class="panel-head">
          <div>
            <p class="section-kicker">待办提醒</p>
            <h3>建议优先处理</h3>
          </div>
          <el-button :loading="loading" @click="loadDashboard">刷新</el-button>
        </div>
        <div class="todo-list">
          <article v-for="todo in todos" :key="todo.title" class="todo-item">
            <span :class="todo.level"></span>
            <div>
              <strong>{{ todo.title }}</strong>
              <p>{{ todo.desc }}</p>
            </div>
          </article>
        </div>
      </div>

      <div class="panel-card">
        <div class="panel-head">
          <div>
            <p class="section-kicker">快捷入口</p>
            <h3>常用管理操作</h3>
          </div>
        </div>
        <div class="quick-grid">
          <button v-for="item in quickLinks" :key="item.path" @click="router.push(item.path)">
            <strong>{{ item.title }}</strong>
            <span>{{ item.desc }}</span>
          </button>
        </div>
      </div>
    </section>

    <section class="panel-card authority-panel">
      <div class="panel-head">
        <div>
          <p class="section-kicker">权限视图</p>
          <h3>当前账号可用权限点</h3>
        </div>
        <el-tag type="success">{{ authStore.authorities.length }} 个权限</el-tag>
      </div>
      <el-empty v-if="!authStore.authorities.length" description="暂无权限数据" />
      <div v-else class="authority-list">
        <el-tag v-for="item in authStore.authorities" :key="item" effect="plain">{{ item }}</el-tag>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { listOrderApi } from "@/api/modules/order";
import { listProductApi } from "@/api/modules/product";
import { useAuthStore } from "@/store/auth";

const router = useRouter();
const authStore = useAuthStore();
const loading = ref(false);

const summary = reactive({
  productTotal: 0,
  publishedProductTotal: 0,
  orderTotal: 0,
  pendingOrderTotal: 0
});

const greeting = computed(() => {
  const hour = new Date().getHours();
  if (hour < 6) return "夜深了";
  if (hour < 12) return "早上好";
  if (hour < 18) return "下午好";
  return "晚上好";
});

const metricCards = computed(() => [
  {
    label: "商品总数",
    value: summary.productTotal,
    badge: "商品",
    hint: authStore.hasPermission("pms:product:read") ? "来自商品管理接口" : "当前账号暂无商品权限"
  },
  {
    label: "上架商品",
    value: summary.publishedProductTotal,
    badge: "上架",
    hint: "买家端只能看到已上架商品"
  },
  {
    label: "待处理订单",
    value: summary.pendingOrderTotal,
    badge: "订单",
    hint: "建议及时关注待支付或待处理订单"
  },
  {
    label: "权限点",
    value: authStore.authorities.length,
    badge: "权限",
    hint: "由当前管理员角色决定"
  }
]);

const todos = computed(() => {
  const unpublished = Math.max(summary.productTotal - summary.publishedProductTotal, 0);
  return [
    {
      title: summary.pendingOrderTotal > 0 ? `${summary.pendingOrderTotal} 个订单需要关注` : "暂无明显订单积压",
      desc: summary.pendingOrderTotal > 0 ? "进入订单管理查看状态，必要时更新订单进度。" : "订单状态整体平稳，可以继续关注商品运营。",
      level: summary.pendingOrderTotal > 0 ? "warning" : "success"
    },
    {
      title: unpublished > 0 ? `${unpublished} 个商品未上架` : "商品上架状态良好",
      desc: unpublished > 0 ? "如果商品资料已完善，可以在商品管理中批量上架。" : "当前商品对买家端展示较完整。",
      level: unpublished > 0 ? "info" : "success"
    },
    {
      title: authStore.authorities.length > 0 ? "权限缓存已加载" : "权限数据为空",
      desc: authStore.authorities.length > 0 ? "菜单和按钮会根据权限点自动显示。" : "如果菜单异常，请检查管理员角色和权限绑定。",
      level: authStore.authorities.length > 0 ? "success" : "warning"
    }
  ];
});

const quickLinks = computed(() =>
  [
    { title: "商品管理", desc: "新增、上下架与维护库存", path: "/product", permission: "pms:product:read" },
    { title: "订单管理", desc: "查询订单与更新状态", path: "/order", permission: "oms:order:read" },
    { title: "权限管理", desc: "查看角色与权限配置", path: "/rbac", permission: "admin:read" },
    { title: "登录日志", desc: "排查登录行为与风险", path: "/login-log", permission: "admin:read" }
  ].filter((item) => authStore.hasPermission(item.permission))
);

function toNumber(value: unknown) {
  const num = Number(value ?? 0);
  return Number.isNaN(num) ? 0 : num;
}

async function loadDashboard() {
  loading.value = true;
  try {
    const tasks: Promise<void>[] = [];

    if (authStore.hasPermission("pms:product:read")) {
      tasks.push(
        listProductApi({ pageNum: 1, pageSize: 1 }).then((page) => {
          summary.productTotal = toNumber(page.total);
        }),
        listProductApi({ pageNum: 1, pageSize: 1, publishStatus: 1 }).then((page) => {
          summary.publishedProductTotal = toNumber(page.total);
        })
      );
    }

    if (authStore.hasPermission("oms:order:read")) {
      tasks.push(
        listOrderApi({ pageNum: 1, pageSize: 1 }).then((page) => {
          summary.orderTotal = toNumber(page.total);
        }),
        listOrderApi({ pageNum: 1, pageSize: 1, status: 0 }).then((page) => {
          summary.pendingOrderTotal = toNumber(page.total);
        })
      );
    }

    await Promise.allSettled(tasks);
  } finally {
    loading.value = false;
  }
}

onMounted(loadDashboard);
</script>

<style scoped>
.dashboard {
  display: grid;
  gap: 20px;
}

.welcome-panel,
.panel-card,
.metric-card {
  border: 1px solid rgba(123, 103, 72, 0.1);
  background: rgba(255, 252, 246, 0.82);
  box-shadow: 0 22px 60px rgba(106, 83, 52, 0.1);
  backdrop-filter: blur(16px);
}

.welcome-panel {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: center;
  padding: 28px;
  border-radius: 28px;
  overflow: hidden;
}

.section-kicker {
  margin: 0 0 10px;
  color: #8a6d4f;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.18em;
}

.welcome-panel h2,
.panel-head h3 {
  margin: 0;
  color: #263027;
  letter-spacing: -0.04em;
}

.welcome-panel h2 {
  font-size: 36px;
}

.welcome-desc {
  max-width: 620px;
  margin: 12px 0 0;
  color: #747970;
  line-height: 1.75;
}

.system-card {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 230px;
  padding: 16px;
  border-radius: 22px;
  background: #fffaf2;
}

.status-dot {
  width: 12px;
  height: 12px;
  border-radius: 999px;
  background: #3d8064;
  box-shadow: 0 0 0 8px rgba(61, 128, 100, 0.12);
}

.system-card strong,
.system-card small {
  display: block;
}

.system-card small {
  margin-top: 4px;
  color: #7b8177;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.metric-card {
  display: grid;
  gap: 7px;
  min-height: 150px;
  padding: 20px;
  border-radius: 24px;
}

.metric-card > span {
  width: fit-content;
  padding: 6px 10px;
  border-radius: 999px;
  color: #3d8064;
  background: #edf5ec;
  font-size: 12px;
  font-weight: 800;
}

.metric-card p,
.metric-card small {
  margin: 0;
  color: #7a7f75;
}

.metric-card strong {
  color: #263027;
  font-size: 38px;
  letter-spacing: -0.05em;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(360px, 0.95fr);
  gap: 18px;
}

.panel-card {
  padding: 22px;
  border-radius: 26px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  margin-bottom: 18px;
}

.todo-list,
.quick-grid,
.authority-list {
  display: grid;
  gap: 12px;
}

.todo-item {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  padding: 15px;
  border-radius: 18px;
  background: #fffaf2;
}

.todo-item > span {
  width: 10px;
  height: 10px;
  margin-top: 6px;
  border-radius: 999px;
  background: #94a3b8;
}

.todo-item > span.success {
  background: #3d8064;
}

.todo-item > span.warning {
  background: #d97706;
}

.todo-item > span.info {
  background: #64748b;
}

.todo-item strong,
.todo-item p {
  margin: 0;
}

.todo-item p {
  margin-top: 5px;
  color: #777d73;
  line-height: 1.55;
}

.quick-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.quick-grid button {
  min-height: 104px;
  border: 1px solid rgba(123, 103, 72, 0.1);
  border-radius: 20px;
  padding: 16px;
  text-align: left;
  color: #263027;
  background: #fffaf2;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.quick-grid button:hover {
  transform: translateY(-3px);
  box-shadow: 0 18px 36px rgba(106, 83, 52, 0.12);
}

.quick-grid strong,
.quick-grid span {
  display: block;
}

.quick-grid strong {
  margin-bottom: 8px;
  font-size: 16px;
}

.quick-grid span {
  color: #7a7f75;
  line-height: 1.5;
}

.authority-list {
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
}

@media (max-width: 1100px) {
  .metric-grid,
  .dashboard-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .welcome-panel,
  .panel-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .metric-grid,
  .dashboard-grid,
  .quick-grid {
    grid-template-columns: 1fr;
  }
}
</style>
