# RTS Day2 Controlled Agentic Retrieval Evolution Plan

> 状态：archived full plan；当前默认入口已压缩为 compact roadmap
> 归档日期：2026-05-08
> 当前摘要：`docs/confirmed/day2-agentic-retrieval-evolution-plan-zh.md`
> 范围：Day1 之后的查询/索引/LLM 服务升级方向
> 粒度：方向和边界，不拆成过细实现任务

## 1. Day2 一句话结论

Day2 应该把 Day1 的受控工具调用扩展成 **Controlled Agentic Retrieval**。

这不是让 LLM 自由搜索、自由推理、自由决定 truth，而是让 LLM 在服务端 guardrail 内做更多多步查询计划：

- 多轮选择工具
- 读取结构后再读取 L2
- 对候选做受控重排
- 使用 negative/confusable 反证
- 生成 impact/test planning 候选
- 通过 MCP 给外部 agent 提供稳定只读工具

Day2 的核心仍然是：**agentic 是检索方式，不是 truth 形成方式。**

## 2. Day2 启动前提

Day2 不应该在 Day1 基础不稳时启动。

至少满足：

- Day1 projection ingest 稳定。
- Day1 active release / scope / permission gate 稳定。
- Day1 deterministic lookup 可用。
- Day1 Lucene BM25 在 golden set 上表现可接受。
- Day1 L2 读取和 content hash 校验稳定。
- Day1 `/ask` harness 不会在无 L2 时硬答。
- Day1 query trace 可以回放。
- Day1 refusal contract 被测试覆盖。

如果 Day1 仍然经常拿错 scope、读不到 L2、trace 不完整，Day2 的 agentic 能力只会放大风险。

## 3. Day2 要解决的问题

Day1 更像“受控问答服务”。

Day2 要解决的是复杂查询：

- 用户问题不直接包含 rule id 或 target path。
- 问题需要先定位 rule，再定位 lookup/helper，再解释组合逻辑。
- 问题需要找相似但不适用的规则作为反证。
- 问题需要生成 impact candidates。
- 问题需要生成 test planning candidates。
- 外部 agent 需要通过 MCP 调 RTS 多个工具，而不是只调一个 ask。
- 检索质量需要在 BM25 之外引入 rerank、entity boost 或向量。

Day2 仍不解决：

- 自动修改 KB truth
- 自动 signoff
- 自动 publish
- pipeline 自动 release gate
- 无人审核的业务裁决

## 4. Day2 从三个项目继续借鉴什么

### 4.1 OpenViking：从 `find/search` 到 query planning

OpenViking 的 `search` 使用 session context 和 intent analysis 生成 TypedQueries，再进入分层检索。

RTS Day2 借鉴：

- query planner 生成多条 typed retrieval steps。
- 不同 intent 使用不同检索计划。
- L0/L1/L2 保持 progressive loading。
- trace 记录每一步查询计划和结果。

RTS Day2 不借鉴：

- session memory 自动进入 truth retrieval
- user/agent memory 对 rule answer 产生影响
- 通用 agent runtime

RTS 对应改造：

- Day1 `QueryResolverLite` 升级为 `QueryPlannerV2`。
- 输入仍然是当前问题和明确 caller profile。
- 可选使用短 conversation context，但不能把 conversation memory 当 truth。
- planner 输出必须是 schema-constrained query plan。

### 4.2 mem0：从单次搜索到多信号排序

mem0 的多信号检索价值在于语义、BM25、entity linking、rerank 的组合。

RTS Day2 借鉴：

- 多信号 score fusion。
- entity/alias/term boost。
- optional reranker。
- over-fetch 后重排。
- metadata filter 永远先行。

RTS Day2 不借鉴：

- memory add/update/delete
- agent-generated facts
- personalization memory

RTS 对应改造：

- Day1 Lucene BM25 保留。
- 加 entity/alias linking。
- 加 optional reranker。
- 视对象规模决定是否加 embedding/vector。
- score fusion 只能作用于 gate 之后的候选。

### 4.3 PageIndex：从结构工具到 agentic tight read

PageIndex 的工具形态是：

- `get_document`
- `get_document_structure`
- `get_page_content`

并要求 agent 先读结构，再读 tight range 内容。

RTS Day2 借鉴：

- 将 RTS object graph 变成工具可导航结构。
- 让 agent 先读 scope/object structure，再读 L2。
- 工具强制 tight read。
- 工具调用过程可观察。

RTS Day2 不借鉴：

- 纯 LLM tree search 作为默认判断
- 无权限门禁的 page/content 读取

RTS 对应改造：

