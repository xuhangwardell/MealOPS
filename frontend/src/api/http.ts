import { getFrontendEnvironment, joinUrl } from "@/config/env";
import { ApiNetworkError, ApiProblemError, normalizeProblemDetail } from "./problem";

export type HttpMethod = "GET" | "POST" | "PUT" | "DELETE";
type QueryPrimitive = string | number | boolean;
export type RequestBody = string | object | ArrayBuffer;
export type QueryValue = QueryPrimitive | readonly QueryPrimitive[] | undefined;

export interface RequestOptions<TBody extends RequestBody = RequestBody> {
    method: HttpMethod;
    path: string;
    query?: Readonly<Record<string, QueryValue>>;
    data?: TBody;
    headers?: Readonly<Record<string, string>>;
    timeout?: number;
}

export function buildQuery(query: RequestOptions["query"]): string {
    if (query === undefined) {
        return "";
    }
    const pairs: string[] = [];
    for (const [key, value] of Object.entries(query)) {
        if (value === undefined) {
            continue;
        }
        const values = Array.isArray(value) ? value : [value];
        for (const item of values) {
            pairs.push(`${encodeURIComponent(key)}=${encodeURIComponent(String(item))}`);
        }
    }
    return pairs.length === 0 ? "" : `?${pairs.join("&")}`;
}

function networkError(message: string): ApiNetworkError {
    const timeout = message.toLowerCase().includes("timeout");
    return new ApiNetworkError(timeout ? "请求超时，请稍后重试" : "无法连接服务，请检查网络后重试",
        timeout ? "timeout" : "network");
}

export function request<TResponse, TBody extends RequestBody = RequestBody>(options: RequestOptions<TBody>): Promise<TResponse> {
    const environment = getFrontendEnvironment();
    const url = `${joinUrl(environment.apiBaseUrl, options.path)}${buildQuery(options.query)}`;
    return new Promise<TResponse>((resolve, reject) => {
        uni.request({
            url,
            method: options.method,
            data: options.data,
            header: options.headers,
            timeout: options.timeout ?? environment.apiTimeoutMs,
            success(response) {
                if (response.statusCode >= 200 && response.statusCode < 300) {
                    resolve((response.statusCode === 204 ? undefined : response.data) as TResponse);
                    return;
                }
                reject(new ApiProblemError(normalizeProblemDetail(response.data, response.statusCode)));
            },
            fail(error) {
                reject(networkError(error.errMsg));
            }
        });
    });
}
