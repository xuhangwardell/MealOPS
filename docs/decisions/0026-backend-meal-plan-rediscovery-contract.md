# ADR 0026：Backend MealPlan Rediscovery Contract

## Status

Accepted — Node 20 prerequisite implemented / under review。

## Background

Node 20 前端需要在浏览器刷新或重新进入 Plans 页面后恢复用户最近创建的 MealPlan。现有后端只有按 ID 读取接口，前端无法在不持久化 planId 的情况下重新发现计划。

## Decision

新增只读接口：

`GET /api/v1/meal-plans/latest`

- 有计划时返回 `200` 和完整现有 `MealPlanResponse`；
- 没有计划时返回 `204 No Content`；
- “latest”表示最近创建的持久化计划，不因 DRAFT、CONFIRMED、COMPLETED 或 CANCELLED 状态而排除；
- 使用 identity `id DESC LIMIT 1` 作为当前 schema 下稳定的创建顺序；
- Repository 通过数据库单行选择并复用既有 aggregate 装配；
- 不新增迁移，不创建历史列表、分页、搜索或认证语义。

## Reasons

后端是 MealPlan 状态的唯一事实来源。显式 latest endpoint 让刷新、重新进入和生命周期变化都重新读取 server truth，同时避免前端复制排序、生命周期和持久化逻辑。当前 `meal_plan.id` 使用数据库 identity 单调生成，且没有 `created_at` 字段，因此数据库 `ORDER BY id DESC LIMIT 1` 是最小且可审查的实现。

## Alternatives

1. 前端使用 `localStorage`、`sessionStorage` 或 Pinia 持久化 planId；
2. 前端读取全部 MealPlan 后自行选择最大 ID；
3. 新增通用 `GET /api/v1/meal-plans` 历史集合接口；
4. 新增 `created_at` 字段并以时间排序。

## Why alternatives were not chosen

客户端持久化会产生过期或跨设备状态，且绕过后端事实来源。读取全集再排序会增加网络和内存开销，并把创建顺序语义复制到前端。当前节点只需要 rediscovery，不需要历史浏览、分页或搜索。新增时间字段会扩大 schema 变更范围；在当前 identity 顺序已满足单用户 V1 需求的情况下没有必要。

## Trade-offs

该方案只支持当前单用户 V1 的“最近创建”语义，不提供用户隔离或历史查询。identity ID 必须继续保持单调创建顺序；若未来引入多用户、分布式写入或需要精确业务时间排序，应重新评估排序字段和认证边界。204 让无计划状态明确区别于资源不存在错误，但调用方需要处理无内容响应。

## Revisit Conditions

- 引入认证或多用户后，latest 必须限定到 authenticated user；
- identity ID 不再能代表稳定创建顺序；
- 需要历史列表、分页、搜索或按时间范围查询；
- 需要跨时区或业务创建时间审计。
