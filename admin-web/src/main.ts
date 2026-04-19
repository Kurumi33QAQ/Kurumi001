import { createApp } from "vue";
import { createPinia } from "pinia";
import ElementPlus from "element-plus";
import "element-plus/dist/index.css";
import App from "./App.vue";
import router from "@/router";
import permissionDirective from "@/directives/permission";
import { setUnauthorizedHandler } from "@/api/http";
import { useAuthStore } from "@/store/auth";
import "@/styles/index.css";

const app = createApp(App);
const pinia = createPinia();

app.use(pinia);
app.use(router);
app.use(ElementPlus);
app.use(permissionDirective);

setUnauthorizedHandler(() => {
  const authStore = useAuthStore();
  authStore.clearAuth();
  const current = router.currentRoute.value.fullPath;
  if (router.currentRoute.value.path !== "/login") {
    router.replace({ path: "/login", query: { redirect: current } });
  }
});

app.mount("#app");
