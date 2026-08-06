# ADR 0025：Foundational Domain Frontend Slices

## Status

Accepted — Node 19 implemented / under review。

## Background

Node 18 建立了 uni-app 前端工程基线，Node 19 prerequisite 提供了 Ingredient 和 Recipe catalog read APIs。本节点第一次把真实后端事实来源接入移动端页面，同时覆盖库存批次、结构化菜谱和 Planning Preferences。

## Decision

- Backend 保持冻结，继续作为唯一事实来源。
- 使用小型 typed API modules，复用 `uni.request` 和 ProblemDetail。
- Ingredient catalog 通过 Pinia 作为跨页面共享引用状态；Inventory、Recipe、Planning Preferences 使用 page-local state。
- mutation 成功后重新读取 server truth，不使用 fake optimistic entity。
- 数量保留字符串，前端不做 BigDecimal 算术、单位换算或规划业务计算。
- 使用原生 uni-app controls 和 mobile-first cards/forms；H5 优先并保持 mp-weixin 编译兼容。
- 本节点不实现认证、MealPlan、Shopping Preview、执行流程或 Node 20 功能。

## Reasons

该边界能让三个页面共享 canonical Ingredient 身份，同时避免过早形成巨大全局状态。小型手写类型在当前 endpoint 数量下更易审查，后端仍拥有校验和业务规则，服务器刷新保证页面不会与数据库事实分叉。

## Alternatives

1. Giant Pinia domain store vs page-local state plus shared Ingredient reference。
2. Optimistic local entities vs server-truth refresh。
3. Frontend calculations vs backend authority。
4. OpenAPI codegen now vs small typed modules。
5. Desktop CRUD tables vs mobile-first cards/forms。
6. Node 19 与 Node 20 一起实现 vs 先建立基础领域入口。

## Why alternatives were not chosen

全局 Store 会把尚未稳定的未来领域状态提前耦合；optimistic entity 会制造与后端不一致的假数据；前端计算会复制库存、单位和规划规则；当前 API 规模不足以抵消 codegen 的生成物和升级成本；桌面表格不适合 375px 首要目标；Node 20 的 MealPlan、Shopping 和执行流程有独立事务边界，应在本节点之后实现。

## Trade-offs

页面本地状态需要各页面分别处理 loading/error/empty，typed modules 需要随 DTO 演进手工维护。原生控件降低依赖和跨端风险，但复杂交互需要后续自行补充。Runtime Smoke 若受到 Docker 沙箱限制，只能标记为环境阻塞，不能用假数据替代。

## Revisit Conditions

当 API 数量显著增长、重复类型维护成本超过收益时评估 OpenAPI codegen；当真实跨页面业务状态出现时评估更多 Pinia store；当 UI 交互复杂度有数据证据时评估轻量组件库；Node 20 再评估 MealPlan、Shopping 和 execution UI；Node 21 再决定浏览器 E2E 与发布验收工具。
