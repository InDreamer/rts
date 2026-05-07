<!-- docmeta
role: leaf
layer: 3
parent: docs/reference/README.md
children: []
summary: LLM-enhanced RTS index mediation, controlled harness, context management, and memory boundary design draft
read_when:
  - 需要设计 LLM 如何增强 RTS 索引/查询服务
  - 需要区分 tool call、scope mediation、context management、memory 和 truth 边界
  - 需要规划 Day1 harness 到更完整 RTS agent/LLM 入口的演进
skip_when:
  - 只需要当前 Day1 已实现代码行为
  - 只需要 confirmed baseline
source_of_truth:
  - docs/confirmed/system-constitution-v1.md
  - docs/confirmed/kb-to-index-projection-contract-zh.md
  - docs/confirmed/day1-query-service-and-llm-harness-plan-zh.md
  - docs/confirmed/day2-agentic-retrieval-evolution-plan-zh.md
-->

# LLM-Enhanced RTS Index and Harness Design

> 状态：reference draft  
> 创建日期：2026-05-06  
> 范围：沉淀关于 LLM 如何增强 RTS 索引系统、controlled harness、上下文管理和 memory 边界的设计讨论  
> 注意：本文不覆盖 confirmed baseline；后续稳定后可拆分/提升到 confirmed Day2/Day3 文档

## 1. 背景问题

当前 RTS Day1 查询服务已经具备：

- active release / scope / permission gate
- object manifest 确定性查找
- Lucene BM25 关键词召回
- dependency graph
- L2 content 回读和 hash 校验
- trace
- controlled LLM harness 骨架

但它还不是完整的智能自然语言入口。

典型问题：

用户问：

```text
fixing time 怎么生成？
```

用户通常不会提供：

```text
channel = tradition
product = stella
pack = fxd-ndf-cutoff-fixing
domain = cutoff-fixing
object_type = rule
```

如果 RTS 直接跨 scope 搜索并猜答案，很容易把相似字段、相似 pack 或 draft 内容误当 truth。

因此需要 LLM，但 LLM 的职责不是拥有 truth，而是做：

- scope mediation
- query understanding
- 参数补全
- 澄清对话
- 同义词/术语扩展
- context window 管理
- 面向用户的答案组织

RTS 的 truth 仍然必须来自 governed projection 和 L2 objects。

## 2. 当前索引系统的真实边界

当前索引不是 LLM 智能索引。

它是：

```text
structured projection + deterministic lookup + Lucene BM25 + scope filter + L2 read
```

当前索引输入不是 canonical YAML pack 全文，而是 runtime projection：

```text
release-manifest
scopes
object-manifest
object-cards
dependency-edges
content-refs
l2 objects
caller-profiles
```

当前 Lucene 索引字段主要来自：

```text
object_id
target_path
source_anchors
object-card.search_text
channel / product / pack / domain
object_type
release_id
```

当前支持两类召回：

1. 确定性查找：URI、rule id、lookup id、helper id、target path、source anchor。
2. BM25 模糊关键词搜索：在明确 scope 内对 card/search text/object id/target path 做相关性排序。

当前不支持：

- LLM 自动理解 pack 后建索引
- vector/embedding semantic search
- reranker
- 跨 scope 智能猜测
- 自动业务同义词理解
- agent memory 参与 truth 判断

因此当前检索质量高度依赖：

- object-card 写得是否清楚
- search_text 是否包含业务术语和同义词
- target_path / source_anchors 是否稳定
- dependency_edges 是否完整
- scope 是否明确

## 3. LLM 的正确位置

LLM 应位于用户和 RTS tools 之间。

推荐形态：

```text
User
  ↓
LLM Mediation Layer
  ↓
RTS Controlled Tools
  ↓
L2 Truth Read
  ↓
LLM Answer Shaping
  ↓
User
```

LLM 可以：

- 和用户澄清 scope
- 基于 UI/workspace/session context 补全 `scope_hint`
- 推荐候选 scope
- 将自然语言转成结构化 query plan
- 做业务术语扩展
- 决定先查 rule、lookup 还是 helper
- 控制读取哪些 L2
- 将 facts/dependencies/trace 翻译成人能读的答案

LLM 不可以：

