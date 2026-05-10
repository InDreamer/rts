<!-- docmeta
role: leaf
layer: 3
parent: docs/confirmed/README.md
children: []
summary: 以做出强 managed AI normal mode 为目标、保持 RTS truth authority 的内部 LLM agent 完整非阶段性落地总纲
read_when:
  - 需要把 RTS service 内部 LLM agent 接入落到代码和接口
  - 需要定义 managed harness、多轮 agent runtime、tool orchestration、scenario endpoint、MCP/tool mode、memory、grounding、evaluation 的完整目标状态
  - 需要解释工具参数、scope、release、dependency、上下文资格和 RTS service 权威边界
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

本文不是阶段性 roadmap，也不是“先接一点 LLM 再慢慢增强”的过渡方案。

本文的目标只有一个：把 RTS service 内部 managed LLM agent 尽可能直接做成完整可用的正常产品形态，让 AI 在 RTS 中真正承担受控分析、证据选择、候选推理、next-evidence synthesis、场景解释和多步工具编排的工作，同时继续把 truth authority、scope、release、permission、grounding、trace 和最终事实裁决牢牢留在 RTS service 内部。

这意味着本文首先服务于“把东西做出来”，而不是把能力拆成一个长期阶段叙事。执行时可以按依赖关系排队，但叙事上不再把“近期弱版本 / 后期完整版本”当主目标；如果某个实现形态不能把 managed AI normal mode 推向更强闭环，只能算过渡措施，不算本文定义的完成态。

因此，这份文档要同时回答三个问题：

- 完整的 RTS internal managed agent 正常态到底长什么样。
- 当前 baseline 已经具备哪些骨架，哪些地方仍然只是保守默认或信息服务式能力。
- 还缺哪些关键闭环，才会让 RTS 从“带一点 LLM 的 truth service”真正升级成“AI 能明显发挥作用、但 truth boundary 不失守的 managed agent service”。

这里的“完整”也不是把 RTS 变成自由 agent 平台，而是在现有 Java truth service 内形成一个可审计、可控、可验证、AI 价值真实可见的 managed LLM agent 闭环：LLM 可以理解任务、提出工具调用计划、草拟受控分析与表达、产出候选解释、unknowns、review questions 和 next steps，并驱动 scenario adapter；RTS service 继续负责 release、scope、permission、L2 hash、dependency、grounding、refusal、trace、evaluation，以及工具参数的权威解释和执行。

如果做不到这一点，系统即使接上了模型，也仍然更像 deterministic information service 加 answer organizer，而不是本文要的 internal managed agent。

## Product Goal: What “Good” Looks Like

本文追求的不是“模型接进来且不出大错”，而是“AI 在 RTS 中开始像一个真正有用、但被严格约束的分析同事”。

具体来说，完整目标态下的 RTS internal managed agent 应该具备以下正常产品表现：

- 用户通过 `/ask` 或 scenario endpoint 进入后，系统能先理解问题，再在受控范围内主动选择下一步证据，而不是只把现有 deterministic 结果换一种说法。
- 系统能在 scope、release、permission 和 budget 不变的前提下执行多步 tool loop，按观察结果继续取证、收缩问题、识别 unknown、形成更像分析结论的候选解释。
- 系统面对 PR diff、exception、failed message、test planning、governance review 等场景时，不只是返回静态信息，而是能给出有 grounded evidence 支撑的 candidate analysis、risk hints、test ideas、next evidence needed 和 reviewer-facing explanation。
- AI 开启后，收益不只是“可以回答”，而是应体现在更少的人肉检索、更低的 reviewer 理解成本、更高的首轮分析可用度，以及更像样的 multi-step managed analysis 体验。
- 同时，所有这些收益都不能通过放松 truth boundary 获得；AI 的价值来自更强的受控分析与编排，而不是来自让模型直接决定事实。

换句话说，本文的成功标准不是“RTS 已支持 LLM”，而是“RTS 已拥有一个强 managed AI normal mode，而且这个 normal mode 没有破坏 truth authority”。

## Critical Capability Loop

无论入口是 `/ask`、MCP managed wrapper 还是 scenario endpoint，只要目标是“让 AI 真正发挥作用”，都必须先形成同一条 service-owned capability loop：

```text
user/scenario input
  -> policy + release/scope/caller binding
  -> planner forms constrained agent plan
  -> orchestrator executes guarded multi-step tool loop
  -> context builder preserves grounded evidence
  -> model drafts structured analysis
  -> claim validator accepts/rejects each claim
  -> answer/report compiler emits managed output
  -> trace supports replay, audit, and evaluation
```

这条链路是否成立，决定 RTS 当前到底只是“带一点 LLM 的信息服务”，还是已经成为“AI 能主动选证据、形成候选解释、表达 unknown 和 next evidence、但仍由 service 裁决事实”的 managed agent service。

对本文而言，下面这些能力不是彼此独立的增强项，而是这条闭环中的刚性环节：

- 没有 planner，AI 就不能在受控前提下真正理解问题和形成 tool plan。
- 没有 service-owned orchestrator，AI 就无法形成像样的多步取证和 observe/revise 能力。
- 没有 context builder，grounding、citation 和 evidence preservation 就会在压缩中失真。
- 没有 claim validator，模型生成的分析草稿就无法被裁决为可发布事实。
- 没有 step-level trace，系统就不能证明为什么选了这些对象、这些参数为什么合法、哪些 claim 为什么被接受或拒绝。

因此，评估工作包时不应只看“是否又多了一个 endpoint / tool / prompt”，而要看它是否把这条闭环往完整 managed AI normal mode 推进了一步。

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

完整 internal agent runtime 还必须显式建模：

```text
AgentSession
  -> AgentRun
  -> TurnIntake
  -> release/scope/caller snapshot
  -> AgentPlan
  -> tool loop: propose -> validate -> execute -> observe -> revise
  -> ContextSnapshot
  -> LlmDraft
  -> claim validation
  -> ServiceAnswer / ScenarioReport
  -> replayable trace and non-truth memory update
```

