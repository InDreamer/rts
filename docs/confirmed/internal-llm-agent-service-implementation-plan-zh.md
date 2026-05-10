<!-- docmeta
role: leaf
layer: 3
parent: docs/confirmed/README.md
children: []
summary: RTS service 内部 LLM agent 接入的完整非阶段性落地计划
read_when:
  - 需要把 RTS service 内部 LLM agent 接入落到代码和接口
  - 需要定义 managed harness、tool orchestration、scenario endpoint、MCP/tool mode、memory、grounding、evaluation 的完整目标状态
  - 需要从当前 Java service 继续实现完整 LLM agent 能力
skip_when:
  - 只需要当前 Day1 query service 的运行命令
  - 只需要 KB 到 runtime projection 的字段契约
  - 只需要历史 LLM agent roadmap 摘要
source_of_truth:
  - docs/confirmed/project-alignment-summary-zh.md
  - docs/confirmed/system-constitution-v1.md
  - docs/confirmed/kb-to-index-projection-contract-zh.md
  - docs/confirmed/day1-query-service-and-llm-harness-plan-zh.md
  - docs/confirmed/day2-agentic-retrieval-evolution-plan-zh.md
  - docs/confirmed/llm-harness-and-agent-integration-alignment-zh.md
  - docs/confirmed/final-llm-agent-service-plan-zh.md
  - src/main/java/com/rts
-->

# RTS Service 内部 LLM Agent 接入完整落地计划

## What This Covers

本文定义 RTS service 内部 LLM agent 接入的完整目标形态和落地工作包。

这里的“完整”不是把 RTS 变成自由 agent 平台，而是在现有 Java truth service 内形成一个可审计、可控、可验证的 managed LLM agent 闭环：LLM 可以理解任务、规划工具调用、读取 RTS truth、执行受控分析与表达、产出候选分析和驱动场景 adapter；RTS service 继续负责 release、scope、permission、L2 hash、dependency、grounding、refusal、trace 和 evaluation。

本文不按阶段拆分。所有工作包共同组成目标状态；执行时可以按依赖关系排队，但任何单个工作包都不能绕过 truth boundary 单独上线为默认能力。

## Source Boundaries

- `src/main/java/com/rts/query` — 现有 truth read boundary、query planning、scope/permission/refusal/trace/grounding 入口。
- `src/main/java/com/rts/llm` — 现有 controlled harness、provider-neutral client、OpenAI-compatible Responses adapter。
- `src/main/java/com/rts/agent` — 现有 agent support tools、analysis、answer view、context/memory、evaluation、governance、metrics、message、pipeline surface。
- `src/main/java/com/rts/api` — REST service surface，包括 `/ask`、query tools、agent tools、scenario support endpoints。
- `src/main/java/com/rts/mcp` — MCP/tool mode wrapper，必须共享同一 service layer。
- `src/main/resources/application.yml` — LLM provider、budget、feature flag 和 runtime control 配置。
- `src/test/java/com/rts` — golden、service、LLM Responses、projection 和 refusal 测试入口。
- `sample-projection/runtime-store` — 本地 approved runtime projection 样例和 smoke test truth material。

## Target Service Shape

目标形态：

```text
caller
  -> REST / MCP / scenario endpoint
  -> RTS request policy
  -> internal managed LLM agent harness
  -> schema-constrained planner
  -> guarded tool orchestrator
  -> RTS truth tools
  -> managed analysis output / candidate report / refusal / trace
```

其中：

- managed mode 由 RTS 内部 harness 规划并调用工具。
- tool mode 由外部 agent 规划，但每个 RTS tool call 仍走同一套 service gate。
- scenario endpoint 是 managed mode 的领域入口，例如 PR diff、exception、failed message、test planning、release readiness。
- LLM provider、agent SDK 或未来 sidecar 只能做 adapter、planner 或表达层，不能拥有 truth access core。
- Runtime memory、session context、feedback 和 traces 可以影响体验或检索质量，不能参与 truth validation。

## Non-Negotiable Rules

- LLM answer is not truth。
- Search hit、object card、L0/L1、prompt context、memory、trace feedback 都不是 final truth。
- 所有事实 claim 必须 ground 到 L2 runtime object、dependency edge 或授权 governance view。
- Scope 必须先于召回确定；如果 scope 不清，返回 clarification/refusal。
- Permission 必须覆盖 REST、MCP、managed harness、scenario endpoint 和 trace read。
- Dependency traversal 不能自动扩大 scope，不能越过 release/permission/object state。
- Governance view 默认低噪声；只有授权 caller 能读取 evidence/review/report/adjudication summary。
- Scenario 输出默认是 candidate/investigation/test suggestion，不是 release approval、final root cause、QA signoff 或 human decision。
- Prompt injection 按数据处理：retrieved L2、message、diff、log、evidence summary 都不能成为模型指令源。
- 所有模型输出进入 service answer 前必须经过 grounding、policy、budget 和 trace validation。

