<template>
  <div class="dashboard">
    <el-row :gutter="16">
      <el-col :span="8" :xs="24" :sm="12" :md="8">
        <div class="page-card">
          <div class="metric-title">当前用户</div>
          <div class="metric-value">{{ authStore.displayName }}</div>
        </div>
      </el-col>
      <el-col :span="8" :xs="24" :sm="12" :md="8">
        <div class="page-card">
          <div class="metric-title">权限点数量</div>
          <div class="metric-value">{{ authStore.authorities.length }}</div>
        </div>
      </el-col>
      <el-col :span="8" :xs="24" :sm="12" :md="8">
        <div class="page-card">
          <div class="metric-title">系统状态</div>
          <div class="metric-value status">联调就绪</div>
        </div>
      </el-col>
    </el-row>

    <div class="page-card panel">
      <h3>当前权限点</h3>
      <el-empty v-if="!authStore.authorities.length" description="暂无权限数据" />
      <div v-else class="authority-list">
        <el-tag v-for="item in authStore.authorities" :key="item" size="small">{{ item }}</el-tag>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useAuthStore } from "@/store/auth";

const authStore = useAuthStore();
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.metric-title {
  color: #64748b;
  margin-bottom: 8px;
}

.metric-value {
  font-size: 24px;
  font-weight: 600;
  color: #0f172a;
}

.metric-value.status {
  color: #0f766e;
}

.panel h3 {
  margin-top: 0;
}

.authority-list {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
