# MealOps

## Current Status

Node 11 Planning Preferences Profile implemented / under review。

新增能力：`Ingredient -> InventoryBatch -> canonical Quantity`。批次保留可为空的 `expiresOn`，并按到期日优先、无到期日置后的确定性顺序返回。

MealOps 是面向独居人群的库存感知型多餐规划系统。它根据库存、保质期、烹饪时间、预算、饮食偏好、带饭需求和食材复用等约束，为用户生成未来 1～3 天的多个餐食计划候选，并在确认后形成库存与购物清单闭环。

MealOps 的核心问题不是“这一顿吃什么”，而是：

> 一个人未来几顿应该怎样安排，才能尽量少购买、少浪费、少重复做饭？

## 当前状态

Node 10 Shopping List Preview / Inventory Shortage Calculation 已完成。预览只读计算库存覆盖和正缺口，不修改库存、不写流水、不持久化购物清单。

Node 11 Planning Preferences Profile 已实现并进入审查。当前只持久化和读取规划偏好，不将偏好应用于 Recipe 筛选或 Planner。

当前能力链为 Ingredient Requirement + accounting-available Inventory → Shopping List Preview；Planning Preferences → Future Planner。MealPlan、MealSlot、Planner、preference application 和 food-safety usability policy 尚未实现。

## 当前架构基线

| 类别 | 选择 |
| --- | --- |
| 项目形态 | Monorepo、前后端分离、模块化单体 |
| 后端 | Java 21、Spring Boot 4.1.x、Spring MVC、Maven |
| 数据 | PostgreSQL 18、MyBatis-Plus Boot 4、核心显式 SQL、Flyway |
| API | REST/JSON、`/api/v1`、RFC 9457 Problem Details、springdoc 3.x |
| 测试 | JUnit Jupiter 6、AssertJ、Spring Boot Test、Testcontainers PostgreSQL |
| 前端 | uni-app、Vue 3、TypeScript、Pinia、封装 `uni.request` |
| 运行目标 | 先 H5，后微信小程序 |
| V1 不引入 | Redis、消息队列、LLM、Agent |

## 已确定的产品闭环

1. 维护标准食材和结构化菜谱；
2. 记录冰箱库存批次及保质期；
3. 输入未来 1～3 天的用餐需求；
4. 根据约束生成多个计划候选；
5. 聚合计划的食材需求；
6. 使用现有有效库存抵扣；
7. 生成缺失食材购物清单；
8. 用户确认做饭结果；
9. 更新库存和计划状态。

## 文档导航

- [产品愿景](docs/product/product-vision.md)
- [MVP 范围](docs/product/mvp-scope.md)
- [核心用户流程](docs/product/core-user-flow.md)
- [开发策略决策](docs/decisions/0001-development-strategy.md)
- [Java 与 Spring 技术栈决策](docs/decisions/0002-java-and-spring-stack.md)
- [持久层与数据库决策](docs/decisions/0003-persistence-and-database.md)
- [API 设计决策](docs/decisions/0004-api-design.md)
- [测试策略决策](docs/decisions/0005-testing-strategy.md)
- [前端策略决策](docs/decisions/0006-frontend-strategy.md)
- [工程版本基线](docs/decisions/0007-engineering-baseline.md)
- [标准食材身份与名称规范化](docs/decisions/0008-ingredient-identity-and-normalization.md)
- [Quantity 与 Unit Domain Foundation](docs/decisions/0009-quantity-unit-foundation.md)
- [Structured Recipe Aggregate](docs/decisions/0010-structured-recipe-aggregate.md)
- [Recipe Scaling](docs/decisions/0011-recipe-scaling.md)
- [Inventory Batch Foundation](docs/decisions/0012-inventory-batch-foundation.md)
- [Inventory Consumption and Concurrency](docs/decisions/0013-inventory-consumption-and-concurrency.md)
- [Ingredient Requirement Aggregation](docs/decisions/0014-ingredient-requirement-aggregation.md)
- [Shopping List Preview and Inventory Shortage](docs/decisions/0015-shopping-list-preview-and-inventory-shortage.md)
- [Planning Preferences Profile](docs/decisions/0016-planning-preferences-profile.md)
- [V1 系统架构](docs/architecture/system-architecture.md)
- [Codex 项目规则](AGENTS.md)

## 本地开发

前置条件：Java 21 和可用的 Docker Engine。

MealOps 本地 PostgreSQL 默认仅绑定 `127.0.0.1:15432`，映射到容器内部标准端口 `5432`，用于避开本机 PostgreSQL `5432` 冲突和 Windows 保留端口并限制数据库只供本机访问。需要其他 host port 时可通过 `MEALOPS_DB_PORT` 覆盖；默认启动不需要设置该环境变量，local profile 也会直接连接 `15432`。

从仓库根目录启动 PostgreSQL：

```powershell
docker compose -f infra/compose.yaml up -d
```

进入 `backend` 后执行测试：

```powershell
.\mvnw.cmd test
.\mvnw.cmd verify
```

显式启用 local profile 启动后端：

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

启动后可检查：

- `GET http://localhost:8080/actuator/health`
- `GET http://localhost:8080/v3/api-docs`

## 开发原则

- 确定性业务系统是事实来源，规划结果必须可测试、可复现、可解释。
- 使用模块化单体，按可验收的纵向业务节点迭代。
- 一次只实现当前节点，不提前引入未来功能或基础设施。
- V1 先完成移动端优先的 H5 闭环，不引入 LLM、Redis 或消息队列。
- 每个节点独立验收并形成一个边界清晰的 Git 提交。

## 后续顺序

后续总体顺序为：产品闭环 → 确定性后端 → 规划器 → H5 前端 → Agent → 工程增强。

具体领域模型和业务能力仍需在后续对应节点确认；节点 2 不创建业务表、业务 API 或前端实现。
