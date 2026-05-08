<!-- docmeta
role: leaf
layer: 3
parent: docs/confirmed/README.md
children: []
summary: Defines the governed runtime projection contract between the RTS Knowledge Base truth layer and the query/index layer
read_when:
  - 需要定义 KB 发布后必须产出什么给索引层
  - 需要判断索引层可以读取哪些对象、不能读取哪些对象
  - 需要设计 query resolver、PostgreSQL projection、OpenSearch 索引或 MCP/API 查询边界
  - 需要区分 canonical truth、release pack、runtime projection、search hit 和 LLM answer
skip_when:
  - 只需要理解 RTS 总体愿景
  - 只需要查看历史 OpenViking 采用边界
  - 只需要实现某个具体数据库表字段
source_of_truth:
  - docs/confirmed/project-alignment-summary-zh.md
  - docs/confirmed/system-constitution-v1.md
-->

# KB-to-Index Runtime Projection Contract

> 状态：confirmed baseline
> 创建日期：2026-05-05
> 范围：定义 RTS Knowledge Base 与查询/索引层之间的运行时投影契约

## 1. 命名和定位

这里建议使用 **Runtime Projection Contract**，中文可以叫 **运行时投影契约**。

如果强调两层边界，也可以叫 **KB-to-Index Projection Contract**。

不建议第一阶段叫 protocol。Protocol 更像网络通信协议或跨系统 wire format。当前真正需要固定的是：

- KB 发布后必须产出哪些结构化投影
- 索引层只允许读取哪些投影
- 索引层如何解释这些投影
- 哪些内容绝不能由索引层自行推断为 truth

因此它首先是 **layer contract**，后续如果暴露成 API、事件或文件包，再定义具体 protocol。

## 2. 核心原则

RTS 的 Knowledge Base 决定什么是 governed truth。查询/索引层只负责让 governed truth 被稳定、低噪声、可追踪地找到。

KB、projection、index documents 都是机器优先结构，不是“人读文档”和“机器读文档”的区别。

更准确的边界是：

- KB 是完整 governed truth graph，包含规则逻辑和治理上下文。
- Runtime projection 是 approved truth 的服务运行视图，面向查询、索引、诊断、影响分析、测试规划和 agent 工具。
- Index documents 是 projection 派生出的召回和导航视图。
- Governance views 是 projection 中受权限控制的 evidence / review / report / adjudication 展开视图。

Projection 可以裁剪治理噪声，但不能把规则真相降级成摘要文本。服务回答事实所需的结构化 L2 语义必须保留在 projection 内。

稳定原则：

- KB owns truth。
- Release owns runtime admissibility。
- Projection owns index-readable shape。
- L2 runtime object owns service-readable structured rule content。
- Index owns retrieval and trace。
- Query resolver owns query plan。
- LLM answer never owns truth。

索引层不能直接读取 candidate pack，也不能把 search hit、LLM summary、agent memory 或 query trace 当成新的规则真相。

## 3. 上游输入和下游读取边界

### 3.1 KB / Truth Layer 必须管理

KB 基座负责管理完整 governed truth graph：

- source material
- candidate pack
- canonical pack
- canonical rule / lookup / helper object logic
- evidence
- review / ambiguity / adjudication
- signoff
- release pack
- truth version and rollback lineage

这些内容属于 truth governance surface。索引层默认不直接消费 canonical authoring files；它应该通过 projection 的 operational view、governance-authorized view 和 index view 读取。

### 3.2 Projection Publisher 必须产出

当一个 pack 或 release 被批准后，KB 侧必须生成一个不可变 runtime projection。

这个 projection 是索引层的唯一默认输入。

Projection publisher 至少要产出：

- `release_manifest`
- `scope_registry`
- `object_manifest`
- `rule_card` / `object_card`
- `dependency_edges`
- `runtime_content_refs`
- structured `L2 runtime objects`
- `l0_l1_navigation_views`
- optional governance access views

