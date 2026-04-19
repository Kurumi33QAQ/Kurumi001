import type { App, DirectiveBinding } from "vue";
import { useAuthStore } from "@/store/auth";

function checkPermission(binding: DirectiveBinding<string | string[]>) {
  const authStore = useAuthStore();
  const required = binding.value;
  return authStore.hasPermission(required);
}

export default {
  install(app: App) {
    app.directive("permission", {
      mounted(el, binding) {
        if (!checkPermission(binding)) {
          el.parentNode?.removeChild(el);
        }
      }
    });
  }
};
