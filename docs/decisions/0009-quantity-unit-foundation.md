# ADR 0009：Quantity 与 Unit Domain Foundation

## Status

Accepted for Node 4。

## Background

Recipe、Inventory、Shopping 和 Planner 都需要可复现的数量与单位运算。Node 4 只建立纯 Domain 模型，不创建数据库表、Mapper、Repository、Controller 或 HTTP API。

## Decision

- 在 `measurement.domain` 中使用 `Dimension`、`Unit` 和不可变 `Quantity`。
- Dimension 仅包含 `MASS`、`VOLUME`、`COUNT`。
- 固定 Java system units：`g`、`kg`、`ml`、`l`、`piece`；基础单位分别为 `g`、`ml`、`piece`。
- `factorToBase` 使用 `BigDecimal`。数量禁止为负，零允许，COUNT 允许小数。
- 仅允许同维度转换；转换、加法和减法使用精确 BigDecimal 运算，不提前引入 rounding policy。
- `add`/`subtract` 将右值转换为左值单位并保持左值单位；减法结果不得为负。
- `equals` 表示相同单位且数值相等（忽略 BigDecimal scale）；`equivalentTo` 表示物理数量相等，可跨同维度单位。
- 当前不建立 unit table，也不持久化 Quantity。
- 未来首次进入业务表时，默认持久化为基础单位值：MASS=g、VOLUME=ml、COUNT=piece。

## Reasons

固定枚举让当前规则可读、可测试、可审查，避免把核心单位语义交给运行时配置。BigDecimal 保证数量换算不受浮点误差影响；不提前取整避免在尚未决定精度政策前损失信息。

## Alternatives

1. 原始数值加 String unit。
2. 动态数据库 unit table。
3. 第三方 measurement library。
4. 持久化原始单位，或同时保存原始与 canonical 单位。

## Why alternatives were not chosen

String 缺少编译期约束，数据库配置会把稳定业务规则变成运行时数据，第三方库会引入当前不必要的依赖和模型映射。当前没有保留用户原始输入单位的业务需求，同时保存两套事实会增加一致性负担；因此先采用最小固定 Java domain model。

## Trade-offs

优点是边界明确、无基础设施依赖、纯单元测试快速且可复现。代价是新增单位需要修改代码并发布，当前不支持密度换算、跨质量/体积换算、signed quantity 或舍入策略；持久化 canonical base unit 的具体列类型仍留给进入业务表的节点决定。

## Revisit Conditions

- 出现非 exact conversion 的单位或明确的精度/舍入需求；
- 用户需要保留 Recipe authoring 的原始输入单位；
- 单位需要由管理员配置或跨租户扩展；
- 出现 OCR、外部标准或密度模型等独立测量能力；
- Quantity 首次进入业务表，需要确定 NUMERIC precision、scale 和迁移策略。
