# ADR 0023：前端工程基线

## Status

Accepted — Node 18 implemented / under review.

## Background

Node 3～17 已建立并验证从食材、菜谱、库存、规划到餐槽完成的后端执行闭环。此时再引入前端，可以让页面以稳定的 REST、Problem Details 和生命周期语义为基础，而不需要用假数据或临时协议驱动架构。Node 18 只建立 Node 19～21 可复用的工程、传输和呈现边界，不提前实现业务页面。

## Decision

- Monorepo 增加独立 `frontend/` 边界，使用官方 uni-app Vue 3 / Vite / TypeScript CLI 模板。
- 运行时合同为 Node.js 24 LTS；包管理器使用 npm，并提交 `package-lock.json`。
- H5 是第一运行目标，同时保持 mp-weixin 编译兼容；不同时开发所有平台的特有能力。
- 页面和一级导航由 `pages.json` / tabBar 管理，不引入 Vue Router。
- Pinia 只管理跨页面应用状态；不提前建立全局领域数据仓库，也不使用持久化插件。
- API module 描述 endpoint contract；基于 `uni.request` 的 typed HTTP client 负责 transport、URL/query、2xx/204、网络错误和后端 ProblemDetail。
- 不引入 Axios，不增加通用 `Result` envelope；保留后端 HTTP status、ProblemDetail 与稳定业务 `code`。
- 错误边界为 API 抛 typed error、Store 转为应用状态、Page 决定呈现；HTTP client 不 Toast、不导航、不重试。
- `.env` 负责 API base URL 和 timeout；Vite 仅在 H5 开发期代理 `/api`、`/actuator`，不修改后端 CORS。
- 使用原生 uni-app 组件和轻量移动端样式，不引入大型 UI library、图片资产、Web font 或主题系统。
- TypeScript 启用 strict、noImplicitAny 与 noUncheckedIndexedAccess；ESLint、vue-tsc、Vitest、H5 build 和 mp-weixin build 构成验证门禁。
- Node 18 唯一真实 API slice 是 system health；Inventory、Recipe、Planning 与 execution UI 由 Node 19/20 纵向实现。
- 当前不实现 authentication，也不引入 OpenAPI codegen；Node 21 再决定浏览器 E2E 与发布验收工具。

## Reasons

- uni-app 在一个 Vue 3 代码库内支持 H5 与微信小程序，符合先可直接预览、再适配微信的产品路径。
- CLI/Vite 工程可由标准命令安装、检查和构建，不要求 HBuilderX 才能协作。
- npm 与官方模板一致，Node 24 和 lockfile 共同固定可复现的安装合同。
- `uni.request` 是跨 H5/小程序的原生边界，避免 Axios 适配层。
- 明确 Page、Store、API module、HTTP client 四层职责，可避免业务错误被多层重复处理。
- 先建立严格类型、测试和双目标编译门禁，能让后续业务页面按可验收纵向切片接入。

## Alternatives

1. uni-app 与纯 Vue H5。
2. uni-app CLI 与 HBuilderX-only 工程。
3. Vue 3 / Vite 与 Vue 2 / webpack。
4. `uni.request` 与 Axios。
5. Pinia 与全部只使用组件本地状态。
6. 原生 uni 组件与大型 UI library。
7. npm 与 pnpm / Yarn。
8. H5-first 与所有平台同时开发。
9. 手写小规模 typed client 与立即引入 OpenAPI codegen。

## Why alternatives were not chosen

- 纯 Vue H5 无法复用到微信小程序；HBuilderX-only 会降低命令行构建和代码审查的可复现性。
- Vue 2 / webpack 是旧基线，与当前官方 Vue 3 / Vite 模板及项目长期方向不符。
- Axios 对小程序不是原生 transport，会增加适配和行为差异。
- 仅用组件本地状态无法共享健康状态等应用级事实；反过来把所有未来领域数据放入 Pinia 又会形成过早的全局状态。
- 当前页面简单，大型 UI library 的体积、平台兼容与升级成本没有足够收益。
- 切换 pnpm/Yarn 没有当前工程收益，反而增加第二套包管理约定。
- 多平台同时开发会把平台差异提前带入基础节点；H5-first 仍通过 mp-weixin build 防止架构失配。
- 目前只有 health contract，OpenAPI codegen 的依赖、生成物与升级成本高于手写小类型的收益；接口稳定且规模扩大后再评估。

## Trade-offs

- uni-app 编译层带来 DCloud 发布序列约束，并可能产生上游工具链 warning；依赖必须保持同一序列并真实双目标构建。
- 手写 typed API module 需要维护与后端契约的一致性，但当前 API 面积极小且更易审查。
- H5 开发代理不能替代微信小程序的合法域名配置；当前只承诺 mp-weixin 编译兼容。
- 没有 UI library 会增加后续基础组件工作，但避免在需求形成前锁定视觉体系。
- Node 18 没有业务页面、认证、E2E 或发布流程；这些缺口是明确的节点边界，不由假数据填补。

## Revisit Conditions

- 后端 API 数量和稳定性足以证明手写类型同步成本过高时，评估 OpenAPI codegen。
- Node 21 需要跨浏览器或发布级交互回归时，单独决策 E2E 工具。
- 微信正式发布时，补充合法域名、平台权限和发布流水线决策。
- 原生 uni 组件无法满足已确认的复杂交互且自建成本有测量证据时，评估小型跨端组件库。
- 出现真实认证需求时，按认证 ADR 增加 token 生命周期和前端边界，不在 HTTP client 中预埋。