- 自己发明 rule
- 自己发明 lookup 结果
- 把相似搜索命中当 truth
- 把聊天记录当 truth
- 把 memory 里的内容当 rule truth
- 绕过 scope / permission / release / L2
- 把 draft pack 说成 approved truth

核心原则：

```text
LLM can guess parameters, but RTS must verify scope, permission, release, and L2 grounding.
```

## 4. Scope Mediation

Scope mediation 是 LLM 增强 RTS 的第一优先级。

### 4.1 为什么需要

用户不会记住完整 scope。真实输入通常是：

```text
fixing time 怎么生成？
NDF 的 cutoff 怎么查？
Cortex 是哪里来的？
quoted currency pair 为什么反了？
```

这些问题必须先定位：

```text
channel / product / pack / domain
```

否则同名字段可能来自错误 pack。

### 4.2 目标行为

当用户没有提供 scope 时，LLM 不应直接调用 `/query` 猜答案。

它应该：

1. 从 session/UI/workspace context 找默认 scope。
2. 如无默认 scope，调用 scope/navigation 工具找候选。
3. 如果只有一个高置信候选，可以带置信说明补全。
4. 如果多个候选相近，应要求用户选择。
5. 如果没有候选，应拒答或要求补充。

示例：

```text
用户：fixing time 怎么生成？
Agent：我需要先确认转换范围。你问的是：
1. Tradition -> Stella / FXD.NDF / cutoff-fixing
2. 其他 pack
```

### 4.3 需要新增的工具

建议新增：

```text
list_scopes
search_scopes
get_scope_summary
get_pack_navigation
find_confusable_scopes
```

这些工具返回结构化 scope/navigation 信息，不返回大段自由文本。

## 5. Query Understanding and Parameter Completion

LLM 应把自然语言问题转成结构化 query plan。

示例输入：

```text
NDF fixing time 怎么生成？
```

期望中间计划：

```yaml
intent: rule_lookup
scope_candidates:
  - channel: tradition
    product: stella
    pack: fxd-ndf-cutoff-fixing
    domain: cutoff-fixing
object_type_preference:
  - rule
business_terms:
  - fixing time
expanded_terms:
  - fixingTime
  - hourMinuteTime
  - businessCenter
  - cutoff_code
needs_clarification: false
```

注意：expanded terms 只能用于检索，不能成为事实。

## 6. Controlled Harness

当前已有 `ControlledLlmHarness` 骨架。

它的正确方向是：

- LLM 只能调用 allowlisted tools。
- 每个 tool 参数必须 schema-constrained。
- 每次 tool call 计入预算。
- L2 read 数量受限。
- dependency traversal depth 受限。
- 每次 ask 都写 trace。
- 最终 answer 必须通过 citation/L2 validation。

需要增强的点：

### 6.1 Tool Set

当前 tools：

```text
resolve_scope
find_objects
get_object_card
read_object_l2
get_dependencies
```

建议增加：

```text
list_scopes
search_scopes
get_scope_summary
get_pack_navigation
find_confusable_objects
read_lookup_sample
read_pack_status
```

### 6.2 Orchestrator

当前 harness 基本是固定流程。后续需要受控 orchestrator：

```text
resolve or clarify scope
find candidate objects
inspect object cards
select object
read L2
optionally read dependency L2
assemble grounded answer
validate answer
write trace
```

Orchestrator 不是自由 agent。它执行的是有限状态机或 schema-constrained plan。

### 6.3 Answer Validation

最终答案必须满足：

- cited object 必须来自本轮 L2 read
- facts 必须来自 L2 或 approved dependency edges
- 不允许新增业务 claim
- draft / not signoff / ambiguity flags 必须保留或转成 warning
- trace 必须记录候选、选中对象和 L2 read

## 7. Context Management

RTS 需要 context management，但它服务于检索和体验，不拥有 truth。

### 7.1 Context 类型

1. UI context

```text
当前页面
当前 pack
当前 object
当前 selected release
```

2. Session context

```text
用户刚刚确认的 scope
上一轮查询对象
当前任务目标
用户偏好语言
```

3. Retrieved context

```text
object cards
L2 facts
dependencies
trace ids
warnings
```

4. Governance context

```text
draft / approved / signoff
ambiguity
review status
release status
```

### 7.2 Context Window 规则

