import { request } from "./http";

export interface HealthResponse {
    status: string;
    groups?: string[];
}

export function getHealth(): Promise<HealthResponse> {
    return request<HealthResponse>({ method: "GET", path: "/actuator/health" });
}