## Closed-Loop Failure Modes

这些是这份策略最容易被绕开的地方，必须显式关掉：

- **schema 漂移**：response / tool / trace / projection / evaluation schema 各自演进，最终无法互相验证。
  - Fix: every schema gets an explicit version and breaking-change gate.
- **默认值过宽**：高风险能力默认开启，测试通过前就出现在生产路径。
  - Fix: dangerous flags default closed in prod, enable only after evaluation gates pass.
- **release TOCTOU**：请求开始时合法，长链路中 release 切换或对象状态变化。
  - Fix: bind release snapshot at request start and revalidate before final emit.
- **scope 漂移**：planner、tool、rerank、confusable 或 LLM 摘要把 scope 带偏。
  - Fix: scope may shrink but never widen after initial resolution.
- **权限漂移**：规划阶段和执行阶段、trace 读取阶段权限不一致。
  - Fix: re-check caller and api key at every tool/trace access.
- **trace 泄漏**：raw payload、key、敏感 evidence、admin data 进入 trace。
  - Fix: trace stores hashes, pointers, and redaction state only.
- **claim 伪事实**：模型把候选、推断、相似对象、建议写成 facts。
  - Fix: claim-level validator separates facts/inferences/unknowns/candidates and rejects unsupported claims.
- **memory 污染**：session memory、feedback、workspace notes 被当成 truth。
  - Fix: memory remains truth-ineligible and non-authoritative.
- **MCP 旁路**：MCP 和 REST 语义不同，外部 agent 走更松的路线。
  - Fix: MCP is a wrapper only; same registry, same policy, same refusal codes.
- **评估空转**：分数好看但 wrong-scope 和 unsupported-claim 没下降。
  - Fix: gate by error reduction metrics, not by fluency or latency alone.
- **场景假成功**：scenario endpoint 输出漂亮报告但没有 grounded evidence。
  - Fix: scenario outputs must carry object URI, release id, content hash, and trace.
- **provider 绑定**：core logic silently depends on one model/provider behavior.
  - Fix: provider-neutral core, provider-specific adapter, deterministic fallback.
- **长链路失控**：多轮 model/tool 调用突破预算或产生状态漂移。
  - Fix: explicit tool/model/L2/dependency/latency/output budgets and replayable trace.
- **审计不可复现**：无法从 trace 重建 claim、tool order 和 evidence。
  - Fix: trace must be sufficient for replay and verification.
- **历史文档误读**：旧 route map、roadmap、reference 与当前策略混读。
  - Fix: current baseline wins; this plan points back to confirmed docs only.

## Strategy Closure Rules

这份策略只有在以下事实同时成立时，才算闭合：

- 所有高风险能力有明确默认值、启用条件、回退条件和审计条件。
- managed `/ask`、MCP、tool mode、scenario endpoint 共享同一 truth gate。
- claim validation 能拒绝 unsupported fact、missing L2、hash mismatch、prompt injection、permission leak、wrong scope。
- evaluation 有明确失败阈值，不是只看相对提升。
- trace 可重放并可证明 release、scope、permission、hash、budget、tool sequence。
- 生产默认保守关闭，验证通过后才逐项放开。
- 任何回答都能从 release snapshot、object URI、L2 hash 和 dependency path 追回去。

## Current Baseline

当前代码已经具备以下可复用底座：

- filesystem runtime projection store、active release、manifest validation、scope registry、object manifest、object cards、dependency edges、L2 content refs。
- Lucene scoped BM25、deterministic lookup、alias/entity expansion。
- `QueryService` 的 `plan/find/getObject/readContent/dependencies/query/trace`。
- `ControlledLlmHarness` 的 tool budget、model budget、L2 read budget、latency budget、LLM run trace 和 final answer validation。
- Provider-neutral `LlmClient` 和 OpenAI-compatible Responses adapter。
- REST `/ask`、query tools、agent tools、analysis、message、governance、pipeline、evaluation、metrics、feedback/memory endpoints。
- MCP tool catalog and wrappers。

当前实现对齐状态：

