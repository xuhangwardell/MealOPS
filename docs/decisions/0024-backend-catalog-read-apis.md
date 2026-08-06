# ADR 0024：Backend Catalog Read APIs

## Status

Accepted — prerequisite for Node 19; implemented / under review.

## Background

Node 18 建立了前端工程基线，但 Contract Gate 发现 Ingredient 与 Recipe 只有按 ID 读取接口，没有供页面加载 canonical catalog 的列表读取能力。Node 19 需要从空数据库开始，真实创建并选择 Ingredient、录入库存、创建 Recipe 和编辑 Planning Preferences；前端不能用会话记忆、候选接口或假数据替代后端事实来源。

## Decision

- 增加 `GET /api/v1/ingredients`，返回所有 `IngredientResponse` 的 bare JSON array。
- 增加 `GET /api/v1/recipes`，返回所有 `RecipeResponse` 的 bare JSON array。
- 两个 endpoint 在空 catalog 时返回 `200` 与 `[]`。
- Ingredient 按 `id ASC`；Recipe 按 `id ASC`；Recipe 的 ingredients 与 steps 复用现有 aggregate 顺序。
- Ingredient list 通过 repository `findAll()` 使用显式 `ORDER BY id ASC`。
- Recipe list 直接复用 Node 13 已有的 `RecipeRepository.findAll()` 批量装配实现，不增加新的 persistence 查询路径。
- 两个 catalog read 不读取 Inventory、Planning Preferences、Shopping，也不调用 Candidate/Planner 语义。
- V1 不加入 pagination、search、filter、sort query、schema migration 或新的依赖。
- 现有 POST、按 ID GET、Ingredient PUT rename 和 Recipe scaling API 保持不变。

## Reasons

- 后端 catalog 是 Ingredient 与 Recipe 的唯一事实来源，能让后续前端 selector 和列表读取现有数据。
- 复用完整 `IngredientResponse` 与 `RecipeResponse`，避免 Node 19 再维护一套不完整 DTO。
- ID 排序不依赖名称、locale 或数据库 collation，结果稳定且容易验收。
- RecipeRepository.findAll 已有 parent + batch child 查询和 aggregate 重建，避免 N+1 回归。
- 空数组是合法的个人单用户 V1 状态，不应被误报为 404 或 204。

## Alternatives

1. 让前端只记住当前会话创建的实体。
2. 复用 `/api/v1/recipe-candidates` 作为 Recipe catalog。
3. 现在就引入 pagination/search/filter。
4. 按 name 排序。
5. 新建只含 id/name 的 Recipe summary DTO。
6. 增加专用 catalog persistence 表或 migration。

## Why alternatives were not chosen

- 会话记忆无法读取已有数据库数据，也会在刷新后丢失事实。
- Candidate endpoint 已经包含 Planning Preferences 与 hard-constraint 语义，不是完整 Recipe catalog。
- 当前是单用户 V1，catalog 规模不足以证明分页协议；过早引入会固定无需求的 envelope 和 query contract。
- name 排序受大小写、Unicode 与数据库 collation 影响；ID ASC 更可复现。
- Recipe 页面需要 ingredients、steps、份量和时间，summary 会迫使前端再次按 ID 请求并造成 N+1。
- 现有 schema 已包含 canonical 数据，读取 API 不需要新表或 migration。

## Trade-offs

- bare array 后续若需要分页，可能要增加新的响应协议；当前以最小、清晰的 V1 contract 换取低复杂度。
- ID 顺序不代表用户推荐优先级；规划候选仍由独立 Candidate API 表达。
- Recipe list 返回完整 aggregate，单次 payload 大于 summary，但避免页面重复读取和 child 串联请求。
- 当前没有 server-side search/filter；数据规模真正增长后需要单独 ADR，而不是隐式扩展本接口。

## Revisit Conditions

- catalog 数据量或网络测量证明完整数组不可接受时，评估分页和字段投影协议。
- 需要搜索、别名或筛选时，新增明确的 catalog query ADR，不复用 Planner 语义。
- 前端 API contract 稳定且类型维护成本显著上升时，再评估 OpenAPI code generation。
- 出现多用户权限或远程 catalog 服务时，重新评估缓存、授权和分页边界；当前不引入这些能力。
