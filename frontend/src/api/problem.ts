export interface ProblemDetail {
    type?: string;
    title: string;
    status: number;
    detail?: string;
    instance?: string;
    code?: string;
}

function isRecord(value: unknown): value is Record<string, unknown> {
    return typeof value === "object" && value !== null && !Array.isArray(value);
}

function optionalString(value: unknown): string | undefined {
    return typeof value === "string" && value.trim() !== "" ? value : undefined;
}

export function isProblemDetail(value: unknown): value is ProblemDetail {
    return isRecord(value) && typeof value.title === "string" && typeof value.status === "number";
}

export function normalizeProblemDetail(value: unknown, httpStatus: number): ProblemDetail {
    if (isProblemDetail(value)) {
        return {
            type: optionalString(value.type),
            title: value.title,
            status: value.status,
            detail: optionalString(value.detail),
            instance: optionalString(value.instance),
            code: optionalString(value.code)
        };
    }
    const source = isRecord(value) ? value : {};
    return {
        title: optionalString(source.title) ?? `HTTP ${httpStatus}`,
        status: httpStatus,
        detail: optionalString(source.detail),
        code: optionalString(source.code)
    };
}

export class ApiProblemError extends Error {
    readonly httpStatus: number;
    readonly businessCode?: string;
    readonly title: string;
    readonly detail?: string;
    readonly problem: ProblemDetail;

    constructor(problem: ProblemDetail) {
        super(problem.detail ?? problem.title ?? `HTTP ${problem.status}`);
        this.name = "ApiProblemError";
        this.httpStatus = problem.status;
        this.businessCode = problem.code;
        this.title = problem.title;
        this.detail = problem.detail;
        this.problem = problem;
    }
}

export class ApiNetworkError extends Error {
    readonly kind: "network" | "timeout";

    constructor(message: string, kind: "network" | "timeout") {
        super(message);
        this.name = "ApiNetworkError";
        this.kind = kind;
    }
}
