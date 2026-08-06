import { request } from "./http";

export type InventoryUnit = "g" | "kg" | "ml" | "l" | "piece";

export interface InventoryBatch {
    id: number;
    ingredientId: number;
    amount: string | number;
    unit: InventoryUnit;
    expiresOn: string | null;
}

export interface CreateInventoryBatchRequest {
    ingredientId: number;
    amount: string;
    unit: InventoryUnit;
    expiresOn: string | null;
}

export function listInventoryBatches(ingredientId?: number): Promise<InventoryBatch[]> {
    return request<InventoryBatch[]>({
        method: "GET",
        path: "/api/v1/inventory/batches",
        query: ingredientId === undefined ? undefined : { ingredientId }
    });
}

export function getInventoryBatch(id: number): Promise<InventoryBatch> {
    return request<InventoryBatch>({ method: "GET", path: `/api/v1/inventory/batches/${id}` });
}

export function createInventoryBatch(data: CreateInventoryBatchRequest): Promise<InventoryBatch> {
    return request<InventoryBatch>({ method: "POST", path: "/api/v1/inventory/batches", data });
}
