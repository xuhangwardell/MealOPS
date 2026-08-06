import { createPinia, setActivePinia } from "pinia";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { getHealth } from "@/api/system";
import { useAppStore } from "@/stores/app";

vi.mock("@/api/system", () => ({ getHealth: vi.fn() }));
const getHealthMock = vi.mocked(getHealth);

describe("application store", () => {
    beforeEach(() => {
        setActivePinia(createPinia());
        getHealthMock.mockReset();
        vi.useFakeTimers();
        vi.setSystemTime(new Date("2026-08-06T00:00:00.000Z"));
    });

    it("starts with unknown backend status", () => {
        const store = useAppStore();
        expect(store.backendStatus).toBe("unknown");
        expect(store.lastCheckedAt).toBeNull();
        expect(store.lastError).toBeNull();
    });

    it("transitions checking to online", async () => {
        getHealthMock.mockResolvedValue({ status: "UP" });
        const store = useAppStore();
        const check = store.checkBackendHealth();
        expect(store.backendStatus).toBe("checking");
        await check;
        expect(store.backendStatus).toBe("online");
        expect(store.lastCheckedAt).toBe("2026-08-06T00:00:00.000Z");
    });

    it("transitions checking to offline with a readable error", async () => {
        getHealthMock.mockRejectedValue(new Error("Backend unavailable"));
        const store = useAppStore();
        await store.checkBackendHealth();
        expect(store.backendStatus).toBe("offline");
        expect(store.lastError).toBe("Backend unavailable");
    });

    it("clears an old error after a successful retry", async () => {
        getHealthMock.mockRejectedValueOnce(new Error("Offline")).mockResolvedValueOnce({ status: "UP" });
        const store = useAppStore();
        await store.checkBackendHealth();
        await store.checkBackendHealth();
        expect(store.backendStatus).toBe("online");
        expect(store.lastError).toBeNull();
    });

    it("suppresses an obvious duplicate check while one is pending", async () => {
        let resolveHealth: ((value: { status: string }) => void) | undefined;
        getHealthMock.mockReturnValue(new Promise((resolve) => { resolveHealth = resolve; }));
        const store = useAppStore();
        const first = store.checkBackendHealth();
        await store.checkBackendHealth();
        expect(getHealthMock).toHaveBeenCalledTimes(1);
        resolveHealth?.({ status: "UP" });
        await first;
        expect(store.backendStatus).toBe("online");
    });
});