| Surface | 当前已完成 | 当前限制 / 未完成 |
|---|---|---|
| `/query`、`find`、object/L2/dependency/trace | deterministic truth/information read 已是稳定原子能力面。 | 不承担 managed scenario synthesis。 |
| `/ask` | endpoint、planner、service-owned orchestrator、tool execution、context/model draft、claim validator、answer view、trace 和 LLM run trace 已接入。 | 生产默认 `RTS_TOOL_ORCHESTRATOR_ENABLED=false`，此时 `/ask` 降级为 deterministic `/query` 风格信息服务；OpenAI-compatible adapter 当前仍主要把 grounded facts 写成可读 answer，不代表完整场景级 AI 分析已完成。 |
| Tool registry / REST / MCP | catalog、permission、refusal 和 schema metadata 已统一到同一批 RTS truth tools。 | expanded MCP 默认关闭，外部 tool mode 仍受 feature flag 和 permission 限制。 |
| Scenario endpoints | PR diff、exception、failed message、test planning、governance review endpoints 和统一 `scenario-report.v1` envelope 已存在；当前能输出 grounded candidate / information-service report、trace 和 warnings。 | 当前实现主要编排 deterministic analysis/message/governance support services；还不能宣称每个 scenario 都已进入 LLM-enhanced managed analysis normal mode。 |
| Context / memory | `truthEligible=false` 和 validator 边界已限制 memory/external input 不能支撑 facts。 | memory/context 仍是检索和交互辅助，不是 truth source。 |
| Evaluation / metrics | 已覆盖 scope、grounding、refusal、wrong-scope、unsupported-claim、permission leak、memory-as-truth、trace completeness 等安全风险。 | AI value metrics 已进入目标口径，但 adoption/usefulness/reviewer-time-saved 等正向指标仍需接入真实评估数据和默认开启门槛。 |
| Runtime defaults | tool orchestrator、confusable、impact/test candidates 和 expanded MCP 生产默认关闭；Provider timeout、不可用或 malformed output 使用 `model_provider_failure` structured refusal。 | 默认关闭是安全门槛，不改变 managed AI 是 AI-centric scenario 目标正常态的产品口径。 |

### Current-state authority

在“当前到底已经存在什么能力、哪些只是目标正常态或默认未开启能力”这个问题上，以上状态矩阵是 active baseline 的权威表述。若其他 confirmed 文档仍残留较早的阶段性描述，以本文的 current baseline、runtime defaults、scenario endpoint availability 和 degraded-mode semantics 为准。后续工作包描述的是目标闭环，不等于全部已经默认启用。

## Completion Definition

内部 LLM agent 接入完成时，RTS service 必须同时满足：

- `/ask` 能返回 human-readable grounded answer，并保留结构化 facts、inferences、unknowns、candidates、human decisions、citations、warnings、trace。
- Harness 能根据 intent 选择受控工具序列，而不是只执行单一固定链路。
- REST、MCP 和内部 harness 使用同一组工具实现和同一组 gates。
- 第一组 managed scenario endpoints 能把外部输入转换为 grounded managed analysis report。
- Tool calls、retrieved context、model output、grounding result、refusal reason 和 budget usage 都可追踪。
- Claim validation 能拒绝 unsupported fact、governance overreach、prompt injection 和未读 L2 claim。
- Context/memory/feedback 只影响检索辅助和交互体验，不会被当作 truth。
- Golden tests 和 evaluation harness 能证明正确 scope、正确 refusal、grounded answer、permission safety 和 trace completeness。

## Work Package A: Service Contract And Response Model

目标是把所有 LLM agent 输出固定到一个 service contract，而不是返回自由文本。

落地内容：

- 扩展 `ServiceAnswer` 或新增 agent answer envelope，保留 `answerType`、`scope`、`releaseId`、`facts`、`inferences`、`unknowns`、`candidateSuggestions`、`humanDecisions`、`citedObjects`、`dependencies`、`traceId`、`refusal`、`warnings`、`answer`。
- 为 scenario report 定义统一 envelope：`status`、`scenarioType`、`inputSummary`、`facts`、`inferences`、`candidates`、`unknowns`、`nextEvidenceNeeded`、`citations`、`groundingMap`、`traceId`、`budgetUsage`。
- 明确四种 response view：human、agent、audit、pipeline。视图可以不同，但事实边界一致。
- 把 `/views/answer` 从辅助 shaping 提升为所有 managed answer 的统一展示层。
- 让 refusal/clarification 也遵守同一个 envelope，而不是异常或散落字段。
- 所有 envelope 和 schema 必须带 version、compatibility note、breaking-change rule。

验收门禁：

- 任何 successful answer 至少有 trace id、release id、resolved scope、cited object 或明确的 no-fact/refusal reason。
- 任何 fact 都能映射到 `GroundingEvidence`。
- Candidate/inference/unknown 不允许进入 facts 字段。
- 新字段默认 deny-by-default，直到测试、文档和路由同步完成。

## Work Package B: Internal Agent Planner

目标是把 query intent、scope mediation、anchor extraction 和 tool plan 从简单 resolver 演进为 schema-constrained planner。

落地内容：

