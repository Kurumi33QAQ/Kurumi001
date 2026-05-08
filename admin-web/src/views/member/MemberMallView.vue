<template>
  <div class="mall-shell">
    <section class="mall-hero">
      <div class="hero-copy">
        <p class="eyebrow">MYMALL SELECT</p>
        <h1>精选好物，即刻抵达你的生活</h1>
        <p class="hero-desc">发现上架好物，参与限时抢购，随时查看订单与消息提醒。每一次点击，都应该清楚、安心、顺手。</p>
        <div class="hero-actions">
          <el-button type="primary" size="large" @click="scrollToProducts">立即逛逛</el-button>
          <el-button size="large" plain @click="activePanel = 'seckill'">限时抢购</el-button>
        </div>
      </div>
      <div class="hero-panel glass-card">
        <div class="pulse-dot" :class="{ online: wsConnected }"></div>
        <p class="panel-label">消息提醒</p>
        <h3>{{ wsConnected ? '提醒已开启' : '登录开启专属提醒' }}</h3>
        <p>{{ memberInfo ? `你好，${memberInfo.nickName || memberInfo.username}` : '订单变化、抢购结果和商城消息会在这里提醒你' }}</p>
        <div class="metric-row">
          <span>未读消息</span>
          <strong>{{ unreadCount }}</strong>
        </div>
        <div class="benefit-stack">
          <span>正品好物</span>
          <span>库存提醒</span>
          <span>订单追踪</span>
        </div>
      </div>
    </section>

    <section class="auth-strip glass-card">
      <div>
        <p class="section-kicker">会员服务</p>
        <h2>{{ memberInfo ? `欢迎回来，${memberInfo.nickName || memberInfo.username}` : '登录后同步订单、抢购与消息提醒' }}</h2>
        <p v-if="!memberInfo">商品可以直接浏览，购买、抢购和消息提醒需要先登录。</p>
      </div>
      <div class="auth-guest" v-if="!memberInfo">
        <el-button type="primary" size="large" @click="goMemberLogin">登录 / 注册</el-button>
      </div>
      <div class="auth-user" v-else>
        <el-tag effect="dark">{{ memberInfo.status === 1 ? '账号正常' : '账号异常' }}</el-tag>
        <el-button plain @click="refreshAll">刷新数据</el-button>
        <el-button type="danger" plain @click="handleLogout">退出</el-button>
      </div>
    </section>

    <section class="mall-workspace">
      <aside class="mall-side glass-card">
        <button :class="{ active: activePanel === 'products' }" @click="activePanel = 'products'">
          <span>精选好物</span>
          <small>逛逛正在售卖的商品</small>
        </button>
        <button :class="{ active: activePanel === 'orders' }" @click="activePanel = 'orders'">
          <span>我的订单</span>
          <small>查看购买记录与状态</small>
        </button>
        <button :class="{ active: activePanel === 'seckill' }" @click="activePanel = 'seckill'">
          <span>限时抢购</span>
          <small>参与正在进行的活动</small>
        </button>
        <button :class="{ active: activePanel === 'notifications' }" @click="activePanel = 'notifications'">
          <span>我的消息 <em v-if="unreadCount">{{ unreadCount }}</em></span>
          <small>订单和抢购结果提醒</small>
        </button>
      </aside>

      <main class="mall-main">
    <section v-show="activePanel === 'products'" ref="productSection" class="content-grid">
      <div class="section-head">
        <div>
          <p class="section-kicker">今日精选</p>
          <h2>正在售卖</h2>
        </div>
        <div class="filters">
          <el-input v-model="productQuery.name" placeholder="搜索喜欢的商品" clearable @keyup.enter="loadProducts" />
          <el-input-number v-model="productQuery.minPrice" :min="0" placeholder="最低价" />
          <el-input-number v-model="productQuery.maxPrice" :min="0" placeholder="最高价" />
          <el-button type="primary" @click="loadProducts">筛选</el-button>
        </div>
      </div>

      <div class="product-grid" v-loading="productLoading">
        <article
          v-for="product in products"
          :key="product.id"
          class="product-card glass-card"
          role="button"
          tabindex="0"
          @click="showProductDetail(product)"
          @keyup.enter="showProductDetail(product)"
        >
          <div class="product-image" :style="product.pic ? { backgroundImage: `url(${product.pic})` } : undefined">
            <span v-if="!product.pic">{{ product.name?.slice(0, 2) || 'MM' }}</span>
          </div>
          <div class="product-body">
            <p class="product-sub">{{ product.subTitle || '精选商城商品' }}</p>
            <h3>{{ product.name }}</h3>
            <div class="product-meta">
              <strong>¥{{ money(product.price) }}</strong>
              <span :class="product.stock > 0 ? 'stock-ok' : 'stock-empty'">库存 {{ product.stock }}</span>
            </div>
            <div class="product-actions" @click.stop>
              <el-input-number v-model="buyQuantity[product.id]" :min="1" :max="Math.max(product.stock || 1, 1)" size="small" />
              <el-button type="primary" :disabled="product.stock <= 0" @click.stop="openOrder(product)">立即购买</el-button>
              <el-button text @click.stop="showProductDetail(product)">查看详情</el-button>
            </div>
          </div>
        </article>
      </div>
    </section>

    <section v-show="activePanel === 'orders'" class="glass-card data-section">
      <div class="section-head compact">
        <div>
          <p class="section-kicker">订单管家</p>
          <h2>我的订单</h2>
        </div>
        <div class="filters">
          <el-select v-model="orderStatus" placeholder="状态" clearable @change="loadOrders">
            <el-option label="待支付" :value="0" />
            <el-option label="已关闭/已取消" :value="4" />
          </el-select>
          <el-button @click="loadOrders">刷新</el-button>
        </div>
      </div>
      <el-table :data="orders" v-loading="orderLoading" class="tech-table">
        <el-table-column prop="orderSn" label="订单号" min-width="180" />
        <el-table-column prop="productId" label="商品编号" width="120" />
        <el-table-column prop="productQuantity" label="数量" width="90" />
        <el-table-column label="金额" width="120">
          <template #default="{ row }">¥{{ money(row.payAmount) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="130">
          <template #default="{ row }"><el-tag :type="orderTag(row.status)">{{ orderStatusText(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="170" />
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" type="danger" link @click="cancelOrder(row.id)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <section v-show="activePanel === 'seckill'" class="content-grid">
      <div class="section-head">
        <div>
          <p class="section-kicker">限时好价</p>
          <h2>限时抢购</h2>
        </div>
        <div class="filters">
          <el-input v-model="seckillName" placeholder="搜索秒杀活动" clearable @keyup.enter="loadSeckills" />
          <el-button type="primary" @click="loadSeckills">刷新</el-button>
        </div>
      </div>
      <div class="seckill-grid" v-loading="seckillLoading">
        <article v-for="item in seckills" :key="item.id" class="seckill-card glass-card">
          <el-tag :type="item.activityStatus === 2 ? 'success' : 'info'">{{ item.activityStatusText || '未知状态' }}</el-tag>
          <h3>{{ item.name }}</h3>
          <p>抢购时间：{{ item.startTime }} 至 {{ item.endTime }}</p>
          <div class="seckill-price">¥{{ money(item.seckillPrice) }}</div>
          <div class="progress-line"><i :style="{ width: seckillProgress(item) + '%' }"></i></div>
          <div class="seckill-meta">
            <span>库存 {{ item.seckillStock }}</span>
            <span>已抢 {{ item.soldCount }}</span>
            <span>限购 {{ item.perLimit }}</span>
          </div>
          <el-button type="primary" :disabled="item.activityStatus !== 2 || item.seckillStock <= item.soldCount" @click="submitSeckill(item)">立即抢购</el-button>
        </article>
      </div>
    </section>

    <section v-show="activePanel === 'notifications'" class="glass-card data-section">
      <div class="section-head compact">
        <div>
          <p class="section-kicker">消息提醒</p>
          <h2>我的消息</h2>
        </div>
        <div class="filters">
          <el-button @click="loadNotifications">刷新</el-button>
          <el-button type="primary" @click="readAllNotifications">全部已读</el-button>
        </div>
      </div>
      <div class="notice-list" v-loading="notificationLoading">
        <article v-for="notice in notifications" :key="notice.id" :class="['notice-item', { unread: notice.readStatus === 0 }]" @click="readNotification(notice)">
          <div>
            <strong>{{ notice.title }}</strong>
            <p>{{ notice.content }}</p>
          </div>
          <span>{{ notice.createTime }}</span>
        </article>
        <el-empty v-if="!notifications.length" description="暂无通知" />
      </div>
    </section>
      </main>
    </section>

    <teleport to="body">
      <div v-if="detailVisible" class="detail-mask" @click.self="closeProductDetail">
        <aside class="detail-panel">
          <button class="detail-close" @click="closeProductDetail">关闭</button>
          <div v-if="currentProduct" class="detail-content" v-loading="detailLoading">
            <div class="product-image large" :style="currentProduct.pic ? { backgroundImage: `url(${currentProduct.pic})` } : undefined">
              <span v-if="!currentProduct.pic">{{ currentProduct.name?.slice(0, 2) || 'MM' }}</span>
            </div>
            <div class="detail-title-row">
              <div>
                <p class="product-sub">精选商城商品</p>
                <h2>{{ currentProduct.name }}</h2>
              </div>
              <el-tag :type="currentProduct.stock > 0 ? 'success' : 'warning'">
                {{ currentProduct.stock > 0 ? '可购买' : '暂时缺货' }}
              </el-tag>
            </div>
            <p class="detail-subtitle">{{ currentProduct.subTitle || '暂无副标题' }}</p>
            <div class="product-meta detail-price"><strong>¥{{ money(currentProduct.price) }}</strong><span>库存 {{ currentProduct.stock }}</span></div>
            <div class="detail-info-grid">
              <div><span>商品编号</span><strong>{{ currentProduct.id }}</strong></div>
              <div><span>销量</span><strong>{{ currentProduct.sale || 0 }}</strong></div>
              <div><span>上架状态</span><strong>{{ currentProduct.publishStatus === 1 ? '已上架' : '未上架' }}</strong></div>
              <div><span>更新时间</span><strong>{{ currentProduct.updateTime || '-' }}</strong></div>
            </div>
            <el-alert v-if="currentProduct.stock <= 0" title="当前库存不足，暂时无法购买" type="warning" show-icon :closable="false" />
            <div class="detail-buy-area">
              <div>
                <span>购买数量</span>
                <el-input-number
                  v-model="buyQuantity[currentProduct.id]"
                  :min="1"
                  :max="Math.max(currentProduct.stock || 1, 1)"
                  :disabled="currentProduct.stock <= 0"
                  controls-position="right"
                />
              </div>
              <el-button class="detail-buy-button" type="primary" :disabled="currentProduct.stock <= 0" @click="openOrder(currentProduct)">
                立即购买
              </el-button>
            </div>
          </div>
          <el-empty v-else description="商品信息加载中" />
        </aside>
      </div>
    </teleport>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox, ElNotification } from "element-plus";
