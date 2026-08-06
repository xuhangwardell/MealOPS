# MealOps

## Current Status

Node 18 Frontend Engineering Baseline implemented / under review.

Node 19 前端尚未开始。为解除前端 Contract Gate，当前正在完成 Backend Catalog Read APIs prerequisite：新增 Ingredient 与 Recipe 的只读 catalog endpoint。

Node 18 在 Monorepo 中建立了 `frontend/`：使用 uni-app、Vue 3、Vite、严格 TypeScript、Pinia 与封装后的 `uni.request`，优先支持 H5，并保持微信小程序编译兼容。当前唯一接入的真实前端 API 是后端健康检查；库存、菜谱与计划页面仍是无虚假数据的产品骨架。

Node 17 将已确认计划中的单个餐槽完成动作与库存消费放在同一数据库事务中：使用持久化的 Recipe selection 与 `targetServings` 计算食材需求，按确定顺序复用 FEFO、乐观 CAS 与 `CONSUME` 库存流水。重复完成同一餐槽是无副作用的成功响应；最后一个餐槽完成后 MealPlan 进入 `COMPLETED`。

已实现：

- persistent MealPlan 与显式 Meal Slots
- DRAFT 全量替换、确认与取消生命周期
- deterministic Recipe filtering、scoring、ranking 与多餐构建
- persisted MealPlan-derived Shopping Preview
- per-slot `PENDING` / `COMPLETED` execution status
- transactional meal-slot completion
- multi-ingredient FEFO inventory consumption 与不可缺失流水
- same-plan row locking 与重复完成幂等
- pending-slot-only Shopping Preview；COMPLETED 计划返回空预览

尚未实现：

- Inventory reservation 或 consume-on-confirm
- completion undo、reversal 或负向库存流水
- ADJUST、RECEIVE、DISCARD
- execution history table
- price、budget、purchase/order workflow
- food-safety expiry policy
- Agent / AI、Redis、MQ
- Inventory UI、Recipe UI、Planning / execution UI
- authentication 与微信发布
- Node 19

## Architecture Baseline

Java 21、Spring Boot 4.1.x、Spring MVC、Maven、PostgreSQL 18、Flyway、MyBatis-Plus Boot 4、REST/JSON、RFC 9457 Problem Details、JUnit Jupiter 6、AssertJ 与 Testcontainers PostgreSQL。

前端基线为 Node.js 24 LTS、npm、uni-app CLI、Vue 3、Vite、严格 TypeScript、Pinia、`uni.request`、Vitest 与 ESLint。

V1 不引入 Redis、消息队列、LLM 或 Agent。

## Product Loop

1. 维护标准食材和结构化菜谱；
2. 记录库存批次及保质期；
3. 输入未来 1～3 天的用餐需求；
4. 生成确定性多餐计划；
5. 聚合计划需求并预览购物缺口；
6. 用户确认计划并逐餐完成；
7. 完成餐槽时原子扣减库存并记录流水。

## Documentation

- [产品愿景](docs/product/product-vision.md)
- [MVP 范围](docs/product/mvp-scope.md)
- [核心用户流程](docs/product/core-user-flow.md)
- [系统架构](docs/architecture/system-architecture.md)
- [Meal Plan and Meal Slot Lifecycle](docs/decisions/0017-meal-plan-and-slot-lifecycle.md)
- [Deterministic Recipe Candidate Filtering](docs/decisions/0018-deterministic-recipe-candidate-filtering.md)
- [Deterministic Candidate Scoring and Ranking](docs/decisions/0019-deterministic-candidate-scoring-and-ranking.md)
- [Deterministic Multi-Meal Plan Construction](docs/decisions/0020-deterministic-multi-meal-plan-construction.md)
- [Plan-Derived Shopping Preview](docs/decisions/0021-plan-derived-shopping-preview.md)
- [Transactional Meal-Slot Completion](docs/decisions/0022-transactional-meal-slot-completion.md)
- [Frontend Engineering Baseline](docs/decisions/0023-frontend-engineering-baseline.md)
- [Backend Catalog Read APIs](docs/decisions/0024-backend-catalog-read-apis.md)
- [Planning Preferences Profile](docs/decisions/0016-planning-preferences-profile.md)
- [项目规则](AGENTS.md)

## Local Development

```powershell
docker compose -f infra/compose.yaml up -d
cd backend
.\mvnw.cmd test
.\mvnw.cmd verify
```

本地 PostgreSQL 默认绑定 `127.0.0.1:15432`，容器端口为 `5432`。

Catalog read APIs：

- `GET /api/v1/ingredients`
- `GET /api/v1/recipes`

两个接口返回 canonical catalog 的 `200` JSON array；空 catalog 返回 `[]`。它们与规划候选接口保持语义分离。Node 19 frontend 仍需等待本 prerequisite 完成并独立验收。

### Frontend

前端要求 Node.js 24 LTS 与 npm：

```powershell
cd frontend
npm ci
npm run dev:h5
npm run lint
npm run type-check
npm run test:run
npm run build:h5
npm run build:mp-weixin
npm run verify
```

本地联调时 Backend 运行于 `127.0.0.1:8080`，H5 运行于 `127.0.0.1:5173`。Vite 仅在开发期将 `/api` 与 `/actuator` 代理到 Backend，不修改后端 CORS，也不把本地地址固化进生产代码。
