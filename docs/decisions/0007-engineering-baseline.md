# 0007：工程版本基线

## Status

- 状态：已接受
- 决策日期：2026-08-03
- 适用阶段：节点 2 及后续 V1 工程

## Background

节点 1 已确定 Java 21、Spring Boot 4.1.x、PostgreSQL 18 和模块化单体方向。节点 2 首次创建可执行工程，需要把可复现构建、依赖管理、本地数据库和集成测试的精确版本固化下来，同时避免把开发机器的 JDK 发行版或某个安全补丁误当成项目接口。

当前实际开发环境为 Oracle JDK 21.0.9，满足 Java 21 major build contract。Eclipse Temurin 21 仍作为参考 OpenJDK 发行版，但项目不绑定 Oracle JDK 或 Temurin，也不固定某个 21.0.x 安全补丁。外部工具曾尝试将工程目标从 Java 21 自动升级为 Java 25；该结果与 ADR 0002 已接受的 Java 21 决策冲突，因此不予采纳。

## Decision

- Java build contract 为 Java 21；当前实际开发 JDK 为 Oracle JDK 21.0.9，参考发行版为 Eclipse Temurin 21；
- 不安装或切换 JDK，不把 JDK 25 用于 MealOps 构建；
- Maven Wrapper 固定使用 Apache Maven 3.9.16，工程命令优先通过 Wrapper 执行；
- Maven parent 固定为 Spring Boot 4.1.0；
- PostgreSQL JDBC、Flyway、Testcontainers、JUnit Jupiter、AssertJ、Spring Framework、Jackson、Tomcat 和 HikariCP 的版本由 Spring Boot 4.1.0 dependency management 管理，不在 POM 中重复指定；
- MyBatis-Plus Boot 4 starter 固定为 3.5.17；
- springdoc-openapi WebMVC starter 固定为 3.0.3；
- 本地数据库和集成测试均使用 `postgres:18.4`；本地服务由 Docker Compose 管理，测试数据库由 Testcontainers 管理；
- 测试框架使用 Spring Boot BOM 管理的 JUnit Jupiter 6，不使用 H2；
- 节点 2 只引入可启动 Web/JDBC/Flyway/Actuator/OpenAPI 基线及真实 PostgreSQL 集成测试所必需的依赖，不加入 Security、Redis、消息队列、Spring AI、Lombok、MapStruct 或 DevTools。

### Local PostgreSQL Port Decision

- PostgreSQL 容器继续监听标准内部端口 5432；
- MealOps 本地开发默认将宿主机 IPv4 loopback `127.0.0.1:15432` 映射到容器 5432，`application-local.yml` 同样默认连接宿主机 15432；
- 数据库只服务本机开发进程，因此 Compose 仅绑定 `127.0.0.1` 以最小化网络暴露，不监听所有 host interface；
- 开发者可通过 `MEALOPS_DB_PORT` 同时覆盖 Compose host port 和 local datasource port；该变量只覆盖 host port 数字，不改变 loopback 地址或容器内部端口；
- 原计划使用宿主机默认端口 5432。节点 2 实际验收确认 Windows 本机 PostgreSQL 15 已合法占用 IPv4 5432，发往该端口的请求没有进入 MealOps 容器；
- 使用 55432 的隔离实验已验证 PostgreSQL 18.4、Hikari、Flyway、Spring Boot、Actuator 和 OpenAPI 均正常；
- 节点 3 runtime validation 进一步确认 Windows TCP excluded range `55334–55433` 包含 55432，且不修改操作系统保留端口策略；经真实检查确认 15432 未被 excluded、未被占用，因此将当前 local default 调整为 `127.0.0.1:15432 → container:5432`；
- 该端口决定只适用于本地开发，不改变生产数据库地址或端口设计。

## Reasons

- Java 21 已是 ADR 0002 接受的长期支持基线，当前 Oracle JDK 21 能满足相同语言与字节码合同；发行版和安全补丁保持可替换，避免构建无必要地绑定单一厂商；
- Maven Wrapper 让开发机和后续 CI 使用相同 Maven 版本，减少全局 Maven 差异；
- Spring Boot BOM 提供经过组合验证的依赖集合，避免手工拼接 PostgreSQL、Flyway、测试框架和 Servlet 栈版本；
- MyBatis-Plus 与 springdoc 不由当前 Boot BOM 提供所需精确版本，因此显式固定并通过真实构建与启动验证；
- PostgreSQL 18.4 在本地与集成测试中保持一致，可直接验证方言、驱动、Flyway 和数据库主版本；
- 本地默认使用 15432 可避开本机 PostgreSQL 5432 冲突和 Windows 当前保留的 55432 区间，使默认 Compose 与 local profile 无需额外环境变量即可协同运行；
- 最小依赖集合降低首次工程的下载、漏洞面、自动配置和长期升级成本。

