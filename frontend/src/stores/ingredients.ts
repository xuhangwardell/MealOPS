import { defineStore } from "pinia";
import { createIngredient, listIngredients, renameIngredient, type Ingredient } from "@/api/ingredients";

function messageOf(error: unknown): string {
    return error instanceof Error ? error.message : "无法加载食材目录";
}

export const useIngredientStore = defineStore("ingredients", {
    state: () => ({
        items: [] as Ingredient[],
        loading: false,
        loaded: false,
        error: null as string | null,
        loadPromise: null as Promise<void> | null
    }),
    actions: {
        async load(): Promise<void> {
            if (this.loadPromise !== null) return this.loadPromise;
            this.loading = true;
            this.error = null;
            this.loadPromise = listIngredients().then((items) => {
                this.items = items;
                this.loaded = true;
            }).catch((error: unknown) => {
                this.error = messageOf(error);
                throw error;
            }).finally(() => {
                this.loading = false;
                this.loadPromise = null;
            });
            return this.loadPromise;
        },
        async refresh(): Promise<void> { this.loaded = false; await this.load(); },
        async create(name: string): Promise<Ingredient> {
            const result = await createIngredient(name);
            await this.refresh();
            return result;
        },
        async rename(id: number, name: string): Promise<Ingredient> {
            const result = await renameIngredient(id, name);
            await this.refresh();
            return result;
        }
    }
});