这条链路里的关键区别是：

- LLM 可以提出 intent、tool plan、tool arguments、analysis draft 和 candidate next steps。
- RTS service 必须解释和绑定这些参数，决定 allowed/refused/clarification/partial，并实际执行工具。
- tool arguments 是请求，不是授权。`scope`、`release_id`、`uri`、`dependency_depth`、`purpose`、`caller_id`、`trace_id` 等参数只有经过 RTS service 校验后才生效。
- 一个 agent run 必须绑定 release snapshot、resolved scope、caller profile、budget envelope 和 trace id；后续多步工具调用只能缩小或细化上下文，不能自行扩大 scope、替换 release 或伪造 trace。

换句话说，RTS service 拥有的不是 canonical truth，而是 runtime authority：参数解释权、工具执行权、上下文资格判断、事实裁决、组合能力状态机和审计账本。LLM 是分析和草拟层；RTS service 是门禁、执行器、裁判和可复盘记录者。

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
- 工具参数不能自证合法；`scope`、`release_id`、`uri`、`purpose`、`dependency_depth`、`caller_id`、`trace_id` 必须被 RTS service 绑定、校验和记录。
- 多轮 agent run 不能把不同 release、不同 scope、不同 caller、不同 trace 的结果混成一个 answer。
- LLM 和外部 agent 不得直接拥有 tool execution authority；它们只能提出计划或调用受控 REST/MCP wrapper。
- 原子工具输出只能按其声明的 truth output type 使用：card/search 是导航，L2/dependency/authorized governance summary 才能支撑 fact。

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
- **参数伪授权**：LLM 或外部 caller 在 tool arguments 中填入 `release_id`、`scope` 或 `uri`，service 误以为参数存在就代表授权成立。
  - Fix: parameters are hints/requests; service binds active release, resolves scope, verifies URI ownership, checks caller permission, and records the policy result.
- **多轮状态污染**：上一轮 selected object、session memory、旧 trace 或旧 release 被下一轮直接复用为事实依据。
  - Fix: session state is non-truth; every factual answer must re-read current release L2/dependency/governance material in the current run.
- **工具组合旁路**：scenario 直接调用底层 service 或拼接 store 数据，绕过统一 tool registry 和 trace。
  - Fix: composite capabilities are recipes over registered atomic tools; direct store access is forbidden for managed outputs.
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
- agent run 有显式状态机、release/scope/caller snapshot、预算 envelope、停止条件和 replayable step log。
- tool contract 明确 input schema、output schema、permission、budget、truth output type、redaction rule 和 allowed intents。
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

这些底座说明 RTS 已经不是从零开始；truth read、tool surface、基础 harness、validator、scenario/report surface 和 trace 骨架都已经存在。

但这不等于“强 managed AI normal mode 已经成形”。当前实现更多说明 RTS 已经具备了进入 internal agent 闭环的骨架，而不是说明完整 AI 价值已经兑现。

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

### What exists now vs. what is still missing

为了避免“已有骨架”和“完整目标态”被混读，这里明确区分：

- **已经存在的** 是：truth read surface、统一 tool surface、基础 harness、claim validation、trace 骨架、scenario/report envelope，以及在保守默认下可运行的 `/ask` 和 scenario support 能力。
- **尚未真正成形的** 是：更强的多步 managed analysis normal mode，也就是能稳定执行 service-owned plan/observe/revise、多步取证、context-preserving synthesis、claim-by-claim裁决，并在核心场景里明显提升 AI 首轮分析价值的那一层能力。
- **因此当前最大缺口** 不是“再接一个模型能力”或“再补几个 endpoint”，而是把这些已有骨架真正闭合成一条更强的 managed AI capability loop。

### Why prior RTS LLM agent attempts often feel weak

如果只停留在当前形态，RTS 很容易看起来已经“有 LLM 了”，但实际体验仍然偏弱，原因通常不是 prompt 不够花，而是以下几类骨架没完全闭合：

- AI 只能整理已有 deterministic read，不能稳定地自己在受控范围内决定下一步证据。
- tool orchestration 还不够像真正的多步 loop，observe/revise 能力有限。
- context compaction、citation preservation 和 claim validation 还不足以支撑更强的分析草稿。
- scenario surface 仍偏 support/report，而不是 AI-first managed analysis normal mode。
- 正向 AI value metrics 还没有形成默认开启依据，所以系统更容易长期停留在“安全但保守”的状态。

这也是本文后续工作包的真正指向：不是把现有 LLM 接入再抛光，而是把 RTS internal agent 补齐成一个 AI 价值更强、但依然严格受管的正常模式。

### Current-state authority

在“当前到底已经存在什么能力、哪些只是目标正常态或默认未开启能力”这个问题上，以上状态矩阵是 active baseline 的权威表述。若其他 confirmed 文档仍残留较早的阶段性描述，以本文的 current baseline、runtime defaults、scenario endpoint availability 和 degraded-mode semantics 为准。后续工作包描述的是目标闭环，不等于全部已经默认启用。

## Completion Definition

内部 LLM agent 接入完成，不等于 RTS 只是“支持 LLM 调用”或“有了几个 AI endpoint”；完成意味着 RTS 已经进入一个更强的 managed AI normal mode。

这个完成态至少要同时满足两组标准：

### A. Truth authority 不能失守

