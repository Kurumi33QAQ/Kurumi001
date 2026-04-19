<template>
  <div class="login-page">
    <div class="login-card">
      <h2 class="title">MyMall 后台{{ isRegisterMode ? "注册" : "登录" }}</h2>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="handleSubmit">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" clearable />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" clearable />
        </el-form-item>
        <el-form-item v-if="isRegisterMode" label="确认密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            show-password
            placeholder="请再次输入密码"
            clearable
          />
        </el-form-item>
        <el-button type="primary" :loading="loading" class="submit" @click="handleSubmit">
          {{ isRegisterMode ? "注册" : "登录" }}
        </el-button>
      </el-form>

      <div class="switch-mode">
        <span>{{ isRegisterMode ? "已有账号？" : "没有账号？" }}</span>
        <el-button link type="primary" @click="toggleMode">
          {{ isRegisterMode ? "去登录" : "去注册" }}
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";
import { useRouter, useRoute } from "vue-router";
import { useAuthStore } from "@/store/auth";

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();
const loading = ref(false);
const formRef = ref<FormInstance>();
const isRegisterMode = ref(false);

const form = reactive({
  username: "",
  password: "",
  confirmPassword: ""
});

const rules: FormRules = {
  username: [{ required: true, message: "请输入用户名", trigger: "blur" }],
  password: [{ required: true, message: "请输入密码", trigger: "blur" }],
  confirmPassword: [
    {
      validator: (_, value, callback) => {
        if (!isRegisterMode.value) {
          callback();
          return;
        }
        if (!value) {
          callback(new Error("请再次输入密码"));
          return;
        }
        if (value !== form.password) {
          callback(new Error("两次输入密码不一致"));
          return;
        }
        callback();
      },
      trigger: "blur"
    }
  ]
};

function toggleMode() {
  isRegisterMode.value = !isRegisterMode.value;
  form.password = "";
  form.confirmPassword = "";
  formRef.value?.clearValidate();
}

async function handleSubmit() {
  if (!formRef.value) return;
  try {
    await formRef.value.validate();
  } catch {
    return;
  }

  loading.value = true;
  try {
    if (isRegisterMode.value) {
      await authStore.register({
        username: form.username,
        password: form.password
      });
      ElMessage.success("注册成功，请使用新账号登录");
      isRegisterMode.value = false;
      form.password = "";
      form.confirmPassword = "";
      return;
    }

    await authStore.login({
      username: form.username,
      password: form.password
    });
    const redirect = (route.query.redirect as string) || "/dashboard";
    router.replace(redirect);
  } catch {
    // 错误弹窗由全局拦截器统一处理
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(140deg, #e8eef6 0%, #f6f8fb 45%, #ecf2f8 100%);
}

.login-card {
  width: 420px;
  max-width: 92vw;
  background: #ffffff;
  border-radius: 12px;
  padding: 28px;
  box-shadow: 0 10px 32px rgba(15, 23, 42, 0.12);
}

.title {
  margin-top: 0;
  margin-bottom: 20px;
  text-align: center;
  color: #0f172a;
}

.submit {
  width: 100%;
  margin-top: 8px;
}

.switch-mode {
  margin-top: 14px;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 4px;
  color: #64748b;
  font-size: 13px;
}
</style>
