<!-- docmeta
role: leaf
layer: 3
parent: docs/confirmed/README.md
children: []
summary: PM-facing runtime projection product guide explaining what each runtime package area does without replacing the technical projection contract
read_when:
  - 需要从产品、PM、集成方或 AI agent 视角解释 runtime projection 是什么
  - 需要理解 active release、manifest、scope、L2、navigation、dependency、governance、permission、index 和 trace 各自解决什么问题
  - 需要向非底层实现读者说明 sample-projection/runtime-store 的目录含义
skip_when:
  - 需要精确字段、schema、admission rule、query resolver 读取约束
  - 需要修改 projection publisher 或 query/index 层实现
source_of_truth:
  - docs/confirmed/project-alignment-summary-zh.md
  - docs/confirmed/kb-to-index-projection-contract-zh.md
  - docs/confirmed/llm-harness-and-agent-integration-alignment-zh.md
-->

# Runtime Projection Product Guide

> 状态：confirmed explanatory guide  
> 创建日期：2026-05-15  
> 范围：用产品和 PM 视角解释 runtime projection 运行包的组成、用途和边界  
> 非目标：不替代 `kb-to-index-projection-contract-zh.md` 的字段契约、schema 约束和发布规则

## 1. 一句话定义

`runtime projection` 是 RTS 把已批准的规则真相发布给运行时服务使用的产品化运行包。

它不是 KB 原文，不是索引文档，不是 HTML 展示页，也不是 LLM 的回答素材拼接。它的产品职责是让 RTS service 能在明确 release、scope、permission、hash、dependency、governance 和 trace 边界内，对外提供查询、解释、影响分析、测试规划、agent tool 和 managed analysis。

从 PM 视角可以这样理解：

```text
KB 负责真相形成和治理
runtime projection 负责把 approved truth 发布成可服务运行包
RTS service 负责受控读取、校验、追踪和拒答
AI/agent 负责在这些边界内做分析和表达
```

## 2. 它解决什么产品问题

Runtime projection 解决的不是“文件怎么存”这个问题，而是以下产品问题：

- 当前线上服务应该使用哪个规则版本。
- 用户或 agent 的问题属于哪个业务 scope。
- 哪些 rule、lookup、helper 已经 released，可以被默认服务读取。
- 最终事实应该从哪里读取，如何证明没有被搜索结果或 LLM 编造。
- 如何低噪声找到正确规则，而不是把相似规则混进来。
- 如何解释规则依赖、影响面和测试候选。
- 哪些 evidence、review、report、open question 可以在授权时被展示。
- 谁能访问哪个 pack、entrypoint 和输出模式。
- 每次回答读了哪些对象、是否 ground 到 L2、为什么拒答或降级。
- 如果当前 release 出问题，如何定位 rollback target。

## 3. 产品心智模型

Runtime projection 可以被看成一个 release package，里面每类文件承担一个产品角色：

| 区域 | 产品角色 | 主要回答的问题 |
|---|---|---|
| active release | 当前服务指针 | 现在运行哪一版？回滚到哪一版？ |
| release manifest | 发布身份证 | 这版从哪里来？能不能用？有没有 blocking issue？ |
| scope registry | 业务边界 | 这个问题属于哪个 channel/product/pack/domain？ |
| object manifest | 对象目录 | 这版有哪些 released rule/lookup/helper？ |
| L2 runtime objects | 事实正文 | 规则到底怎么做？lookup/helper 怎么定义？ |
| navigation views | 查找和消歧 | 用户不知道精确 id 时怎么找到正确对象？ |
| dependencies / bindings | 关系和影响 | 规则依赖谁？字段从哪里来？改动影响哪里？ |
| governance summaries | 可信度和风险 | 为什么可信？有什么 warning、open question、production gate？ |
| caller profiles | 权限边界 | 谁能看哪些 pack、调用哪些入口？ |
| index artifacts | 检索加速 | 怎么更快召回候选？ |
| traces | 审计复盘 | 这次回答读了什么、为什么这么答或拒答？ |