- 增强 `get_scope_tree`
- 增强 `get_pack_navigation`
- 增强 `read_object_l2`
- 增强 `get_dependency_subgraph`
- 工具返回结构化对象，不只返回大段文本

## 5. Day2 能力地图

Day2 建议包含以下能力，但不必一次全部上线。

### 5.1 Query Planner V2

从 Day1 的轻量 resolver 升级为受控 planner。

能力：

- 多 intent 分解
- 多 step retrieval plan
- anchor normalization
- candidate expansion
- dependency traversal plan
- output mode selection

Planner 输出示例语义：

```text
intent: explain_rule
scope: channel/product/pack
steps:
  1. find target rule candidates
  2. inspect top object cards
  3. read selected rule L2
  4. load required lookup/helper dependencies
  5. assemble grounded explanation
```

约束：

- planner 只生成计划，不执行 bypass。
- 每一步仍由 service gate 校验。
- planner 不能直接引用未读取对象。

### 5.2 Controlled Tool Orchestrator

Day1 `/ask` 可能是一轮或少量工具调用。

Day2 引入明确 orchestrator：

- 管理 tool call budget。
- 管理 max L2 reads。
- 管理 dependency traversal depth。
- 管理 retry/clarification。
- 记录每一步 tool input/output hash。
- 在失败时降级到 deterministic response。

Orchestrator 不是自由 agent。

它执行的是：

- allowlisted tools
- schema-constrained args
- policy-checked results
- traceable steps

### 5.3 Negative / Confusable Retrieval

Day2 应引入 negative retrieval。

目的：

- 不是多召回相似内容。
- 而是防止相似但错误的规则被误用。

典型场景：

- COMMON rule 被 product-specific rule override。
- 两个 product 有同名 lookup，但含义不同。
- target path 相似，但 channel 不同。
- old rule 被 superseded。
- helper 名相似，但 output meaning 不同。

实现方向：

- KB projection 可选产出 confusable index。
- Query service 存储 confusable relations。
- Retrieval 输出时标记 “similar but not applicable”。
- LLM final answer 必须把反证和适用性分开。

### 5.4 Rerank and Score Fusion

Day2 可加入 rerank。

候选信号：

- exact match score
- BM25 score
- alias/entity boost
- dependency proximity
- object type prior
- release/state prior
- negative/confusable penalty
- optional LLM/cross-encoder rerank score
- optional vector score

硬规则：

- scope gate 先于 score。
- permission gate 先于 score。
- release gate 先于 score。
- dependency release gate 先于 score。
- reranker 只能重排已通过 gate 的候选。

### 5.5 Optional Vector Retrieval

Day2 可以评估向量检索，但不默认必须做。

触发条件：

- 对象规模增长到 Lucene + alias 不够。
- 用户经常用不标准业务术语提问。
- golden set 显示 BM25 top-k 召回不足。
- 多语言或隐含语义查询增多。

技术选择：

- 小规模仍可 PostgreSQL + pgvector。
- 中大规模或企业统一基础设施成熟后再考虑 OpenSearch。
- embedding 只索引 card/L0/L1，不建议直接把完整 L2 作为主要向量对象。

注意：

- 向量只做候选发现。
- 向量不能绕过 deterministic gates。
- 向量结果必须回到 object URI 和 L2。

### 5.6 MCP Tool Surface Expansion

Day1 MCP 是薄 adapter。

Day2 可以把 MCP 做成正式 agent-facing 工具面。

建议工具：

- `rts_resolve_scope`
- `rts_find_rules`
- `rts_find_lookups`
- `rts_find_helpers`
- `rts_read_object`
- `rts_get_dependencies`
- `rts_impact_preview`
- `rts_test_plan_candidates`
- `rts_explain_trace`

仍不提供：

- publish
- signoff
- edit canonical truth
- write runtime projection
- save memory as truth

MCP 输出应保持结构化，避免只返回长段自然语言。

### 5.7 Impact Analysis Candidates

Day2 可以把 impact preview 提升为更有用的候选分析。

输入：

- changed source path
- changed lookup/helper
- changed rule
- changed target path
- optional changed expression summary

输出：

- impacted rules
- impacted targets
- dependency paths
- confidence / reason
- unknowns
- suggested reviewer questions

边界：

- 输出是 impact candidates。
- 不是最终 impact approval。
- 不能替代人工或 pipeline gate。

### 5.8 Test Planning Candidates

Day2 可以生成测试规划候选。

输入：

- selected rule / lookup / helper
- dependency subgraph
- examples
- applicability / not-applicable conditions
- risk flags

输出：