LLM 不应该一次拿全部 pack。

推荐顺序：

```text
scope summary
object cards
selected L2
required dependency L2
warnings/review summary if authorized
```

上下文管理器要做：

- 去重
- 控制 L2 数量
- 控制 dependency depth
- 保留 URI/trace
- 区分 fact/inference/unknown
- 不把 session memory 混进事实上下文

## 8. Memory 边界

RTS agent 可以有 memory，但 memory 不能成为 truth。

### 8.1 允许的 Memory

Session memory：

```text
当前 scope
用户刚选择的候选
上一轮对象
当前对话语言
```

Workspace/user context：

```text
用户当前项目
默认 channel/product
当前 UI 打开的 pack
```

Trace/feedback memory：

```text
哪些 query 搜歪
用户最终选择哪个 scope
哪些 card/search_text 需要优化
```

### 8.2 禁止的 Memory

Agent memory 不得自动保存为 truth：

```text
rule logic
lookup mapping
helper behavior
human adjudication
approval/signoff
release status
```

如果用户在聊天中说：

```text
primary rate source 应该是 XXX
```

这最多是 candidate clarification，必须进入 review/adjudication 流程，不能直接写入 runtime truth。

## 9. Human-Facing Output

RTS API 原始输出适合机器和审计，不适合直接给业务用户。

Agent 应把输出分层展示：

```text
结论
生成步骤
依赖对象
不确定/草稿状态
trace
```

例如：

```text
fixingTime 的 hourMinuteTime 和 businessCenter 来自 shared cutoff lookup。

生成步骤：
1. 读取 fixing_currency1
2. 读取 fixing_currency2
3. 读取 cutoff_code
4. 用三者查询 lk_fxd_ndf_cutoff_by_pair_and_locode
5. 将返回的 fixing_time 和 fixing_business_center 写入 Stella fixingTime

注意：当前 pack 是 photo reconstructed draft，不是 signoff truth。
Trace: trace-...
```

## 10. Implementation Roadmap

### Phase A：Day1 Harness Hardening

- 明确 `/query` 和 `/ask` 的区别。
- 让 `/query` 保持 deterministic/BM25/L2。
- 让 `/ask` 成为 LLM mediation 入口。
- 增加 `response_view=human|agent|audit`。
- 保留 L2 validation。

### Phase B：Scope Tools

- 新增 `list_scopes`。
- 新增 `search_scopes`。
- 新增 `get_scope_summary`。
- 为 scope/card 加别名和业务术语。
- 增加 scope ambiguity refusal/clarification。

### Phase C：Query Planner V2

- LLM/规则混合生成 query plan。
- 支持 object type preference。
- 支持 business term expansion。
- 支持候选 scope 排序。
- 支持 ask-time clarification。

### Phase D：Context Manager

- 控制 L2 read 数量。
- 控制 dependency depth。
- 支持 read selected dependency L2。
- 区分 facts/inferences/unknowns/warnings。
- 输出 compact context 给 answerer。

### Phase E：Answer Shaping

- 使用 LLM 组织 human answer。
- 强制每个 claim 对应 fact/dependency。
- draft/review/signoff 状态转 warning。
- 生成可读步骤和必要 trace。

### Phase F：Memory

- Session memory 保存当前 scope/object/task。
- Workspace context 保存默认项目/pack。
- Trace/feedback memory 用于检索质量改进。
- 禁止 memory 自动写入 truth。

## 11. Lessons from OpenViking, mem0, and PageIndex

本节基于对 OpenViking、mem0、PageIndex 的本地实现观察，提炼它们对 RTS 的可参考点。

结论先行：

```text
OpenViking: LLM 主要用于 session -> long-term memory 的抽取、压缩、去重和合并。
mem0: LLM 主要用于 memory extraction 和 optional rerank，检索层融合 embedding/BM25/entity/filter。
PageIndex: LLM 主要用于 indexing-time tree construction 和 retrieval-time tree navigation。
RTS: 可借鉴它们的 mediation / memory / navigation / rerank 方法，但不能借鉴“记忆即事实”或“LLM 直接生成 truth”。
```

### 11.1 OpenViking

OpenViking 的 LLM 不只是 tool call adapter。它深度参与 session memory lifecycle：

