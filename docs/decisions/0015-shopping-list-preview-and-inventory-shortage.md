# ADR 0015：Shopping List Preview 与 Inventory Shortage

## Status
Accepted for Node 10；实现后进入最终审查。

## Background
Node 9 产生理论食材需求，Node 10 首次读取现有 accounting-available inventory，计算需求覆盖和缺口。

## Decision
使用只读、派生、不可持久化的 Shopping List Preview。应用服务复用 Node 9 requirement service，并一次读取 `InventoryBatchRepository.findAvailable()`；按 `ingredientId + canonical unit` 聚合库存，缺口为 `required - available`，仅正缺口生成 item。过期和无过期批次只要 remaining 大于零均计入 accounting availability；不做跨维度换算、FEFO、锁、预留或库存 mutation。

## Reasons
保持 Node 9 为需求事实来源，复用 Node 8 的库存读取边界；预览是 advisory derived result，不引入 V5 或购物状态一致性。

## Alternatives
持久化购物清单、逐需求查询库存、仅按 ingredientId 聚合、排除过期库存、预留库存。

## Trade-offs
实现简单且可解释，但预览可能因库存变化立即过时；shortage 不是零售包装购买量，也不表达食品安全可用性。

## Revisit Conditions
需要历史购物清单、预留、包装规格、价格、食品安全策略或并发购买流程时，重新评估持久化、锁和独立购买模型。
