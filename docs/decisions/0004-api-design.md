# 0004：API 设计

## Status

- 状态：已接受
- 决策日期：2026-08-03
- 适用阶段：V1

## Background

MealOps 的 H5 和后续微信小程序需要通过稳定的 HTTP 契约访问后端。API 既要正确使用 HTTP 语义，也要为校验失败、库存冲突、候选失效和业务约束提供机器可识别且便于用户理解的错误。

V1 的数据访问模式以明确资源和用例为主，没有客户端任意拼装复杂数据图的需求。

## Decision

### 协议与资源风格

- 使用 REST + JSON；
- V1 API 统一使用 `/api/v1` URI 前缀；
- 资源 URI 使用复数名词和稳定标识；
- 使用标准 HTTP 方法和状态码；在具体用例需要时使用条件请求和幂等语义；
- 列表接口根据用例提供分页，不返回无限集合；
- 不在成功或失败响应外层统一包装 `Result<T>`。

### 成功响应

- 查询成功返回 `200 OK` 和资源表示；
- 创建成功返回 `201 Created`，在可行时提供 `Location`；
- 无响应体的成功操作返回 `204 No Content`；
- 异步状态码不在没有真实异步边界时提前使用；
- 业务失败不得伪装为 `200 OK`。

### 错误响应

- 使用 RFC 9457 Problem Details；
- 响应媒体类型为 `application/problem+json`；
- 标准字段包括 `type`、`title`、`status`、`detail` 和 `instance`；
- 通过扩展字段 `code` 提供稳定、可测试的业务错误码；
- HTTP status、Problem Details `status` 和业务错误语义必须一致；
- 不在 `detail` 中暴露堆栈、SQL、密钥或内部实现信息。

### 版本策略

- V1 使用 URI major version：`/api/v1`；
- 向后兼容的字段增加和新资源通常不提升 major version；
- 删除字段、改变字段语义或破坏现有客户端的行为需要新 major version 或明确迁移期；
- minor 与 patch 版本不进入 URI；
- 具体弃用周期为“待确认”，应在第一次破坏性变更前通过 ADR 确定。

### API 文档

- 使用 springdoc-openapi 3.x 生成 OpenAPI 文档；
- Spring Boot 4 对应 springdoc 3.x；
- DTO 校验、状态码、Problem Details 和 OpenAPI 描述必须保持一致；
- `/v3/api-docs` 作为机器可读契约，Swagger UI 用于开发和人工验证；
- 精确 springdoc patch 版本在工程节点按兼容性验证后固定。

## Reasons

- REST 与 MealOps 的资源和状态操作匹配，客户端与后端边界清晰；
- 标准 HTTP status 可被浏览器、网关、日志、监控和测试工具直接理解；
- RFC 9457 是标准错误格式，Spring MVC 原生提供 `ProblemDetail`、`ErrorResponse` 和异常处理支持；
- 稳定业务 `code` 让前端不需要解析中文 `detail`；
- URI major version 对 H5 和微信小程序均直观，便于并行迁移；
- OpenAPI 能作为后续前端类型、契约测试和人工审查的共同来源。

## Alternatives

| 决策 | 当前选择 | 替代方案 | 替代方案优势 |
| --- | --- | --- | --- |
| API 风格 | REST | GraphQL | 客户端可按需选择字段，聚合数据图灵活 |
| 成功响应 | 原生资源 + HTTP status | `Result<T>` | 结构统一，部分团队客户端处理习惯成熟 |
| 错误协议 | RFC 9457 + `code` | 自定义错误 DTO | 字段可完全按团队偏好设计 |
| 版本 | URI major version | Header 或媒体类型版本 | URI 更稳定，版本协商更灵活 |
| 文档 | springdoc-openapi 3.x | 手写 OpenAPI | 完全控制规范，不依赖运行时代码扫描 |

## Why alternatives were not chosen

### GraphQL

MealOps 没有客户端任意选择复杂数据图的强需求。GraphQL 会新增 Schema、Resolver、N+1、查询复杂度限制和字段级授权问题，而不会提升库存事务或 Planner 的核心价值。

### `Result<T>`

HTTP status 已负责传递协议级成功或失败语义，成功资源再统一包装 `code`、`message` 和 `data` 会形成一层重复协议；失败响应已经采用 RFC 9457 Problem Details，因此 V1 不再维护第二套统一响应 envelope。部分团队可以正确组合 `Result<T>` 与标准 HTTP status，但 MealOps 当前没有足够收益支持这一额外抽象。

### 完全自定义错误 DTO

可以满足当前字段需要，但会放弃标准媒体类型和 Spring 原生支持。RFC 9457 已允许通过扩展字段增加稳定业务码。

### Header 或媒体类型版本

版本协商能力更强，但本项目客户端数量和版本并行需求有限。URI major version 更直观，也更容易在日志、路由和文档中识别。

### 手写 OpenAPI

设计优先的 OpenAPI 有价值，但当前个人项目更适合从已验证 Controller、DTO 和校验生成文档，再用契约检查防止漂移。若自动生成无法表达契约，再评估设计优先流程。

## Trade-offs

- URI 版本会出现在资源路径中，未来 major 升级需要维护并行端点或迁移客户端；
- springdoc 是社区项目而非 Spring 官方模块，需要跟踪 Boot 4 兼容矩阵；
- 自动生成 OpenAPI 仍需补充业务语义，不能只依赖反射得到完整文档；
- Problem Details 的 `type` URI 命名、校验错误扩展结构和关联追踪标识尚待 API 工程节点细化；
- REST 端点若按页面随意聚合，仍可能退化为不稳定的 RPC，需要以用例和资源边界审查。

## Revisit Conditions

满足以下任一条件时重新评估：

- 客户端出现大量不同形态的数据图需求，REST 导致持续过取或多次往返；
- 需要对外开放公共 API，并形成正式弃用与多版本支持承诺；
- springdoc 3.x 无法稳定支持当前 Spring Boot 版本；
- 自动生成规范无法满足契约优先、SDK 生成或治理要求；
- Problem Details 扩展在多个服务或客户端之间需要统一治理。

## References

- [Spring Framework Error Responses](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html)
- [springdoc Compatibility FAQ](https://springdoc.org/faq.html)
- [springdoc-openapi v3 Documentation](https://springdoc.org/v4/index.html)