- `/ask` 能返回 human-readable grounded answer，并保留结构化 facts、inferences、unknowns、candidates、human decisions、citations、warnings、trace。
- 有显式 `AgentSession`、`AgentRun`、`AgentStep`、`ToolInvocation`、`ToolObservation`、`ContextSnapshot` 和 `ValidatedClaim` 等 runtime model，支持单次请求内多步 tool loop 和跨 turn session continuity。
- Harness 支持多轮 plan/observe/revise，但每一轮都受 release/scope/caller/budget/permission snapshot 约束。
- REST、MCP 和内部 harness 使用同一组工具实现和同一组 gates。
- Claim validation 能拒绝 unsupported fact、governance overreach、prompt injection 和未读 L2 claim。
- Context/memory/feedback 只影响检索辅助和交互体验，不会被当作 truth。
- Golden tests 和 evaluation harness 能证明正确 scope、正确 refusal、grounded answer、permission safety 和 trace completeness。

### B. AI 已经开始像真正有用的 managed analysis engine

- Harness 能根据 intent 选择受控工具序列，而不是只执行单一固定链路。
- `/ask` 不只是组织 deterministic facts，而是能在预算内完成受控多步取证、候选分析、unknown synthesis 和 next-evidence guidance。
- 第一组 managed scenario endpoints 不只是 support/report surface，而是已经能稳定输出由 managed harness 主导的 grounded analysis report。
- Tool calls、retrieved context、model output、grounding result、refusal reason 和 budget usage 都可追踪，并能解释 AI 为什么做出当前分析路径。
- Evaluation 不只证明系统更安全，还能证明 AI 的首轮分析可用度、candidate usefulness、reviewer-time-saved 或同类正向价值达到默认开启门槛。

如果只满足 A 而 B 仍然很弱，RTS 仍更像“安全的 LLM-enhanced information service”；只有 A 和 B 同时成立，才算达到本文要的 internal managed agent 完成态。

## Work Package 0: Agent Runtime State Machine


目标是把 internal LLM agent 从“单次 prompt 调用”升级为 service-owned runtime，而不是把状态隐含在 prompt、调用方或模型输出里。

落地内容：

- 新增或明确 `AgentSession`：跨用户 turn 的交互容器，只保存 recent scope、selected object、last trace、caller preference、clarification answer、retrieval hint 等非真相状态。
- 新增或明确 `AgentRun`：一次 `/ask` 或 scenario request 的权威执行单元，绑定 `runId`、`traceId`、caller、api key hash/ref、outputMode、entrypoint、release snapshot、resolved scope、budget envelope、feature flag snapshot。
- 新增或明确 `AgentStep`：记录 plan、tool_call、tool_observation、context_build、model_draft、validation、answer_compile、refusal 等步骤。
- 新增或明确 `ToolInvocation` 和 `ToolObservation`：分别记录 tool name、input schema version、input hash、selected arguments、policy result、output schema version、output hash、selected URIs、redaction state。
- 新增或明确 `ContextSnapshot`：记录本轮给模型的 context item hashes、truthEligible map、URI/hash preservation、token budget usage。
- 新增或明确 `ValidatedClaim`：记录 claim text、claim type、grounding evidence、validation status、rejection reason。
- 多轮 loop 必须由 service 状态机推进：`plan -> validate tool request -> execute tool -> observe -> maybe revise -> draft -> validate -> emit`。
- LLM 可以提出下一步 tool need 或 revision，但不能直接推进 run state。

运行不变量：

- `release_id` 在 run 开始时绑定为 snapshot；除非明确返回 refusal/clarification，新工具调用不能切换 release。
- resolved scope 可以被澄清或缩小，不能因为语义相似、rerank 或 confusable 自动扩大。
- caller/profile/outputMode 在 run 内不可漂移；每次 tool/trace/governance read 仍要 re-check。
- L2 reads、dependency reads、governance summaries 是本次 run 的事实材料；previous trace 和 memory 只是提示。
- 每个状态转移都必须可追踪，并能从 trace 复盘执行序列。

验收门禁：

- 任何 managed answer / scenario report 都能定位到一个 `AgentRun`。
- 同一 run 内混用不同 release、不同 resolved scope、不同 caller 时直接 refusal。
- 单次请求内多步调用达到预算、停止条件或 validator refusal 时，不返回半可信成功。
- 跨 turn session 只能影响默认 scope/偏好/澄清上下文，不能直接支撑 facts。

## Work Package 1: Tool Argument Authority And Parameter Binding

目标是明确工具有参数，但参数本身不是权限；RTS service 必须拥有参数解释、绑定、校验、拒绝和记录。

落地内容：

- 所有 tool input 都必须声明 schema version 和 required fields；常见字段包括 `caller_id`、`scope`、`release_id`、`uri`、`purpose`、`output_mode`、`trace_id`、`dependency_depth`、`object_types`、`anchors`、`max_objects`。
- `scope` 字段作为 caller/LLM 提供的 hint 或 resolved value；service 负责从 caller profile、scope registry、object ownership、scenario input 中解析并绑定 authoritative resolved scope。
- `release_id` 默认由 service 绑定 active release snapshot；caller/LLM 提供的 release 只能用于受控回放/审计/对比工具，不能绕过 active release gate。
- `uri` 必须验证属于当前 release、当前 resolved scope、caller permission 和 object state。
- `dependency_depth`、`direction`、`edge_type`、`purpose` 必须受 max depth、allowed intents、scope boundary 和 permission 控制。
- `trace_id` 由 service 创建或绑定；LLM 不得伪造权威 trace。
- Tool arguments 进入执行前生成 normalized argument object；trace 记录 normalized argument hash，而不是原始敏感 payload。
- 对参数缺失、参数冲突、scope/release/caller 漂移，统一返回 structured clarification/refusal。

验收门禁：

- LLM 填入合法-looking `scope` 或 `release_id` 但不匹配 run snapshot 时必须拒绝。
- Caller 对 URI 无权限时，即使 URI 存在、release 正确，也必须拒绝。
- Dependency tool 不得因参数指定而跨 scope、跨 release 或越过最大深度。
- Trace 必须记录每个 tool 的 policy result：allowed/refused/rechecked/redacted。

## Work Package 2: Atomic Tool Contracts And Composition Recipes

