import { describe, expect, it, vi } from "vitest";
import { request } from "@/api/http";
import { createIngredient, listIngredients, renameIngredient } from "@/api/ingredients";
import { createInventoryBatch, listInventoryBatches } from "@/api/inventory";
import { createRecipe, listRecipes } from "@/api/recipes";
import { getPlanningPreferences, replacePlanningPreferences } from "@/api/planning-preferences";

vi.mock("@/api/http", () => ({ request: vi.fn() }));
const requestMock = vi.mocked(request);

describe("catalog API modules", () => {
    it("maps ingredient list/create/rename endpoints", async () => {
        requestMock.mockResolvedValueOnce([]).mockResolvedValueOnce({ id: 1, name: "Tomato" }).mockResolvedValueOnce({ id: 1, name: "Cherry Tomato" });
        await listIngredients(); await createIngredient("Tomato"); await renameIngredient(1, "Cherry Tomato");
        expect(requestMock.mock.calls.map(([value]) => value)).toEqual([
            { method: "GET", path: "/api/v1/ingredients" },
            { method: "POST", path: "/api/v1/ingredients", data: { name: "Tomato" } },
            { method: "PUT", path: "/api/v1/ingredients/1", data: { name: "Cherry Tomato" } }
        ]);
    });
    it("maps inventory and recipe list/create contracts", async () => {
        requestMock.mockResolvedValue([]); await listInventoryBatches(); await createInventoryBatch({ ingredientId: 1, amount: "0.10", unit: "g", expiresOn: null }); await listRecipes(); await createRecipe({ name: "Rice", baseServings: 2, estimatedMinutes: 20, ingredients: [{ ingredientId: 1, amount: "100.00", unit: "g" }], steps: ["Cook"] });
        expect(requestMock.mock.calls.map(([value]) => value)).toEqual([
            { method: "GET", path: "/api/v1/inventory/batches", query: undefined },
            { method: "POST", path: "/api/v1/inventory/batches", data: { ingredientId: 1, amount: "0.10", unit: "g", expiresOn: null } },
            { method: "GET", path: "/api/v1/recipes" },
            { method: "POST", path: "/api/v1/recipes", data: { name: "Rice", baseServings: 2, estimatedMinutes: 20, ingredients: [{ ingredientId: 1, amount: "100.00", unit: "g" }], steps: ["Cook"] } }
        ]);
    });
    it("maps planning preference GET and full PUT", async () => {
        requestMock.mockResolvedValue({ defaultServings: 2, maxCookingMinutes: null, excludedIngredientIds: [3] });
        await getPlanningPreferences(); await replacePlanningPreferences({ defaultServings: 2, maxCookingMinutes: null, excludedIngredientIds: [3] });
        expect(requestMock.mock.calls[0]?.[0]).toEqual({ method: "GET", path: "/api/v1/planning-preferences" });
        expect(requestMock.mock.calls[1]?.[0]).toEqual({ method: "PUT", path: "/api/v1/planning-preferences", data: { defaultServings: 2, maxCookingMinutes: null, excludedIngredientIds: [3] } });
    });
});
