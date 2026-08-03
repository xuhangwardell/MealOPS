# 0003：持久层与数据库

## Status

- 状态：已接受
- 决策日期：2026-08-03
- 适用阶段：V1

## Background

MealOps 需要维护标准食材、结构化菜谱、库存批次、库存流水、餐食计划和购物清单。其数据特点包括精确数量、强业务约束、跨批次分配、复杂排序、需求聚合、事务、乐观锁和幂等。

持久层既要减少无价值的简单 CRUD 样板代码，也必须让临期库存、FEFO、聚合与 Planner 候选召回等核心 SQL 可见、可审查、可解释执行计划。

## Decision

### 数据库

- 使用 PostgreSQL 18；
- 开发与核心集成测试使用 PostgreSQL 18 同一主版本；
- 业务数量、金额和换算系数在 Java 中使用 `BigDecimal`，在 PostgreSQL 中使用 `NUMERIC`；
- 重要不变量同时由应用逻辑和数据库约束保护；
- 生产 schema 不允许手工修改。

### 数据库迁移

- 所有 schema 变更使用 Flyway 版本化迁移；
- 迁移与使用它的纵向业务节点在同一次变更中交付；
- 不提前创建未来节点的表或字段；
- 迁移应可在空数据库执行，并由集成测试验证。

### 持久层

- 使用 MyBatis-Plus 3.5.x 的 Spring Boot 4 starter；
- 节点 2 选择当时最新且验证通过的 3.5.x patch，不使用动态版本；
- MyBatis-Plus 仅用于减少简单按主键访问、简单插入更新和基础分页的样板代码；
- 禁止使用 `IService`、`ServiceImpl` 作为业务 Service 架构；
- 禁止 ActiveRecord；
- 领域或应用服务接口不得暴露 MyBatis-Plus Wrapper、分页对象或持久化实体；
- 临期库存、FEFO 排序、聚合、多表查询、并发更新和 Planner Candidate 查询必须使用可审查的显式 SQL；
- 复杂 SQL 必须有对应查询场景、索引依据和集成测试。

## Reasons

### PostgreSQL 18

- 适合依赖 `CHECK`、唯一约束、外键、`NUMERIC`、事务、复杂排序、聚合和部分索引的数据一致性系统；
- PostgreSQL 18 是当前稳定主版本，18.4 是 2026 年 8 月的稳定维护版本；
- PostgreSQL 19 仍处于 Beta，不适合作为活动开发项目基线；
- 对后续复杂候选查询和查询计划分析保留足够能力。

### MyBatis-Plus 与显式 SQL 的组合

- 简单 CRUD 不值得重复手写；
- 核心查询必须能直接看到 SQL、参数、排序与索引关系；
- 通过业务 Repository 或 Mapper 边界隔离框架类型，避免 MyBatis-Plus API 渗透领域层；
- MyBatis-Plus 官方从 3.5.13 起提供 Spring Boot 4 starter，当前 3.5.x 具备可用基线。

### Flyway

- Schema 是版本化代码的一部分；
- 迁移文件能让开发、测试和部署环境按同一路径演进；
- 顺序 SQL 迁移与当前纵向节点的审查方式匹配；
- Flyway 官方文档已将 PostgreSQL 18 列为验证版本。

## Alternatives

| 决策 | 当前选择 | 替代方案 | 替代方案优势 |
| --- | --- | --- | --- |
| 数据库 | PostgreSQL 18 | MySQL | Java 生态普及、运维经验广泛 |
| 持久层 | MyBatis-Plus + 显式 SQL | 纯 MyBatis | 控制力完整、框架魔法更少 |
| 持久层 | MyBatis-Plus + 显式 SQL | JPA/Hibernate | 聚合持久化和普通 CRUD 开发效率高 |
| 迁移 | Flyway | Liquibase | 支持 XML/YAML/JSON changelog 和更丰富抽象 |
| 迁移 | Flyway | 手工 schema | 初始操作直接、无需工具 |
| 数据库集成测试 | Testcontainers PostgreSQL | H2 | 启动快、无需 Docker |

## Why alternatives were not chosen

### MySQL

MySQL 完全可以实现 MealOps，但当前项目更希望突出数据库约束、复杂排序、聚合、部分索引和 SQL 分析。PostgreSQL 与这些数据特征更吻合。该选择不是对 MySQL 能力的普遍否定。

### 纯 MyBatis

纯 MyBatis 提供最高控制力，但会让按主键查询、简单插入更新和分页产生较多无业务价值的重复代码。MealOps 只需要在真正重要的查询上承担显式 SQL 成本。

### JPA/Hibernate

JPA/Hibernate 对普通 CRUD 很高效，但 MealOps 的教育与工程重点是明确掌握发出的 SQL、索引和执行计划。库存批次排序、需求聚合与候选召回使用 SQL-first 模型更直接，也能避免意外加载和 N+1 等隐性行为。

### Liquibase

Liquibase 的数据库无关 changelog 和变更类型更丰富，但 MealOps 已确定 PostgreSQL 单一数据库，按版本执行显式 SQL 更便于审查。当前不需要额外的抽象格式。

### 手工 schema

手工修改无法可靠重放、审查或与应用版本绑定，会造成环境漂移，因此禁止。

### H2

H2 的 SQL 方言、类型、锁、时间处理、索引和约束行为与 PostgreSQL 不完全一致。它不能证明生产 SQL 和并发行为正确，因此不得替代核心 PostgreSQL 集成测试。

## Trade-offs

- PostgreSQL 相比用户可能更熟悉的 MySQL 增加了一定学习成本；
- Testcontainers 依赖可用的容器运行时，集成测试启动比内存数据库慢；
- MyBatis-Plus 与显式 SQL 混用需要清晰规则，否则可能出现风格不一致；
- 显式 SQL 要求团队主动维护列映射、查询测试和索引说明；
- Flyway 迁移一旦进入共享环境不得随意重写，需要用新迁移修正；
- PostgreSQL 18 的容器镜像 patch 版本与升级策略尚待工程节点明确。

## Revisit Conditions

满足以下任一条件时重新评估：

- 目标部署环境无法可靠运行或托管 PostgreSQL 18；
- 数据访问以简单聚合持久化为主且显式 SQL 不再带来可审查性收益；
- MyBatis-Plus Boot 4 starter 出现无法规避的兼容或维护问题；
- 需要支持多个数据库厂商，显式 PostgreSQL SQL 成为主要阻碍；
- 迁移规模、回滚或跨数据库需求使 Flyway 无法满足；
- 容器测试在 CI 中不可用，且存在能够保持 PostgreSQL 等价性的替代测试环境。

## References

- [PostgreSQL 18.4 Release Notes](https://www.postgresql.org/docs/release/18.4/)
- [PostgreSQL Beta Information](https://www.postgresql.org/developer/beta/)
- [MyBatis-Plus Installation](https://baomidou.com/getting-started/install/)
- [Flyway PostgreSQL Support](https://documentation.red-gate.com/flyway/reference/database-driver-reference/postgresql-database)
