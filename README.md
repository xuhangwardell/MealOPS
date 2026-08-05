# MealOps

## Current Status

Node 16 Plan-Derived Shopping Preview implemented / under review.

Node 15 生成并持久化完整但尚未确认的 DRAFT MealPlan。Node 16 读取该持久化计划中的 Recipe selections 和各自 stored `targetServings`，先聚合整份计划的食材需求，再与请求时的 current accounting Inventory 比较，生成 live、derived、read-only 的 Shopping Preview。Planning Preferences 后续变化不会改写既有计划的购物需求。

已实现：

- persistent MealPlan
- explicit Meal Slots
- manual Recipe assignment
- DRAFT full replacement
- confirm / cancel lifecycle
- pure hard-constraint Recipe filtering
- recipeId ASC deterministic candidate representation
- accounting inventory coverage scorecard
- coverage DESC / shortage count ASC / cooking time ASC / recipeId ASC ranking
- deterministic greedy multi-meal construction
- rolling in-memory accounting inventory simulation
- complete DRAFT MealPlan persistence and explicit confirm lifecycle
- persisted MealPlan-derived whole-plan requirement aggregation
- live current-inventory shopping shortage preview
- complete DRAFT and CONFIRMED preview support

尚未实现：

- opaque `totalScore` 或 arbitrary weights（明确不使用）
- food-safety coverage 或 expiry penalty
- global optimization、beam search 或 backtracking
- diversity/repetition penalty 与 MealType suitability
- Shopping persistence、purchase/order workflow 或 package/price optimization
- Inventory reservation/consumption
- food-safety expiry policy
- COMPLETED workflow
- Node 17

Node 11 Planning Preferences 是历史节点，不代表当前状态。

## Architecture Baseline

Java 21、Spring Boot 4.1.x、Spring MVC、Maven、PostgreSQL 18、Flyway、MyBatis-Plus Boot 4、REST/JSON、RFC 9457 Problem Details、JUnit Jupiter 6、AssertJ 和 Testcontainers PostgreSQL。

V1 不引入 Redis、消息队列、LLM 或 Agent。

## Product Loop

1. 维护标准食材和结构化菜谱；
2. 记录库存批次及保质期；
3. 输入未来 1～3 天的用餐需求；
4. 生成餐食计划候选；
5. 聚合食材需求并抵扣库存；
6. 生成购物清单；
7. 确认做饭结果并更新状态。

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
