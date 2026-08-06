import { createPinia, setActivePinia } from "pinia";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { createIngredient, listIngredients, renameIngredient } from "@/api/ingredients";
import { useIngredientStore } from "@/stores/ingredients";

vi.mock("@/api/ingredients", () => ({ createIngredient: vi.fn(), listIngredients: vi.fn(), renameIngredient: vi.fn() }));
const listMock = vi.mocked(listIngredients); const createMock = vi.mocked(createIngredient); const renameMock = vi.mocked(renameIngredient);

describe("ingredient reference store", () => {
    beforeEach(() => { setActivePinia(createPinia()); vi.clearAllMocks(); });
    it("loads an empty catalog and suppresses concurrent duplicate loads", async () => {
        let resolve: ((value: []) => void) | undefined; listMock.mockReturnValue(new Promise((r) => { resolve = r; }));
        const store = useIngredientStore(); const first = store.load(); const second = store.load(); expect(listMock).toHaveBeenCalledTimes(1); resolve?.([]); await Promise.all([first, second]); expect(store.loaded).toBe(true); expect(store.items).toEqual([]);
    });
    it("refreshes from server after create and rename", async () => {
        listMock.mockResolvedValueOnce([{ id: 1, name: "Tomato" }]).mockResolvedValueOnce([{ id: 1, name: "Rice" }]).mockResolvedValueOnce([{ id: 1, name: "Cherry Tomato" }]); createMock.mockResolvedValue({ id: 1, name: "Rice" }); renameMock.mockResolvedValue({ id: 1, name: "Cherry Tomato" });
        const store = useIngredientStore(); await store.load(); await store.create("Rice"); expect(store.items[0]?.name).toBe("Rice"); await store.rename(1, "Cherry Tomato"); expect(store.items[0]?.name).toBe("Cherry Tomato");
    });
});
