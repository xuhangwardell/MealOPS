# ADR 0016：Planning Preferences Profile

## Status

Accepted for Node 11；实现后进入审查。

## Background

MealOps 尚无 User、Account 或认证模型，但未来 MealPlan 与 Planner 需要稳定读取规划偏好。本节点只保存偏好，不执行菜谱筛选、排序或计划生成。

## Decision

V1 采用单用户、持久化的 singleton `PlanningPreferences` profile。V5 创建 `planning_preferences` 的唯一 `id=1` 行，并以关系表保存排除食材 ID。公开领域模型不暴露持久化 ID。

字段为 `defaultServings`、可空的 `maxCookingMinutes` 和唯一、正数、按升序返回的 `excludedIngredientIds`。API 使用 GET 与完整替换语义的 PUT；缺失食材在替换前校验并使整个 PUT 失败。当前并发语义为最后一次成功 PUT 生效，不增加版本、ETag 或锁。

## Reasons

- 单例行在没有 User 模型时仍能提供确定的默认 profile；V5 seed 保证 GET 不返回 404。
- 关系表和外键保留数据完整性，避免 JSONB 中无法直接约束食材存在性。
- PUT full replacement 能明确表达删除旧排除项和清空列表，避免 merge 语义歧义。
- 偏好存储与未来 Planner 应用解耦，避免本节点提前查询 Recipe 或改变 Shopping/Inventory 行为。

## Alternatives

- 创建 fake User 或 `userId=1`；
- 仅在请求中携带临时偏好；
- 持久化 JSONB 的 excluded IDs；
- 使用 PATCH 增量修改；
- 引入乐观锁、ETag 或 `SELECT FOR UPDATE`；
- 直接将偏好应用到 Recipe 筛选或 Planner。

## Why alternatives were not chosen

Fake User 会固化尚未确认的认证边界；请求级偏好无法跨请求读取。JSONB 放弃 FK 与关系唯一约束。PATCH 需要额外定义删除和 merge 语义。V1 没有真实并发冲突需求，锁和版本会增加复杂度。Planner 应用规则属于后续节点，不在偏好存储节点提前实现。

## Trade-offs

单例方案简单、可迁移且有数据库约束，但暂时只能支持一个 profile，且最后写入者覆盖并发更新。关系表增加一次删除加批量插入的替换操作，但查询和完整性清晰。未来加入认证时需要迁移 singleton 到 per-user 数据，并重新评估并发控制。

## Revisit Conditions

- 认证和 User 模型正式落地；
- 多用户或家庭协作需求出现；
- 并发 PUT 冲突造成实际数据丢失；
- excluded 之外的偏好字段需要结构化查询；
- Planner 开始真正解释 `maxCookingMinutes` 或排除食材。