目标是把稳定原子工具和复合能力分开：原子工具提供可审计能力，复合能力只能通过 recipe 组合原子工具。

落地内容：

- 每个 `RtsTool` contract 至少包含：`name`、`purpose`、`inputSchema`、`outputSchema`、`requiredPermission`、`allowedIntents`、`sideEffectClass`、`truthOutputType`、`budgetCost`、`maxResultSize`、`redactionRule`、`idempotency`、`possibleRefusalReasons`、`featureFlag`。
- `sideEffectClass` 取值示例：`read_only`、`candidate_write`、`human_decision_draft`、`admin_mutation`。
- `truthOutputType` 取值示例：`none`、`navigation_only`、`candidate`、`l2_fact`、`dependency`、`authorized_governance_summary`、`trace_metadata`。
- Planner 可以选择 recipe 或建议 next tool；orchestrator 只能执行 registry 中的 tool 和 recipe 中允许的 tool。
- 复合能力必须声明 recipe version、required atomic tools、stop conditions、required evidence、answer/report compiler 和 validation rule。
- Scenario service 不能直接绕过 registry 读 store 或手写跨层逻辑；它只能调用 recipe executor 或共享 service layer。

必备 atomic tools：

- scope/navigation：`resolve_scope`、`list_scopes`、`search_scopes`、`get_scope_summary`、`get_pack_navigation`。
- object read：`find_objects`、`find_by_target_path`、`find_by_source_anchor`、`find_by_lookup_key`、`get_object_card`、`read_object_l2`。
- dependency：`get_dependencies`、`find_reverse_dependencies`、`get_dependency_subgraph`。
- disambiguation：`find_confusable_objects`、`find_confusable_scopes`。
- grounding：`check_grounding`、`validate_claims`。
- governance：`read_evidence_summary`、`governance_review`、`record_human_decision_draft`。
- analysis：`analyze_impact`、`plan_tests`、`compare_rules`、`explain_conflict`。
- message：`parse_raw_message_candidate`、`map_source_fields_to_rules`、`resolve_required_lookups`、`simulate_rule_application_candidate`、`assemble_target_message_candidate`、`validate_target_message_grounding`。
- operational：`get_trace`、`trace_report`、`metrics_snapshot`、`run_evaluation`、`feature_flags`。

必备 composition recipes：

```text
managed_ask.v1 =
  resolve_scope
  -> find_objects
  -> get_object_card
  -> read_object_l2
  -> get_dependencies
  -> build_context
  -> model_draft
  -> validate_claims
  -> compile_answer
```

```text
pr_diff_impact.v1 =
  extract_external_anchors
  -> find_by_source_anchor / find_by_target_path / find_objects
  -> find_reverse_dependencies
  -> read_object_l2
  -> analyze_impact
  -> plan_tests
  -> validate_claims
  -> compile_scenario_report
```

```text
exception_investigation.v1 =
  extract_exception_anchors
  -> resolve_scope
  -> find_objects
  -> get_dependency_subgraph
  -> read_object_l2
  -> draft_investigation_paths
  -> validate_claims
  -> compile_scenario_report
```

```text
failed_message_analysis.v1 =
  parse_raw_message_candidate
  -> map_source_fields_to_rules
  -> resolve_required_lookups
  -> read_object_l2
  -> simulate_rule_application_candidate
  -> validate_target_message_grounding
  -> compile_scenario_report
```

```text
test_planning.v1 =
  resolve_scope
  -> find_objects / read_object_l2
  -> get_dependencies
  -> plan_tests
  -> validate_claims
  -> compile_candidate_test_plan
```

验收门禁：

- Tool catalog 中的每个 tool 都必须声明 `truthOutputType`；validator 按类型决定是否可支撑 facts。
- 复合 recipe 未声明 required evidence 时不能上线为 managed output。
- Scenario endpoint 与 `/ask` 使用相同原子工具时，permission/refusal/trace 行为必须一致。
- 任何 recipe 不能把 `navigation_only`、`candidate`、memory 或 external input 升格为 fact。

## Work Package 3: Multi-Turn Conversation And Clarification Flow

目标是支持真实多轮对话，但不让聊天历史变成 truth。

落地内容：

- Clarification response 必须带 `clarificationId` 或 trace-linked pending state，记录缺少的是 scope、anchor、permission、object disambiguation、outputMode 还是 evidence。
- 用户下一轮回答 clarification 时，service 将其作为新的 hint 重新进入 planner，而不是直接沿用旧候选作为事实。
- Session memory 可以保存 recent scope、preferred output view、last selected object、last refused reason、user-provided disambiguation。
- Previous answer/trace 可以作为 navigation hint，但 factual answer 必须在当前 active release 下重新读取 L2/dependency/governance material。
- AgentRun 可以引用 previous trace id；trace report 必须区分 previous hint 和 current evidence。
- 多轮中如果 active release 已变化，必须重新 resolve scope 和 reread L2；不能复用旧 hash 作为当前事实。

验收门禁：

- 多轮 clarification 后得到 answer 时，trace 中必须包含当前 run 的 L2 reads。
- Session memory 中的 `selectedObject` 只能作为 candidate seed；若当前 caller/scope/release 不匹配，必须拒绝或重新澄清。
- 用户纠正模型时，纠正内容进入 feedback/review queue；不能直接写入 active answer facts。
- 跨 turn provider/model drift 不影响 truth gate。

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
- 引入 `AgentPlan` 模型，字段至少包括 `intent`、`scenarioType`、`scope`、`anchors`、`requiredState`、`toolPlan`、`recipeVersion`、`budgets`、`expectedEvidence`、`clarificationQuestion`、`refusalIfMissing`、`releaseSnapshot`、`scopeSnapshot`。
- Planner 可以调用 LLM，但输出必须是 JSON/schema constrained，并由 service validator 复核；LLM 输出只是一份 proposal，不是 run state。
- Scope 不清时只允许输出 clarification，不允许进入 find/read。
- Strong anchors 如 URI、rule id、target path、source anchor 优先 deterministic lookup；LLM 不能把弱语义相似结果升格为确定对象。
- Planner 输出必须记录到 trace，并可 replay。
- Planner must record scope snapshot and release snapshot.
- Planner 必须声明预期 evidence 类型，例如 L2 runtime object、dependency edge、authorized governance summary；后续 validator 只接受已声明且已读取的 evidence。
- Planner 可以建议 next tool，但不能自行执行或绕过 orchestrator。

