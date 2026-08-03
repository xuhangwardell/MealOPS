# 0005：测试策略

## Status

- 状态：已接受
- 决策日期：2026-08-03
- 适用阶段：V1

### Amendment：节点 2 精确依赖修正

节点 1 将测试框架概括为 JUnit 5。节点 2 在创建可执行工程并核验 Spring Boot 4.1.0 BOM 后，确认其管理的测试基线已经是 JUnit Jupiter 6。因此 MealOps 跟随 Spring Boot dependency management 使用 JUnit Jupiter 6，不额外固定旧版 JUnit。该修正只更新精确工具版本，不改变纯单元测试、Spring 集成测试和真实 PostgreSQL 集成测试的分层策略。

## Background

MealOps 的主要风险不只来自普通 CRUD，还包括单位精度、份数缩放、库存跨批次分配、事务回滚、乐观锁、幂等、数据库约束、复杂 SQL 和多餐规划排序。测试策略必须把纯业务算法与框架、数据库行为分开验证。

项目的生产数据库是 PostgreSQL 18，因此数据库集成测试需要覆盖真实方言、类型、锁和约束，不能由兼容模式内存数据库代替。

## Decision

### 测试工具

- 使用 Spring Boot 4.1 管理的 JUnit Jupiter 6 作为测试框架；
- 使用 AssertJ 编写可读断言；
- 使用 Spring Boot Test 进行需要 Spring 容器的集成测试；
- 使用 Testcontainers 启动真实 PostgreSQL 18；
- 可使用 Spring Boot Testcontainers 的 Service Connection 集成简化测试连接；
- 不使用 H2 替代 PostgreSQL 进行核心数据库集成测试；
- Mockito 只用于隔离明确、昂贵或不稳定的外部依赖，不为每个类机械创建 mock。

### 测试分层

#### 纯单元测试

不启动 Spring，不连接数据库，覆盖：

- 单位换算和精度；
- 菜谱份数缩放；
- 库存批次选择与分配策略；
- 需求聚合；
- Planner 硬约束、评分、搜索和稳定排序；
- 状态机中可纯函数表达的转换规则。

#### Spring 切片或应用集成测试

只在需要验证 Spring 配置、校验、序列化、异常映射、安全过滤器或事务编排时启动必要上下文。是否使用具体测试切片按节点决定，不为了统一形式加载完整应用。

#### PostgreSQL 集成测试

使用 Testcontainers + PostgreSQL 18 覆盖：

- Flyway 从空库执行和 migration 校验；
- Mapper 映射与显式 SQL；
- `NUMERIC` 精度和时间类型；
- 唯一、外键、`CHECK` 和非空约束；
- 排序、聚合、部分索引相关查询；
- 事务提交与回滚；
- 乐观锁与并发更新；
- 幂等唯一性和重复请求；
- PostgreSQL 特有语法及执行行为。

### Mocking boundary

- 领域对象、值对象和被测业务规则使用真实对象；
- Repository 若是纯领域单元测试的端口，可以用小型 fake 或明确 stub；
- 数据库 Mapper 不通过 mock 证明 SQL 正确，必须使用 PostgreSQL；
- Controller 测试可以 mock 应用服务以隔离 HTTP 协议，但应用用例仍需单独测试；
- 后续第三方模型、OCR、对象存储或消息服务可以在明确边界处 mock；
- 禁止为了提高覆盖率而验证实现细节或机械 mock 类之间的每次调用。

## Reasons

- 纯单元测试反馈快，适合复杂确定性算法的边界和组合覆盖；
- Spring 集成测试验证真实装配、协议和事务，不让框架配置风险被忽略；
- Testcontainers 让本地与 CI 使用相同 PostgreSQL 主版本；
- AssertJ 能让集合、异常和 BigDecimal 断言更清晰；
- 明确 mock 边界可以避免脆弱、只复述实现的测试。

## Alternatives

| 方案 | 优势 | 局限 |
| --- | --- | --- |
| 只做纯单元测试 | 速度快、定位直接 | 无法验证 SQL、迁移、约束和 Spring 装配 |
| 所有测试都使用完整 Spring 上下文 | 接近运行环境 | 慢、定位困难，掩盖领域逻辑边界 |
| Testcontainers PostgreSQL | 方言和行为接近生产 | 需要容器运行时，启动较慢 |
| H2 内存数据库 | 速度快、配置简单 | 方言、锁、类型和约束与 PostgreSQL 不等价 |
| 大量 Mockito mock | 测试执行快、隔离彻底 | 易耦合实现细节，无法证明组件协作正确 |
| 共享外部测试数据库 | 不需每次启动容器 | 状态污染、并发冲突、环境不可复现 |

## Why alternatives were not chosen

### 只做纯单元测试

无法证明 Flyway、Mapper、数据库约束、事务与并发行为正确，不能覆盖项目最重要的持久化风险。

### 所有测试加载完整 Spring

会降低反馈速度，并把可以独立测试的 Planner、换算和分配规则绑到框架。测试范围应与风险匹配。

### H2

H2 即使启用兼容模式，也不能等价模拟 PostgreSQL 的所有类型、函数、排序、索引、锁和事务行为。核心数据库测试通过 H2 不代表生产可用。

### 大量 Mockito

机械 mock 会让重构造成大量无业务意义的测试破坏，并可能在所有 mock 都按预期返回时掩盖真实集成错误。

### 共享外部数据库

测试难以隔离和并行，状态清理容易出错，也无法保证开发者与 CI 使用相同版本。

## Trade-offs

- PostgreSQL 容器增加集成测试启动时间和 Docker 依赖；
- 本地没有可用容器运行时的开发者无法执行完整数据库测试；
- 需要维护测试数据构造器和稳定的并发测试方法；
- 测试层次多于单一 `@SpringBootTest`，开发者必须判断正确测试边界；
- 当前已经确定 `*Test.java` 由 Maven Surefire Plugin 在 `test` phase 运行，`*IT.java` 由 Maven Failsafe Plugin 在 `verify` phase 运行；
- CI 并行策略、覆盖率指标、测试反馈时长目标和更细的流水线分层策略仍待后续确定。

## Revisit Conditions

满足以下任一条件时重新评估：

- Testcontainers 在目标 CI 环境中持续不可用或成本不可接受；
- 集成测试时长超过已确定的反馈目标，需要容器复用、测试分组或流水线分层；
- 引入新的基础设施，需要对应真实服务容器或契约测试；
- 前后端契约需要独立发布或多客户端协作，需要增加契约测试；
- 出现覆盖率高但缺陷仍集中在未覆盖业务路径的情况，需要调整质量指标。

## References

- [Spring Boot Testcontainers Support](https://docs.spring.io/spring-boot/reference/testing/testcontainers.html)
- [PostgreSQL 18 Release Documentation](https://www.postgresql.org/docs/current/release.html)

