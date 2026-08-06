import { defineConfig, loadEnv } from "vite";
import uni from "@dcloudio/vite-plugin-uni";

export default defineConfig(({ mode }) => {
    const environment = loadEnv(mode, process.cwd(), "");
    const proxyTarget = environment.VITE_API_PROXY_TARGET || "http://127.0.0.1:8080";

    return {
        plugins: [uni()],
        server: {
            host: "127.0.0.1",
            port: 5173,
            strictPort: true,
            proxy: {
                "/api": { target: proxyTarget, changeOrigin: true },
                "/actuator": { target: proxyTarget, changeOrigin: true }
            }
        }
    };
});
