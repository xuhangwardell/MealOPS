# ADR 0022：事务型餐槽完成与库存消费

## Status

Accepted — Node 17 implemented / under review.

## Background

Node 12～16 已能持久化、确认 MealPlan，并从计划派生购物预览，但尚无“这一餐已经做完”的业务事实，也不会因此扣减库存。Node 17 需要把持久化餐槽、Recipe 用量、FEFO 库存消费和库存流水连接成可回滚、可并发验证的纵向闭环。

## Decision

- MealSlot 增加 `PENDING` / `COMPLETED` execution status；新建餐槽固定为 `PENDING`。
- MealPlan 增加 `COMPLETED`：最后一个餐槽完成时自动进入该状态。
- 只允许 CONFIRMED 计划中的 PENDING 餐槽完成；DRAFT/CANCELLED 冲突，已完成餐槽重试为成功 no-op。
- `POST /api/v1/meal-plans/{planId}/slots/{mealDate}/{mealType}/complete` 不接受请求体，返回完整最新 MealPlan。
- 持久化的 Recipe selection 和 `targetServings` 是事实来源；完成用例不读取 Planning Preferences，也不重跑 Planner。
- 用 `RecipeScaler` 与 `IngredientRequirementAggregator` 计算单餐需求，并按 ingredient ID、unit 确定性消费。
- 抽取共享 `InventoryConsumptionCoordinator`；Node 8 与 Node 17 复用同一 FEFO、乐观 CAS 和 `CONSUME` 流水实现。
- 整个多食材消费和餐槽状态更新处于同一个外层事务中，不使用 `REQUIRES_NEW`。
- 通过 MealPlan 父记录 `SELECT ... FOR UPDATE` 串行化同计划的完成和取消；重复并发完成只消费一次。
- V7 只增加 execution status 并允许 MealPlan `COMPLETED`，不新增执行历史或预留表。
- Shopping Preview 对 CONFIRMED 计划只包含 PENDING 餐槽；COMPLETED 返回空预览。

## Reasons

- 完成餐槽是库存减少的明确业务事件，比 confirm 或 preview 更符合用户事实。
- 单事务保证多食材消费要么全部成功，要么库存、version、流水和计划状态全部保持原样。
- 父记录行锁为同一聚合提供简单、可审查的串行化边界，并使重试幂等不依赖新的幂等表。
- 复用 Node 8 协调器避免不同入口产生不一致的 FEFO、CAS 或流水规则。
- 保存 execution status 而非从流水推断，能直接表达计划生命周期并稳定支持查询。

## Alternatives

1. Confirm 时立即消费库存。
2. 完成具体 MealSlot 时消费库存。
3. 先建立 Inventory reservation，再在完成时兑现。
4. 每种食材独立事务、允许部分成功。
5. 多食材与状态更新使用一个原子事务。
6. 只依赖 Inventory optimistic CAS，不锁 MealPlan。
7. 对 MealPlan 父记录使用 pessimistic row lock。
8. 为完成请求新增通用 Idempotency-Key 表。
9. 以持久化 execution status 提供自然幂等。
10. 新建 execution history / reversal ledger。
11. 当前只保存状态并继续使用既有库存流水。

## Why alternatives were not chosen

- consume-on-confirm 会在用户尚未实际做饭时提前改变库存，且取消需要复杂补偿。
- reservation 引入保留量、释放、过期和并发规则，超出当前节点。
- 分食材事务或 best-effort 会留下无法解释的半完成餐食。
- 仅靠 Inventory CAS 不能防止两个请求基于同一 PENDING 餐槽分别成功消费不同批次。
- 通用幂等键不是当前必要条件；锁定计划后重新读取持久化 execution status 已能提供本用例的重复请求语义。
- execution history、undo 和 reversal 需要额外业务模型，不能以删除 `CONSUME` 流水实现。

## Trade-offs

- 行锁会串行化同一 MealPlan 的操作，但计划只有少量餐槽，锁粒度与业务聚合一致。
- 多食材事务持续时间比单食材消费更长；换来完整原子性和简单恢复语义。
- 当前 last completed state 没有独立完成时间、操作者或事件历史，不能执行撤销。
- Recipe 缺失被视为持久化不变量破坏，而不是用户可恢复的普通 404。
- Inventory CAS 冲突仍返回既有并发错误，不自动重试，避免隐藏竞争和重复副作用。

## Revisit Conditions

- 需要跨设备离线重试或通用 API 幂等键时，评估请求幂等记录。
- 需要预占库存、多人协作或长时间计划时，单独设计 reservation ADR。
- 需要撤销、纠错、审计完成时间或操作者时，引入 execution event 与补偿流水，而不是改写历史。
- 单个计划出现大量并发操作且父记录锁成为已测量瓶颈时，重新评估锁粒度。
- Inventory 消费拆成远程服务时，重新设计跨边界一致性；当前不因此提前引入 MQ 或分布式事务。