- 从会话中抽取长期记忆。
- 将记忆分为 profile、preferences、entities、events、cases、patterns、tools、skills。
- 每条 memory 采用 L0/L1/L2 结构：abstract / overview / content。
- 从 tool call records 中抽取工具和 skill 的使用经验。
- 用 embedding/vector prefilter 找相似 memory，再由 LLM 做 dedup/merge/delete 决策。
- prompt template 独立管理，并带变量校验、输出 schema、temperature 控制。
- 记忆写入、合并、删除周围有 telemetry 和异步索引流程。

对 RTS 可借鉴：

- L0/L1/L2 仍然是正确的低噪声导航模型。
- session/context memory 应该有明确分类，而不是把聊天记录整段塞给模型。
- tool call/trace 可以被整理成检索质量反馈，例如“哪些 query 搜歪”“用户最终选了哪个 scope”“哪些 object-card search_text 缺少同义词”。
- LLM 可以用于抽取用户偏好、当前工作区、默认 scope、上次选择对象、常用 product/channel。
- dedup 流程可参考“先召回相似项，再让 LLM 做非破坏性合并判断”。
- prompt 应该模板化、版本化、schema-constrained，而不是散落在代码里。

对 RTS 不应借鉴：

- 不应把 session memory 写回 rule truth。
- 不应让 agent memory 自动改变 rule、lookup、helper。
- 不应让 tool/skill memory 参与事实裁决。
- 不应把“用户曾经这么说过”当作 approved rule evidence。

RTS 适配方式：

```text
OpenViking memory lifecycle
  -> RTS session context store
  -> RTS trace/feedback memory
  -> card/search_text improvement candidates
  -> human/governance review
  -> optional projection update
```

也就是说，OpenViking 的 memory 能帮助 RTS 更懂用户、更会导航、更会改进检索，但不能直接拥有规则真相。

### 11.2 mem0

mem0 的核心是 memory service，不是 rule truth service。

它的 LLM 作用集中在：

- 从 conversation messages 中抽取 memory JSON。
- 结合 existing memories 生成新增 memory。
- 可选执行 LLM reranker，对候选 memory 打 0.0-1.0 相关性分。
- procedural memory 场景下生成 agent 过程性记忆。

它的检索层不是纯 LLM：

- filter 是强约束入口，至少要求 user_id / agent_id / run_id 等 scope。
- query 会同时走 semantic vector search 和 keyword/BM25 search。
- BM25 score 会归一化。
- entity extraction / entity store 用于 entity boost。
- score fusion 将 semantic、BM25、entity boost 组合。
- optional reranker 在候选集之后运行。
- openclaw isolation 避免 cron、heartbeat、automation、subagent 等非交互场景污染 memory。

对 RTS 可借鉴：

- scope/filter 必须先于召回和 rerank。
- BM25 + entity/alias boost + optional rerank 是 Day2 可落地的检索增强路线。
- over-fetch 后重排，比一开始只取 top-k 更稳。
- LLM rerank 可以作为可选层，但只能重排已过 scope/permission/release gate 的候选。
- memory isolation 很重要：不同 user/agent/session/run 的上下文不能互相污染。
- 对自动捕获 memory 要有触发器过滤，CI、cron、subagent、pipeline 不能默认写长期记忆。

对 RTS 不应借鉴：

- 不应提供 runtime memory add/update/delete 作为 rule truth 修改入口。
- 不应让 personalization memory 改变 approved truth。
- 不应把 agent-scoped memory 和 pack-scoped rule 混为一层。
- 不应允许 reranker 越过 scope gate。

RTS 适配方式：

```text
query terms
  -> scope/permission/release gate
  -> deterministic lookup + BM25
  -> alias/entity boost
  -> optional embedding/vector candidates
  -> optional reranker
  -> L2 read/hash validation
  -> grounded answer
```

其中 score 只影响“先看哪个候选”，不决定“哪个是真的”。

### 11.3 PageIndex

PageIndex 的关键思想是 vectorless agentic retrieval：

- indexing-time 使用 LLM 检测 TOC、清洗目录、补齐 section/page mapping。
- 没有 TOC 时，LLM 直接从内容生成 hierarchical tree。
- 可生成 node summary 和 document description。
- retrieval tools 很少：get document metadata、get structure、get page content。
- agent prompt 强制 progressive retrieval：先读 metadata，再读 structure，最后读 tight page ranges。
- retrieval-time 可以让 LLM 在 tree 上做节点选择，而不是直接塞全文。

