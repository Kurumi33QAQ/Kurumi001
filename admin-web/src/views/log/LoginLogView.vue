<template>
  <div class="page-card">
    <el-form :inline="true" :model="queryForm" class="toolbar-left">
      <el-form-item label="用户名">
        <el-input v-model="queryForm.username" clearable placeholder="请输入用户名" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryForm.status" clearable placeholder="全部" style="width: 130px">
          <el-option label="成功" :value="1" />
          <el-option label="失败" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="时间范围">
        <el-date-picker
          v-model="dateRange"
          type="datetimerange"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          value-format="x"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <div class="toolbar-left">
        <el-button type="success" @click="handleExport">导出 CSV</el-button>
        <el-date-picker v-model="cleanBefore" type="datetime" placeholder="清理该时间之前日志" value-format="x" />
        <el-button type="danger" plain @click="handleClean">清理日志</el-button>
      </div>
    </div>

    <el-table :data="list" border :loading="loading">
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column prop="adminId" label="用户ID" width="100" />
      <el-table-column prop="username" label="用户名" width="140" />
      <el-table-column prop="ip" label="IP" width="140" />
      <el-table-column prop="userAgent" label="客户端" min-width="240" show-overflow-tooltip />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? "成功" : "失败" }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="message" label="说明" min-width="180" show-overflow-tooltip />
      <el-table-column label="时间" width="180">
        <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
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
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { cleanLoginLogApi, exportLoginLogApi, listLoginLogApi } from "@/api/modules/loginLog";
import type { LoginLog, LoginLogQuery } from "@/types/log";
import { formatDateTime } from "@/utils/format";

const loading = ref(false);
const list = ref<LoginLog[]>([]);
const total = ref(0);
const dateRange = ref<string[]>();
const cleanBefore = ref<string>();

const query = reactive<LoginLogQuery>({
  pageNum: 1,
  pageSize: 10,
  username: "",
  status: undefined,
  startTime: undefined,
  endTime: undefined
});

const queryForm = reactive({
  username: "",
  status: undefined as number | undefined
});

function formatDateForQuery(timestamp?: string) {
  if (!timestamp) return undefined;
  const date = new Date(Number(timestamp));
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  const hh = String(date.getHours()).padStart(2, "0");
  const mm = String(date.getMinutes()).padStart(2, "0");
  const ss = String(date.getSeconds()).padStart(2, "0");
  return `${y}-${m}-${d} ${hh}:${mm}:${ss}`;
}

function formatDateForBody(timestamp?: string) {
  if (!timestamp) return "";
  const date = new Date(Number(timestamp));
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  const hh = String(date.getHours()).padStart(2, "0");
  const mm = String(date.getMinutes()).padStart(2, "0");
  const ss = String(date.getSeconds()).padStart(2, "0");
  return `${y}-${m}-${d}T${hh}:${mm}:${ss}`;
}

async function fetchList() {
  loading.value = true;
  try {
    const data = await listLoginLogApi(query);
    list.value = data.records || [];
    total.value = data.total || 0;
  } finally {
    loading.value = false;
  }
}

function buildQueryFromForm() {
  query.username = queryForm.username || undefined;
  query.status = queryForm.status;
  query.startTime = formatDateForQuery(dateRange.value?.[0]);
  query.endTime = formatDateForQuery(dateRange.value?.[1]);
}

function handleSearch() {
  query.pageNum = 1;
  buildQueryFromForm();
  fetchList();
}

function handleReset() {
  queryForm.username = "";
  queryForm.status = undefined;
  dateRange.value = undefined;
  query.pageNum = 1;
  buildQueryFromForm();
  fetchList();
}

async function handleExport() {
  buildQueryFromForm();
  const response = await exportLoginLogApi({
    username: query.username,
    status: query.status,
    startTime: query.startTime,
    endTime: query.endTime
  });
  const blob = new Blob([response.data], { type: "text/csv;charset=UTF-8" });
  const fileName = "login_logs.csv";
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = fileName;
  link.click();
  URL.revokeObjectURL(url);
  ElMessage.success("导出成功");
}

async function handleClean() {
  if (!cleanBefore.value) {
    ElMessage.warning("请选择清理截止时间");
    return;
  }
  try {
    await ElMessageBox.confirm("确定清理该时间之前的登录日志吗？", "提示", { type: "warning" });
    const count = await cleanLoginLogApi(formatDateForBody(cleanBefore.value));
    ElMessage.success(`清理完成，共删除 ${count} 条日志`);
    fetchList();
  } catch {
    // 用户取消时不处理
  }
}

onMounted(fetchList);
</script>