这些输出可以是 YAML、JSON、数据库 staging tables 或对象存储文件。第一阶段不强制文件格式，但必须有稳定 schema、版本、hash 和 release identity。

### 3.3 Index / Query Layer 只能读取

普通运行时查询只能读取 projection 内已发布并通过权限过滤的视图：

- active runtime projection
- projection manifest
- projected rule / lookup / helper L2 content
- projected rule cards and manifests
- dependency edges
- L0/L1 navigation views
- permission-filtered governance summary, if explicitly authorized

普通运行时查询不能把以下内容当成默认 operational truth 输入：

- candidate pack
- unapproved canonical pack
- raw review notes
- raw evidence bundle
- extraction report
- agent scratchpad
- chat history or memory
- Git working tree as runtime truth
- OpenSearch document as final truth source

如果 governance view 需要 evidence、review 或 report，必须通过 projection 中受权限控制的 governance access view 读取，并在 query trace 中标明 purpose、caller、redaction state 和引用对象。它不应该混入默认 operational answer context，也不应该通过旁路读取 KB 文件绕过 release / permission / trace。

## 4. Projection Admission Rules

一个对象进入 runtime projection 前必须满足：

- 所属 release 已批准
- 所属 pack 已 signoff
- 对象 schema validation 通过
- object id / URI 无冲突
- target coverage conflict 已处理
- COMMON 与 product-specific precedence 已处理
- required lookup / helper / rule dependency 已发布
- L2 object 可读取
- projection manifest 可追溯到 canonical revision

如果上述条件不满足，正确行为是拒绝发布 projection，或者把对象排除并记录 blocking reason。索引层不应该在查询时临时修复这些问题。

## 5. Required Projection Outputs

### 5.1 `release_manifest`

`release_manifest` 说明这次 runtime projection 是谁、从哪里来、是否可用。

必须包含：

- release id
- canonical revision
- source release pack id
- projection schema version
- summary / card schema version
- generated at / by
- released at / by
- activation state
- rollback target
- included channel / product / pack
- content hash summary
- blocking issues count

索引层读取 projection 的第一步必须验证 manifest。没有有效 manifest，就没有可用 runtime truth。

### 5.2 `scope_registry`

`scope_registry` 定义查询可以进入哪些 scope。

至少覆盖：

- channel
- product
- pack
- domain
- active release
- permission boundary
- default precedence
- product-specific override rule
- deprecated / superseded state

Query resolver 必须先解析 scope，再执行召回。scope 不是搜索 filter 的附属品，而是 truth safety boundary。

### 5.3 `object_manifest`

`object_manifest` 是所有可索引对象的清单。

每个对象至少包含：

- stable URI
- object id
- object type: rule / lookup / helper
- channel / product / pack
- domain
- target path, if applicable
- source anchors
- content hash
- L2 content reference
- object card reference
- release id
- schema version

字段名、target path、source path、lookup id、helper id 的确定性查找都应该优先从 object manifest 建索引，而不是从全文搜索开始。

### 5.4 `rule_card` / `object_card`

`rule_card` 是给索引层和 query resolver 使用的结构化导航单元，不是自然语言摘要。

它应该由 KB projection 生成，索引层只能读取和索引，不能自行补业务含义。Card 可以包含 summary 和 search text，但它的职责是定位、消歧、导航和初步排序，不是承载完整规则真相。

`target_rule` card 至少包含：

- rule URI and id
- target kind and target path
- source anchors
- logic operation types
- business summary from governed rule
- applicability conditions
- not-applicable conditions, if known
- lookup dependencies
- helper dependencies
- rule dependencies
- example summary
- risk / ambiguity flags from release state
- override / supersession relation
- L2 content reference

`lookup_definition` card 至少包含：

- lookup URI and id
- lookup key columns
- output columns
- fallback behavior
- used by rules/helpers
- source table or mapping source reference
- released state
- L2 content reference