- positive test candidates
- negative test candidates
- boundary cases
- dependency coverage suggestions
- regression focus
- unknowns requiring reviewer/QA input

边界：

- LLM 只能补充候选。
- 测试充分性不是 LLM 最终判断。
- 输出必须区分 deterministic baseline 和 LLM suggestions。

### 5.9 Governance-authorized Evidence View

Day2 可以考虑授权 evidence/review/report summary 和 pointer。

默认 operational query 仍不读取 raw evidence。

只有在 caller profile 允许时，工具可读取：

- evidence summary
- review/adjudication pointer
- report summary or pointer
- conflict status summary
- redacted evidence pointer

边界：

- raw evidence 不进入默认 Q&A。
- review notes 不进入普通 agent context。
- governance mode 和 operational mode 分开。
- governance 信息也应通过 projection view、权限和 trace 读取；不要让 agent 直接旁路读取 canonical KB 文件。
- operational projection 可以保留必要 risk / ambiguity / production gate summary，但不能把完整 review/report 噪声塞进普通上下文。

## 6. Day2 架构演进

Day2 不推翻 Day1 架构，而是在 Day1 上加层。

```text
Day1 Core
  |
  | stable projection store / Lucene / L2 / API / trace
  v
Day2 Additions
  |-- QueryPlannerV2
  |-- ControlledToolOrchestrator
  |-- ConfusableService
  |-- ScoreFusionService
  |-- OptionalReranker
  |-- OptionalEmbeddingIndex
  |-- EvaluationHarness
  |-- ExpandedMcpAdapter
  |-- ImpactAndTestCandidateService
```

Day2 必须保持：

- Day1 API backward compatible。
- Day1 deterministic query 仍可单独运行。
- Day1 no-LLM mode 仍可运行。
- Day1 trace schema 可以扩展但不能断裂。

## 7. Day2 数据模型增量

Day2 可增加：

### 7.1 `confusable_relation`

用途：

- 保存相似但不适用的对象关系。

字段方向：

- `from_uri`
- `to_uri`
- `relation_type`
- `reason`
- `scope_constraint`
- `release_id`

### 7.2 `alias_entity_index`

用途：

- 保存业务术语、字段 alias、lookup/helper alias。

字段方向：

- `term`
- `normalized_term`
- `entity_type`
- `linked_uri`
- `scope`
- `weight`

### 7.3 `retrieval_step_trace`

用途：

- 记录多步 agentic retrieval。

字段方向：

- `trace_id`
- `step_no`
- `tool_name`
- `tool_input_hash`
- `tool_output_hash`
- `selected_uris`
- `policy_result`

### 7.4 `evaluation_case`

用途：

- 保存 golden set 和回归评估。

字段方向：

- `case_id`
- `query`
- `expected_scope`
- `expected_objects`
- `expected_refusal`
- `case_type`

### 7.5 `retrieval_evaluation_result`

用途：

- 保存每次评估结果。

字段方向：

- `run_id`
- `case_id`
- `mode`
- `top_k`
- `correct_object_found`
- `unsupported_claim_count`
- `refusal_correct`
- `latency_ms`

## 8. Day2 技术路线选择

### 8.1 继续 JDK 17

Day2 不需要因为 LLM/agentic retrieval 换语言。

Java 继续负责：

- service boundary
- policy gate
- persistence
- API/MCP
- tool orchestration
- trace/audit
- deterministic query

如果某些模型能力在 Python 更方便，可以通过外部服务接入，不需要把 RTS 主服务改成 Python。

### 8.2 OpenSearch 何时引入

不要因为“Day2”就自动引入 OpenSearch。

建议引入条件：

- 对象规模达到几万级以上。
- Lucene embedded 运维或并发成为瓶颈。
- 需要多实例共享索引。
- 公司已有或愿意维护 OpenSearch。
- 向量 + BM25 + aggregation 的统一需求明显。

否则继续 Lucene + PostgreSQL。

### 8.3 向量何时引入

建议先证明 BM25 + alias + card 不够，再引入向量。

向量评估标准：

- 对 fuzzy business term 的 recall@k 是否明显提升。
- 是否引入更多 wrong-scope 干扰。
- 是否降低 unsupported answer。
- 是否值得额外部署和 embedding 成本。

### 8.4 Reranker 何时引入

reranker 比向量更适合先试。

原因：

- 对象少时 over-fetch 成本低。
- reranker 可只看 top candidates。
- 不改变主索引架构。

约束：

- reranker 失败必须 fallback。
- reranker 不参与 hard gate。
- reranker 输入不应包含未授权 L2。

## 9. Day2 Evaluation Harness

