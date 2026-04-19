<template>
  <div class="forbidden">
    <el-result icon="warning" title="403 无权限" sub-title="当前账号没有访问该功能的权限。">
      <template #extra>
        <el-button type="primary" @click="backDashboard">返回工作台</el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from "vue-router";

const router = useRouter();

function backDashboard() {
  router.replace("/dashboard");
}
</script>

<style scoped>
.forbidden {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8fafc;
}
</style>
