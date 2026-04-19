<template>
  <div class="rbac">
    <div class="page-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-select
            v-model="selectedAdminId"
            filterable
            placeholder="请选择用户"
            style="width: 280px"
            @change="onAdminChange"
          >
            <el-option
              v-for="item in adminList"
              :key="item.id"
              :label="`${item.username}（ID:${item.id}）`"
              :value="item.id"
            />
          </el-select>
          <el-button @click="refreshAll">刷新数据</el-button>
        </div>
      </div>

      <el-row :gutter="16">
        <el-col :span="12" :xs="24">
          <div class="section">
            <h3>用户角色分配</h3>
            <el-empty v-if="!selectedAdminId" description="先选择用户" />
            <template v-else>
              <el-checkbox-group v-model="adminRoleIds" class="role-group">
                <el-checkbox v-for="role in roleOptions" :key="role.id" :label="role.id">
                  {{ role.name }}
                </el-checkbox>
              </el-checkbox-group>
              <div class="action-row">
                <el-button type="primary" :loading="assignRoleLoading" @click="submitAdminRoles">
                  保存用户角色
                </el-button>
              </div>
            </template>
          </div>
        </el-col>

        <el-col :span="12" :xs="24">
          <div class="section">
            <h3>角色资源分配</h3>
            <el-select
              v-model="selectedRoleId"
              filterable
              placeholder="请选择角色"
              style="width: 100%; margin-bottom: 12px"
              @change="onRoleChange"
            >
              <el-option v-for="role in roleOptions" :key="role.id" :label="role.name" :value="role.id" />
            </el-select>
            <el-empty v-if="!selectedRoleId" description="请选择角色" />
            <template v-else>
              <el-transfer
                v-model="roleResourceIds"
                :data="resourceTransferList"
                filterable
                :titles="['可选资源', '已分配资源']"
                style="text-align: left"
              />
              <div class="action-row">
                <el-button type="primary" :loading="assignResLoading" @click="submitRoleResources">
                  保存角色资源
                </el-button>
              </div>
            </template>
          </div>
        </el-col>
      </el-row>
    </div>

    <div class="page-card">
      <h3>权限汇总回显</h3>
      <el-empty v-if="!permissionSummary" description="请选择用户查看权限汇总" />
      <el-descriptions v-else :column="2" border>
        <el-descriptions-item label="用户ID">{{ permissionSummary.adminId }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ permissionSummary.username }}</el-descriptions-item>
        <el-descriptions-item label="角色" :span="2">
          <el-tag v-for="role in permissionSummary.roleNames" :key="role" class="tag">{{ role }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="权限点" :span="2">
          <el-tag v-for="auth in permissionSummary.authorities" :key="auth" class="tag" type="success">
            {{ auth }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import { permissionSummaryApi } from "@/api/modules/auth";
import {
  assignAdminRolesApi,
  assignRoleResourcesApi,
  listAdminApi,
  listAdminRoleIdsApi,
  listResourceOptionsApi,
  listRoleOptionsApi,
  listRoleResourceIdsApi
} from "@/api/modules/rbac";
import type { OptionItem } from "@/types/api";
import type { PermissionSummary } from "@/types/auth";
import type { AdminUser } from "@/types/rbac";

const adminList = ref<AdminUser[]>([]);
const roleOptions = ref<OptionItem[]>([]);
const resourceOptions = ref<OptionItem[]>([]);

const selectedAdminId = ref<number>();
const selectedRoleId = ref<number>();
const adminRoleIds = ref<number[]>([]);
const roleResourceIds = ref<number[]>([]);

const permissionSummary = ref<PermissionSummary | null>(null);
const assignRoleLoading = ref(false);
const assignResLoading = ref(false);

const resourceTransferList = computed(() =>
  resourceOptions.value.map((item) => ({
    key: item.id,
    label: `${item.name}（ID:${item.id}）`
  }))
);

async function refreshAll() {
  const [admins, roles, resources] = await Promise.all([
    listAdminApi(),
    listRoleOptionsApi(),
    listResourceOptionsApi()
  ]);
  adminList.value = admins || [];
  roleOptions.value = roles || [];
  resourceOptions.value = resources || [];

  if (!selectedAdminId.value && adminList.value.length > 0) {
    selectedAdminId.value = adminList.value[0].id;
    await onAdminChange(selectedAdminId.value);
  }
}

async function onAdminChange(adminId: number) {
  adminRoleIds.value = await listAdminRoleIdsApi(adminId);
  permissionSummary.value = await permissionSummaryApi(adminId);
}

async function onRoleChange(roleId: number) {
  roleResourceIds.value = await listRoleResourceIdsApi(roleId);
}

async function submitAdminRoles() {
  if (!selectedAdminId.value) {
    ElMessage.warning("请先选择用户");
    return;
  }
  if (!adminRoleIds.value.length) {
    ElMessage.warning("至少选择一个角色");
    return;
  }
  assignRoleLoading.value = true;
  try {
    await assignAdminRolesApi(selectedAdminId.value, adminRoleIds.value);
    ElMessage.success("用户角色分配成功");
    permissionSummary.value = await permissionSummaryApi(selectedAdminId.value);
  } finally {
    assignRoleLoading.value = false;
  }
}

async function submitRoleResources() {
  if (!selectedRoleId.value) {
    ElMessage.warning("请先选择角色");
    return;
  }
  if (!roleResourceIds.value.length) {
    ElMessage.warning("至少选择一个资源");
    return;
  }
  assignResLoading.value = true;
  try {
    await assignRoleResourcesApi(selectedRoleId.value, roleResourceIds.value);
    ElMessage.success("角色资源分配成功");
    if (selectedAdminId.value) {
      permissionSummary.value = await permissionSummaryApi(selectedAdminId.value);
    }
  } finally {
    assignResLoading.value = false;
  }
}

onMounted(refreshAll);
</script>

<style scoped>
.rbac {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.section {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 14px;
  min-height: 280px;
}

.section h3 {
  margin-top: 0;
  margin-bottom: 12px;
}

.role-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.action-row {
  margin-top: 14px;
}

.tag {
  margin-right: 6px;
  margin-bottom: 6px;
}
</style>