- 保留 deterministic resolver 作为第一道 scope/anchor safety pass。
- 引入 `AgentPlan` 模型，字段至少包括 `intent`、`scenarioType`、`scope`、`anchors`、`requiredState`、`toolPlan`、`budgets`、`expectedEvidence`、`clarificationQuestion`、`refusalIfMissing`。
- Planner 可以调用 LLM，但输出必须是 JSON/schema constrained，并由 service validator 复核。
- Scope 不清时只允许输出 clarification，不允许进入 find/read。
- Strong anchors 如 URI、rule id、target path、source anchor 优先 deterministic lookup；LLM 不能把弱语义相似结果升格为确定对象。
- Planner 输出必须记录到 trace，并可 replay。
- Planner must record scope snapshot and release snapshot.

验收门禁：

- planner 不能直接返回业务事实。
- planner 不能要求读取 caller 无权访问的 scope。
- planner 失败时 fallback 到 deterministic Day1 resolver 或返回 structured refusal。
- planner may not widen scope based on semantic similarity alone.

## Work Package C: Guarded Tool Orchestrator

目标是让 internal LLM agent 能执行多步工具调用，但所有调用都被 service policy 管住。

落地内容：

- 建立统一 `RtsTool` registry，REST、MCP、harness 共用同一 tool implementation。
- 每个 tool 定义 name、purpose、input schema、output schema、required permission、budget cost、allowed intents、trace redaction rule。
- Orchestrator 按 `AgentPlan.toolPlan` 执行，不允许模型随意构造未知 tool。
- 每步调用前做 policy check：scope、caller、release、purpose、outputMode、feature flag、budget。
- 每步调用后做 output check：object state、L2 hash、dependency scope、governance redaction、retrieved token estimate。
- 对 `read_object_l2`、`read_evidence_summary`、`get_dependency_subgraph` 设置独立预算和最大深度。
- Tool output 进入 model context 前做 compaction，保留 URI/hash/path，不丢失 grounding。
- REST、MCP、harness must share the same registry and refusal codes.

必备 tool catalog：

- scope/navigation：`resolve_scope`、`list_scopes`、`search_scopes`、`get_scope_summary`、`get_pack_navigation`。
- object read：`find_objects`、`find_by_target_path`、`find_by_source_anchor`、`find_by_lookup_key`、`get_object_card`、`read_object_l2`。
- dependency：`get_dependencies`、`find_reverse_dependencies`、`get_dependency_subgraph`。
- disambiguation：`find_confusable_objects`、`find_confusable_scopes`。
- governance：`read_evidence_summary`、`governance_review`、`record_human_decision`。
- analysis：`analyze_impact`、`plan_tests`、`compare_rules`、`explain_conflict`、`check_grounding`。
- message：`parse_raw_message_candidate`、`map_source_fields_to_rules`、`resolve_required_lookups`、`simulate_rule_application`、`assemble_target_message_candidate`、`validate_target_message_grounding`。
- operational：`get_trace`、`trace_report`、`metrics_snapshot`、`run_evaluation`、`feature_flags`。

验收门禁：

- Unknown tool 直接 refusal。
- Tool budget exhausted 直接 refusal，不返回半可信答案。
- Tool trace 必须含 tool name、input hash、output hash、selected URI、policy result。
- Tool trace must be replayable and check object-state stability.

## Work Package D: Context Builder

目标是把 retrieved context、session context、workspace context 和 governance context 分层提供给 LLM。

落地内容：

- 定义 `ContextItem` 分层：system policy、caller profile、query plan、object card、L2 fact、dependency、governance warning、trace metadata、session memory、workspace context、inference、unknown。
- 每个 context item 必须标记 `truthEligible`。
- 只有 L2/dependency/authorized governance view 可以 `truthEligible=true`。
- User query、diff、log、raw message、session memory、workspace note 都必须 `truthEligible=false`。
- Context builder 负责 token budget、dedupe、URI/hash preservation 和 prompt-injection neutralization。
- Context builder 输出也写入 trace 或 hash summary，保证审计可定位。
- Context builder must preserve original evidence pointers and redaction state.

验收门禁：

- Memory 和 external input 不能通过 validator 成为 fact source。
- Governance raw text 不进入默认 answer context。
- Context compaction 后仍能保留 citation object URI 和 content hash。

## Work Package E: Controlled Analysis-And-Expression And Claim Validation

目标是让模型在 authority boundary 内负责受控分析与表达，而不是负责决定事实。

落地内容：