这些角色共同组成 RTS 的 runtime truth access boundary。任何单独一类文件都不应该被误读为完整真相服务。

## 4. 每类内容做什么

### 4.1 `active-release.json`

这是当前服务应该使用的 release 指针。

它通常包含：

- active release id
- rollback target release id
- updated at / by

PM 视角：这是发布开关。它让 RTS 明确“当前线上回答基于哪版规则”，并保留出现问题时回滚到哪一版的产品路径。

### 4.2 `release-manifest.json`

这是某个 runtime projection release 的身份证。

它说明：

- release id
- canonical revision
- projection / card / summary schema version
- generated / released 时间
- activation state
- rollback target
- content hash summary
- blocking issues count

PM 视角：这是上线可用性证明。没有有效 manifest，就没有可用 runtime truth。

### 4.3 `scopes.jsonl`

这是业务范围注册表。

它定义服务能进入哪些业务区域，例如：

```text
channel / product / pack / domain
```

PM 视角：这是防串线机制。它防止用户问某个 product 或 pack 时，系统误拿另一个相似 product、相似 lookup 或相似字段的规则回答。

### 4.4 `object-manifest.jsonl`

这是 released 对象目录。

它列出每个可服务对象：

- stable URI
- object id
- object type: rule / lookup / helper
- scope
- target path / source anchors
- content hash
- card ref
- L2 content ref
- schema version
- release state

PM 视角：这是“本 release 提供哪些规则能力”的产品目录。确定性查找应优先从这里开始，而不是从全文搜索开始。

### 4.5 `l2/`

`l2/rules/*.json`、`l2/lookups/*.json`、`l2/helpers/*.json` 是 runtime projection 中最重要的事实正文。

它们保存服务可读的结构化规则事实，例如：

- inputs
- source path
- logic pipeline
- target path
- constants / conditions / fallback
- lookup/helper/rule dependencies
- examples
- warnings
- review state
- content hash

PM 视角：这是 RTS 回答事实时必须回到的地方。搜索命中、card、HTML 展示、LLM summary 都不能替代 L2。

### 4.6 `navigation/`

`navigation/` 是为了找对象，不是为了承载最终事实。

常见内容：

- `object-cards.jsonl`：对象卡片，用于定位、消歧、初步排序。
- `l0-l1-views.jsonl`：低成本过滤和结构化导航视图。
- `aliases.jsonl`：业务词、同义词、简称和别名。
- `confusables.jsonl`：相似但不应混用的对象或反例候选。

PM 视角：这是用户和 AI 的低噪声入口。它让调用方不用知道精确 rule id，也能找到正确对象；但找到以后必须回读 L2。

### 4.7 `dependencies/`

`dependencies/` 描述对象关系和字段绑定。

常见内容：

- `dependency-edges.jsonl`：rule -> lookup、rule -> helper、rule -> rule 等依赖关系。
- `field-bindings.jsonl`：source field、target field、output field、via object 的绑定关系。

PM 视角：这是影响分析、解释链和测试规划的基础。用户问“改这个 lookup 会影响哪些规则？”或“这个 target 字段从哪里来？”时，需要这里的信息。

### 4.8 `governance/`

`governance/` 是受权限控制的可信度和治理解释视图。

常见内容：

- `governance-access-refs.jsonl`：每个对象有哪些 evidence/review/report summary 可访问，以及 redaction/access 状态。
- `evidence-summaries/*.json`：证据摘要。
- `review-summaries/*.json`：review 结论、open questions、production gate。
- `report-summaries/*.json`：pack 或 release 级别总结。

PM 视角：这是“为什么可信、哪里有风险、哪些仍需确认”的产品解释层。普通 operational answer 不应默认混入大量治理噪声；治理、审计或 AI review 场景可以在授权后读取。