验收门禁：

- planner 不能直接返回业务事实。
- planner 不能要求读取 caller 无权访问的 scope。
- planner 失败时 fallback 到 deterministic Day1 resolver 或返回 structured refusal。
- planner may not widen scope based on semantic similarity alone.
- planner proposal 中的 `release_id`、`scope`、`uri` 与 run snapshot 不一致时必须 refusal 或 clarification。

## Work Package C: Guarded Tool Orchestrator

目标是让 internal LLM agent 能执行多步工具调用和观察-修正循环，但所有调用都被 service policy 管住。

落地内容：

- 建立统一 `RtsTool` registry，REST、MCP、harness 共用同一 tool implementation。
- 每个 tool 定义 name、purpose、input schema、output schema、required permission、budget cost、allowed intents、side effect class、truth output type、trace redaction rule。
- Orchestrator 按 `AgentPlan.toolPlan` 或 recipe 执行；模型只能提出 next tool proposal，不允许构造未知 tool 或直接修改执行状态。
- 每步调用前做 policy check：scope、caller、release、purpose、outputMode、feature flag、budget。
- 每步调用后做 output check：object state、L2 hash、dependency scope、governance redaction、retrieved token estimate。
- 对 `read_object_l2`、`read_evidence_summary`、`get_dependency_subgraph` 设置独立预算和最大深度。
- Tool output 进入 model context 前做 compaction，保留 URI/hash/path，不丢失 grounding。
- REST、MCP、harness must share the same registry and refusal codes.
- 支持 observe/revise：工具观察结果可以触发继续读依赖、澄清、partial 或 refusal，但状态转移由 service 代码执行。
- 支持 hard stop：answer ready、clarification needed、object not found、permission denied、unsupported claim、budget exhausted、provider failure、latency exceeded、tool sequence invalid。

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
- Tool output type 是 `navigation_only` 或 `candidate` 时，不能直接支撑 `facts`。
- Model-initiated tool proposal 与 plan/recipe 不一致时，必须 refusal 或重新规划，不允许自由执行。

## Work Package D: Context Builder

目标是把 retrieved context、session context、workspace context 和 governance context 分层提供给 LLM。

落地内容：

- 定义 `ContextItem` 分层：system policy、caller profile、query plan、object card、L2 fact、dependency、governance warning、trace metadata、session memory、workspace context、inference、unknown。
- 每个 context item 必须标记 `truthEligible`。
- 只有 L2/dependency/authorized governance summary 可以 `truthEligible=true`。
- User query、diff、log、raw message、session memory、workspace note 都必须 `truthEligible=false`。
- Object card、search hit、L0/L1、previous trace summary 默认 `truthEligible=false`，只能用于导航、消歧或解释为什么需要继续读 L2。
- Context builder 负责 token budget、dedupe、URI/hash preservation 和 prompt-injection neutralization。
- Context builder 输出也写入 trace 或 hash summary，保证审计可定位。
- Context builder must preserve original evidence pointers and redaction state.
- Context builder 必须输出 `ContextSnapshot`：item count、truthEligible count、token estimate、context hash、redaction state、included URI/hash list。
- Context compaction 不能删除 citation 必需的 release id、object URI、content hash、field path 和 dependency path。

验收门禁：

- Memory 和 external input 不能通过 validator 成为 fact source。
- Governance raw text 不进入默认 answer context。
- Context compaction 后仍能保留 citation object URI 和 content hash。
- 如果 context 中没有任何 truthEligible item，answer 只能是 clarification、partial 或 refusal。

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
- 处理链路：policy guard -> create AgentRun -> bind release/caller/scope snapshot -> planner -> guarded multi-step orchestrator -> context builder -> model draft -> claim validation -> answer view -> trace。
- 支持 clarification：scope 缺失、anchor 不足、多个候选冲突、权限不足但可提供更具体 scope 时返回明确问题。
- 支持 partial：找到候选但 L2/gov view 不足时返回候选和缺失项，不编造答案。
- 支持 answer：只有 L2/dependency/governance grounding 通过后返回。
- 支持 refusal：权限、release、schema、hash、budget、prompt policy、unsupported claim、unresolved ambiguity。
- 支持 multi-turn：上一轮 clarification、recent scope、selected object 可以作为 hint，但本轮 answer 必须重新通过当前 run 的 L2/dependency/governance evidence。
- 支持 multi-tool：planner 可以选择 recipe，orchestrator 可以在预算内执行多步 tool loop，并在每步后决定 answer/continue/clarify/refuse。

验收门禁：

- `/ask` 不直接读 store，只能通过 tool registry。
- `/ask` 返回 `answer` 字段时，必须同时返回 structured facts/citations/trace。
- 模型不可用时可退化到 deterministic `/query` 或 structured refusal，不能返回空白成功。
- `/ask` must re-check release and permission before final emit.
- `/ask` 的每个 successful answer 必须有当前 run 的 L2 read 或 authorized governance summary read；previous trace/memory 不算。

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
- Scenario endpoint 必须通过 recipe executor 或共享 tool registry 组合原子工具；不能各自绕过 registry 形成第二套 truth access logic。
- LLM-enhanced scenario 可以做 anchor extraction、candidate synthesis、unknown/next-evidence synthesis，但 final report 中的 facts 仍必须由 validator 接受。

验收门禁：

