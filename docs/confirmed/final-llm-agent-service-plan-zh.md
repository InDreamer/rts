<!-- docmeta
role: leaf
layer: 3
parent: docs/confirmed/README.md
children: []
summary: compact final RTS LLM-agent service roadmap and current confirmed capability boundaries
read_when:
  - 需要理解 RTS 最终如何服务 LLM agent
  - 需要把 Day1/Day2、LLM harness、MCP/API/Q&A/pipeline 对齐到最终路线图
  - 需要确认最终形态中 managed mode、tool mode、context/memory、answer contract 和治理边界
skip_when:
  - 只需要当前 Day1 代码行为
  - 只需要 KB 到索引层的 projection contract
  - 需要完整历史长版 final plan 原文
source_of_truth:
  - docs/confirmed/project-alignment-summary-zh.md
  - docs/confirmed/system-constitution-v1.md
  - docs/confirmed/kb-to-index-projection-contract-zh.md
  - docs/confirmed/day1-query-service-and-llm-harness-plan-zh.md
  - docs/confirmed/day2-agentic-retrieval-evolution-plan-zh.md
  - docs/confirmed/llm-harness-and-agent-integration-alignment-zh.md
-->

# RTS Final LLM Agent Service Roadmap

> 状态：confirmed roadmap summary
> 压缩日期：2026-05-08
> 原完整长版：`docs/archive/reference-proposals/final-llm-agent-service-plan-zh.md`
> 目的：保留最终服务方向和关键边界，把细节实现回收到 Day1、Day2 和 LLM harness alignment 文档，避免 final plan 成为第二套全量实现规格。

## 1. Final Goal

RTS 最终应成为 **双核心、LLM-agent-ready 的规则真相服务**。

核心裁决：

```text
RTS owns governed transformation truth access.
Runtime projection supplies approved truth material.
RTS exposes a stable truth-source atomic capability surface.
Managed LLM analysis performs controlled analysis and expression over that surface.
External agents may use the same RTS tools, but cannot bypass RTS gates.
Every factual claim must ground back to L2 / dependency / authorized governance view / trace.
```

RTS 不是裸索引服务、普通 RAG、自由 ReAct agent，也不是把 OpenAI/Claude/LangChain/PageIndex 直接放进 truth core 的系统。

## 2. Two Consumption Modes

RTS 必须同时支持两种消费模式：

| Mode | Caller | Who plans | RTS must still enforce |
|---|---|---|---|
| **Managed mode** | `/ask`、scenario endpoints、普通服务、PR bot、pipeline adapter | RTS internal controlled LLM harness | scope、permission、release、L2 hash、dependency、grounding、refusal、trace |
| **Tool mode** | Codex/Claude/OpenAI agent、CI bot、外部 workflow agent、MCP client | external agent | every RTS tool call still enforces the same gates |

这两个模式共享同一个 runtime projection access boundary。区别只在“谁负责 workflow planning”，不是“谁拥有 truth”。

## 3. Capability Map

最终 RTS service surface 分为六组能力：

1. **Truth-source atomic capabilities**
   query、find、read object card、read L2、dependency traversal、trace read、scope/navigation、grounding checks。

2. **Managed LLM analysis service**
   intent classification、scope mediation、query planning、tool orchestration、context management、controlled analysis and expression、claim validation、refusal/clarification。

3. **Agent/MCP tools**
   stable tool catalog、schema-constrained tool input/output、permission-aware tool calls、agent-view response。

4. **Scenario adapters**
   `/ask`、PR diff impact analysis、exception investigation、failed message analysis、test planning candidate、pipeline/report adapters。

5. **Context and memory**
   session/workspace/UI/retrieved/governance/trace context can improve usability. Memory is not truth and cannot participate in truth validation.

6. **Governance and evaluation**
   evidence/review/adjudication visibility by permission, candidate/fact separation, golden set evaluation, value metrics, failure taxonomy, trace and metrics.

## 4. Current Implementation Position

截至 2026-05-10，按当前实现和本地验证结果，当前阶段更应按下面的判断理解：

```text
Dual-core RTS baseline established
Truth-source atomic capability service mostly working
Managed LLM harness present, but not yet the stable default surface across every AI-centric scenario
Some scenario surfaces exist, but many are still described or operated as degraded information-service or candidate support surfaces rather than stable AI-normal-mode products
```

已经明确的能力方向：

- filesystem runtime projection store
- active release
- scope and permission gates
- object manifest / cards
- Lucene candidate search
- L2 read and hash validation
- dependency traversal
- structured REST API
- MCP/tool surface skeleton
- trace and refusal contract
- provider-neutral LLM client direction

主要缺口：

- `/ask` 需要在产品口径和服务合同里稳定成为 managed analysis surface，而不是被描述成附加表达层
- claim grounding validation 需要更强
- scope mediator、planner、tool orchestrator 需要从简单实现演进为受控 harness
- impact/test/governance 当前实现仍主要是 deterministic/candidate support surface；文档和产品口径需要继续把目标正常态固定为 managed analysis
- PR diff、exception、failed message 等场景还需要在文档和默认体验里明确为 managed AI 正常态