- LLM 输出采用结构化草稿：`answerText`、`claims`、`citations`、`inferences`、`unknowns`、`candidates`、`warnings`。
- 每个 claim 必须引用一个或多个 `GroundingEvidence`。
- Claim validator 对照本次 trace 的 L2 reads、dependency edges、governance summaries 做逐 claim 校验。
- 对不能验证的 claim：删除、降级为 inference/unknown，或拒答。
- 对 prompt policy 违规内容、raw governance overreach、未授权 evidence、跨 scope fact 直接 refusal。
- 最终 answer text 只能由 validated facts、allowed inferences、unknowns 和 candidate suggestions 组成。
- `FinalAnswerValidator` 从 uri-level validation 升级到 claim-level validation。
- Claim validation must tag facts, inferences, unknowns, and candidates explicitly.

验收门禁：

- LLM controlled analysis text 不得覆盖 service answer 的 structured facts。
- Unsupported claim count 必须进入 metrics。
- Grounding check endpoint 可以解释每个 claim 的 grounded/rejected/warning 状态。

## Work Package F: Managed `/ask`

目标是让 `/ask` 成为完整 managed internal agent 入口。

落地内容：

- `/ask` 接收 query、caller、scopeHint、outputMode、maxToolCalls、可选 session/workspace context。
- 处理链路：policy guard -> planner -> orchestrator -> context builder -> model draft -> claim validation -> answer view -> trace。
- 支持 clarification：scope 缺失、anchor 不足、多个候选冲突、权限不足但可提供更具体 scope 时返回明确问题。
- 支持 partial：找到候选但 L2/gov view 不足时返回候选和缺失项，不编造答案。
- 支持 answer：只有 L2/dependency/governance grounding 通过后返回。
- 支持 refusal：权限、release、schema、hash、budget、prompt policy、unsupported claim、unresolved ambiguity。

验收门禁：

- `/ask` 不直接读 store，只能通过 tool registry。
- `/ask` 返回 `answer` 字段时，必须同时返回 structured facts/citations/trace。
- 模型不可用时可退化到 deterministic `/query` 或 structured refusal，不能返回空白成功。
- `/ask` must re-check release and permission before final emit.

## Work Package G: Managed Scenario Endpoints

目标是把外部复杂输入交给 RTS 内部 harness，而不是要求调用方自己集成 agent。

必须覆盖的 scenario：

- PR diff impact analysis：从 diff 中抽取 changed source field、target path、mapping term、lookup/helper/rule anchor，输出 impact candidates、test candidates、unknowns、citations、trace。
- Exception investigation：从 exception、stack、log、failure location 中抽取 message type、field、rule/helper/lookup anchor，输出 investigation paths、likely related rules、unknown inputs、next evidence needed、trace。
- Failed message analysis：从 raw/failed message 中解析 source fields，匹配 rules/lookups/helpers，输出 target candidate、unsupported fields、lookup requirements、grounding validation。
- Test planning：基于 rule condition、dependency、lookup fallback、examples、risk flags 生成 positive/negative/boundary/regression candidates。
- Governance review assistant：基于 authorized governance view 生成 conflict/ambiguity candidates、reviewer questions 和 human decision record draft。
- Release/pipeline report：输出 deterministic machine status，加上可选 LLM-readable explanation；不能把 explanation 当 gate result。

统一要求：

- Scenario 输入永远不是真相，只是线索。
- Scenario 输出默认是 candidate/investigation/test suggestion。
- 所有 scenario 都必须回到 object URI、L2 hash、dependency path 或 authorized governance summary。
- 每个 scenario 都要有 trace report 和 grounding check。
- Scenario reports must degrade to useful information-service output when LLM is disabled.

验收门禁：

- PR diff、exception、failed message 至少各有正常、scope unclear、no object found、permission denied、unsupported claim 的 golden cases。
- Scenario endpoint 不允许返回 release approval、final root cause 或 human decision，除非读取到已记录的人类裁决。

## Work Package H: Tool Mode And MCP

目标是让外部 agent 能安全复用 RTS truth tools。

落地内容：

- MCP `/tools` 返回稳定 catalog，包含 tool schema、purpose、required fields、possible refusal reasons。
- MCP tool 和 REST tool 共用 service implementation，不复制业务逻辑。
- 外部 agent 的每次 tool call 必须传 caller、api key、scope/purpose/output mode；缺失时拒绝或要求 clarification。
- MCP 返回结构化结果：facts、candidates、unknowns、warnings、groundingMap、traceId。
- 外部 agent 可以计划 broader workflow，但不能拿 RTS tool output 绕过 grounding。
- `rts_ask` 和 `rts_contextual_ask` 明确是 managed mode wrapper；其他 `rts_*` 是原子 tool mode。
- MCP catalog must encode the same permission matrix as REST.

验收门禁：

- MCP minimal catalog 和 expanded catalog 都可通过 feature flag 控制。
- MCP tool 不得暴露 direct file path、raw store path 或未授权 governance raw text。
- REST/MCP 同一输入应得到同一 refusal/permission 行为。