- PR diff、exception、failed message 至少各有正常、scope unclear、no object found、permission denied、unsupported claim 的 golden cases。
- Scenario endpoint 不允许返回 release approval、final root cause 或 human decision，除非读取到已记录的人类裁决。
- Scenario recipe 的 tool sequence、external input hash、selected URIs、grounding map 必须进入 trace。

## Work Package H: Tool Mode And MCP

目标是让外部 agent 能安全复用 RTS truth tools。

落地内容：

- MCP `/tools` 返回稳定 catalog，包含 tool schema、purpose、required fields、possible refusal reasons。
- MCP tool 和 REST tool 共用 service implementation，不复制业务逻辑。
- 外部 agent 的每次 tool call 必须传 caller、api key、scope/purpose/output mode；缺失时拒绝或要求 clarification。参数由 service 重新绑定和校验，不因为外部 agent 提供了字段就自动授权。
- MCP 返回结构化结果：facts、candidates、unknowns、warnings、groundingMap、traceId。
- 外部 agent 可以计划 broader workflow，但不能拿 RTS tool output 绕过 grounding。
- `rts_ask` 和 `rts_contextual_ask` 明确是 managed mode wrapper；其他 `rts_*` 是原子 tool mode。
- MCP catalog must encode the same permission matrix as REST.
- MCP catalog 必须暴露 tool contract 关键信息：side effect class、truth output type、budget cost、allowed intents、redaction rule。
- 外部 agent 多步调用时，每步都独立 re-check permission/release/scope；外部 agent 的 workflow state 不等于 RTS AgentRun authority。

验收门禁：

- MCP minimal catalog 和 expanded catalog 都可通过 feature flag 控制。
- MCP tool 不得暴露 direct file path、raw store path 或未授权 governance raw text。
- REST/MCP 同一输入应得到同一 refusal/permission 行为。
- MCP tool 返回的 `candidate`、`navigation_only` 或 `trace_metadata` 不得被 RTS 自己的 managed answer 当作 fact，除非后续同 run 读取了 L2/dependency/governance evidence。

## Work Package I: Memory, Feedback, And Context

目标是支持交互连续性和检索质量改进，但不让 memory 变成 truth。

落地内容：

- Session memory 只记录 user preference、recent scope、selected object、feedback summary、retrieval hint。
- Session memory 可以帮助后续 planner 选择默认 scope 或候选 seed，但必须标记来源和过期策略。
- Feedback route 分为 trace_feedback、retrieval_quality_queue、card_improvement_candidate、review_workflow、ignored_not_truth。
- Memory write 默认 `truthEligible=false`。
- 只有 governance workflow 中的 human decision record 能成为后续 truth pipeline 的输入，而且仍不能直接修改 runtime projection。
- Contextual ask 可以读取 session/workspace memory，但 validator 不允许 memory 支撑 facts。
- Memory and feedback are immutable inputs to truth, never truth sources.
- 如果用户在对话中纠正规则事实，service 只能生成 feedback/review candidate；直到 canonical truth pipeline 发布新 projection，active answer 仍以当前 release 为准。
- Previous trace summary 可以帮助解释“上次查过什么”，但不能替代当前 run 的 evidence read。

验收门禁：

- metrics 中 `memoryAsTruthCount` 必须保持 0。
- 用户纠错只能进入 feedback/review queue，不能修改 active release answer。
- Reopened ambiguity 必须走新 truth/release path。
- Contextual ask 命中 session selected object 时，仍必须重新做 permission、release、scope 和 L2 hash 校验。

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
- Agent run trace 记录 run id、session id、release snapshot、scope snapshot、caller/outputMode snapshot、feature flag snapshot、recipe version、state transitions、stop reason。
- Agent step trace 记录 step number、step type、tool/model/validator name、normalized input hash、output hash、selected URIs、policy result、budget after step、redaction state。
- LLM run trace 记录 model、prompt version、tool outputs hash、final output hash、validation result、duration。
- Scenario trace 记录 external input summary/hash、extracted anchors、selected tools、managed analysis report、grounding/refusal。
- Trace report 提供 human/audit/pipeline views。
- Metrics snapshot 汇总 evaluation 和 runtime counters。

验收门禁：

- 成功、partial、clarification、refusal 都必须有 trace。
- Trace 不泄露 API key、raw secret、未授权 raw evidence。
- Grounding check 能从 trace 重建 claims 和 evidence。
- Trace retention/redaction policy must be documented and bounded.
- 审计者必须能从 trace 回放：为什么选这个 scope、为什么读这些对象、每个参数如何校验、每条 fact 为什么 grounded 或 rejected。

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

## Execution Spine

虽然本文不按“近期 / 后期”讲故事，但真正落地时仍然有一条不能打乱的主脊梁。它不是阶段叙事，而是 capability loop 的依赖顺序：

1. **先把 runtime authority 立住**：`AgentRun` / `AgentStep` / 参数绑定 / trace replay / tool contract 必须先明确，否则 AI 的每一步都不可裁决。
2. **再把 multi-step managed loop 做强**：planner、orchestrator、recipe executor、hard stop、observe/revise 必须闭合，否则 AI 仍然只是在整理 deterministic read。
3. **再把 analysis draft 和 context preservation 做强**：`ContextBuilder`、结构化 LLM draft、claim validator、citation/grounding preservation 必须成套出现，否则 AI 生成质量提不上去。
4. **再把第一批 AI-centric scenario 做成正常模式**：至少要有一个核心 scenario 真正进入 managed harness 主导的证据选择、候选分析、unknown synthesis 和 next-evidence guidance，而不是停留在 support/report surface。
5. **最后用评估和默认开启门槛把产品形态固定下来**：如果没有正向 AI value metrics 和明确 enablement gate，系统会长期停留在“安全但默认关闭”的半完成状态。

因此，本文后续 work packages 虽然平铺展开，但执行判断上应优先问一个问题：它是否推动上面这条主脊梁向前闭合，而不是只让某个局部功能看起来更完整。