## Alternatives

| 决策 | 当前选择 | 替代方案 | 替代方案优势 |
| --- | --- | --- | --- |
| JDK 发行版 | Java 21 合同，Oracle JDK 21.0.9 可用，Temurin 21 为参考 | 强制绑定 Temurin | 所有开发环境发行版完全一致 |
| 依赖版本 | Spring Boot BOM 管理覆盖范围 | 每个依赖手工固定版本 | 单个版本看起来更直观 |
| Maven | Wrapper 3.9.16 | 依赖系统 Maven | 文件更少，无 Wrapper 下载 |
| 数据库测试 | Testcontainers PostgreSQL 18.4 | H2 | 启动更快，不依赖 Docker |
| 工程生成 | 审查后的最小手工骨架 | Spring Initializr 默认输出 | 初始化速度快、模板完整 |
| 本地 PostgreSQL 端口 | host 15432 → container 5432 | 停止本机 PostgreSQL 15 | 可以继续使用 host 5432 |
| 本地 PostgreSQL 端口 | host 15432 → container 5432 | 每位开发者手工设置端口变量 | 仓库无需选择非默认 host port |
| PostgreSQL 内部端口 | container 5432 | 修改容器内部端口 | host 与 container 端口保持相同数字 |

## Why alternatives were not chosen

- 强制绑定 Temurin 会把发行厂商变成无业务收益的门禁；Java 21 major、编译目标和实际构建验证才是工程合同。若某发行版出现缺陷，可在不改源码合同的情况下替换；
- 为所有依赖手工固定版本会覆盖 Boot 已测试的组合，增加重复配置和不兼容风险；只有 Boot 未管理或项目明确要求的版本才显式声明；
- 系统 Maven 版本会随开发机变化，无法保证本地与 CI 重现同一构建行为；
- H2 不能等价验证 PostgreSQL 的方言、类型、锁和约束；节点 2 已有可用 Docker 环境，不需要该替代；
- 未经审查的 Initializr 输出可能带入当前节点不需要的依赖或配置，本节点文件范围足够小，直接建立最小骨架更易审查；
- 本机 PostgreSQL 15 是独立且合法的开发服务，MealOps 不应停止或修改它来占用 5432；
- 强制所有开发者手工设置 `MEALOPS_DB_PORT` 会让默认启动路径不可复现，也容易造成 Compose 与 Spring 配置不一致；
- 不修改 Windows excluded port range，避免改变宿主机 Hyper-V/WSL/Docker 等系统网络策略；15432 已通过当前环境的 excluded range、listener 和服务占用检查；
- 修改容器内部标准端口 5432 没有必要，host port 映射已经能隔离冲突，并且保留标准端口更符合镜像和工具默认约定。

## Trade-offs

- Wrapper 增加少量脚本和元数据文件，首次运行仍需下载 Maven 与依赖；
- Testcontainers 需要可用 Docker Engine，集成测试比内存数据库慢；
- Boot BOM 升级可能连带改变多个传递依赖，升级时必须重新执行依赖树、测试和启动检查；
- Oracle JDK 与参考 Temurin 并非同一发行版，若发现发行版相关差异，需要在两者上复现；
- 固定 Spring Boot、MyBatis-Plus、springdoc 和 PostgreSQL patch 可复现，但安全修复不会自动进入，必须通过受控升级处理；
- 本地数据库 URL 使用非默认 host port 15432，开发文档和 IDE 数据库客户端必须明确使用该端口。

## Revisit Conditions

满足以下任一条件时重新评估：

- Java 21 的可用发行版无法继续获得满足项目要求的安全更新；
- 核心依赖稳定支持 Java 25，且新版本为 MealOps 带来明确、可验证的收益；
- Spring Boot 4.1.0 BOM 管理的某个依赖出现安全或兼容问题，需要升级 Boot patch/minor 或显式覆盖；
- MyBatis-Plus 3.5.17、springdoc 3.0.3 或 Testcontainers 与当前 Boot/PostgreSQL 组合出现无法规避的兼容问题；
- CI 无法提供 Docker/Testcontainers，且出现能够保持真实 PostgreSQL 18 行为的替代环境；
- Maven Wrapper 版本停止受支持或无法运行在项目 Java build contract 上；
- 本地基础设施策略改变、不再存在 5432 端口冲突、改用远程开发数据库，或 Docker Compose 被其他开发环境工具替代。
