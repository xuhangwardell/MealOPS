import { request } from "./http";

export interface Ingredient {
    id: number;
    name: string;
}

export interface IngredientRequest {
    name: string;
}

export function listIngredients(): Promise<Ingredient[]> {
    return request<Ingredient[]>({ method: "GET", path: "/api/v1/ingredients" });
}

export function createIngredient(name: string): Promise<Ingredient> {
    return request<Ingredient>({ method: "POST", path: "/api/v1/ingredients", data: { name } });
}

export function getIngredient(id: number): Promise<Ingredient> {
    return request<Ingredient>({ method: "GET", path: `/api/v1/ingredients/${id}` });
}

export function renameIngredient(id: number, name: string): Promise<Ingredient> {
    return request<Ingredient>({ method: "PUT", path: `/api/v1/ingredients/${id}`, data: { name } });
}
