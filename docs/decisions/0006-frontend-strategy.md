# 0006：前端策略

## Status

- 状态：已接受
- 决策日期：2026-08-03
- 适用阶段：V1 H5 与后续微信小程序适配

## Background

MealOps 的主要使用场景发生在移动设备上，包括查看库存、安排未来餐次、购物和做饭确认。V1 需要一个容易在浏览器调试和演示的移动端界面，同时为后续微信小程序保留复用路径。

前端不是项目的主要技术展示目标，因此应控制框架数量和跨端适配复杂度，避免 H5 与小程序分别维护两套实现。

## Decision

- 使用 uni-app CLI/Vite 工程；
- 使用 Vue 3 + TypeScript；
- 使用 Pinia 管理跨页面共享状态；
- 第一运行目标为移动端优先 H5；
- 第二运行目标为微信小程序；
- HTTP Client 封装 `uni.request`，统一处理 base URL、Bearer Token、超时、RFC 9457 Problem Details 和取消等跨端行为；
- 不引入 Axios；
- 暂不选择大型 UI 组件库；
- 平台差异必须被限制在明确的适配层或条件编译边界，领域与 API 类型不得复制两套；
- 本节点只确定策略，不创建前端工程或依赖。

## Reasons

### uni-app + Vue 3 + TypeScript

- uni-app 官方支持 Web/H5 与微信小程序等多个目标；
- Vue 3 小程序编译使用 Vite，符合 CLI 开发和现代构建需求；
- Vue 3 能以较低前端认知成本支持移动端交互；
- TypeScript 有助于保持 OpenAPI 契约、页面状态和 Problem Details 处理的一致性。

### H5 first

- 浏览器调试、自动化测试和演示路径更直接；
- 不依赖微信登录、开发者资质、审核或开发者工具即可验证闭环；
- 先稳定 API 和交互，再处理小程序平台限制，降低同时调试两个目标的成本。

### Pinia

- 是 Vue 生态当前推荐的状态管理方案；
- TypeScript 类型推断、DevTools、测试和模块化 store 能力满足项目需要；
- 用于认证态、用户偏好、活动计划和购物清单等真正跨页面状态，不把所有服务端数据无差别放入全局 store。

### `uni.request`

- uni-app 提供跨 H5 与小程序的官方请求 API；
- 避免 Axios 在小程序环境中的额外适配层；
- 统一封装仍可向业务层提供 Promise、类型和错误归一化接口。

### 暂不选择 UI Framework

- 当前尚未形成稳定页面和组件需求；
- 大型组件库会影响包体、主题、样式覆盖和小程序兼容；
- 先用基础组件验证设计语言，再根据重复模式做有证据的选择。

## Alternatives

| 决策 | 当前选择 | 替代方案 | 替代方案优势 |
| --- | --- | --- | --- |
| 跨端框架 | uni-app | 微信原生小程序 | 微信平台适配和原生能力最直接 |
| 跨端框架 | uni-app | Taro | React 生态成熟，适合 React 团队 |
| 语言 | Vue 3 + TypeScript | Vue 3 + JavaScript | 初期代码更少、无类型配置 |
| 首发平台 | H5 first | 微信小程序 first | 更接近微信用户入口 |
| HTTP | 封装 `uni.request` | Axios | Web 生态成熟、拦截器与适配器丰富 |
| 状态 | Pinia | 仅 composable/local state | 依赖更少、简单页面足够 |
| UI | 暂不选择 | uni-ui 或其他组件库 | 可快速获得完整组件和视觉一致性 |

## Why alternatives were not chosen

### 微信原生小程序

原生方案会把首版演示绑定微信开发者工具，并使 H5 需要第二套实现。微信专属能力不属于 V1，因此不值得牺牲浏览器交付路径。

### Taro

Taro 是成熟选择，但更适合 React 技术栈。MealOps 的主要能力展示是 Java 后端与规划系统，Vue 3 + uni-app 能降低前端认知和交付成本。

### JavaScript

API、状态和跨端差异较多，缺少静态类型会增加字段漂移和错误处理不一致风险。

### 微信小程序 first

会提前引入登录、域名白名单、平台审核和真机调试等与当前核心闭环无关的约束。

### Axios

Axios 在 H5 很自然，但微信小程序不是标准浏览器环境。uni-app 已提供跨平台请求 API，再引入 Axios 会增加依赖和适配工作。

### 只使用局部状态

对简单页面足够，但认证态、活动计划和购物清单会跨多个页面。Pinia 提供明确、可测试的共享状态边界。

### 提前选择 UI Framework

尚无真实页面证明所需组件集合。提前选择可能导致大量主题覆盖和未使用依赖，也违反按当前节点最小引入依赖的规则。

## Trade-offs

- uni-app 不能保证所有 Vue/Web 能力在微信小程序完全一致，必须持续验证两种目标构建；
- 跨端抽象可能限制某些平台原生体验，需要受控的平台适配；
- H5 首发意味着首版没有微信生态入口；
- 自建 HTTP wrapper 需要维护类型、取消、超时和错误映射；
- 暂无 UI 组件库会增加首批基础交互组件的工作量；
- CLI 模板、uni-app 精确版本、前端测试工具和 UI 规范为“待确认”，应在前端工程节点确定。

## Revisit Conditions

满足以下任一条件时重新评估：

- 微信小程序成为唯一主要渠道，跨端复用不再有价值；
- uni-app 对关键 API、性能或组件行为存在无法接受的限制；
- 团队技术栈转为 React，Taro 能显著降低维护成本；
- H5 与小程序的平台差异代码超过可接受范围；
- 页面出现稳定且大量重复的复杂组件需求，需要评估 UI Framework；
- `uni.request` 无法满足经过验证的网络能力需求。

## References

- [uni-app Vue 3 Support](https://uniapp.dcloud.net.cn/tutorial/vue3-basics.html)
- [uni.request Documentation](https://uniapp.dcloud.net.cn/api/request/request.html)
- [Pinia Introduction](https://pinia.vuejs.org/introduction.html)