import type { Product } from "@/types/product";
import type { Order } from "@/types/order";
import {
  memberCancelOrderApi,
  memberCreateOrderApi,
  memberLogoutApi,
  memberMeApi,
  memberNotificationListApi,
  memberOrderListApi,
  memberProductDetailApi,
  memberProductListApi,
  memberReadAllNotificationApi,
  memberReadNotificationApi,
  memberSeckillListApi,
  memberSubmitSeckillApi,
  memberUnreadCountApi,
  type MemberInfo,
  type MemberNotification,
  type SeckillActivity
} from "@/api/modules/member";
import { clearMemberAccessToken, getMemberAccessToken } from "@/utils/memberAuth";

const router = useRouter();
const activePanel = ref("products");
const productSection = ref<HTMLElement | null>(null);
const memberInfo = ref<MemberInfo | null>(null);

const products = ref<Product[]>([]);
const productLoading = ref(false);
const productQuery = reactive<{ pageNum: number; pageSize: number; name?: string; minPrice?: number; maxPrice?: number }>({ pageNum: 1, pageSize: 12 });
const buyQuantity = reactive<Record<string | number, number>>({});
const currentProduct = ref<Product | null>(null);
const detailVisible = ref(false);
const detailLoading = ref(false);

const orders = ref<Order[]>([]);
const orderStatus = ref<number | undefined>();
const orderLoading = ref(false);