## 5. Roadmap

### Step 0: Constitution And Projection Contract Locked

已完成/持续维护：

- truth owner、LLM boundary、runtime projection contract、refusal principle、permissioned governance view。

### Step 1: Day1 Query / Tool Service

目标：

- 让 approved runtime projection 可通过 Java service 被安全读取。
- 提供 deterministic lookup、Lucene search、L2 read、dependency traversal、REST/MCP skeleton。

成功标准：

- service 不绕过 active release、scope、permission、L2 hash、trace 和 refusal。

### Step 2: Managed `/ask`

目标：

- 让 RTS 自己完成受控多步查询，并返回 human-readable grounded managed analysis answer。

必须包含：

- intent/scope mediation
- tool plan
- L2-backed citations
- controlled analysis and expression
- claim validation
- refusal/clarification
- trace

### Step 3: Stable Tool Mode And MCP

目标：

- 外部 agent 可以通过 RTS REST/MCP tools 查询 truth，而不是直接读文件、PG 表或索引。

必须包含：

- stable tool catalog
- schema-constrained tool outputs
- permissioned tool execution
- trace per tool call
- agent-friendly structured result

### Step 4: First Scenario Endpoint

建议顺序：

1. `/ask` managed analysis surface 完整化
2. `analyze_pr_diff`
3. `investigate_exception` 或 `analyze_failed_message`

现有 impact/test/message/governance/pipeline 工具可以作为同一真相源原子能力面复用。当前 scenario endpoints 已能返回 grounded candidate report；这里的 first scenario endpoint 指把其中一个 AI-centric 场景升级为 managed LLM 端到端正常模式。所有场景输出默认遵守 authority boundary：它们不是 release approval、final root cause 或 final business decision；但对这些场景而言，managed AI 应是目标正常产品模式，deterministic 输出是当前底座或 LLM 不可用/受限时的降级信息服务。

### Step 5: Context, Memory, And Feedback

目标：

- 支持 session/workspace/UI context 和 retrieval feedback。
- 让用户体验变好，但不让 memory 污染 truth。

规则：

- session memory is not truth.
- pipeline/subagent/automation does not write long-term memory by default.
- memory cannot satisfy grounding validation.

### Step 6: Retrieval Enhancement

顺序：

1. deterministic routing
2. BM25 / Lucene
3. alias and entity boost
4. negative/confusable retrieval
5. optional rerank
6. optional vector after evidence shows need

vector/rerank 只能在 scope gate 后运行，且必须回到 URI/L2 才能成为事实依据。

### Step 7: Advanced Agentic Analysis

候选方向：

- impact analysis
- test planning
- rule diff explanation
- source-to-target investigation
- governance/audit explanation

边界：

- output separates facts, inferences, unknowns, candidates, and human decisions.
- LLM can propose; human/governance decides.
- no autonomous truth mutation.

### Step 8: Quality Loop

需要持续建设：

- golden set
- grounded answer rate
- refusal correctness
- wrong-scope answer detection
- unsupported assertion tracking
- critical miss tracking
- impact/test candidate adoption and usefulness
- review question usefulness
- conflict simplification quality
- evidence review time saved
- prompt injection tests
- cost/budget metrics
- trace review

## 6. Answer Contract

所有最终服务输出都应该按消费方区分视图，但共享同一事实边界。

| View | Audience | Shape |
|---|---|---|
| Human view | business/BA/dev user | readable explanation, citations, warnings, unknowns |
| Agent view | coding/review/workflow agents | structured facts, object URIs, dependencies, tool trace, next actions |
| Audit view | reviewers/governance | projection release, evidence/review pointers, L2 hash, claim grounding, refusal reason |
| Pipeline view | CI/CD or service integration | deterministic status, machine-readable candidate/result, strict failure semantics |

输出必须分清：

- facts
- inferences
- unknowns
- candidates
- human decisions

## 7. Non-Negotiable Boundaries

- LLM answer is not truth.
- Search hit is not truth.
- Object card/L0/L1 is not final truth.
- Memory is not truth.
- External SDK/framework is not truth core.
- Final factual claims must ground to L2 or authorized governance view.
- RTS service owns release/scope/permission/hash/grounding/refusal/trace gates.
- External agents may plan broader workflows, but RTS tools remain the controlled truth access boundary.

## 8. Where Details Live Now

- Current baseline and service vision: `project-alignment-summary-zh.md`
- Constitutional boundaries: `system-constitution-v1.md`
- Runtime projection contract: `kb-to-index-projection-contract-zh.md`
- Day1 query/tool service and harness implementation plan: `day1-query-service-and-llm-harness-plan-zh.md`
- Day2 retrieval/LLM/MCP evolution: `day2-agentic-retrieval-evolution-plan-zh.md`
- LLM placement, managed mode/tool mode, framework boundary, scenario priority: `llm-harness-and-agent-integration-alignment-zh.md`
- Complete historical final plan: `docs/archive/reference-proposals/final-llm-agent-service-plan-zh.md`
