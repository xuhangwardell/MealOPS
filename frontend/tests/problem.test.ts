import { describe, expect, it } from "vitest";
import { ApiProblemError, isProblemDetail, normalizeProblemDetail } from "@/api/problem";

describe("ProblemDetail handling", () => {
    it("recognizes the backend ProblemDetail shape", () => {
        expect(isProblemDetail({ title: "Not found", status: 404, code: "INGREDIENT_NOT_FOUND" })).toBe(true);
    });

    it("normalizes validation ProblemDetail and preserves its business code", () => {
        const problem = normalizeProblemDetail({
            title: "Validation failed",
            status: 400,
            detail: "Request validation failed",
            instance: "/api/v1/ingredients",
            code: "VALIDATION_FAILED"
        }, 400);
        const error = new ApiProblemError(problem);
        expect(error.businessCode).toBe("VALIDATION_FAILED");
        expect(error.message).toBe("Request validation failed");
    });

    it("uses a safe fallback for malformed responses", () => {
        expect(normalizeProblemDetail("server exploded", 500)).toEqual({
            title: "HTTP 500",
            status: 500,
            detail: undefined,
            code: undefined
        });
    });

    it("prefers detail over title for display messages", () => {
        expect(new ApiProblemError({ title: "Conflict", status: 409, detail: "库存不足" }).message)
            .toBe("库存不足");
    });

    it("falls back to title when detail is absent", () => {
        expect(new ApiProblemError({ title: "Not found", status: 404 }).message).toBe("Not found");
    });
});