const seckills = ref<SeckillActivity[]>([]);
const seckillName = ref("");
const seckillLoading = ref(false);

const notifications = ref<MemberNotification[]>([]);
const notificationLoading = ref(false);
const unreadCount = ref(0);
const wsConnected = ref(false);
let socket: WebSocket | null = null;

const backendBase = computed(() => import.meta.env.VITE_PROXY_TARGET || "http://127.0.0.1:8080");

function money(value: unknown) {
  const num = Number(value || 0);
  return num.toFixed(2);
}

function requireLogin() {
  if (!memberInfo.value) {
    ElMessage.warning("登录后即可继续操作");
    goMemberLogin();
    return false;
  }
  return true;
}

function goMemberLogin() {
  router.push({ path: "/mall/login", query: { redirect: "/mall" } });
}

async function handleLogout() {
  try {
    await memberLogoutApi();
  } catch {
    // 本地退出优先，接口失败不阻断用户操作。
  }
  clearMemberAccessToken();
  memberInfo.value = null;
  closeWebSocket();
  unreadCount.value = 0;
  notifications.value = [];
  ElMessage.success("已退出买家账号");
}

async function bootstrapMember() {
  if (!getMemberAccessToken()) return;
  try {
    memberInfo.value = await memberMeApi();
    connectWebSocket();
  } catch {
    clearMemberAccessToken();
  }
}

