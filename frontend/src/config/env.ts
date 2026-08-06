const DEFAULT_TIMEOUT_MS = 10_000;

export interface FrontendEnvironment {
    apiBaseUrl: string;
    apiTimeoutMs: number;
}

export function normalizeBaseUrl(value: string | undefined): string {
    return (value ?? "").trim().replace(/\/+$/, "");
}

export function parseTimeout(value: string | undefined): number {
    if (value === undefined || value.trim() === "") {
        return DEFAULT_TIMEOUT_MS;
    }
    const timeout = Number(value);
    return Number.isInteger(timeout) && timeout > 0 ? timeout : DEFAULT_TIMEOUT_MS;
}

export function joinUrl(baseUrl: string, path: string): string {
    const normalizedBase = normalizeBaseUrl(baseUrl);
    const normalizedPath = `/${path.replace(/^\/+/, "")}`;
    return normalizedBase === "" ? normalizedPath : `${normalizedBase}${normalizedPath}`;
}

export function getFrontendEnvironment(): FrontendEnvironment {
    return {
        apiBaseUrl: normalizeBaseUrl(import.meta.env.VITE_API_BASE_URL),
        apiTimeoutMs: parseTimeout(import.meta.env.VITE_API_TIMEOUT_MS)
    };
}
