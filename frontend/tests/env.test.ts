import { describe, expect, it } from "vitest";
import { joinUrl, normalizeBaseUrl, parseTimeout } from "@/config/env";
import { buildQuery } from "@/api/http";

describe("frontend environment and URL handling", () => {
    it("supports an empty API base URL", () => {
        expect(normalizeBaseUrl(undefined)).toBe("");
        expect(joinUrl("", "/actuator/health")).toBe("/actuator/health");
    });

    it("normalizes base and path slashes", () => {
        expect(joinUrl("https://mealops.example///", "//api/v1/items")).toBe(
            "https://mealops.example/api/v1/items"
        );
    });

    it("accepts a valid timeout", () => {
        expect(parseTimeout("2500")).toBe(2500);
    });

    it("falls back for invalid timeouts", () => {
        expect(parseTimeout("invalid")).toBe(10_000);
        expect(parseTimeout("0")).toBe(10_000);
    });

    it("encodes query keys, Chinese text, spaces, and repeated array values", () => {
        expect(buildQuery({ "食 材": "番茄 鸡蛋", tag: ["快手", "晚餐"] })).toBe(
            "?%E9%A3%9F%20%E6%9D%90=%E7%95%AA%E8%8C%84%20%E9%B8%A1%E8%9B%8B&tag=%E5%BF%AB%E6%89%8B&tag=%E6%99%9A%E9%A4%90"
        );
    });

    it("omits undefined query values", () => {
        expect(buildQuery({ present: 1, missing: undefined })).toBe("?present=1");
    });
});