async function loadProducts() {
  productLoading.value = true;
  try {
    const page = await memberProductListApi(productQuery);
    products.value = page.records || [];
    products.value.forEach(ensureBuyQuantity);
  } finally {
    productLoading.value = false;
  }
}

function ensureBuyQuantity(product: Product) {
  const maxQuantity = Math.max(product.stock || 1, 1);
  if (!buyQuantity[product.id]) {
    buyQuantity[product.id] = 1;
  }
  if (buyQuantity[product.id] > maxQuantity) {
    buyQuantity[product.id] = maxQuantity;
  }
}

async function showProductDetail(product: Product) {
  ensureBuyQuantity(product);
  currentProduct.value = product;
  detailVisible.value = true;
  detailLoading.value = true;
  try {
    const detail = await memberProductDetailApi(product.id);
    ensureBuyQuantity(detail);
    currentProduct.value = detail;
  } catch {
    // 详情刷新失败时，仍保留列表里的基础商品信息，避免用户看到空白抽屉。
  } finally {
    detailLoading.value = false;
  }
}

function closeProductDetail() {
  detailVisible.value = false;
  detailLoading.value = false;
}

async function openOrder(product: Product) {
  if (!requireLogin()) return;
  if (product.stock <= 0) {
    await ElMessageBox.alert("当前商品库存不足，暂时不能下单。你可以先看看其他商品。", "库存不足", { type: "warning" });
    return;
  }
  const quantity = buyQuantity[product.id] || 1;
  if (quantity > product.stock) {
    await ElMessageBox.alert(`你选择了 ${quantity} 件，但当前库存只有 ${product.stock} 件。`, "库存不足", { type: "warning" });
    return;
  }
  await ElMessageBox.confirm(`确认购买「${product.name}」x ${quantity} 吗？`, "确认下单", { type: "info" });
  const orderId = await memberCreateOrderApi({ productId: product.id, quantity, note: "商城下单" });
  ElNotification.success({ title: "下单成功", message: `订单 ${orderId} 已创建，库存已同步扣减` });
  await Promise.all([loadProducts(), loadOrders(), loadNotifications(), loadUnreadCount()]);
  activePanel.value = "orders";
}

async function loadOrders() {
  if (!memberInfo.value) return;
  orderLoading.value = true;
  try {
    const page = await memberOrderListApi({ pageNum: 1, pageSize: 20, status: orderStatus.value });
    orders.value = page.records || [];
  } finally {
    orderLoading.value = false;
  }
}

async function cancelOrder(orderId: string | number) {
  await ElMessageBox.confirm("取消后会恢复库存，确认取消该订单吗？", "取消订单", { type: "warning" });
  await memberCancelOrderApi(orderId);
  ElMessage.success("订单已取消");
  await Promise.all([loadOrders(), loadProducts(), loadNotifications(), loadUnreadCount()]);
}

function orderStatusText(status: number) {
  return ({ 0: "待支付", 1: "已发货", 2: "已发货", 3: "已完成", 4: "已关闭" } as Record<number, string>)[status] || `状态${status}`;
}

function orderTag(status: number) {
  return status === 0 ? "warning" : status === 4 ? "info" : "success";
}

