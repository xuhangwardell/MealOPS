# MealOps

## Current Status

Node 19 Foundational Domain Frontend Slices implemented / under review.

当前真实前端链路为：Ingredient → Inventory → Recipe → Planning Preferences。
Backend Catalog Read APIs 已在 `11b0ca8` 提供 `GET /api/v1/ingredients` 与 `GET /api/v1/recipes`。

Node 19 前端只实现基础领域页面，不代表 V1 前端完整。Node 20 尚未开始，仍未实现 MealPlan generation、Shopping Preview、Confirm、Cancel 或 Slot completion UI。

## Architecture Baseline

后端使用 Java 21、Spring Boot 4.1、Spring MVC、PostgreSQL 18、Flyway、MyBatis-Plus Boot 4；前端使用 Node.js 24、uni-app CLI/Vite、Vue 3、TypeScript、Pinia、`uni.request`。不引入 Axios、Vue Router、UI library、OpenAPI codegen、Redis、MQ 或 Agent。

Frontend domain boundary：

```text
Inventory Page       ─┐
Recipe Page            ├─ Typed API modules → uni.request → Backend
Plans Page            ─┘
Ingredient catalog → Pinia shared reference state
Inventory / Recipe / Planning Preferences → page-local state
```

## Documentation

- [产品愿景](docs/product/product-vision.md)
- [MVP 范围](docs/product/mvp-scope.md)
- [系统架构](docs/architecture/system-architecture.md)
- [Frontend Engineering Baseline](docs/decisions/0023-frontend-engineering-baseline.md)
- [Backend Catalog Read APIs](docs/decisions/0024-backend-catalog-read-apis.md)
- [Foundational Domain Frontend Slices](docs/decisions/0025-foundational-domain-frontend-slices.md)

## Local Development

```powershell
cd frontend
npm ci
npm run dev:h5
npm run lint
npm run type-check
npm run test:run
npm run build:h5
npm run build:mp-weixin
```

H5 默认运行在 `127.0.0.1:5173`，Backend 默认运行在 `127.0.0.1:8080`。Vite 只在开发期代理 `/api` 与 `/actuator`。
