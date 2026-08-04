# 0008：标准食材身份与名称规范化

## Status

- 状态：已接受
- 决策日期：2026-08-04
- 适用阶段：节点 3 Canonical Ingredient

## Background

MealOps 的菜谱、库存和购物清单最终都需要引用同一个标准食材身份。节点 3 首先建立最小的标准食材纵向切片：创建、按 ID 查询和重命名。当前不实现别名、语义匹配、单位或用户归属。

如果只保存用户输入的原始名称，大小写、全角字符和多余空白会让同一食材产生多个数据库身份；如果直接进行语义推断，又会把“鸡蛋”和“土鸡蛋”等可能不同的业务事实错误合并。因此需要确定性、可测试且不承担语义解释的名称策略。

## Decision

Ingredient 的事实身份是数据库生成的 `id`。名称同时保存：

- `name`：规范化后的 display name，用于 API 展示；
- `normalized_name`：用于唯一性判断的确定性 normalized key。

Java `IngredientName` 按以下顺序规范化：

1. Unicode NFKC；
2. 去除首尾 Unicode whitespace；
3. 将连续 Unicode whitespace 压缩为一个 ASCII space；
4. 以结果作为 display name；
5. 使用 `Locale.ROOT` lowercase 得到 normalized key；
6. display name 最大为 100 个 Unicode code points，不能为空。

数据库使用 PostgreSQL `UNIQUE` 约束 `uq_ingredient_normalized_name` 作为最终唯一性防线，并使用显式命名的非空 CHECK 约束保护两个名称字段。规范化不进行别名解析、翻译、拼音、模糊匹配或单位解析。

## Reasons

- 数据库 ID 是稳定且不依赖展示文本的事实身份；
- display name 与 normalized key 分离，既保留用户可读名称，又能稳定处理格式差异；
- NFKC 能统一全角等兼容字符，Unicode whitespace 处理能避免不可见格式差异；
- `Locale.ROOT` lowercase 不依赖运行机器的用户语言环境；
- Java 规范化逻辑可以用纯单元测试覆盖，PostgreSQL UNIQUE 可以在真实并发/约束环境中验证；
- 业务含义未被猜测，后续别名节点仍可以独立决定语义映射。

## Alternatives

| 方案 | 优点 | 局限 |
| --- | --- | --- |
| 原始 name 直接 unique | 实现最简单，保留输入原文 | 大小写、全角字符和空白差异会产生重复身份 |
| trim + lowercase | 成本低，能处理常见大小写差异 | 无法统一 NFKC 兼容字符和 Unicode whitespace |
| NFKC + whitespace + lowercase | 确定性、可解释、跨环境稳定 | 不能处理真正的语义同义词，需要维护规范化代码 |
| PostgreSQL `citext` | 数据库层大小写不敏感方便 | 不能覆盖 NFKC、空白策略，规则隐含在数据库类型中 |
| AI / fuzzy semantic normalization | 能处理同义词和模糊表达 | 不确定、难以复现，可能错误合并业务事实并引入模型依赖 |

## Why alternatives were not chosen

- 原始 name 直接唯一无法满足全角 `Ｅｇｇ` 与 `Egg` 应视为同一格式名称的要求；
- trim + lowercase 规则不足以处理 NFKC 和连续 Unicode whitespace；
- `citext` 只能解决部分大小写行为，不能表达完整的应用规范化 pipeline，也会把关键规则隐藏在 PostgreSQL 类型中；
- AI 或 fuzzy normalization 属于语义判断，不应在第一个确定性身份切片中决定“鸡蛋”和“土鸡蛋”是否相同；
- 当前没有足够业务收益引入额外数据库扩展，Java 规范化加显式 normalized key 已能满足节点 3。

## Trade-offs

- 应用层和数据库层同时保存规范化结果，写入时必须保持两者一致；
- 规范化规则变化可能改变未来名称的唯一性行为，需要通过受控迁移和 ADR 评估；
- 规则只处理格式，不解决别名和语义同义词，用户可能仍需后续维护别名；
- 当前名称长度限制按 Unicode code points 在应用层校验，数据库 `VARCHAR(100)` 提供额外边界保护，但复杂 Unicode 存储行为仍需保持测试覆盖。

## Revisit Conditions

- 业务确认需要把别名或同义词映射到同一 Ingredient 身份；
- 出现跨语言、拼音、模糊搜索或真实零售名称匹配需求；
- 规范化规则需要改变，导致已有 `normalized_name` 冲突或需要批量迁移；
- 数据库查询规模或部署约束证明需要 `citext`、专用搜索索引或其他数据库能力；
- 用户反馈表明仅格式规范化无法支持主要录入场景。