`helper_definition` card 至少包含：

- helper URI and id
- produced intermediate meaning
- source anchors
- logic operation types
- consumers
- dependencies
- released state
- L2 content reference

Rule card 的价值是让查询层先做结构化 resolver，而不是直接把问题丢给语义搜索。

### 5.4.1 `L2 runtime object`

`L2 runtime object` 是 projection 内的服务可读事实对象。

它不是对 KB rule 的自然语言摘要，也不是只给最终答案拼一句话的 text blob。它应该尽量保留 canonical object 中对 AI、索引、诊断、影响分析和测试规划有用的结构化语义。

`target_rule` L2 至少应包含：

- rule URI and id
- status / signoff / release state
- target kind, target path, emitted child paths or output bindings
- source inputs and source anchors
- structured logic pipeline or equivalent rule expression
- constants, conditions, fallback behavior, not-applicable conditions if known
- lookup / helper / rule dependencies with binding purpose
- examples with inputs, decisions, and outputs
- warnings / ambiguity / production gate summary that is allowed in operational view
- content hash and source canonical revision reference

`lookup_definition` L2 至少应包含：

- lookup URI and id
- key composition and key columns
- return fields and source columns
- fallback / reverse / coalesce behavior
- blank / missing value semantics
- consumer rules/helpers
- examples including fallback or edge cases
- warnings and governance summary allowed in operational view

`helper_definition` L2 至少应包含：

- helper URI and id
- inputs and outputs
- structured logic pipeline or equivalent expression
- branch conditions
- consumers and dependencies
- examples
- warnings and governance summary allowed in operational view

L2 可以比 canonical KB 裁剪 raw evidence、长 review notes 和 report body，但不能裁剪到只剩 `logic` 摘要。否则 AI 和服务端只能“读懂大意”，不能可靠回答异常诊断、改动分析、测试规划和 grounding 校验。

### 5.5 `dependency_edges`

`dependency_edges` 是影响分析、解释、测试规划和必要上下文加载的基础。

至少支持：

- rule -> lookup
- rule -> helper
- helper -> lookup
- helper -> helper
- rule -> rule
- rule -> source anchor
- rule -> target path
- lookup/helper -> consumer

每条边至少包含：

- from URI
- to URI or external anchor
- edge type
- required / optional
- release id
- direction
- traversal purpose: navigation / impact / explanation / test planning

第一阶段 dependency 可以用于导航、解释和影响分析。不要让 dependency 自动扩大 query scope，也不要让它越过权限或发布状态。

### 5.6 `runtime_content_refs`

`runtime_content_refs` 说明 L2 原文对象在哪里。

索引层最终回答事实时必须能回到 L2。

如果只有 L0/L1/card 命中，但没有 L2 object，查询层应该拒答或降级为“只能定位候选，不能给出规则事实”。

### 5.7 `l0_l1_navigation_views`

L0/L1 是导航输出，不是最终 truth。

L0 用于低成本候选过滤。L1 用于结构化导航、消歧和重排。

L0/L1 也不是“给 L2 做摘要替代”。它们是 index views：可以浓缩、重排和标注，但事实回答必须回到 L2 runtime object 或授权治理视图。

L0/L1 必须：

- 从 approved projection 生成
- 绑定 release id
- 绑定 summary schema version
- 可重新生成
- 有 hash
- 不包含未授权 evidence/review 细节
- 不覆盖 L2 object 的事实

高风险 rule content 不应依赖未审查的自由生成摘要。

## 6. Optional But Recommended Projection Outputs

### 6.1 `confusable_index`

RTS 很容易出现相似但不该混用的规则，例如 COMMON 与 product-specific override、FXD 与 FXO 相似字段、同名 lookup 在不同 product 中含义不同。

建议 KB projection 产出 confusable / negative candidates：

