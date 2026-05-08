<template>
  <div class="member-login-shell">
    <section class="login-visual">
      <button class="back-home" @click="goHome">返回商城</button>
      <div class="brand-card">
        <p class="eyebrow">MYMALL SELECT</p>
        <h1>登录后，继续你的购物旅程</h1>
        <p>查看订单、接收提醒、参与限时抢购，都从这里开始。</p>
        <div class="visual-tags">
          <span>订单同步</span>
          <span>抢购提醒</span>
          <span>会员服务</span>
        </div>
      </div>
    </section>

    <section class="login-panel">
      <div class="login-card">
        <p class="eyebrow">会员中心</p>
        <h2>{{ mode === 'login' ? '欢迎回来' : '创建你的会员账号' }}</h2>
        <p class="panel-desc">{{ mode === 'login' ? '登录后即可购买商品、查看订单和接收消息提醒。' : '注册成功后会自动登录，方便你继续逛商城。' }}</p>

        <div class="mode-tabs">
          <button :class="{ active: mode === 'login' }" @click="mode = 'login'">登录</button>
          <button :class="{ active: mode === 'register' }" @click="mode = 'register'">注册</button>
        </div>

        <el-form class="member-form" :model="form" label-position="top" @submit.prevent>
          <el-form-item label="用户名">
            <el-input v-model="form.username" placeholder="请输入买家用户名" size="large" clearable />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="form.password" placeholder="请输入密码" size="large" type="password" show-password />
          </el-form-item>
          <el-button class="submit-btn" type="primary" size="large" :loading="loading" @click="submit">
            {{ mode === 'login' ? '登录并返回商城' : '注册并登录' }}
          </el-button>
        </el-form>

        <p class="switch-tip">
          {{ mode === 'login' ? '还没有账号？' : '已经有账号？' }}
          <button @click="toggleMode">{{ mode === 'login' ? '立即注册' : '去登录' }}</button>
        </p>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { memberLoginApi, memberRegisterApi } from "@/api/modules/member";
import { setMemberAccessToken } from "@/utils/memberAuth";

const route = useRoute();
const router = useRouter();
const mode = ref<"login" | "register">("login");
const loading = ref(false);
const form = reactive({
  username: "",
  password: ""
});

function getRedirectPath() {
  const redirect = route.query.redirect;
  return typeof redirect === "string" && redirect.startsWith("/") ? redirect : "/mall";
}

function toggleMode() {
  mode.value = mode.value === "login" ? "register" : "login";
}

function goHome() {
  router.push("/mall");
}

async function submit() {
  if (!form.username || !form.password) {
    ElMessage.warning("请输入用户名和密码");
    return;
  }

  loading.value = true;
  try {
    if (mode.value === "register") {
      await memberRegisterApi(form);
      ElMessage.success("注册成功，正在为你登录");
    }

    const data = await memberLoginApi(form);
    setMemberAccessToken(data.token);
    ElMessage.success("欢迎回来");
    await router.replace(getRedirectPath());
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.member-login-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(420px, 0.95fr);
  color: #2f332f;
  background:
    radial-gradient(circle at 12% 12%, rgba(236, 191, 132, 0.34), transparent 30%),
    radial-gradient(circle at 82% 78%, rgba(128, 180, 151, 0.24), transparent 32%),
    linear-gradient(135deg, #fbf6ec 0%, #f5eadc 45%, #eef3e7 100%);
}

.login-visual,
.login-panel {
  position: relative;
  display: grid;
  align-items: center;
  padding: 48px;
}

.login-visual {
  overflow: hidden;
}

.login-visual::before,
.login-visual::after {
  content: "";
  position: absolute;
  border-radius: 999px;
  pointer-events: none;
}

.login-visual::before {
  width: 420px;
  height: 420px;
  left: -110px;
  bottom: -130px;
  background: rgba(128, 180, 151, 0.22);
}

.login-visual::after {
  width: 260px;
  height: 260px;
  right: 70px;
  top: 74px;
  background: rgba(229, 160, 112, 0.2);
}

.back-home {
  position: absolute;
  top: 34px;
  left: 42px;
  z-index: 2;
  border: 0;
  border-radius: 999px;
  padding: 10px 16px;
  color: #4f5b4d;
  background: rgba(255, 255, 255, 0.68);
  box-shadow: 0 12px 30px rgba(90, 72, 45, 0.1);
  cursor: pointer;
}

.brand-card,
.login-card {
  position: relative;
  z-index: 1;
  border: 1px solid rgba(133, 105, 70, 0.12);
  background: rgba(255, 255, 255, 0.74);
  box-shadow: 0 30px 80px rgba(105, 83, 54, 0.12);
  backdrop-filter: blur(18px);
}

.brand-card {
  max-width: 590px;
  padding: 58px;
  border-radius: 38px;
}

.brand-card h1 {
  margin: 0;
  font-size: clamp(42px, 5vw, 74px);
  line-height: 1;
  letter-spacing: -0.07em;
  color: #263027;
}

.brand-card p:not(.eyebrow) {
  margin: 22px 0 0;
  max-width: 470px;
  color: #6f746c;
  font-size: 18px;
  line-height: 1.8;
}

.eyebrow {
  margin: 0 0 14px;
  color: #8b6f4e;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.2em;
}

.visual-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 34px;
}

.visual-tags span {
  border-radius: 999px;
  padding: 10px 14px;
  color: #466554;
  background: #edf5ec;
}

.login-panel {
  justify-items: center;
}

.login-card {
  width: min(100%, 480px);
  padding: 42px;
  border-radius: 34px;
}

.login-card h2 {
  margin: 0;
  font-size: 34px;
  letter-spacing: -0.04em;
}

.panel-desc {
  margin: 12px 0 26px;
  color: #73766e;
  line-height: 1.7;
}

.mode-tabs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  padding: 5px;
  margin-bottom: 24px;
  border-radius: 18px;
  background: #f1eadf;
}

.mode-tabs button {
  border: 0;
  border-radius: 14px;
  padding: 12px;
  color: #7a756c;
  background: transparent;
  cursor: pointer;
  font-weight: 800;
}

.mode-tabs button.active {
  color: #263027;
  background: #fffaf2;
  box-shadow: 0 12px 24px rgba(113, 85, 47, 0.1);
}

.member-form {
  display: grid;
  gap: 4px;
}

.submit-btn {
  width: 100%;
  height: 48px;
  margin-top: 8px;
}

.switch-tip {
  margin: 22px 0 0;
  text-align: center;
  color: #77736b;
}

.switch-tip button {
  border: 0;
  color: #3d8064;
  background: transparent;
  cursor: pointer;
  font-weight: 800;
}

:deep(.el-button--primary) {
  --el-button-bg-color: #3d8064;
  --el-button-border-color: #3d8064;
  --el-button-hover-bg-color: #2f6d54;
  --el-button-hover-border-color: #2f6d54;
  font-weight: 800;
}

:deep(.el-input__wrapper) {
  min-height: 46px;
  border-radius: 16px;
  background: #fffaf2;
  box-shadow: 0 0 0 1px rgba(118, 99, 71, 0.11) inset;
}

@media (max-width: 900px) {
  .member-login-shell {
    grid-template-columns: 1fr;
  }

  .login-visual,
  .login-panel {
    padding: 24px;
  }

  .login-visual {
    min-height: 410px;
  }

  .back-home {
    left: 24px;
    top: 22px;
  }

  .brand-card,
  .login-card {
    padding: 30px;
    border-radius: 28px;
  }
}
</style>
