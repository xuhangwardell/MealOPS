# MealOps

MealOps 是面向独居人群的库存感知型多餐规划系统。它根据库存、保质期、烹饪时间、预算、饮食偏好、带饭需求和食材复用等约束，为用户生成未来 1～3 天的多个餐食计划候选，并在确认后形成库存与购物清单闭环。

MealOps 的核心问题不是“这一顿吃什么”，而是：

> 一个人未来几顿应该怎样安排，才能尽量少购买、少浪费、少重复做饭？

## 当前状态

当前处于节点 1：技术架构选型。仓库已经固定产品范围、协作规则和 V1 技术架构基线。

仓库目前仍刻意不包含 `backend/`、`frontend/`、数据库、容器、依赖或业务代码。工程初始化属于后续节点，不会自动开始。

## 当前架构基线

| 类别 | 选择 |
| --- | --- |
| 项目形态 | Monorepo、前后端分离、模块化单体 |
| 后端 | Java 21、Spring Boot 4.1.x、Spring MVC、Maven |
| 数据 | PostgreSQL 18、MyBatis-Plus Boot 4、核心显式 SQL、Flyway |
| API | REST/JSON、`/api/v1`、RFC 9457 Problem Details、springdoc 3.x |
| 测试 | JUnit 5、AssertJ、Spring Boot Test、Testcontainers PostgreSQL |
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
- [V1 系统架构](docs/architecture/system-architecture.md)
- [Codex 项目规则](AGENTS.md)

## 开发原则

- 确定性业务系统是事实来源，规划结果必须可测试、可复现、可解释。
- 使用模块化单体，按可验收的纵向业务节点迭代。
- 一次只实现当前节点，不提前引入未来功能或基础设施。
- V1 先完成移动端优先的 H5 闭环，不引入 LLM、Redis 或消息队列。
- 每个节点独立验收并形成一个边界清晰的 Git 提交。

## 后续顺序

后续总体顺序为：产品闭环 → 确定性后端 → 规划器 → H5 前端 → Agent → 工程增强。

具体技术选型、领域模型和工程初始化仍需在对应节点确认。本仓库当前没有可运行的应用或测试命令。
