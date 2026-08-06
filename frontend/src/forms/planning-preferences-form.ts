import type { PlanningPreferences } from "@/api/planning-preferences";

export interface PlanningPreferencesFormState {
    defaultServings: string;
    unlimitedCookingMinutes: boolean;
    maxCookingMinutes: string;
    excludedIngredientIds: number[];
}

export function fromPlanningPreferences(value: PlanningPreferences): PlanningPreferencesFormState {
    return { defaultServings: String(value.defaultServings), unlimitedCookingMinutes: value.maxCookingMinutes === null,
        maxCookingMinutes: value.maxCookingMinutes === null ? "" : String(value.maxCookingMinutes), excludedIngredientIds: [...value.excludedIngredientIds] };
}

export function toPlanningPreferencesRequest(form: PlanningPreferencesFormState): PlanningPreferences {
    const defaultServings = Number(form.defaultServings);
    const maxCookingMinutes = form.unlimitedCookingMinutes ? null : Number(form.maxCookingMinutes);
    if (!Number.isInteger(defaultServings) || defaultServings <= 0 || (maxCookingMinutes !== null && (!Number.isInteger(maxCookingMinutes) || maxCookingMinutes <= 0))) {
        throw new Error("请填写有效的规划偏好");
    }
    return { defaultServings, maxCookingMinutes, excludedIngredientIds: [...new Set(form.excludedIngredientIds)].sort((a, b) => a - b) };
}