Day2 必须把 evaluation 提升为核心工程能力。

评估对象：

- deterministic mode
- Day1 ask mode
- Day2 planner mode
- reranker on/off
- vector on/off
- negative retrieval on/off

指标：

- correct object top-1
- correct object top-3
- correct scope rate
- refusal correctness
- unsupported claim count
- dependency coverage
- impact candidate usefulness
- test candidate usefulness
- latency
- tool call count

评估集分类：

- straightforward exact queries
- fuzzy business term queries
- wrong product confusables
- COMMON vs product-specific override
- missing L2
- unresolved ambiguity
- dependency impact
- test planning

Day2 每个新能力必须通过 evaluation，而不是凭感觉上线。

## 10. Day2 Guardrails

Day2 的智能增强必须继续受治理约束。

### 10.1 Hard gates never become ranking features

以下永远是 hard gate：

- scope
- permission
- active release
- signoff
- dependency release state
- conflict/precedence
- L2 readability

### 10.2 Tool calls are policy-checked

每个工具调用都要检查：

- caller profile
- scope
- max reads
- object state
- release id
- output mode

### 10.3 Agentic retrieval has budgets

建议限制：

- max steps
- max L2 reads
- max dependency depth
- max candidates
- max tokens
- max runtime

### 10.4 Final answer remains grounded

LLM final answer 必须：

- 引用已读对象。
- 区分 facts/inferences/unknowns/suggestions。
- 不把 impact/test candidates 写成 final decision。
- 不输出 unsupported material claim。

### 10.5 No runtime learning into truth

Day2 仍然不允许：

- 把用户问题写成 memory。
- 把 LLM 答案写回 projection。
- 把 agent trace 写成 KB truth。
- 自动修改 canonical pack。

## 11. Day2 服务入口

Day2 可以形成三类入口。

### 11.1 Human / Q&A

面向人：

- `ask`
- `explain`
- `impact preview`
- `test planning candidates`

输出要清楚易读。

### 11.2 API / Pipeline-adjacent

面向系统：

- structured query
- dependency traversal
- coverage check
- impact candidate export

Day2 只建议 pipeline-adjacent，不建议直接作为 release gate。

### 11.3 MCP / External Agent

面向外部 agent：

- 细粒度只读工具
- 结构化返回
- trace id
- strict scope

外部 agent 不应拿到比 API caller 更多权限。

## 12. Day2 和 Day1 的边界

### 12.1 Day1 必须做

- JDK 17 service
- PostgreSQL projection store
- Lucene BM25
- local L2 store
- deterministic lookup
- controlled `/ask`
- minimal MCP adapter
- trace/refusal

### 12.2 Day2 可以做

- QueryPlannerV2
- multi-step tool orchestrator
- expanded MCP tools
- confusable/negative retrieval
- alias/entity boost
- reranker
- optional vector
- impact/test planning candidates
- evaluation harness
- governance-authorized evidence/review/report summary

### 12.3 Day2 仍不做

- auto publish
- auto signoff
- agent memory as truth
- pipeline release gate
- unrestricted raw evidence search
- unrestricted raw review/report search
- unrestricted autonomous agents

## 13. Day2 Rollout Strategy

Day2 应按 feature flag 渐进开启。

建议开关：

- `planner_v2_enabled`
- `tool_orchestrator_enabled`
- `reranker_enabled`
- `confusable_check_enabled`
- `vector_recall_enabled`
- `impact_candidates_enabled`
- `test_plan_candidates_enabled`
- `mcp_expanded_tools_enabled`

每个开关都应能：

- 单独开启
- 单独关闭
- 在 evaluation harness 中对比
- 在 trace 中显示

## 14. Day2 Exit Criteria

Day2 可以视为成熟，当满足：

- 多步 retrieval 在 golden set 上稳定优于 Day1。
- wrong-scope 或 confusable 错误没有上升。
- unsupported claim 明显受控。
- impact/test candidates 被 reviewer/QA 认为有增量价值。
- MCP 外部 agent 调用不会绕过 gate。
- trace 能完整回放 agentic steps。
- 所有 Day2 能力都可禁用回退到 Day1。

## 15. 最终判断

Day2 的价值不在于“让 RTS 更像通用 AI agent”，而在于让 RTS 在保持 truth 边界的前提下，处理更复杂的问题：

- 更复杂的自然语言
- 更复杂的依赖路径
- 更容易混淆的相似规则
- 更高价值的影响分析和测试规划
- 更稳定的外部 agent 接入

Day2 的原则仍然是：

**LLM 可以更主动地找 truth，但不能更自由地定义 truth。**
