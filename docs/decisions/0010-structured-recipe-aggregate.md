# ADR 0010：Structured Recipe Aggregate

## Status

Accepted for Node 5。

## Background

Recipe 是 Ingredient identity 与 Quantity foundation 的第一个真实业务消费者。系统需要保存菜名、基准份数、预计耗时、食材用量和有序步骤，并保证整个 aggregate 原子创建。

## Decision

- Recipe 作为 aggregate root，包含 RecipeIngredient 和 RecipeStep。
- RecipeIngredient 通过 `ingredientId` 引用标准食材，不保存 Ingredient name snapshot。
- 输入允许 `g`、`kg`、`ml`、`l`、`piece`，Application 在建模前转换为 `g`、`ml` 或 `piece`。
- RecipeIngredient 数量必须大于零，单个 Recipe 内同一 ingredient 只能出现一次。
- RecipeStep 和 RecipeIngredient 均使用从请求数组生成的 1-based position。
- Recipe name 不做 lowercase 或 unique 约束；相同名称可以对应不同 Recipe。
- Node 5 只提供 Create/Get API；不实现 PUT、DELETE、LIST、SEARCH 或 Scaling。
- 持久化使用 `V2__create_recipe.sql`，amount 使用 PostgreSQL unconstrained `NUMERIC`，unit_code 只保存 canonical base unit。
- Create Recipe 使用一个事务，Recipe、ingredients 和 steps 全部成功或全部回滚。

## Reasons

aggregate 内部不变式集中在 Domain，避免 Controller 或数据库适配器重复实现。canonical unit 让查询、持久化和后续 Planner 使用统一表示；Ingredient ID 引用避免名称快照过期和 N+1 查询。显式 child SQL 保持顺序和约束可审查。

## Alternatives

1. 在 Recipe 中直接保存食材名称或名称快照。
2. 保存原始单位与原始数量，不转换 canonical unit。
3. 使用 `NUMERIC(p,s)` 固定精度。
4. 为 child 表增加 surrogate id。
5. 使用 JPA/Hibernate 自动映射 aggregate。
6. 首次实现同时提供 PUT、DELETE、LIST 和 Scaling。

## Why alternatives were not chosen

名称快照会产生身份与名称一致性问题；原始单位会让业务查询承担多种表示；Node 4 尚未决定固定 scale，因此使用 unconstrained NUMERIC；child 没有独立生命周期，不需要 surrogate id。JPA 映射会隐藏关键 SQL 和排序；额外写操作与 Scaling 会扩大当前节点边界。

## Trade-offs

当前方案拥有清晰的 aggregate 不变式、原子事务和稳定 canonical representation。代价是新增单位需要更新 Java domain，Recipe name 不具备唯一约束，读取时需要固定的 parent/ingredient/step 查询，且暂不支持更新和缩放。

## Revisit Conditions

- Recipe authoring 需要保留原始单位或展示名称；
- 需要确定 NUMERIC precision/scale 或 rounding policy；
- Recipe child 获得独立生命周期；
- 需要 PUT 时必须先决定 optimistic concurrency、ETag/If-Match 和 version；
- Scaling、qualitative quantity（适量/少许）、Inventory 或 Planner 进入后续节点；
- Recipe 查询规模需要专门 projection 或分页策略。