## Work Package I: Memory, Feedback, And Context

目标是支持交互连续性和检索质量改进，但不让 memory 变成 truth。

落地内容：

- Session memory 只记录 user preference、recent scope、selected object、feedback summary、retrieval hint。
- Feedback route 分为 trace_feedback、retrieval_quality_queue、card_improvement_candidate、review_workflow、ignored_not_truth。
- Memory write 默认 `truthEligible=false`。
- 只有 governance workflow 中的 human decision record 能成为后续 truth pipeline 的输入，而且仍不能直接修改 runtime projection。
- Contextual ask 可以读取 session/workspace memory，但 validator 不允许 memory 支撑 facts。
- Memory and feedback are immutable inputs to truth, never truth sources.

验收门禁：

- metrics 中 `memoryAsTruthCount` 必须保持 0。
- 用户纠错只能进入 feedback/review queue，不能修改 active release answer。
- Reopened ambiguity 必须走新 truth/release path。

## Work Package J: Security And Prompt Policy

目标是把 LLM agent 风险作为服务边界处理，而不是只靠 prompt。

落地内容：

- User input、retrieved content、diff、log、raw message、evidence summary 全部按 untrusted data 处理。
- Prompt policy guard 检查用户输入和模型输出。
- Tool call allowlist、schema validation、budget enforcement、scope gate、permission gate 全部在 Java service 内执行。
- LLM adapter 只接收 compacted tool outputs，不接收 store credentials、filesystem paths 或 admin secrets。
- Trace redaction 对 API key、raw external payload、敏感 source excerpt 做 hash 或 summary。
- Admin operations 如 projection ingest、human decision record、governance raw expansion 需要更高 permission。
- Model/provider failures must produce refusal or deterministic fallback, never silent success.

验收门禁：

- Prompt injection 样例不能让模型调用未授权工具或返回未授权事实。
- Caller id spoofing、wrong API key、cross product scope、non-active release 都必须拒绝。
- Model provider failure、timeout、invalid JSON、malformed output 都必须 refusal 或 deterministic fallback。

## Work Package K: Provider Adapter And Runtime Config

目标是保持 core provider-neutral，同时让第一套 model adapter 可运行、可替换、可观测。

落地内容：

- `LlmClient` 继续作为 provider-neutral 接口。
- OpenAI-compatible Responses adapter 支持 model、base URL、API key、wire API、store flag、reasoning effort、max tokens。
- LLM request body 必须只包含 system policy、user task、validated grounded context 和 expected output schema。
- 未来 Claude/LangChain/LangGraph 只能作为 adapter/sidecar，不进入 truth core。
- 所有 LLM 能力受 feature flags 和 budgets 控制：planner、orchestrator、rerank、confusable、vector、impact/test candidates、MCP expanded tools。
- High-risk feature flags default closed in production until evaluation thresholds pass.

验收门禁：

- `rts.llm-enabled=false` 时 service 仍能 deterministic query/refusal。
- `rts.llm-enabled=true` 且 provider 不可用时不影响 truth read APIs。
- LLM run trace 包含 model、prompt version、tool outputs hash、final output hash、validation result、duration。

## Work Package L: Retrieval And Ranking

目标是提升召回质量，但不改变 truth authority。

落地内容：

- Deterministic lookup 优先：URI、object id、target path、source anchor、lookup key、helper id。
- Scope gate 先于 BM25、alias、confusable、rerank、vector。
- Alias/entity boost 可用于 scoped recall。
- Confusable/negative retrieval 用于提醒“相似但不适用”，不能自动扩大 truth scope。
- Rerank 只能排序候选，不能生成事实。
- Vector 仅在评估证明必要后启用，且只能在 scoped candidate discovery 中使用。

验收门禁：

- Wrong-scope answer count 不得因 rerank/vector 增加。
- 只有 search/card 命中而无 L2 时，必须 partial/refusal。
- Retrieval trace 能解释 deterministic、BM25、alias、confusable、rerank 的贡献。
- Retrieval improvements must be proven against wrong-scope regression, not only recall gains.

## Work Package M: Governance And Human Decision Support

目标是让 LLM agent 帮助 review，而不是替人裁决。

落地内容：

- Governance assistant 读取 authorized evidence/review/report summaries。
- 输出 conflict candidates、ambiguity candidates、reviewer questions、decision options。
- Human decision record 只记录裁决，不直接修改 runtime truth。
- 人工裁决进入后续 canonical truth/release process，再由 projection publisher 发布。
- Service answer 可以展示 human decisions，但必须来自 governed record 或 projection。

验收门禁：

- AI suggestion 不得写入 humanDecisions。
- Governance unauthorized caller 只能看到 operational warning 或 refusal。
- Draft/review/conflict-open 状态不能作为默认 approved truth 回答。

