# ADR 0011：Recipe Scaling

## Status

Accepted for Node 6。

## Background

Recipe 的 base servings 是事实来源。用户需要在不同 target servings 下查看确定性食材数量，但该结果不应改变或持久化基础 Recipe。

## Decision

- 使用纯 Java `RecipeScaler` 计算不可变 `ScaledRecipe`，不注册为 Spring Bean。
- 公式为 `originalAmount * targetServings / baseServings`，直接计算 numerator，不预先舍入 scale factor。
- 能够 exact divide 时保持完整精度；non-terminating divide 使用 `MathContext.DECIMAL128`。
- 保留 canonical base units，支持 fractional COUNT。
- estimatedMinutes、steps、positions 和基础 Recipe 均保持不变。
- Application Service 只读加载 Recipe 并调用 scaler；API 为 `GET /api/v1/recipes/{id}/scaled?targetServings=n`。
- ScaledRecipe 是 ephemeral derived result，无 Repository、migration、数据库表或缓存。

## Reasons

该方案保持 Recipe 单一事实来源，避免读操作产生写入副作用。先 exact 再 DECIMAL128 fallback 能保留可表示的精确结果，并将非终止小数的精度策略限定在 Scaling，而不修改 Node 4 Quantity 语义。

## Alternatives

1. 原地修改 Recipe。
2. 持久化 scaled Recipe。
3. 预先计算并舍入 scale factor。
4. 固定 scale rounding。
5. 使用 DECIMAL64。
6. 使用 rational/fraction 表示。

## Why alternatives were not chosen

原地修改和持久化会破坏 base Recipe source of truth；预先舍入或固定 scale 会提前丢失精度；DECIMAL64 精度不足以作为当前明确策略；rational model 虽数学精确，但会扩大 API、Domain 和下游复杂度。

## Trade-offs

结果可复现、无数据库副作用且支持小数 COUNT。DECIMAL128 对非终止小数提供有限精度，不能表达无限展开；Scaling 当前只读，不能直接产生计划或库存变化。

## Revisit Conditions

- 需要用户可编辑或持久化缩放结果；
- 出现更高精度或货币/包装舍入规则；
- 需要 rational arithmetic 或单位特定 rounding；
- Recipe Scaling 与 Inventory/Shopping 形成新的事务边界；
- 需要 PUT、ETag 或 optimistic concurrency。