对 RTS 可借鉴：

- RTS pack/object graph 应该有可导航结构，不只是 Lucene 搜索框。
- LLM 应先读 scope summary / pack navigation / object cards，再读必要 L2。
- 工具接口应限制 tight read，例如一次最多读 N 个 object L2，dependency depth 受限。
- object card / dependency graph / pack navigation 对 agent 很关键，等价于 PageIndex 的 tree structure。
- 对长 pack 或 source evidence，可借鉴 LLM 生成 L0/L1 navigation，但必须版本化和可回归。

对 RTS 不应借鉴：

- 不应把纯 LLM tree search 作为 truth 判断。
- 不应让 LLM 在没有 scope gate 的情况下遍历所有 pack。
- 不应把 page/document section 结构当 RTS 的主 truth shape。
- 不应允许一次读取整个 pack 让模型自由总结。

RTS 适配方式：

```text
PageIndex document tree
  -> RTS scope tree / pack navigation / object graph

PageIndex page tight read
  -> RTS selected object L2 read + dependency L2 read

PageIndex document metadata first
  -> RTS active release + scope summary + pack status first
```

### 11.4 RTS 应采用的综合设计

三个项目合在一起，对 RTS 最有价值的不是某个库，而是职责分层：

```text
User utterance
  -> LLM scope/query mediation
  -> controlled RTS tools
  -> structured navigation
  -> gated retrieval
  -> optional rerank
  -> L2 truth read
  -> answer shaping
  -> trace/feedback memory
  -> offline KB/card improvement
```

建议拆成四个子系统：

1. LLM Mediation Harness

```text
职责：理解用户模糊问题、补全参数、提出澄清、生成受控 query plan。
不可做：直接回答 rule truth、绕过工具、写 approved truth。
```

2. Context and Memory Manager

```text
职责：保存当前 scope、workspace、用户偏好、上轮对象、trace feedback。
不可做：保存或覆盖 rule truth。
```

3. Retrieval and Ranking Layer

```text
职责：deterministic lookup、BM25、alias/entity boost、optional vector、optional rerank。
不可做：突破 scope/permission/release gate。
```

4. Navigation and Tight Read Tools

```text
职责：list/search scopes、pack navigation、object cards、dependency graph、L2 read。
不可做：一次性把全 pack 当自由上下文交给 LLM。
```

### 11.5 RTS Harness 需要补齐的能力

基于三个项目，RTS harness 后续应补齐：

- prompt registry：所有 planner、clarifier、reranker、answerer prompt 版本化管理。
- schema-constrained plan：LLM 输出 query plan JSON，不直接拼任意请求。
- scope clarifier：scope 不明确时先返回候选和问题。
- session context store：保存用户刚确认的 channel/product/pack/object。
- workspace context resolver：从 UI/工作区补默认 scope。
- trace feedback extractor：从失败/澄清/用户选择中抽取检索改进信号。
- card quality loop：把常见 query miss 转成 object-card/search_text 改进候选。
- optional reranker：只对 gated candidates 评分。
- negative/confusable retrieval：找相似但不适用对象，防止误答。
- memory isolation：interactive session、pipeline、subagent、cron 分开处理。

### 11.6 最小可落地顺序

不建议一次性把三套项目能力都搬进 RTS。

推荐顺序：

1. Scope mediation tools
2. Prompt registry and schema-constrained query plan
3. Session/workspace context store
4. Pack navigation / object graph tools
5. Alias/entity boost
6. Trace feedback memory
7. Optional reranker
8. Card/search_text improvement workflow
9. Optional vector retrieval

这个顺序的原因是：RTS 当前最大风险不是“语义召回不够先进”，而是“用户问题模糊时容易拿错 scope”。先解决 scope mediation 和受控 planning，再增强排序。

## 12. Design Decision

LLM 增强 RTS 的目标不是让模型更自由，而是让用户更自然地进入受控 truth service。

最终边界：

```text
LLM owns mediation and presentation.
RTS owns governed truth access.
KB/review/release owns truth.
Memory owns convenience, never truth.
Trace owns auditability.
```
