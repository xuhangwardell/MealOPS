# 0002：Java 与 Spring 技术栈

## Status

- 状态：已接受
- 决策日期：2026-08-03
- 适用阶段：V1

## Background

MealOps 是一个以事务型业务、确定性规划算法和显式数据访问为核心的新项目。后端需要支持库存一致性、数据库约束、幂等、并发控制、状态机、SQL、规划器计算、REST API、后续认证以及未来 Agent Tool Calling，但 V1 不需要大规模长连接或全链路响应式处理。

V1 的核心问题不是 ML 模型训练、GPU 推理、高性能科学计算，也不是大规模网络代理或百万连接系统。后端主语言应服务于当前真实业务复杂度，不能为了增加技术栈而人为拆出多语言边界。

节点 1 需要确定未来半年不轻易更换的 Java、Spring、Web 和构建基线，同时避免用构建边界替代尚未验证的业务边界。

截至 2026 年 8 月，Java 25 是最新 LTS，Java 21 是上一代 LTS，Java 26 是当前非 LTS Feature Release。Spring Boot 4.1.0 要求至少 Java 17，并支持至 Java 26；Spring AI 2.0.x 官方支持 Spring Boot 4.0.x 与 4.1.x。

## Decision

MealOps 后端采用以下基线：

- Java 是 V1 唯一主后端语言，不采用 Python、Go 或 Java + Python/Go 多语言架构；
- Java 21 LTS；
- Spring Boot 4.1.x，只在 4.1 系列内跟进经过验证的补丁版本；
- Spring MVC、Servlet 与 JDBC 同步模型，不采用 WebFlux 或 R2DBC；
- Maven；
- Monorepo 中只设置一个 `backend` Maven module；
- 后端采用 package-level modular monolith，按业务领域组织 package；
- 不在 V1 中拆分 Maven 多业务模块或微服务；
- Spring Validation 用于外部输入校验；
- Spring Security 作为后续认证实现方向，但本节点不设计令牌生命周期或添加依赖。

计划中的领域 package 包括 `user`、`ingredient`、`recipe`、`inventory`、`mealplan`、`shopping` 和 `planner`。模块内部职责分层由系统架构文档定义。

## Reasons

### Java 作为唯一主后端语言

- MealOps 的主要工程问题是事务型业务、库存一致性、数据库约束、幂等、并发控制、状态机、SQL 和确定性 Planner，Java 与 Spring 的企业应用生态直接覆盖这些问题；
- Spring Transaction、Spring Security、Spring Validation、MyBatis、Testcontainers 和后续 Spring AI 可以保持统一技术体系，减少跨语言协议、部署、可观测性和故障处理成本；
- Java 的类型系统、成熟并发工具和测试生态适合表达长期演进的业务状态与确定性规则；
- Agent Tool Calling 后续可以直接调用同一进程中的 Application Service，不需要为了 AI 入口复制一套业务逻辑或跨语言 RPC；
- MealOps 本身也是 Java 后端工程能力的展示项目，统一使用 Java 能让架构、事务、SQL、测试和 Agent 接入形成连贯的工程叙事。

### Java 21

- 已提供 record、sealed class、模式匹配和虚拟线程等本项目可能使用的现代能力；
- 属于长期支持版本，且处于 Spring Boot 4.1 的正式支持范围；
- 项目的差异化应来自库存一致性、SQL、幂等和 Planner，而不是仅追求最新语言版本；
- 相比 Java 17，为未来阻塞 I/O 场景保留虚拟线程等能力。

### Spring Boot 4.1.x

- 是 2026 年新项目可采用的当前稳定基线；
- 与 Java 21 兼容；
- Spring AI 2.0.x 已明确支持 Boot 4.0/4.1，不会阻碍后续 Agent 节点；
- 选择固定 minor line，避免为了版本号频繁进行破坏性升级。

### Spring MVC

- MealOps 的主要调用链是 HTTP → 应用服务 → 领域规则 → JDBC/PostgreSQL；
- 库存更新和购物清单等核心用例依赖清晰的同步事务边界；
- Spring MVC 可以在未来处理 SSE，不需要仅为 Agent 流式输出迁移全链路 Reactive。

### Maven 与单 backend module

- `pom.xml` 的依赖和插件变更便于人工审查；
- 当前规模不需要 Gradle 的构建性能与定制能力；
- 业务隔离首先通过 package、依赖方向和测试实现，而不是提前增加父 POM、模块依赖与构建顺序；
- Monorepo 仍能统一维护后端、前端、文档和基础设施版本。

## Alternatives

| 决策 | 当前选择 | 替代方案 | 替代方案优势 |
| --- | --- | --- | --- |
| 后端主语言 | Java | Python/FastAPI | AI/ML、OCR、模型推理和快速原型生态丰富 |
| 后端主语言 | Java | Go | 高并发网络服务、云原生工具和基础设施开发能力强 |
| 语言架构 | 仅 Java | Java + Python | 可以让 Python 独立承载 AI、模型或数据处理工作负载 |
| 语言架构 | 仅 Java | Java + Go | 可以让 Go 独立承载高吞吐网络或基础设施服务 |
| Java | Java 21 | Java 25 | 最新 LTS、支持周期更长 |
| Java | Java 21 | Java 17 | 生态成熟、运行环境普遍 |
| Spring Boot | 4.1.x | 3.5.x | 发布更久、第三方兼容经验更多 |
| Web | Spring MVC | WebFlux | 适合全链路非阻塞和大量长连接 |
| 构建 | Maven | Gradle Kotlin DSL | 构建模型灵活、增量构建能力强 |
| 后端构建结构 | 单 Maven module | Maven multi-module | 编译期模块依赖更明确，可独立产物化 |

