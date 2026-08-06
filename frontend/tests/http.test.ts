import { beforeEach, describe, expect, it, vi } from "vitest";
import { request } from "@/api/http";
import { ApiNetworkError, ApiProblemError } from "@/api/problem";

type UniRequestOptions = Parameters<typeof uni.request>[0];
const requestMock = vi.fn<(options: UniRequestOptions) => UniApp.RequestTask>();

function requestTask(): UniApp.RequestTask {
    return {
        abort: vi.fn(),
        onHeadersReceived: vi.fn(),
        offHeadersReceived: vi.fn()
    } as unknown as UniApp.RequestTask;
}

function respond(statusCode: number, data: UniApp.RequestSuccessCallbackResult["data"]): void {
    requestMock.mockImplementationOnce((options) => {
        options.success?.({ statusCode, data, header: {}, cookies: [], errMsg: "request:ok" });
        return requestTask();
    });
}

function fail(errMsg: string): void {
    requestMock.mockImplementationOnce((options) => {
        options.fail?.({ errMsg });
        return requestTask();
    });
}

describe("typed HTTP client", () => {
    beforeEach(() => {
        requestMock.mockReset();
        vi.stubGlobal("uni", { request: requestMock });
    });

    it("returns typed JSON for HTTP 200", async () => {
        respond(200, { status: "UP" });
        await expect(request<{ status: string }>({ method: "GET", path: "/actuator/health" }))
            .resolves.toEqual({ status: "UP" });
    });

    it("accepts another 2xx response", async () => {
        respond(201, { id: 7 });
        await expect(request<{ id: number }>({ method: "POST", path: "/api/v1/items" }))
            .resolves.toEqual({ id: 7 });
    });

    it("returns undefined for HTTP 204", async () => {
        respond(204, "ignored");
        await expect(request<void>({ method: "DELETE", path: "/api/v1/items/7" })).resolves.toBeUndefined();
    });

    it.each([400, 404, 409])("throws ProblemDetail for HTTP %s", async (status) => {
        respond(status, { title: "Request failed", status, detail: "Readable detail", code: `CODE_${status}` });
        const promise = request({ method: "GET", path: "/api/v1/failure" });
        await expect(promise).rejects.toMatchObject({
            name: "ApiProblemError",
            httpStatus: status,
            businessCode: `CODE_${status}`,
            message: "Readable detail"
        } satisfies Partial<ApiProblemError>);
    });

    it("uses a safe ProblemDetail fallback for a non-Problem 500 body", async () => {
        respond(500, "Internal Server Error");
        await expect(request({ method: "GET", path: "/broken" })).rejects.toMatchObject({
            httpStatus: 500,
            message: "HTTP 500"
        });
    });

    it("maps network failures", async () => {
        fail("request:fail network unavailable");
        await expect(request({ method: "GET", path: "/health" })).rejects.toMatchObject({
            name: "ApiNetworkError",
            kind: "network"
        } satisfies Partial<ApiNetworkError>);
    });

    it("keeps timeout failures distinguishable", async () => {
        fail("request:fail timeout");
        await expect(request({ method: "GET", path: "/health" })).rejects.toMatchObject({
            name: "ApiNetworkError",
            kind: "timeout"
        } satisfies Partial<ApiNetworkError>);
    });

    it("does not automatically retry a failed request", async () => {
        fail("request:fail network unavailable");
        await expect(request({ method: "GET", path: "/health" })).rejects.toBeInstanceOf(ApiNetworkError);
        expect(requestMock).toHaveBeenCalledTimes(1);
    });
});
