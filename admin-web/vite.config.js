import { defineConfig, loadEnv } from "vite";
import vue from "@vitejs/plugin-vue";
import path from "node:path";
export default defineConfig(function (_a) {
    var mode = _a.mode;
    var env = loadEnv(mode, process.cwd());
    var proxyTarget = env.VITE_PROXY_TARGET || "http://127.0.0.1:8080";
    return {
        plugins: [vue()],
        resolve: {
            alias: {
                "@": path.resolve(__dirname, "src")
            }
        },
        server: {
            host: "0.0.0.0",
            port: 5173,
            proxy: {
                "/api": {
                    target: proxyTarget,
                    changeOrigin: true,
                    rewrite: function (pathStr) { return pathStr.replace(/^\/api/, ""); }
                }
            }
        }
    };
});
