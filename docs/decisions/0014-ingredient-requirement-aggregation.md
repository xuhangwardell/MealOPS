# ADR 0014：Ingredient Requirement Aggregation

## Status

Accepted for Node 9；实现后进入最终审查。

## Background

多个菜谱选择需要先分别按目标份数缩放，再回答这些菜谱理论上需要多少食材。该结果是需求侧派生数据，不是库存或购物状态。

## Decision

使用不可持久化的 `RecipeSelection -> RecipeScaler -> ScaledRecipe -> IngredientRequirementAggregator` 流程。聚合键为 `ingredientId + canonical unit`，每个选择独立缩放，重复 recipe selection 不预合并；结果按 ingredient ID、unit code 稳定排序。只使用 `BigDecimal.add`，不新增 rounding policy；应用服务只读加载菜谱，不访问库存。

## Reasons

保持 Node 6 的缩放逻辑单一来源，避免重复选择合并改变 DECIMAL128 计算路径；显式区分同食材的 g、ml、piece，避免无依据的跨维度换算；派生结果无需 schema、迁移或生命周期管理。

## Alternatives

- 持久化 requirements；
- 仅按 ingredientId 聚合；
- 先合并重复 recipe selection 再缩放；
- 由库存感知的缺口或购物清单直接产出；
- 纯需求侧派生聚合。

## Why alternatives were not chosen

持久化会引入 V5 和状态一致性；忽略 unit 会把不同维度错误相加；预合并选择会改变逐次缩放的精度语义；库存、缺口和购物属于后续节点。当前选择纯需求侧派生聚合。

## Trade-offs

收益是确定性、可解释、无新表且复用现有 RecipeScaler；代价是每次请求需要读取并重建菜谱，且结果不保存。允许在一次请求内缓存相同 recipe ID 的加载，但不得合并 selection。

## Revisit Conditions

当需要历史快照、异步计划、跨请求复用、库存扣减或购物清单时，重新评估持久化、缓存和更完整的计划模型。跨维度换算仍需独立业务决策。