async function loadSeckills() {
  seckillLoading.value = true;
  try {
    const page = await memberSeckillListApi({ pageNum: 1, pageSize: 12, name: seckillName.value || undefined });
    seckills.value = page.records || [];
  } finally {
    seckillLoading.value = false;
  }
}

function seckillProgress(item: SeckillActivity) {
  if (!item.seckillStock) return 0;
  return Math.min(100, Math.round(((item.soldCount || 0) / item.seckillStock) * 100));
}

async function submitSeckill(item: SeckillActivity) {
  if (!requireLogin()) return;
  if (item.activityStatus !== 2) {
    ElMessage.warning("当前不在秒杀时间窗口内");
    return;
  }
  const recordId = await memberSubmitSeckillApi({ activityId: item.id, quantity: 1 });
  ElNotification.success({ title: "抢购已提交", message: `我们正在为你确认结果，请留意我的消息。编号：${recordId}` });
  await Promise.all([loadSeckills(), loadNotifications(), loadUnreadCount()]);
}

async function loadNotifications() {
  if (!memberInfo.value) return;
  notificationLoading.value = true;
  try {
    const page = await memberNotificationListApi({ pageNum: 1, pageSize: 20 });
    notifications.value = page.records || [];
  } finally {
    notificationLoading.value = false;
  }
}

async function loadUnreadCount() {
  if (!memberInfo.value) return;
  unreadCount.value = await memberUnreadCountApi();
}

async function readNotification(notice: MemberNotification) {
  if (notice.readStatus === 1) return;
  await memberReadNotificationApi(notice.id);
  notice.readStatus = 1;
  await loadUnreadCount();
}

async function readAllNotifications() {
  if (!requireLogin()) return;
  await memberReadAllNotificationApi();
  ElMessage.success("全部通知已读");
  await Promise.all([loadNotifications(), loadUnreadCount()]);
}

function connectWebSocket() {
  closeWebSocket();
  const token = getMemberAccessToken();
  if (!token) return;
  const wsBase = backendBase.value.replace(/^http/, "ws");
  socket = new WebSocket(`${wsBase}/ws/member?token=${encodeURIComponent(`Bearer ${token}`)}`);
  socket.onopen = () => {
    wsConnected.value = true;
  };
  socket.onclose = () => {
    wsConnected.value = false;
  };
  socket.onerror = () => {
    wsConnected.value = false;
  };
  socket.onmessage = async (event) => {
    try {
      const payload = JSON.parse(event.data);
      if (payload.messageType === "MEMBER_NOTIFICATION") {
        ElNotification({ title: payload.title || "新通知", message: payload.content || "你有一条新消息", type: "success" });
        await loadNotifications();
      }
      if (payload.messageType === "UNREAD_COUNT_CHANGED") {
        unreadCount.value = Number(payload.unreadCount || 0);
      }
    } catch {
      // 握手成功文本消息不是 JSON，忽略即可。
    }
  };
}

function closeWebSocket() {
  if (socket) {
    socket.close();
    socket = null;
  }
  wsConnected.value = false;
}

async function refreshAll() {
  await Promise.all([loadProducts(), loadSeckills(), loadOrders(), loadNotifications(), loadUnreadCount()]);
}

function scrollToProducts() {
  activePanel.value = "products";
  setTimeout(() => productSection.value?.scrollIntoView({ behavior: "smooth", block: "start" }), 60);
}

onMounted(async () => {
  await bootstrapMember();
  await Promise.all([loadProducts(), loadSeckills()]);
  if (memberInfo.value) {
    await Promise.all([loadOrders(), loadNotifications(), loadUnreadCount()]);
  }
});

onBeforeUnmount(() => closeWebSocket());
</script>