## Why alternatives were not chosen

### Python/FastAPI

Python/FastAPI 适合 AI/ML、OCR、模型推理和快速原型，但 MealOps V1 的主体不是模型服务。把 Python 作为主后端不能显著改善库存事务、数据库约束、并发控制或确定性 Planner 等核心问题，反而会放弃当前希望统一使用的 Spring 事务、安全、校验和数据访问体系。

### Go

Go 适合高并发网络服务、云原生和基础设施工具，但 MealOps 当前的性能问题不在连接吞吐，也没有独立的高吞吐网络边界。为使用 Go 而拆服务会提前引入 RPC 契约、序列化、部署、跨服务事务、可观测性和故障恢复复杂度。

### Java + Python

当前没有必须依赖 Python 生态的独立 OCR、CV、模型推理或 ML Pipeline。语言边界必须由真实业务或计算边界产生，不能为了增加技术栈而建立。提前拆分只会让事务型业务状态跨进程传播，并增加接口和运维成本。

### Java + Go

当前没有需要独立扩缩的基础设施组件或高吞吐网络服务。使用 Java + Go 不会改善库存一致性或 Planner，反而会增加两套构建、部署、监控、调试和工程规范。

### Java 25

Java 25 本身是合理的 LTS，但 MealOps 没有必须依赖 Java 25 的核心需求。提高运行基线会缩小可用环境，而当前收益主要是版本新，不足以抵消依赖兼容验证和团队环境成本。

### Java 17

Java 17 可以满足基础需求，但会放弃 Java 21 已稳定提供的现代语言与运行时能力。新项目没有被旧运行环境约束，因此没有必要退回 17。

### Spring Boot 3.5.x

Boot 3.5 更成熟，但 MealOps 是 2026 年新项目，Boot 4.1 已有正式稳定版，关键候选依赖也提供 Boot 4 支持。留在 3.5 会让未来升级成为一项独立迁移工作。

### WebFlux

WebFlux 只有在数据库驱动、事务、调用链和团队心智模型均采用 Reactive 时才发挥完整价值。MealOps 选择 JDBC 和同步事务，混合 Reactor 与阻塞访问只会增加上下文传播、调试和事务复杂度。

### Gradle

项目规模不足以让 Gradle 的构建性能或定制能力成为决定因素。额外的 DSL、任务图和插件模型不是当前产品价值来源。

### Maven multi-module

当前需要的是业务边界，不是独立构建产物。过早拆分会增加依赖管理和构建顺序，却不能自动阻止错误的领域耦合。

## Trade-offs

- 统一使用 Java 意味着 V1 不直接获得 Python 在 AI/ML、OCR 和科学计算方面的库优势；
- 如果未来出现计算特征明显不同的 AI 工作负载，届时需要新增跨语言 API、部署与可观测性边界；
- 不使用 Go 意味着不会为尚未出现的高连接吞吐场景预先优化；
- 单语言架构降低当前复杂度，但要求后续引入新语言时必须用独立 ADR 证明收益大于分布式成本；
- Java 21 不是最新 LTS，需要显式管理发行版与升级计划；
- Spring Boot 4.1 较新，节点 2 创建工程时必须验证所有选定依赖的精确版本，而不能只依赖兼容性声明；
- Spring MVC 的请求线程模型不提供全链路 Reactive 的吞吐特性，但更符合当前同步事务系统；
- 单 Maven module 依赖 package 规则和架构测试维持边界，无法单靠构建系统阻止跨模块调用；
- Maven 配置相对冗长，但审查路径更直接。

### 已识别兼容性风险

Oracle 的路线图显示，Oracle JDK 21 在 2026 年 9 月后的更新许可将发生变化。MealOps 的 Java 21 决策不等于绑定 Oracle JDK；具体 OpenJDK 发行版属于“待确认”，必须在工程环境节点明确并记录其许可与更新策略。

Spring Boot 4.1、MyBatis-Plus Boot 4 starter 和 springdoc 3.x 虽有官方兼容声明，但组合后的最小工程构建、启动和测试仍需在节点 2 实际验证。

## Revisit Conditions

满足以下任一条件时重新评估：

- 出现明确独立的 OCR、CV、模型推理或 ML Pipeline，并且 Python 生态相对 Java 产生可测量的开发、模型兼容或运行收益；此时可以评估独立 Python AI Service，但不得把核心业务状态迁出 Java 系统；
- 出现真实独立的基础设施组件或经过测量的高吞吐网络服务需求，并且 Java 实现无法满足目标；此时可以评估 Go 服务；
- 新语言具有清晰的业务或计算边界、独立扩缩价值和可接受的 RPC、部署、数据一致性与运维成本；
- Java 21 的选定发行版停止提供满足项目要求的安全更新；
- 核心依赖均稳定支持 Java 25，且 Java 25 带来明确的项目收益；
- Spring Boot 4.1 停止维护或出现必须升级 minor line 才能修复的安全问题；
- 出现经过测量的大量长连接或全链路非阻塞需求，且 JDBC 已不适用；
- 单 backend module 的编译时间、所有权或复用需求形成真实瓶颈；
- 某业务模块需要独立部署或发布，而 package 边界无法满足。

## References

- [Oracle Java SE Support Roadmap](https://www.oracle.com/java/technologies/java-se-support-roadmap.html)
- [Spring Boot 4.1 System Requirements](https://docs.spring.io/spring-boot/system-requirements.html)
- [Spring AI Getting Started](https://docs.spring.io/spring-ai/reference/getting-started.html)