## Work Package N: Evaluation, Metrics, And Golden Tests

目标是让 LLM agent 上线标准可测，而不是凭体感。

必须跟踪：

- scope resolution accuracy
- clarification precision
- top-K object recall
- grounded answer rate
- unsupported claim rate/count
- wrong-scope count
- refusal correctness
- L2 read efficiency
- trace completeness
- user correction rate
- card improvement yield
- memory-as-truth count
- permission leak count
- draft-as-approved count
- conflict-hidden count
- impact/test candidate adoption and usefulness
- review question usefulness
- conflict simplification quality
- evidence review time saved

必须建设的测试集：

- normal rule Q&A
- ambiguous scope clarification
- missing L2 refusal
- hash mismatch refusal
- unauthorized scope refusal
- non-active release refusal
- dependency cross-scope refusal
- governance unauthorized refusal
- prompt injection refusal
- unsupported model claim rejection
- PR diff impact scenario
- exception investigation scenario
- failed message grounding scenario
- MCP/REST parity
- provider timeout and invalid output

验收门禁：

- Golden tests 覆盖每个 refusal reason 的关键路径。
- LLM integration tests 不依赖真实外部 provider；使用 local fake server。
- Evaluation output 可以被 metrics snapshot 聚合。
- Evaluation must have explicit pass/fail thresholds for default enablement.
- 对任何准备默认开启的 feature flag，至少满足：unsupported approved facts = 0、wrong-scope automation-visible outputs = 0、trace completeness = 100%、refusal correctness 达到文档定义阈值。
- 一旦默认开启后出现阈值回退，必须能够通过 feature flag 回滚到上一安全模式。

## Work Package O: Observability And Audit

目标是任何答案都能被解释、复盘和审计。

落地内容：

- Query trace 记录 entrypoint、caller、query text hash or redacted text、query plan、resolved scope、candidate URIs、selected URIs、L2 read URIs、tool calls、tool step hashes、grounding map、budget usage、status。
- LLM run trace 记录 model、prompt version、tool outputs hash、final output hash、validation result、duration。
- Scenario trace 记录 external input summary/hash、extracted anchors、selected tools、managed analysis report、grounding/refusal。
- Trace report 提供 human/audit/pipeline views。
- Metrics snapshot 汇总 evaluation 和 runtime counters。

验收门禁：

- 成功、partial、clarification、refusal 都必须有 trace。
- Trace 不泄露 API key、raw secret、未授权 raw evidence。
- Grounding check 能从 trace 重建 claims 和 evidence。
- Trace retention/redaction policy must be documented and bounded.

## Work Package P: API Surface

目标是把内部 agent 能力暴露成稳定服务面。

核心 REST：

- `/api/v1/ask` — managed Q&A。
- `/api/v1/ask/contextual` — 带 session/workspace context 的 managed Q&A。
- `/api/v1/query`、`/api/v1/find`、`/api/v1/objects/*` — deterministic truth read。
- `/api/v1/tools/*` — agent tool surface。
- `/api/v1/analyze/*` — impact、test plan、grounding、compare、conflict、raw-message candidate。
- `/api/v1/message/*` — failed/raw message support surface。
- `/api/v1/governance/*` — governance assistant and human decision record。
- `/api/v1/pipeline/*`、`/api/v1/reports/*` — pipeline and audit reports。
- `/api/v1/evaluation/run`、`/api/v1/metrics/snapshot` — quality loop。

核心 MCP：

- `rts_ask`、`rts_contextual_ask`。
- `rts_find_objects`、`rts_read_object_l2`、`rts_get_dependencies`、`rts_get_trace`。
- Expanded catalog 中暴露 scope、confusable、impact、test、message、governance、metrics、feedback tools。
- Tool catalog entries must include permission requirements and refusal reasons.

验收门禁：

- API caller guide 必须同步说明 request/response/refusal 字段。
- MCP tool catalog 必须和 REST capability map 对齐。
- breaking schema change 必须更新 docs 和 tests。

## Work Package Q: Data And Projection Requirements

目标是确保 LLM agent 有足够的 governed truth material，而不是靠自然语言猜。

Runtime projection 必须继续提供：

- release manifest
- scope registry
- object manifest
- object cards
- L2 runtime objects
- dependency edges
- field bindings
- aliases
- confusables
- governance access refs
- evidence/review/report summaries
- caller profiles

为了完整 agent 支持，L2 应尽量包含：

- structured logic pipeline
- target/source bindings
- conditions and fallback behavior
- lookup/helper/rule dependencies with purpose
- examples and edge cases
- warnings and allowed governance summary
- content hash and schema version

验收门禁：