<style scoped>
.mall-shell {
  position: relative;
  min-height: 100vh;
  overflow: clip;
  padding: 28px;
  color: #40463f;
  background:
    radial-gradient(circle at 12% 8%, rgba(170, 157, 136, 0.14), transparent 34%),
    radial-gradient(circle at 86% 14%, rgba(127, 149, 134, 0.13), transparent 34%),
    linear-gradient(135deg, #ebe6db 0%, #e6dfd2 48%, #e5ebe1 100%);
}

.mall-shell::before,
.mall-shell::after {
  content: "";
  position: absolute;
  pointer-events: none;
  border-radius: 999px;
  z-index: 0;
}

.mall-shell::before {
  width: 320px;
  height: 320px;
  left: -160px;
  bottom: -150px;
  background: rgba(119, 139, 125, 0.1);
}

.mall-shell::after {
  width: 260px;
  height: 260px;
  right: -120px;
  top: 190px;
  background: rgba(164, 142, 116, 0.1);
}

.mall-shell > * {
  position: relative;
  z-index: 1;
}

.glass-card {
  border: 1px solid rgba(105, 96, 78, 0.13);
  background: rgba(245, 241, 232, 0.9);
  box-shadow: 0 12px 28px rgba(74, 66, 50, 0.08);
}

.mall-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) 360px;
  gap: 22px;
  align-items: stretch;
  animation: rise-in 0.65s ease both;
}

.hero-copy,
.auth-strip,
.data-section,
.section-head {
  border-radius: 30px;
}

.hero-copy {
  position: relative;
  overflow: hidden;
  min-height: 390px;
  padding: 50px;
  border: 1px solid rgba(105, 96, 78, 0.12);
  background:
    linear-gradient(135deg, rgba(246, 242, 233, 0.94), rgba(237, 231, 219, 0.78)),
    radial-gradient(circle at 85% 22%, rgba(157, 140, 116, 0.11), transparent 34%);
  box-shadow: 0 16px 36px rgba(74, 66, 50, 0.08);
}

.hero-copy::after {
  content: "";
  position: absolute;
  width: 230px;
  height: 230px;
  right: 44px;
  bottom: 34px;
  border-radius: 45% 55% 50% 50%;
  background:
    radial-gradient(circle at 45% 42%, rgba(246, 242, 233, 0.5), transparent 30%),
    linear-gradient(135deg, rgba(132, 151, 137, 0.18), rgba(157, 140, 116, 0.12));
  box-shadow: 0 12px 30px rgba(74, 88, 69, 0.07);
}

.eyebrow,
.section-kicker,
.panel-label {
  margin: 0 0 12px;
  color: #756b5a;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  font-size: 12px;
  font-weight: 800;
}

.hero-copy h1 {
  position: relative;
  z-index: 1;
  margin: 0;
  max-width: 760px;
  color: #394038;
  font-size: clamp(34px, 4.7vw, 62px);
  line-height: 1.08;
  letter-spacing: -0.055em;
  font-weight: 800;
  text-wrap: balance;
}

.hero-desc {
  position: relative;
  z-index: 1;
  width: min(660px, 100%);
  margin: 22px 0 0;
  color: #6f746c;
  font-size: 17px;
  line-height: 1.85;
}

.hero-actions,
.auth-user,
.auth-guest,
.filters,
.product-actions,
.seckill-meta,
.product-meta,
.metric-row,
.benefit-stack {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.hero-actions {
  position: relative;
  z-index: 1;
  margin-top: 32px;
}

.hero-panel {
  position: relative;
  overflow: hidden;
  padding: 28px;
  border-radius: 30px;
}

.hero-panel::before {
  content: "";
  position: absolute;
  inset: auto -80px -110px auto;
  width: 240px;
  height: 240px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(127, 149, 134, 0.12), transparent 68%);
}

.pulse-dot {
  width: 12px;
  height: 12px;
  border-radius: 999px;
  background: #aaa395;
  box-shadow: 0 0 0 7px rgba(170, 163, 149, 0.18);
}

.pulse-dot.online {
  background: #718b74;
  box-shadow: 0 0 0 7px rgba(113, 139, 116, 0.12);
}

.hero-panel h3 {
  margin: 0 0 8px;
  color: #263027;
  font-size: 28px;
  letter-spacing: -0.03em;
}

.hero-panel p:not(.panel-label) {
  color: #73766e;
  line-height: 1.7;
}

.metric-row {
  margin-top: 34px;
  justify-content: space-between;
  color: #6d746b;
}

.metric-row strong {
  color: #8f6f55;
  font-size: 54px;
  line-height: 1;
}

.benefit-stack {
  margin-top: 24px;
}

.benefit-stack span {
  padding: 9px 12px;
  border-radius: 999px;
  color: #59695d;
  background: #e8ece5;
  font-size: 13px;
}

.auth-strip {
  margin: 22px 0;
  padding: 22px 24px;
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: center;
}

