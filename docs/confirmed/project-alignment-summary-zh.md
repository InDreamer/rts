<!-- docmeta
role: leaf
layer: 3
parent: AGENTS.md
children: []
summary: 中文项目总览，统一项目定义、治理边界、OV 采用边界、运行时投影、检索分层、当前状态与推荐阅读顺序
read_when:
  - 需要一次性对齐当前项目文档的关键结论
  - 需要先建立项目全局认知，再进入 constitution、boundary、retrieval 等叶子文档
  - 需要区分 canonical pack、runtime projection 与 OV retrieval shell
skip_when:
  - 已经明确只需要某一篇叶子文档的细节
source_of_truth:
  - docs/reference/handoff/HANDOFF-PROMPT.md
  - docs/confirmed/system-constitution-v1.md
  - docs/reference/ov-boundary-and-adoption.md
  - docs/reference/minimal-ov-integration.md
  - docs/reference/ov-kb-retrieval-design.md
  - docs/archive/discussions/session-handoff-2026-04-16.md
-->

# 项目对齐总览

## 这篇文档解决什么问题

仓库里已经有完整交接稿、治理原则、OV 边界、最小集成方案、检索设计和 session handoff，但这些内容分散在多篇文档里，阅读顺序不对时很容易混淆三个层面：

- 什么是项目的 canonical truth source
- 什么是 signoff 之后给 agent 用的 runtime projection
- OpenViking 在这个项目里到底扮演什么角色

这篇文档不是替代原文，而是把已经稳定下来的共识收敛成一份可快速对齐的总览。

## 一句话定义项目

Transformation Rule System 是一个面向银行报文转换场景的规则真相源系统。它的核心目标不是“让 LLM 更会编”，而是把已审核规则整理成可治理、可追溯、可检索、可解释的结构化真相源，并把 LLM 限制为受约束的读取者、导航者和解释者。

## 项目到底在解决什么

项目当前聚焦 `source_system -> target_system` 的报文转换规则治理，首批样本是 `Tradition -> Stella`。

稳定目标有五个：

- 用 YAML 承载 canonical rule，而不是把真相散落在聊天、代码片段或临时笔记里
- 用显式的 evidence、review、signoff 机制建立可信边界
- 让 AI 能回答“这个 target 字段从哪里来、为什么这么生成、依赖哪个 lookup/helper”
- 在运行时只暴露低噪声、高确定性的已批准内容
- 用结构化检索降低 hallucination，而不是依赖 prompt 口头约束

## 当前已经稳定的总原则

### 1. Pack completion establishes truth

一个 pack 一旦通过既定 review 和 signoff 边界，它就是该范围内的 goldensource。

### 2. Canonical truth lives in governed pack objects

真相源在 pack 内的治理对象里，不在 chat transcript、agent memory、临时分析笔记或运行时 session 里。

### 3. LLM reads truth, but does not invent truth

LLM 可以定位对象、总结规则、解释规则、指出不确定性；不能补业务逻辑、不能替缺失证据脑补答案、不能把 ambiguity 静默改写成 certainty。

### 4. Unknown is better than wrong

证据不足时，系统应该返回“不足以判断”或“存在歧义”，而不是输出流畅但站不住脚的答案。

### 5. Anti-hallucination is mainly a structure problem

降低幻觉的首要手段不是“要求模型更小心”，而是限制它看到什么、按什么顺序看到、以及上下文是否处于正确边界内。

## 必须区分的三层边界

### 1. Canonical pack: 治理真相源

这是项目真正的 court of record。

canonical pack 的职责包括：

- 规则主对象建模
- evidence 追溯
- review 与 open items 管理
- ambiguity 显式表达
- signoff 与 approval 状态管理

按照原始 handoff 约定，一个 canonical pack 通常包含：

- `rules/`
- `lookups/`
- `helpers/`
- `evidence/`
- `review/`
- `reports/`

这里的四层分离是治理结构，不是运行时检索结构。

### 2. Runtime projection: 给 agent 的只读投影层

按 2026-04-16 后的设计收敛，进入运行时检索面的第一阶段内容必须比 canonical pack 更窄。

第一阶段只投影：

- `rules`
- `lookups`
- `helpers`

第一阶段不投影：

- `evidence`
- `review`
- `reports`

原因很明确：signoff 之前需要回答“为什么它可信”；signoff 之后运行时更关心“如何高效、安全地使用这条真相”。

### 3. OV retrieval shell: 检索与上下文加载层

OpenViking 在这个项目里的定位已经明确：

- 它不拥有规则真相
- 它不决定什么算 authoritative
- 它不负责 review 和 signoff
- 它不应该把 runtime memory 写回 canonical rules

它真正适合做的是：

- 给已确认真相提供稳定地址
- 提供分层摘要与导航
- 提供低噪声检索入口
- 在需要时暴露轻量依赖关系

可以把关系理解成：

- TRS 决定什么是真的
- OV 决定 agent 如何更容易找到并读取这些真相

## Canonical 对象模型的稳定共识

项目当前主对象分三类：

- `target_rule`
- `lookup_definition`
- `helper_definition`

主对象应保留的核心字段是：

- `id`
- `source`
- `logic`
- `target`
- `dependencies`
- `examples`

治理状态如 `status`、`signoff_status` 可以存在于 canonical 层，但 runtime projection 会进一步裁剪，只保留 agent 真正需要的运行时字段。