## Sequential Full-Delivery Plan

下面这份顺序化实施计划的目的，不是给出逐文件施工说明，而是明确全量落地时每个阶段的 goal 和收口标准。新窗口实施时，只要严格按这个顺序推进并在每阶段收口后再进入下一阶段，就不会重新滑回“先做 demo，再补骨架”的路线。

### Phase 1 — Runtime authority foundation

**Goal**

把 internal managed agent 的 authority skeleton 固化出来，让一次 agent run 成为 RTS service 内部显式、可裁决、可追踪的执行单元，而不是隐式 prompt 流程。

**Closure criteria**

- `AgentSession`、`AgentRun`、`AgentStep`、`ToolInvocation`、`ToolObservation`、`ContextSnapshot`、`ValidatedClaim` 都已成为显式 runtime records。
- release snapshot、resolved scope、caller snapshot、feature flag snapshot、budget envelope、trace id 都绑定到 authority-bearing run model。
- planner、orchestrator、validator、answer compiler 只能推进显式 state transition，不能绕过 runtime state。
- managed answer / scenario report / clarification / refusal 都能定位到单一 `AgentRun`。

### Phase 2 — Tool authority and execution contract

**Goal**

把所有工具调用变成“由 RTS service 重新解释和授权”的执行模型，确保参数不是权限、tool 不是旁路、REST/MCP/managed harness 不是三套语义。

**Closure criteria**

- 每个 tool 都具备完整 contract：schema version、truth output type、side effect class、budget cost、feature flag、redaction rule、possible refusal reasons。
- `scope`、`release_id`、`uri`、`purpose`、`dependency_depth`、`trace_id` 等字段都经过统一参数绑定和权威解释层。
- REST、MCP、managed harness 对同一 tool 的 permission、refusal、policy result 和 truth semantics 完全一致。
- caller 或 LLM 提交的 tool arguments 不再能被直接当作授权成立的依据。

### Phase 3 — Managed multi-step orchestration

**Goal**

让 RTS internal agent 从“单轮模型组织答案”升级为真正的 service-owned multi-step managed loop，能够在受控范围内 plan、取证、观察、修正、停止。

**Closure criteria**

- orchestrator 已支持受控 multi-step tool loop，而不是固定单链路调用。
- recipe/composition executor 已成为 `/ask` 与 scenario endpoint 的统一组合入口。
- hard stop 条件已经统一纳入 runtime flow，包括 budget exhaustion、invalid tool sequence、provider failure、unsupported claim、clarification needed 等。
- AI 已经可以在单次 run 中做受控多步取证，而不是只调用一次模型后整理 deterministic read。

### Phase 4 — Grounded analysis generation

**Goal**

让 AI 不只是“换一种说法”，而是基于 preserved grounded context 生成结构化分析草稿，同时保持 truth boundary 不被突破。

**Closure criteria**

- `ContextBuilder` 已统一管理 truthEligible、dedupe、token budget、redaction、URI/hash preservation、prompt-injection neutralization。
- LLM draft 已是结构化 schema output，而不是自由文本 answer organizer。
- 草稿中至少显式区分 claims、inferences、unknowns、candidates、warnings、citation intents、tool needs。
- citation 和 grounding 必需信息在 context compaction 和 answer shaping 过程中不会丢失。

### Phase 5 — Claim validation and replayable audit closure

**Goal**

把“模型生成分析”收紧成“只有经过证据裁决的分析才能进入服务输出”，同时让整个 agent run 可回放、可解释、可审计。

**Closure criteria**

- claim validator 已和 `ValidatedClaim`、grounding evidence、answer/report compiler 打通。
- 每条 claim 都能被明确标记为 grounded、rejected、downgraded、inference、unknown 或 candidate。
- trace 已升级为完整 agent step replay log，能解释参数如何绑定、tool 为什么 allowed/refused、claim 为什么 accepted/rejected。
- grounding check、trace report、final answer/report 使用同一 evidence map，不存在各说各话的验证结果。

### Phase 6 — Multi-turn continuity without truth contamination

**Goal**

支持真实多轮交互和澄清，但不让历史状态、session memory、旧 trace 或上轮候选污染本轮事实。

**Closure criteria**

- clarification、previous trace reference、selected object、session hints 都已进入显式 multi-turn runtime flow。
- 上一轮信息只能作为 hint，本轮 facts 仍必须重新读取当前 release 的 L2/dependency/governance evidence。
- release、scope、permission 变化时会自动重新绑定并阻止脏复用。
- multi-turn continuity 提升的是交互效率，不是用历史状态替代当前 truth read。

### Phase 7 — All core scenarios upgraded to managed normal mode

**Goal**

把当前定义的核心 scenario surface 依次全部升级到 AI-centric managed normal mode，而不是停留在 deterministic support/report surface 或只完成一个示范点。

**Closure criteria**

- PR diff impact、exception investigation、failed message analysis、test planning、governance review、release/pipeline explanation 都进入 managed harness 主导的分析模式。
- 所有 scenario 都能输出 grounded candidate analysis、unknowns、next evidence guidance、trace、grounding map 和 refusal semantics。
- scenario output 不再只是静态 report，而是能够体现 AI 主导的证据选择和候选推理价值。
- scenario surface 的升级没有绕开统一 runtime、tool registry、validator 和 trace。

### Phase 8 — MCP and tool-mode parity closure

**Goal**

确保外部 agent 复用 RTS 能力时看到的是同一个 truth-safe system，而不是另一套放松后的执行面。

**Closure criteria**

- MCP catalog 只是统一 registry 的投影，不引入第二套业务语义。
- REST 与 MCP 在 tool schema、permission、refusal、truth output type、trace semantics 上完全对齐。
- managed mode wrapper 与 atomic tool mode 的边界清晰，外部 workflow state 不会被误当作 RTS authority state。
- tool mode 的存在不会削弱 managed mode 的 truth gate。

