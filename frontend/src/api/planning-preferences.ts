import { request } from "./http";

export interface PlanningPreferences {
    defaultServings: number;
    maxCookingMinutes: number | null;
    excludedIngredientIds: number[];
}

export function getPlanningPreferences(): Promise<PlanningPreferences> {
    return request<PlanningPreferences>({ method: "GET", path: "/api/v1/planning-preferences" });
}

export function replacePlanningPreferences(data: PlanningPreferences): Promise<PlanningPreferences> {
    return request<PlanningPreferences>({ method: "PUT", path: "/api/v1/planning-preferences", data });
}
