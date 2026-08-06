import type { CreateRecipeRequest } from "@/api/recipes";
import type { InventoryUnit } from "@/api/inventory";

export interface RecipeIngredientForm { ingredientId: number | null; amount: string; unit: InventoryUnit; }
export interface RecipeFormState {
    name: string;
    baseServings: string;
    estimatedMinutes: string;
    ingredients: RecipeIngredientForm[];
    steps: string[];
}

function isPositiveDecimal(value: string): boolean {
    return /^(?:0*\d+)(?:\.\d+)?$/.test(value) && /[1-9]/.test(value);
}

export function toRecipeRequest(form: RecipeFormState): CreateRecipeRequest {
    const baseServings = Number(form.baseServings);
    const estimatedMinutes = Number(form.estimatedMinutes);
    if (!form.name.trim() || !Number.isInteger(baseServings) || baseServings <= 0 || !Number.isInteger(estimatedMinutes) || estimatedMinutes < 0) {
        throw new Error("请完整填写菜谱基础信息");
    }
    if (form.ingredients.length === 0 || form.ingredients.some((row) => row.ingredientId === null || !isPositiveDecimal(row.amount))) {
        throw new Error("请完整填写食材用量");
    }
    if (form.steps.length === 0 || form.steps.some((step) => !step.trim())) throw new Error("请至少填写一个烹饪步骤");
    return {
        name: form.name.trim(), baseServings, estimatedMinutes,
        ingredients: form.ingredients.map((row) => ({ ingredientId: row.ingredientId as number, amount: row.amount, unit: row.unit })),
        steps: form.steps.map((step) => step.trim())
    };
}