### 4.9 `caller-profiles.jsonl`

这是运行时 caller 权限配置。

它定义：

- caller id
- api key hash
- allowed channels / products / packs
- allowed entrypoints
- allowed output modes
- active flag

PM 视角：这是产品访问控制。不同用户、pipeline、agent 或 admin 不应天然拥有同样的规则和治理视图权限。

### 4.10 `index-artifacts/`

这是 projection 派生出的索引副本或本地索引文件。

例如：

- `opensearch-docs.jsonl`
- Lucene index files

PM 视角：这是搜索性能和召回体验层。它可以帮助更快找到候选，但不能成为 final truth source。

### 4.11 `traces/`

`traces/` 保存运行审计记录。

常见内容：

- `query-trace.jsonl`
- `llm-run-trace.jsonl`

它记录：

- query plan
- resolved scope
- candidate / selected URIs
- L2 read URIs
- refusal reason
- tool calls
- grounding map
- budget usage
- model run hash

PM 视角：这是可解释、可复盘、可审计。用户质疑回答、生产排障、模型输出验证和权限审计都需要 trace。

## 5. 它不是什么

Runtime projection 不应该被理解为：

- 不是 KB 的完整治理图。
- 不是 candidate pack。
- 不是 raw evidence bundle。
- 不是人工或 AI 的审阅草稿。
- 不是搜索引擎索引。
- 不是 HTML/dashboard 展示页。
- 不是 LLM memory。
- 不是 query trace 反向写回 truth 的通道。

如果需要修改规则真相，应回到 canonical KB / governance / signoff / release process，再发布新的 projection。

## 6. AI 和 agent 应该如何使用它

AI/agent 不应该直接遍历 projection 文件并自行判断 truth。正确产品路径是：

```text
caller / agent
  -> RTS API / MCP / managed harness
  -> resolve scope
  -> find object by manifest/card/navigation
  -> read L2 and allowed dependency/governance view
  -> produce grounded facts / inferences / unknowns / candidates
  -> emit trace
```

AI 可以阅读和分析 projection 材料，但事实 claim 必须 ground 到：

- L2 runtime object
- dependency edge / field binding
- authorized governance summary
- trace 中已记录的读取结果

AI 输出仍然不是 truth owner。

## 7. 与技术契约的关系

本文只解释产品含义和运行包角色。

如果需要决定以下问题，以 `kb-to-index-projection-contract-zh.md` 为准：

- projection publisher 必须产出哪些结构。
- 每类对象必须有哪些字段。
- admission rule 如何判断对象是否能进入 projection。
- query/index 层只能读取哪些内容。
- L0/L1/L2 的精确定义。
- refusal/degraded output 的触发条件。
- storage mapping 和 MVP cut。

一句话：

```text
本 guide 解释“为什么这些东西存在、产品上解决什么问题”。
projection contract 决定“这些东西必须长什么样、服务怎么安全读取”。
```

## 8. PM 检查清单

评审一个 runtime projection release 时，PM 或产品 owner 可以先问：

- 当前 active release 是哪一版？rollback target 是哪一版？
- release manifest 是否显示 blocking issue 为 0？
- scope 是否足够明确，是否可能与相似 product/pack 串线？
- object manifest 是否覆盖本次要服务的 rule/lookup/helper？
- 关键事实是否能回到 L2，而不是只存在 card 或搜索文本里？
- dependency 和 field binding 是否足够支持影响分析和测试规划？
- governance summary 是否明确 warning、open question 和 production gate？
- caller profile 是否符合预期用户/agent/pipeline 权限？
- index artifacts 是否只是加速层，没有被当成 truth？
- trace 是否能复盘一次回答读了哪些对象、基于哪个 release、是否有 L2 hash？

如果这些问题答不清，说明 release 包还不是一个健康的 runtime projection 产品运行包。