主对象不应该吸收：

- 长 trace
- 审批过程细节
- review 讨论正文
- evidence 原文堆叠

这些内容应该待在治理侧，而不是污染主对象。

## OV 采用边界的稳定结论

### 应该借的能力

- URI-like addressing
- layered reading
- resource ingestion and indexing
- directory-scoped retrieval
- 必要时的轻量依赖关系

### 要谨慎使用的能力

- vector search：必须带 scope 约束，避免跨 channel 或跨产品线污染
- 自动摘要：对治理敏感内容不能直接放行，至少要 LLM-assisted + human-confirmed

### 第一阶段明确不要碰的能力

- session memory extraction
- runtime learning 写回 truth
- bidirectional sync
- 把 OV 当作 canonical rule owner
- 为第一版引入重型 relation graph 或深度 OV core 改造

## 检索架构的稳定共识

### L0 / L1 / L2 不是目录层级

这是整个文档集中最容易误解的一点。

L0、L1、L2 是同一资源的三种精度视图，不是三层文件夹。每个目录节点都可以有自己的 L0/L1。

### 三层的职责

- `L0`：短摘要，负责 recall 过滤
- `L1`：结构化 overview，负责 rerank 和导航
- `L2`：最终原始对象，负责回答问题

### 推荐加载顺序

先缩小范围，再读原文：

1. 先找正确的 channel
2. 再找正确的 product
3. 再找正确的 pack
4. 再定位 rule / lookup / helper
5. 最后才加载 YAML 原文

这背后的核心不是“省 token”本身，而是避免模型在错误范围里看到过多相似但不相关的规则。

### 为什么 L1 很关键

L0 决定 recall 是否过宽，L1 决定 rerank 是否靠谱。

如果 L1 质量差，系统即使能搜到相关 pack，也很难把误召回的候选过滤掉；如果 L1 质量高，agent 能在不读取全部 YAML 的情况下就知道：

- 这个 pack 覆盖哪些对象
- 每个对象大致负责什么
- 是否依赖 lookup/helper
- 当前问题是不是根本不在这个 pack 里

## 多系统扩展时的结构原则

项目未来不是单一 `Tradition -> Stella` 通道，而是可能扩展到多个 source-target channel。

这时最重要的结构原则是：

- channel 必须是物理顶层分区
- 各 channel 结构应尽量同构
- 检索必须优先受 channel scope 约束

这不只是目录整齐问题，而是反幻觉设计的一部分。相似产品线或相似规则如果混在一起，agent 很容易引用到错误系统的规则。

## 当前项目状态快照

根据 handoff 文档，2026-04-16 时的项目状态是：

- 总计 18 个 packs
- 18 个 packs 已完成 authoring
- 9 个 packs 已完成 review
- 已 review 的主要是 `COMMON` 和 `FXD_NDF`
- `FXO` 方向还有 9 个 packs 待 review

因此，当前项目不是“从零搭设计”，而是：

- canonical pack 治理方法已经成形
- OV 边界和 retrieval 架构已经完成一轮收敛
- 后续重点会逐步转向 runtime projection、检索模板和 FXO review 补齐

## 文档之间该怎么读

如果你第一次进入这个仓库，推荐阅读顺序如下：

1. 先读本文，对齐全局边界和术语
2. 再读 `system-constitution-v1.md`，确认治理底线
3. 再读 `ov-boundary-and-adoption.md`，确认 TRS 与 OV 的责任分界
4. 再读 `ov-kb-retrieval-design.md`，进入具体检索架构
5. 如果要落地工程，再读 `minimal-ov-integration.md`
6. 如果要追溯为什么会得出这些结论，再读 `ov-kb-discussion-synthesis-2026-04-16.md`
7. 如果只是准备开启下一轮对话，读 `session-handoff-2026-04-16.md`

如果你需要历史性全量背景，再回头读 `HANDOFF-PROMPT.md`。它覆盖项目出身、pack 工作流、样本材料和 2026-04-16 时的执行快照，但它不是后续 OV 边界收敛的唯一来源。

## 最需要避免的三种误读

### 1. 把 canonical pack 和 runtime projection 混为一谈

治理结构比运行时结构更宽。不能因为 canonical pack 有 evidence/review/reports，就把这些全部塞进第一阶段 retrieval shell。

### 2. 把 OV 当成 truth owner

OV 在这个项目里是 librarian，不是 court of record。它帮助 agent 找真相，不负责定义真相。

### 3. 把“AI 可参与生成”误解为“AI 可直接认证真相”

AI 可以帮助生成候选结构、摘要和运行时投影，但 final truth 仍然依赖治理链条而不是模型自证。

## 现在最清晰的下一步

如果继续推进文档或工程，优先级最清楚的工作有三类：

- 固化 L0 / L1 模板规范
- 设计 canonical pack 到 runtime projection 的适配器
- 补齐 FXO packs 的 review，使治理侧覆盖闭环

## 执行摘要

这个项目的核心已经不是“要不要用 OV”，而是“如何在不污染真相源的前提下，把已 signoff 的规则投影成低噪声、可分层检索、可稳定寻址的 agent runtime surface”。TRS 负责 truth governance，OV 负责 retrieval mechanics，二者是分层协作而不是系统合并。只要这个边界不被打破，后续无论是继续补 review、做投影适配器，还是落地 L0/L1/L2 检索，都有明确的设计基线。