- this object is similar to X but not applicable because Y
- this product-specific rule overrides COMMON rule X
- this rule supersedes old rule X
- this lookup name is shared but scoped differently

这不是让模型“多看一些相似内容”，而是给 resolver 提供反证，避免拿错相似规则。

### 6.2 `evidence_access_index`

默认 operational query 不读取 raw evidence。

但授权解释、审计或 governance view 需要知道 evidence 在哪里。

建议单独产出 governance / evidence access view：

- object URI
- evidence summary
- evidence pointer
- review / report pointer
- conflict / ambiguity / adjudication status
- access level
- redaction state

这样普通查询不会被治理噪声污染，授权查询仍然可以追溯证据。

## 7. Query / Index Layer Obligations

索引层读取 projection 后，必须遵守以下义务：

- 先验证 release manifest
- 先按权限和 scope 收窄候选
- 优先使用 object manifest 做确定性查找
- 优先使用 rule card 做结构化 resolver
- dependency edge 只在允许的 traversal purpose 下使用
- OpenSearch / vector / BM25 只能在已收窄 scope 内召回
- 最终事实必须读取 L2 object
- 每次回答必须记录 query trace
- 不得把搜索分数变成 truth confidence
- 不得把 LLM 解释写回 projection

查询层可以生成解释、影响候选和测试建议，但输出必须区分 approved truth、inference、unknown、candidate suggestion 和 human decision。

## 8. Query Resolver Input Contract

Query resolver 不应该只接收一段自然语言然后直接搜索。

它应该把问题解析成结构化查询计划：

- intent: rule_lookup / explain / impact / test_plan / conflict_check / coverage_check
- scope: channel / product / pack / domain
- anchors: target path / source path / rule id / lookup id / helper id / business term
- required states: approved / released / governance-authorized
- output mode: concise answer / trace / impact candidates / test suggestions / refusal

这个 resolver 可以由规则、轻量 parser、LLM 辅助或混合方式实现。关键不是技术，而是查询必须先进入结构化计划，再读取 projection。

## 9. Refusal and Degraded Output

查询层在以下情况必须拒答、降级或要求澄清：

- scope 不清
- caller 无权访问目标 scope
- active release 不存在或不唯一
- manifest 校验失败
- projection schema 不兼容
- object card 命中但 L2 不可读
- dependency 未发布
- conflict / ambiguity 仍未解决
- COMMON 与 product-specific precedence 不明确
- 只有语义相似结果，没有结构化对象命中
- 查询要求的 evidence/review 超出权限

拒答不是失败。对 RTS 来说，unknown is safer than wrong。

## 10. Storage Mapping Guidance

第一阶段推荐这样落地：

- PostgreSQL 保存 release manifest、scope registry、object manifest、rule cards、dependency edges、content refs、governance access refs、query trace
- OpenSearch 保存 L0/L1/card text 的召回副本
- object storage 或文件系统保存结构化 L2 runtime object 和授权治理视图内容
- API/MCP/query service 只通过 PostgreSQL active projection 读取默认 truth

OpenSearch 可以帮助找候选，但不能成为 authoritative runtime boundary。

## 11. MVP Cut

第一版必须做：

- immutable release manifest
- scope registry
- object manifest
- rule / lookup / helper card
- dependency edges
- L2 content reference
- structured L2 runtime object
- deterministic L0/L1 generation
- manifest validation
- query trace
- refusal contract

第一版可以暂缓：

- full graph database
- autonomous agentic retrieval
- vector as primary entry
- object-level independent release
- raw evidence in default query
- full raw report/review expansion in default query
- complex DSL compiler
- cross-domain memory

这份契约的目标不是把系统做重，而是让第一版的边界足够清楚：KB 产出受治理、可验证、可追踪的 runtime projection；索引层只读取这个 projection，并在 scope、权限、状态和 L2 truth 约束下服务查询。Projection 不是 KB 的摘要替身，而是 approved truth 面向服务端和 AI 的受控机器视图。