.auth-strip h2,
.section-head h2 {
  margin: 0;
  color: #394038;
  font-size: 28px;
  letter-spacing: -0.04em;
}

.auth-strip p:not(.section-kicker) {
  margin: 8px 0 0;
  color: #6e736b;
}

.mall-workspace {
  display: grid;
  grid-template-columns: 245px minmax(0, 1fr);
  gap: 22px;
  align-items: start;
}

.mall-side {
  position: sticky;
  top: 18px;
  display: grid;
  gap: 10px;
  padding: 14px;
  border-radius: 26px;
}

.mall-side button {
  display: grid;
  gap: 5px;
  width: 100%;
  border: 0;
  border-radius: 20px;
  padding: 16px;
  text-align: left;
  color: #62685f;
  background: transparent;
  cursor: pointer;
  transition: transform 0.2s ease, background 0.2s ease, box-shadow 0.2s ease;
}

.mall-side button:hover {
  transform: translateX(4px);
  background: rgba(241, 236, 226, 0.72);
}

.mall-side button.active {
  color: #343b33;
  background: #f4efe5;
  box-shadow: 0 8px 18px rgba(74, 66, 50, 0.07);
}

.mall-side span {
  font-weight: 900;
  font-size: 16px;
}

.mall-side small {
  color: #777a72;
  line-height: 1.4;
}

.mall-side em {
  display: inline-grid;
  place-items: center;
  min-width: 22px;
  height: 22px;
  margin-left: 6px;
  border-radius: 999px;
  color: #fffaf2;
  background: #c17848;
  font-size: 12px;
  font-style: normal;
}

.mall-main {
  min-width: 0;
}

.content-grid,
.data-section {
  margin-top: 0;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 18px;
  padding: 4px 2px;
}

.filters {
  justify-content: flex-end;
}

.product-grid,
.seckill-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(270px, 1fr));
  gap: 18px;
}

.product-card,
.seckill-card {
  overflow: hidden;
  border-radius: 28px;
  cursor: pointer;
  transition: box-shadow 0.2s ease, border-color 0.2s ease;
}

.seckill-card {
  cursor: default;
}

.product-card:hover,
.seckill-card:hover {
  border-color: rgba(125, 103, 72, 0.2);
  box-shadow: 0 16px 34px rgba(74, 66, 50, 0.1);
}

.product-card:focus-visible {
  outline: 3px solid rgba(121, 157, 128, 0.34);
  outline-offset: 3px;
}

.product-image {
  height: 176px;
  display: grid;
  place-items: center;
  background:
    linear-gradient(135deg, rgba(132, 151, 137, 0.18), rgba(241, 236, 226, 0.78)),
    radial-gradient(circle at 75% 22%, rgba(157, 140, 116, 0.15), transparent 34%);
  background-size: cover;
  background-position: center;
  color: #75806c;
  font-size: 42px;
  font-weight: 900;
}

.product-image.large {
  height: 270px;
  border-radius: 24px;
}

.product-body,
.seckill-card {
  padding: 20px;
}

.product-sub,
.seckill-card p,
.notice-item p {
  color: #70756d;
}

.product-card h3,
.seckill-card h3 {
  min-height: 54px;
  margin: 6px 0 14px;
  color: #394038;
  font-size: 21px;
  line-height: 1.3;
}

.product-meta {
  justify-content: space-between;
  margin-bottom: 16px;
}

.product-meta strong,
.seckill-price {
  color: #8f6f55;
  font-size: 27px;
  letter-spacing: -0.04em;
}

.stock-ok {
  color: #657d69;
}

.stock-empty {
  color: #a7665b;
}

.data-section {
  padding: 22px;
  border-radius: 30px;
}

.progress-line {
  height: 8px;
  overflow: hidden;
  border-radius: 999px;
  background: #ddd4c5;
  margin: 18px 0;
}

.progress-line i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #718b74, #b09573);
}

.notice-list {
  display: grid;
  gap: 12px;
}

.notice-item {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  padding: 17px;
  border-radius: 20px;
  background: #f4efe5;
  border: 1px solid rgba(125, 103, 72, 0.1);
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease;
}

.notice-item:hover {
  transform: translateX(4px);
  border-color: rgba(61, 128, 100, 0.32);
}

.notice-item.unread {
  border-color: rgba(193, 120, 72, 0.36);
  box-shadow: 0 10px 22px rgba(193, 120, 72, 0.06);
}

