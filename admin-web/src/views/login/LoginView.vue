<template>
  <div class="login-page">
    <section class="login-hero">
      <div class="hero-card">
        <p>MYMALL ADMIN</p>
        <h1>让商城运营更清楚、更稳妥</h1>
        <span>商品、订单、权限和日志集中管理，适合后台管理员日常工作。</span>
      </div>
    </section>

    <section class="login-panel">
      <div class="login-card">
        <p class="section-kicker">管理后台</p>
        <h2>{{ isRegisterMode ? "创建管理员账号" : "管理员登录" }}</h2>
        <p class="desc">{{ isRegisterMode ? "注册后请使用新账号登录后台。" : "请使用管理员账号进入运营管理后台。" }}</p>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="handleSubmit">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名" size="large" clearable />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" type="password" size="large" show-password placeholder="请输入密码" clearable />
          </el-form-item>
          <el-form-item v-if="isRegisterMode" label="确认密码" prop="confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              type="password"
              size="large"
              show-password
              placeholder="请再次输入密码"
              clearable
            />
          </el-form-item>
          <el-button type="primary" :loading="loading" class="submit" size="large" @click="handleSubmit">
            {{ isRegisterMode ? "注册账号" : "登录后台" }}
          </el-button>
        </el-form>

        <div class="switch-mode">
          <span>{{ isRegisterMode ? "已有账号？" : "没有账号？" }}</span>
          <el-button link type="primary" @click="toggleMode">
            {{ isRegisterMode ? "去登录" : "去注册" }}
          </el-button>
        </div>
      </div>
    </section>
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
          callback(new Error("两次输入的密码不一致"));
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
    // 错误提示由请求拦截器统一处理。
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(420px, 0.95fr);
  color: #263027;
  background:
    radial-gradient(circle at 18% 12%, rgba(243, 215, 164, 0.38), transparent 30%),
    radial-gradient(circle at 82% 80%, rgba(61, 128, 100, 0.2), transparent 30%),
    linear-gradient(135deg, #f6f3ed 0%, #edf3ef 100%);
}

.login-hero,
.login-panel {
  display: grid;
  align-items: center;
  padding: 48px;
}

.hero-card,
.login-card {
  border: 1px solid rgba(123, 103, 72, 0.1);
  background: rgba(255, 252, 246, 0.78);
  box-shadow: 0 28px 76px rgba(106, 83, 52, 0.12);
  backdrop-filter: blur(16px);
}

.hero-card {
  max-width: 560px;
  padding: 56px;
  border-radius: 38px;
}

.hero-card p,
.section-kicker {
  margin: 0 0 14px;
  color: #8a6d4f;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.18em;
}

.hero-card h1 {
  margin: 0;
  font-size: clamp(42px, 5vw, 72px);
  line-height: 1;
  letter-spacing: -0.07em;
}

.hero-card span {
  display: block;
  max-width: 440px;
  margin-top: 22px;
  color: #747970;
  font-size: 17px;
  line-height: 1.8;
}

.login-panel {
  justify-items: center;
}

.login-card {
  width: min(100%, 440px);
  padding: 38px;
  border-radius: 32px;
}

.login-card h2 {
  margin: 0;
  font-size: 32px;
  letter-spacing: -0.04em;
}

.desc {
  margin: 12px 0 26px;
  color: #747970;
}

.submit {
  width: 100%;
  height: 48px;
  margin-top: 8px;
}

.switch-mode {
  margin-top: 16px;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 4px;
  color: #747970;
  font-size: 13px;
}

:deep(.el-button--primary) {
  --el-button-bg-color: #3d8064;
  --el-button-border-color: #3d8064;
  --el-button-hover-bg-color: #2f6d54;
  --el-button-hover-border-color: #2f6d54;
  font-weight: 800;
}

:deep(.el-input__wrapper) {
  border-radius: 15px;
  background: #fffaf2;
  box-shadow: 0 0 0 1px rgba(123, 103, 72, 0.1) inset;
}

@media (max-width: 880px) {
  .login-page {
    grid-template-columns: 1fr;
  }

  .login-hero,
  .login-panel {
    padding: 24px;
  }

  .hero-card,
  .login-card {
    padding: 30px;
    border-radius: 28px;
  }
}
</style>
