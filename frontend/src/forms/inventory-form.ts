import type { CreateInventoryBatchRequest, InventoryUnit } from "@/api/inventory";

export interface InventoryFormState {
    ingredientId: number | null;
    amount: string;
    unit: InventoryUnit;
    expiresOn: string;
}

function isPositiveDecimal(value: string): boolean {
    return /^(?:0*\d+)(?:\.\d+)?$/.test(value) && /[1-9]/.test(value);
}

export function toInventoryRequest(form: InventoryFormState): CreateInventoryBatchRequest {
    if (form.ingredientId === null) throw new Error("请选择食材");
    if (!isPositiveDecimal(form.amount)) throw new Error("请输入正数数量");
    return { ingredientId: form.ingredientId, amount: form.amount, unit: form.unit, expiresOn: form.expiresOn.trim() || null };
}