### Phase 9 — Evaluation, AI value proof, and enablement gate

**Goal**

让系统从“功能上可以运行”升级到“有证据证明值得默认开启”，把安全门槛和 AI 价值门槛同时建立起来。

**Closure criteria**

- evaluation harness 同时覆盖安全指标和正向 AI value metrics。
- candidate usefulness、reviewer-time-saved、首轮分析可用度、question usefulness、impact/test adoption 等指标已经进入正式评估口径。
- 每个准备默认开启的高风险 feature flag 都有明确 pass/fail threshold 和 rollback condition。
- 系统具备从 conservative default 走向 evidence-backed enablement 的条件，而不是长期停留在实验状态。

### Phase 10 — Final operational closeout

**Goal**

完成代码、契约、测试、文档、运行控制和默认开启依据的最终收口，使这套能力从“实现中”变成“完整可交付状态”。

**Closure criteria**

- API caller guide、MCP catalog docs、runbook、evaluation thresholds、doc contract 都已同步到最终口径。
- runtime records、tool contracts、recipe executor、multi-turn clarification、scenario managed mode、trace replay 都有对应 golden tests 和 integration coverage。
- Java tests、LLM fake-provider integration tests、documentation contract validator 和 `git diff --check` 均通过。
- 新窗口中的实施成果已经同时满足产品目标、truth boundary、测试验证和运行控制要求。

### Sequential execution rule

如果在新窗口按本文推进，应遵守以下顺序规则：

- 不要先做 scenario polishing，再回头补 runtime authority。
- 不要先追求 answer 文风或 prompt 技巧，再回头补 context/claim/trace 闭环。
- 不要把某个单一 endpoint 跑通视为完成，除非它已经挂在统一 runtime、tool registry、validator 和 trace 之上。
- 只有当前阶段的 goal 已被 closure criteria 证明成立，才进入下一阶段。

换句话说，这份实施计划是“依次尽可能全部落地”的工程顺序，不是“先做一点能 demo 的 AI，再慢慢补骨架”的路线。

## Implementation Checklist

当前 baseline 已经具备：

[x] 固定 agent answer envelope 和 scenario report envelope。
[x] 建立统一 tool registry，并让 REST/MCP/harness 共用实现。
[x] 引入 `AgentPlan` 和 planner validator。
[x] 将 `ControlledLlmHarness` 改造为 planner -> guarded tool execution -> model -> claim validator 链路。
[x] 升级 `FinalAnswerValidator` 到 claim-level grounding validation。
[x] 将 `/ask` 的 grounded managed answer 作为 validated service answer view 返回。
[x] 为 PR diff、exception、failed message、test planning、governance review 建立 scenario adapters 和统一 candidate report envelope。
[x] 明确 `ContextItem.truthEligible` enforcement，让 memory/external input 不能支撑 facts。
[x] 扩展 trace，覆盖 tool step、context hash、grounding map、budget、scenario input summary。
[x] 补齐 feature flags 和 runtime config 文档。
[x] 补齐 REST/MCP parity tests。

完整目标还必须补齐：

### P0 — 先把 managed AI capability loop 立住

[ ] 增加显式 `AgentSession`、`AgentRun`、`AgentStep`、`ToolInvocation`、`ToolObservation`、`ContextSnapshot`、`ValidatedClaim` runtime records。
[ ] 将 `ControlledLlmHarness` 从固定核心工具链升级为 service-owned multi-step loop executor，支持 plan/observe/revise 和 hard stop。
[ ] 将 `RtsToolRegistry` 升级为完整 tool contract registry，包含 side effect class、truth output type、budget cost、max result size、idempotency、feature flag、schema version。
[ ] 增加 tool argument normalization/binding 层，统一处理 `scope`、`release_id`、`uri`、`purpose`、`dependency_depth`、`trace_id` 的权威解释和校验。
[ ] 将 trace 升级为完整 agent step replay log，能证明每个参数如何绑定、每步为什么 allowed/refused、每条 claim 为什么 grounded/rejected。

### P1 — 让 AI 分析质量真正上去

[ ] 增加 recipe/composition executor，让 `/ask` 和 scenario endpoints 通过原子工具 recipe 组合能力，而不是各自手写第二套编排。
[ ] 增加专门 `ContextBuilder`，输出 `ContextSnapshot`，统一管理 truthEligible、token budget、redaction、URI/hash preservation 和 prompt-injection neutralization。
[ ] 将 OpenAI-compatible adapter 的 prompt strategy 从 answer organizer 升级为受控分析草稿生成，同时继续由 RTS validator 裁决事实。
[ ] 将 LLM draft 改为结构化 schema output：claims、tool_needs、inferences、unknowns、candidates、warnings、citation intents。
[ ] 增加 multi-turn clarification/session flow，让上一轮澄清和 selected object 只能作为 hint，本轮事实必须重新读取 L2/dependency/governance evidence。

### P2 — 让第一批 AI-centric scenario 真进入 normal mode

[ ] 将第一个 AI-centric scenario 从 deterministic/candidate support surface 升级为真正由 managed harness 进行场景规划、证据选择、候选解释、unknown/next-evidence synthesis 的正常产品模式。
[ ] 为正向 AI value metrics 接入评估数据和默认开启阈值。
[ ] 为新增 runtime records、tool contracts、recipe executor、multi-turn clarification、context snapshot 和 agent step trace 补齐 golden tests。
[ ] 更新 API caller guide、runbook、MCP catalog docs 和 evaluation thresholds。
[ ] 运行 Java tests、LLM fake-provider integration tests、documentation contract validator 和 `git diff --check`。

这里的 `P0/P1/P2` 不是重新回到“长期阶段性 roadmap”，而是执行优先顺序：先把闭环立住，再把 AI 质量做强，最后把场景价值和默认开启门槛固定下来。

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
