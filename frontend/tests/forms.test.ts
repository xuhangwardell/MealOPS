import { describe, expect, it } from "vitest";
import { toInventoryRequest } from "@/forms/inventory-form";
import { toRecipeRequest } from "@/forms/recipe-form";
import { fromPlanningPreferences, toPlanningPreferencesRequest } from "@/forms/planning-preferences-form";

describe("frontend request mappings", () => {
    it("preserves decimal text and nullable expiry", () => {
        expect(toInventoryRequest({ ingredientId: 3, amount: "0.10", unit: "g", expiresOn: "" })).toEqual({ ingredientId: 3, amount: "0.10", unit: "g", expiresOn: null });
        expect(() => toInventoryRequest({ ingredientId: 3, amount: "0", unit: "g", expiresOn: "" })).toThrow();
    });
    it("keeps recipe ingredient decimal and step order", () => {
        expect(toRecipeRequest({ name: "Rice", baseServings: "2", estimatedMinutes: "20", ingredients: [{ ingredientId: 3, amount: "100.00", unit: "g" }], steps: ["First", "Second"] })).toEqual({ name: "Rice", baseServings: 2, estimatedMinutes: 20, ingredients: [{ ingredientId: 3, amount: "100.00", unit: "g" }], steps: ["First", "Second"] });
    });
    it("round-trips unlimited cooking time as null and sorts exclusions", () => {
        const state = fromPlanningPreferences({ defaultServings: 1, maxCookingMinutes: null, excludedIngredientIds: [8, 3] });
        expect(state.unlimitedCookingMinutes).toBe(true);
        expect(toPlanningPreferencesRequest({ ...state, excludedIngredientIds: [8, 3] })).toEqual({ defaultServings: 1, maxCookingMinutes: null, excludedIngredientIds: [3, 8] });
    });
});
