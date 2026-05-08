<!-- docmeta
role: leaf
layer: 3
parent: docs/reference/README.md
children: []
summary: distilled LLM-mediated RTS query and harness reference for scope mediation, context, memory, and controlled tool use
read_when:
  - 需要从旧 LLM-enhanced index/harness 讨论中提取仍适用的设计原则
  - 需要设计 scope mediation、context management、memory boundary 或 controlled harness 增强
  - 需要理解 OpenViking、mem0、PageIndex 对 RTS 可借鉴但不可照搬的部分
skip_when:
  - 需要当前 Day1 代码行为
  - 需要最终 LLM agent service 路线图
  - 需要完整历史讨论原文
source_of_truth:
  - docs/confirmed/day1-query-service-and-llm-harness-plan-zh.md
  - docs/confirmed/day2-agentic-retrieval-evolution-plan-zh.md
  - docs/confirmed/final-llm-agent-service-plan-zh.md
  - docs/confirmed/llm-harness-and-agent-integration-alignment-zh.md
-->

# LLM-Mediated Query And Harness Reference

> 状态：distilled reference
> 压缩日期：2026-05-08
> 原完整长版：`docs/archive/reference-proposals/llm-enhanced-index-and-harness-design-zh.md`
> 目的：保留 LLM 如何增强 RTS query/index/harness 的实用原则，删除与 final roadmap、Day1/Day2 和 alignment note 重叠的长讨论。

## 1. Current Boundary

LLM 应位于用户和 RTS tools 之间，而不是位于 truth core 之内。

推荐心智模型：

```text
user / external agent
  -> LLM mediation
  -> RTS controlled tools
  -> L2 truth read / dependency / authorized governance view
  -> answer shaping
  -> grounding validation and trace
```

LLM 可以猜测 intent、候选 scope、术语扩展、读取顺序和答案表达。RTS 必须验证 scope、permission、release、L2 hash、grounding、refusal 和 trace。

## 2. Scope Mediation Is First Priority

当前最大风险不是语义召回不够高级，而是用户问题模糊时拿错 scope。

用户常问：

```text
fixing time 怎么生成？
NDF cutoff 怎么查？
Cortex 是哪里来的？
quoted currency pair 为什么反了？
```

LLM 不应在 scope 不明确时直接跨 pack 搜索并硬答。目标行为：

1. 从 UI/workspace/session context 找默认 scope。
2. 无默认 scope 时调用 scope/navigation 工具找候选。
3. 唯一高置信候选可以带说明补全。
4. 多个候选接近时要求用户选择。
5. 无候选时拒答或要求补充。

建议工具：

- `list_scopes`
- `search_scopes`
- `get_scope_summary`
- `get_pack_navigation`
- `find_confusable_scopes`

这些工具返回结构化 scope/navigation 信息，不返回大段自由文本。

## 3. Query Plan Before Tool Calls

LLM 应把自然语言问题转换成结构化 query plan，再由 controlled harness 执行。

query plan 可以包含：

- intent
- scope candidates
- object type preference
- business terms
- expanded terms
- clarification requirement
- allowed tool sequence
- max tool calls
- max dependency depth

注意：expanded terms 只能用于检索，不能成为事实。

## 4. Controlled Harness

Controlled harness 不是自由 agent。它应是 schema-constrained plan 或有限状态机。

推荐执行顺序：

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

必须约束：

- allowlisted tools only
- schema-constrained tool arguments
- tool call budget
- L2 read budget
- dependency depth limit
- every ask writes trace
- final answer passes L2/dependency grounding validation

## 5. Tool Surface To Add Over Time

Day1 已有或接近已有：

- `resolve_scope`
- `find_objects`
- `get_object_card`
- `read_object_l2`
- `get_dependencies`

后续可补：

- `list_scopes`
- `search_scopes`
- `get_scope_summary`
- `get_pack_navigation`
- `find_confusable_objects`
- `read_lookup_sample`
- `read_pack_status`

工具输出必须结构化；human-readable answer 由 harness 或 caller 组织。

## 6. Context Management

RTS 需要 context management，但 context 不拥有 truth。

上下文类型：

| Type | Examples | Truth eligible |
|---|---|---|
| UI context | 当前页面、当前 pack、当前 object、selected release | only as hints |
| Session context | 刚确认的 scope、上一轮对象、任务目标、语言偏好 | no |
| Retrieved context | object cards、L2 facts、dependencies、trace ids、warnings | L2/dependency yes; cards only navigation |
| Governance context | draft/approved/signoff、ambiguity、review/release state | yes only if authorized |

Context window 顺序：

```text
scope summary
object cards
selected L2
required dependency L2
authorized warnings / governance summary
```

上下文管理器必须去重、控制 L2 数量、控制 dependency depth、保留 URI/trace、区分 facts/inferences/unknowns，并避免把 session memory 混进事实上下文。

## 7. Memory Boundary

RTS agent 可以有 memory，但 memory 只能提升体验和检索质量，不能成为 truth。

允许：

- current scope
- user-selected candidate
- previous object
- current language preference
- workspace default channel/product
- retrieval feedback such as missed aliases or weak card search text

禁止 memory 自动保存为：

- rule logic
- lookup mapping
- helper behavior
- human adjudication
- approval/signoff
- release status

聊天中的业务判断最多是 candidate clarification，必须进入 review/adjudication flow，不能直接写入 runtime truth。

## 8. Lessons From OV, mem0, And PageIndex

可借鉴：

- OpenViking: session/context memory 分类、tool call trace 可形成检索质量反馈。
- mem0: memory isolation、filter/scope before recall、optional rerank after gated candidates。
- PageIndex: metadata/structure first, tight read later;少工具、强结构、按需读内容。

不可照搬：

- 不把 session memory 写回 rule truth。
- 不让 personalization memory 改变 approved truth。
- 不让 reranker 或 vector 越过 scope/permission/release gate。
- 不让 LLM 在无 scope gate 时遍历所有 pack。

综合落地顺序：

1. Scope mediation tools
2. Controlled query plan
3. Session/workspace context store
4. Pack navigation / object graph tools
5. Human/agent/audit answer shaping
6. Trace feedback memory
7. Optional reranker
8. Optional alias/entity boost
9. Optional vector retrieval

## 9. Decision

```text
LLM owns mediation, never truth.
RTS tools own gated truth reads.
Context owns relevance, never authority.
Memory owns convenience, never truth.
```

For current implementation priority, use the confirmed Day1/Day2 plans and the LLM harness alignment note. Use this reference only for design rationale around scope mediation, context, memory, and controlled tool orchestration.