- 只有 summary/card 而无 L2 的对象不能支持 factual answer。
- Dependency/field binding 缺失时，impact/test/message scenario 必须返回 unknown 或 partial。
- Governance summary 缺失时，governance explanation 不得补写原因。
- Citations must include release id, object URI, and content hash.

## Work Package R: Rollout And Operational Control

目标是让完整 LLM agent 能力可以安全启停、降级和排障。

落地内容：

- 默认 truth read APIs 不依赖 LLM provider。
- 每个高风险能力都有 feature flag。
- Budgets 覆盖 model calls、tool calls、L2 reads、dependency depth、retrieved tokens、latency、output tokens。
- Provider credentials 只走环境变量或 secret manager，不写入 projection 或 trace。
- Runbook 覆盖 LLM 开关、provider 配置、fake server tests、trace review、metrics snapshot、rollback。
- API caller guide 覆盖调用方如何处理 clarification/refusal/partial/candidate。
- Production default should be conservative; enablement requires explicit review and evaluation evidence.

验收门禁：

- 禁用 LLM 后，service 保留 deterministic `/query` 和 tool mode。
- 禁用 expanded MCP 后，minimal tools 仍可用。
- 预算耗尽、provider timeout、invalid output 都能返回稳定 refusal。

## Implementation Checklist

[x] 固定 agent answer envelope 和 scenario report envelope。
[x] 建立统一 tool registry，并让 REST/MCP/harness 共用实现。
[x] 引入 `AgentPlan` 和 planner validator。
[x] 将 `ControlledLlmHarness` 改造为 planner -> orchestrator -> context builder -> model -> claim validator 链路。
[x] 升级 `FinalAnswerValidator` 到 claim-level grounding validation。
[x] 将 `/ask` 的 grounded managed answer 作为 validated service answer view 返回。
[x] 为 PR diff、exception、failed message、test planning、governance review 建立 scenario adapters 和统一 candidate report envelope。
[x] 明确 `ContextItem.truthEligible` enforcement，让 memory/external input 不能支撑 facts。
[x] 扩展 trace，覆盖 tool step、context hash、grounding map、budget、scenario input summary。
[x] 补齐 feature flags 和 runtime config 文档。
[x] 补齐 REST/MCP parity tests。
[ ] 将第一个 AI-centric scenario 从 deterministic/candidate support surface 升级为真正由 managed harness 进行场景规划、证据选择、候选解释、unknown/next-evidence synthesis 的正常产品模式。
[ ] 将 OpenAI-compatible adapter 的 prompt strategy 从 answer organizer 升级为受控分析草稿生成，同时继续由 RTS validator 裁决事实。
[ ] 为正向 AI value metrics 接入评估数据和默认开启阈值。
[x] 补齐 LLM fake-provider integration tests。
[x] 补齐 prompt injection、unsupported claim、permission leak、wrong-scope、memory-as-truth golden tests。
[x] 更新 API caller guide 和 runbook。
[x] 运行 Java tests 和 documentation contract validator。

## Safe Edit Surface

安全修改：

- 增加 agent-specific records、tool registry、planner/orchestrator、context builder、claim validator。
- 扩展 REST/MCP wrappers，只要它们继续调用同一 service layer。
- 增加 feature flags、budgets、tests、docs。
- 增加 scenario adapters，输出 candidate/report，不改变 runtime truth。

需要谨慎：

- 修改 `ServiceAnswer` 等公共 response model 时，要同步 API guide、MCP wrappers 和 tests。
- 修改 permission/refusal semantics 时，要覆盖 REST、MCP、harness 和 trace。
- 修改 projection schema 时，要同步 sample projection、store validation 和 contract docs。
- 引入 provider SDK 或 workflow framework 时，必须保持 provider-neutral core。

禁止：

- 让 LLM 或外部 agent 直接读取 projection store 文件并自行回答。
- 让 memory、feedback、diff、log、raw message 支撑 facts。
- 让 search hit、card、L0/L1 替代 L2。
- 让 scenario endpoint 输出 final approval、final root cause、QA signoff 或 unrecorded human decision。
- 把 permission、release、hash、grounding 只写进 prompt。

## Related Docs

- `project-alignment-summary-zh.md` — RTS identity and truth boundary。
- `system-constitution-v1.md` — non-negotiable safety principles。
- `kb-to-index-projection-contract-zh.md` — runtime projection and L2 contract。
- `day1-query-service-and-llm-harness-plan-zh.md` — current controlled query/harness baseline。
- `day2-agentic-retrieval-evolution-plan-zh.md` — controlled retrieval and MCP evolution constraints。
- `llm-harness-and-agent-integration-alignment-zh.md` — managed mode/tool mode and framework boundary。
- `final-llm-agent-service-plan-zh.md` — compact final service roadmap。
