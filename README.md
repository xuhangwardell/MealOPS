# MealOps

## Current Status

Node 17 Transactional Meal-Slot Completion & Inventory Consumption implemented / under review.

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
- Node 18

## Architecture Baseline

Java 21、Spring Boot 4.1.x、Spring MVC、Maven、PostgreSQL 18、Flyway、MyBatis-Plus Boot 4、REST/JSON、RFC 9457 Problem Details、JUnit Jupiter 6、AssertJ 与 Testcontainers PostgreSQL。

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
