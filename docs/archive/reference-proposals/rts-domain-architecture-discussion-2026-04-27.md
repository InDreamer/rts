# RTS 领域架构讨论记录 — 2026-04-27

## 1. 当前定位

RTS（Transformation Rule System，也与既有 TRS 文档语义一致）不应被定位为单一 AI use case、聊天机器人或一次性 RAG 应用。

当前更准确的定位是：

> RTS 是面向银行报文转换场景的受治理 AI 语义基座 / truth-based knowledge foundation。

它把分散在 XML、mapping、Java/XSLT、lookup、evidence、review、signoff 中的报文转换知识，抽象成可治理、可寻址、可检索、可解释的规则对象。

核心架构不变量：

> RTS 决定什么是真的；索引层决定如何找到真相；AI 解释真相，但不创造真相。

## 2. 为什么不是单个 use case

RTS 不应被讲成“规则问答”“排障助手”或“知识库聊天机器人”。这些说法容易被 Copilot、Yoda 或普通知识库问答替代。

更有说服力的叙事是：

```text
受治理规则真相层
  -> 运行时投影
  -> 索引与范围感知检索
  -> API 化 AI 消费
  -> 多个业务 / 交付 / 治理 use cases
```

也就是说，RTS 是 use-case generator，不是一个 use case。

## 3. 建议的 use case 分层

### P0 — 基座能力

优先验证 RTS 作为语义基座是否成立：

- 规则解释
- 字段血缘 / Source-to-Target 追踪
- 规则依赖导航
- 范围感知检索
- 歧义 / 签核感知回答
- 证据支撑解释

### P1 — 交付与治理流程

证明 RTS 能进入真实交付链路：

- 规则驱动影响分析
- 回归测试推荐
- 发布准备度检查
- 规则驱动 incident 解释
- 变更意图校验

### P2 — 业务侧应用

用于拉业务赞助和拓展需求：

- 客户 / 上游接入加速
- 新产品 / 新交易流准备度评估
- 客户影响分析
- 监管 / 审计解释层
- 组织知识恢复

### Future — 后续扩展

适合作为 future opportunities，而非第一阶段主线：

- 知识过期检测
- 报文质量雷达
- 值班交接
- 隐性依赖发现
- 运营摩擦分析

## 4. Copilot / Yoda / 普通 RAG 的边界

### Copilot

Copilot 是个人生产力助手。它可以帮助工程师或分析师读取代码、文档、mapping、样例，并生成候选规则草稿。

但 Copilot 不应被视为规则真相层，因为它不拥有：

- 规范规则对象模型
- evidence 绑定
- ambiguity 状态
- review / signoff 生命周期
- runtime projection 控制
- 稳定索引和检索轨迹
- workflow API 消费能力

一句话：

> Copilot 可以是 authoring assistant，但 RTS 必须是 system of record。

或者：

> Copilot 帮助生成候选知识；RTS 决定哪些知识成为可信真相。

### Yoda / Confluence

Yoda 适合 Confluence 页面问答，但 RTS 需要处理的不只是文档页面，还包括：

- XML / FpML / SCBML 样本
- mapping extract
- Java / XSLT 逻辑
- lookup 依赖
- rule object
- review / signoff 状态
- defect / incident / release evidence

Yoda 可以消费或展示 RTS 产出的解释，但不能成为 governed rule truth layer。

### 普通 RAG

普通 RAG 可以检索文本，但不能天然提供：

- 规则真相状态
- 签核边界
- 字段血缘
- 依赖图
- scope-aware retrieval
- 固定输出 schema
- workflow audit trace

RTS 补齐的是这些现有工具没有的治理和结构层。

## 5. 为什么需要 API 化 AI 服务接入

讨论中明确区分了两件事：

```text
Copilot / 人工 prompt 可以辅助 authoring
API 化 RTS 支撑的是 governed consumption
```

如果目标只是一次性把文档整理成格式化知识，Copilot + skill 可能够用。

但如果目标是建设一个可长期复用、可审计、可签核、可投影、可索引、可被多个业务流程调用的规则真相基座，就需要 RTS 和 API 化能力。

API token / API 接入的真正理由不是“再做一个聊天入口”，而是：

- 嵌入 onboarding / UAT / release / incident / audit 等后台流程
- 接收结构化输入：XML、mapping extract、rule object、defect list、metadata
- 输出固定 schema：JSON、checklist、assessment、traceable summary
- 记录 input source、prompt version、model version、output、reviewer、evidence links
- 支持 scope enforcement、脱敏、访问控制、human-in-the-loop
- 让多个 workflow consumer 复用同一套 RTS 基座

## 6. 运行时投影边界

根据 RTS confirmed docs，第一阶段 runtime projection 只投影：

- rules
- lookups
- helpers

不投影：

- evidence
- review
- reports

因此 PDF 中采用的边界表达是：

> 运行时默认只暴露已批准的规则、查表逻辑和辅助逻辑；证据、评审和报告保持在治理侧，并通过受控引用支持审计和解释。

## 7. 业务价值表达

RTS 的优先价值不是让 AI 回答更多问题，而是：

- 缩短规则理解周期
- 缩短客户 / 上游接入周期
- 降低发布回归风险
- 提升审计准备度
- 减少对少数 SME 口头知识的依赖
- 提升客户影响可见性

可用于 pilot 的指标包括：

- 正确规则包 / 规则对象命中率
- 回答可追溯率
- 范围误召回率
- 专家复核时间下降
- 测试推荐采纳率
- 审计轨迹完整率

## 8. 本次已生成材料

最新版 PDF：

```text
out/rts-domain-architecture-proposal-cn-v2.pdf
```

同步到仓库引用路径：

```text
docs/reference/proposals/rts-domain-architecture-proposal-cn-v2.pdf
```

生成脚本：

```text
tools/generate_rts_domain_architecture_pdf_cn_v2.py
```

对应 Markdown 摘要：

```text
docs/reference/proposals/rts-domain-architecture-proposal-cn-v2.md
```
