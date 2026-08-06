import { request } from "./http";
import type { InventoryUnit } from "./inventory";

export interface RecipeIngredient {
    position: number;
    ingredientId: number;
    amount: string | number;
    unit: InventoryUnit;
}

export interface RecipeStep {
    position: number;
    instruction: string;
}

export interface Recipe {
    id: number;
    name: string;
    baseServings: number;
    estimatedMinutes: number;
    ingredients: RecipeIngredient[];
    steps: RecipeStep[];
}

export interface CreateRecipeIngredientRequest {
    ingredientId: number;
    amount: string;
    unit: InventoryUnit;
}

export interface CreateRecipeRequest {
    name: string;
    baseServings: number;
    estimatedMinutes: number;
    ingredients: CreateRecipeIngredientRequest[];
    steps: string[];
}

export function listRecipes(): Promise<Recipe[]> {
    return request<Recipe[]>({ method: "GET", path: "/api/v1/recipes" });
}

export function getRecipe(id: number): Promise<Recipe> {
    return request<Recipe>({ method: "GET", path: `/api/v1/recipes/${id}` });
}

export function createRecipe(data: CreateRecipeRequest): Promise<Recipe> {
    return request<Recipe>({ method: "POST", path: "/api/v1/recipes", data });
}
