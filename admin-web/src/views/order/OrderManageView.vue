<template>
  <div class="page-card">
    <el-form :inline="true" :model="query" class="toolbar-left">
      <el-form-item label="订单号">
        <el-input v-model="query.orderSn" clearable placeholder="请输入订单号" />
      </el-form-item>
      <el-form-item label="下单用户">
        <el-input v-model="query.memberUsername" clearable placeholder="请输入用户名" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable placeholder="全部" style="width: 130px">
          <el-option v-for="item in ORDER_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="list" border :loading="loading">
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column prop="orderSn" label="订单号" min-width="210" />
      <el-table-column prop="memberUsername" label="下单用户" min-width="140" />
      <el-table-column label="总金额" width="120">
        <template #default="{ row }">¥ {{ formatAmount(row.totalAmount) }}</template>
      </el-table-column>
      <el-table-column label="支付金额" width="120">
        <template #default="{ row }">¥ {{ formatAmount(row.payAmount) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="更新时间" width="180">
        <template #default="{ row }">{{ formatDateTime(row.updateTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row.id)">详情</el-button>
          <el-button
            v-permission="'oms:order:write'"
            link
            type="warning"
            @click="openStatusDialog(row)"
          >
            更新状态
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div style="margin-top: 14px; display: flex; justify-content: flex-end">
      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        :page-sizes="[10, 20, 50]"
        @size-change="fetchList"
        @current-change="fetchList"
      />
    </div>

    <el-drawer v-model="detailVisible" title="订单详情" size="520px">
      <el-descriptions :column="1" border v-if="currentOrder">
        <el-descriptions-item label="ID">{{ currentOrder.id }}</el-descriptions-item>
        <el-descriptions-item label="订单号">{{ currentOrder.orderSn }}</el-descriptions-item>
        <el-descriptions-item label="下单用户">{{ currentOrder.memberUsername }}</el-descriptions-item>
        <el-descriptions-item label="总金额">¥ {{ formatAmount(currentOrder.totalAmount) }}</el-descriptions-item>
        <el-descriptions-item label="支付金额">¥ {{ formatAmount(currentOrder.payAmount) }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusLabel(currentOrder.status) }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ currentOrder.note || "-" }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(currentOrder.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatDateTime(currentOrder.updateTime) }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>

    <el-dialog v-model="statusVisible" title="更新订单状态" width="500px">
      <el-form ref="statusFormRef" :model="statusForm" :rules="statusRules" label-width="88px">
        <el-form-item label="订单ID">
          <el-input v-model="statusForm.id" disabled />
        </el-form-item>
        <el-form-item label="目标状态" prop="status">
          <el-select v-model="statusForm.status" style="width: 100%">
            <el-option v-for="item in ORDER_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="note">
          <el-input v-model="statusForm.note" type="textarea" :rows="3" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="statusVisible = false">取消</el-button>
        <el-button type="primary" :loading="statusLoading" @click="submitStatus">确认更新</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";
import { detailOrderApi, listOrderApi, updateOrderStatusApi } from "@/api/modules/order";
import type { Order } from "@/types/order";
import { formatAmount, formatDateTime } from "@/utils/format";

const ORDER_STATUS_OPTIONS = [
  { value: 0, label: "待付款" },
  { value: 1, label: "待发货" },
  { value: 2, label: "已发货" },
  { value: 3, label: "已完成" },
  { value: 4, label: "已关闭" }
];

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  orderSn: "",
  memberUsername: "",
  status: undefined as number | undefined
});

const loading = ref(false);
const list = ref<Order[]>([]);
const total = ref(0);

const detailVisible = ref(false);
const currentOrder = ref<Order | null>(null);

const statusVisible = ref(false);
const statusLoading = ref(false);
const statusFormRef = ref<FormInstance>();
const statusForm = reactive({
  id: 0,
  status: 0,
  note: ""
});

const statusRules: FormRules = {
  status: [{ required: true, message: "请选择状态", trigger: "change" }]
};

function statusLabel(status: number) {
  return ORDER_STATUS_OPTIONS.find((item) => item.value === status)?.label || `状态${status}`;
}

function statusTag(status: number) {
  if (status === 3) return "success";
  if (status === 4) return "danger";
  if (status === 2) return "warning";
  return "info";
}

async function fetchList() {
  loading.value = true;
  try {
    const data = await listOrderApi(query);
    list.value = data.records || [];
    total.value = data.total || 0;
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  query.pageNum = 1;
  fetchList();
}

function handleReset() {
  query.orderSn = "";
  query.memberUsername = "";
  query.status = undefined;
  query.pageNum = 1;
  fetchList();
}

async function openDetail(id: number) {
  currentOrder.value = await detailOrderApi(id);
  detailVisible.value = true;
}

function openStatusDialog(row: Order) {
  statusForm.id = row.id;
  statusForm.status = row.status;
  statusForm.note = row.note || "";
  statusVisible.value = true;
}

async function submitStatus() {
  if (!statusFormRef.value) return;
  await statusFormRef.value.validate();
  statusLoading.value = true;
  try {
    await updateOrderStatusApi(statusForm);
    ElMessage.success("订单状态更新成功");
    statusVisible.value = false;
    fetchList();
  } finally {
    statusLoading.value = false;
  }
}

onMounted(fetchList);
</script>