.detail-title-row {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: flex-start;
  margin-top: 18px;
}

.detail-mask {
  position: fixed;
  inset: 0;
  z-index: 3000;
  display: flex;
  justify-content: flex-end;
  background: rgba(43, 47, 40, 0.24);
}

.detail-panel {
  width: min(520px, calc(100vw - 24px));
  height: calc(100vh - 24px);
  margin: 12px;
  overflow-y: auto;
  color: #2f332f;
  border-radius: 28px;
  background: #f4efe5;
  border: 1px solid rgba(125, 103, 72, 0.12);
  box-shadow: -10px 0 34px rgba(68, 58, 43, 0.14);
  animation: detail-slide-in 0.2s ease both;
}

.detail-content {
  min-height: 100%;
  padding: 22px;
}

.detail-close {
  position: sticky;
  top: 12px;
  z-index: 2;
  float: right;
  margin: 14px 14px 0 0;
  border: 0;
  border-radius: 999px;
  padding: 8px 13px;
  color: #5f665c;
  background: rgba(229, 222, 210, 0.95);
  cursor: pointer;
  font-weight: 800;
}

.detail-title-row h2 {
  margin: 0;
  color: #394038;
  font-size: 26px;
  line-height: 1.28;
  letter-spacing: -0.04em;
}

.detail-subtitle {
  margin: 12px 0 16px;
  color: #6f746c;
  line-height: 1.7;
}

.detail-price {
  padding: 14px 0;
  border-top: 1px solid rgba(125, 103, 72, 0.1);
  border-bottom: 1px solid rgba(125, 103, 72, 0.1);
}

.detail-info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin: 16px 0;
}

.detail-info-grid div {
  display: grid;
  gap: 6px;
  padding: 13px;
  border-radius: 16px;
  background: #ece6da;
  border: 1px solid rgba(125, 103, 72, 0.08);
}

.detail-info-grid span {
  color: #74776f;
  font-size: 12px;
}

.detail-info-grid strong {
  color: #394038;
  word-break: break-all;
}

.detail-buy-area {
  display: grid;
  gap: 12px;
  margin-top: 18px;
}

.detail-buy-area > div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  padding: 14px;
  border-radius: 16px;
  background: #ece6da;
  border: 1px solid rgba(105, 96, 78, 0.1);
}

.detail-buy-area span {
  color: #65695f;
  font-weight: 800;
}

.detail-buy-button {
  width: 100%;
}

:deep(.el-button--primary) {
  --el-button-bg-color: #647d69;
  --el-button-border-color: #647d69;
  --el-button-hover-bg-color: #566e5b;
  --el-button-hover-border-color: #566e5b;
  font-weight: 800;
}

:deep(.el-button.is-plain) {
  background: rgba(241, 236, 226, 0.72);
}

:deep(.el-input__wrapper),
:deep(.el-input-number),
:deep(.el-select__wrapper) {
  border-radius: 14px;
  background: #f4efe5;
}

:deep(.el-table) {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: #e9e1d4;
  --el-table-row-hover-bg-color: #f2eadf;
  --el-table-text-color: #44483f;
  --el-table-header-text-color: #6a604f;
  background: transparent;
  border-radius: 20px;
  overflow: hidden;
}

:deep(.el-table__inner-wrapper::before) {
  display: none;
}

@keyframes rise-in {
  from {
    opacity: 0;
    transform: translateY(14px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes detail-slide-in {
  from {
    opacity: 0;
    transform: translateX(22px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@media (max-width: 1080px) {
  .mall-hero {
    grid-template-columns: 1fr;
  }

  .mall-workspace {
    grid-template-columns: 1fr;
  }

  .mall-side {
    position: relative;
    top: auto;
    grid-template-columns: repeat(4, minmax(160px, 1fr));
    overflow-x: auto;
  }

  .mall-side button:hover {
    transform: translateY(-2px);
  }
}

@media (max-width: 720px) {
  .mall-shell {
    padding: 16px;
  }

  .hero-copy {
    min-height: auto;
    padding: 30px;
  }

  .auth-strip,
  .section-head {
    align-items: stretch;
    flex-direction: column;
  }

  .filters {
    justify-content: flex-start;
  }

  .mall-side {
    grid-template-columns: 1fr 1fr;
  }
}
</style>


