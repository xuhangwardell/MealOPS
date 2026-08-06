import { defineStore } from "pinia";
import { getHealth } from "@/api/system";

export type BackendStatus = "unknown" | "checking" | "online" | "offline";

function readableError(error: unknown): string {
    return error instanceof Error ? error.message : "后端连接检查失败";
}

export const useAppStore = defineStore("app", {
    state: () => ({
        backendStatus: "unknown" as BackendStatus,
        lastCheckedAt: null as string | null,
        lastError: null as string | null
    }),
    actions: {
        async checkBackendHealth(): Promise<void> {
            if (this.backendStatus === "checking") {
                return;
            }
            this.backendStatus = "checking";
            try {
                const health = await getHealth();
                if (health.status !== "UP") {
                    throw new Error(`后端状态为 ${health.status}`);
                }
                this.backendStatus = "online";
                this.lastError = null;
            } catch (error: unknown) {
                this.backendStatus = "offline";
                this.lastError = readableError(error);
            } finally {
                this.lastCheckedAt = new Date().toISOString();
            }
        }
    }
});
