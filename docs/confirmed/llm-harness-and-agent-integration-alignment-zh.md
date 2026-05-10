<!-- docmeta
role: leaf
layer: 3
parent: docs/confirmed/README.md
children: []
summary: align RTS managed LLM harness, external agent tool mode, runtime projection access boundary, and scenario integration priorities
read_when:
  - 需要判断 LLM 应该放在 RTS service 内部还是外部 agent 中
  - 需要解释 RTS service API、MCP tool surface、LLM harness 和场景 adapter 的分工
  - 需要选择 OpenAI Agents SDK、Claude SDK、LangChain、PageIndex 等框架的边界
  - 需要规划 PR diff、exception investigation、pipeline 或其他服务接入 RTS
skip_when:
  - 只需要 KB 到 runtime projection 的字段契约
  - 只需要 Day1 查询 API 的运行命令
source_of_truth:
  - docs/confirmed/project-alignment-summary-zh.md
  - docs/confirmed/system-constitution-v1.md
  - docs/confirmed/day1-query-service-and-llm-harness-plan-zh.md
  - docs/confirmed/day2-agentic-retrieval-evolution-plan-zh.md
  - docs/confirmed/final-llm-agent-service-plan-zh.md
-->

# RTS LLM Harness And Agent Integration Alignment

> 状态：confirmed alignment note
> 日期：2026-05-08
> 目的：记录 RTS 当前阶段、LLM 放置位置、外部 agent 接入方式和场景化集成路线，避免把“索引服务”“truth service”“agent framework”混成一个概念。

## 1. 当前结论

RTS 不应在“内置 LLM harness”和“外部 agent 调 API”之间二选一。

RTS 应同时支持两种模式：

```text
managed mode:
  caller -> RTS scenario endpoint or /ask
  RTS internal LLM harness plans and calls RTS tools
  RTS returns grounded answer / managed analysis output / trace

tool mode:
  external agent -> RTS REST/MCP tools
  external agent plans the broader workflow
  each RTS tool still enforces scope, permission, release, L2 and trace
```

这两个模式共享同一个 RTS core service 和同一套 runtime projection access boundary。它们共同复用同一组稳定原子能力，而不是各自拥有一套 truth access logic。

## 1.1 Mode Selection Matrix

为了避免实现者各自理解“什么时候该走 managed mode、什么时候只该走 deterministic/tool mode”，当前 active baseline 固定如下：

| Surface | Target normal product mode | Degraded / fallback mode | Forbidden shortcut |
|---|---|---|---|
| `/api/v1/query` | deterministic truth/information service | N/A | 不能假装自己提供 managed scenario synthesis |
| `/api/v1/find` / `objects/*` / `dependencies` / trace | deterministic truth-source atomic capability | N/A | 不能绕过 scope/release/permission/L2 |
| `/api/v1/ask` | managed analysis service | LLM-unavailable 时降级为 bounded information-service output | 不能把降级输出描述成等价 AI 分析 |
| `scenario/analyze-pr-diff` | managed analysis normal mode | 降级为 impact/test information or candidate support surface | 不能直接退化成“无语义的 query 包装” |
| `scenario/investigate-exception` | managed analysis normal mode | 降级为 investigation information surface | 不能把降级输出当 final root cause |
| `scenario/analyze-failed-message` | managed analysis normal mode | 降级为 grounded target information/candidate surface | 不能把降级输出当生产转换结果 |
| `scenario/plan-tests` | managed analysis normal mode | 降级为 deterministic template / candidate support surface | 不能把 candidate 自动当 QA signoff |
| `scenario/governance-review` | managed analysis normal mode | 降级为 governance information/candidate surface | 不能把 AI suggestion 当 human decision |
| MCP / external tool mode | external agent orchestration over shared atomic capabilities | same atomic capabilities, possibly narrower catalog under flags | 不能拥有比 internal managed agent 更宽松的 truth access |

这张矩阵解决的是产品语义优先级，不是实现完成度声明：只要某个 surface 被归类为 AI-centric scenario，它的目标正常产品模式就是 managed analysis；deterministic 输出只承担当前底座、降级连续性和信息提供职责。当前是否已经达到 managed analysis normal mode，以 internal implementation plan 的 current-state 矩阵为准。

## 2. 三个容易混淆的概念

### 2.1 Runtime projection 是真相材料

runtime projection 是 KB 发布出来的 approved truth 服务运行视图。

它可以存放在文件系统、PostgreSQL、对象存储或其他介质中。存储位置不决定服务边界。

### 2.2 RTS service 是受控读取器

RTS service 不拥有 canonical truth，但它拥有 runtime truth access boundary。

它负责：

- active release 选择
- scope partition
- caller permission
- object state / release state gate
- L2 content hash validation
- dependency traversal limit
- search hit 到 final fact 的升级规则
- grounding validation
- refusal contract
- trace and audit

这些能力不只是普通索引查询；它们决定 runtime projection 如何被安全消费。

### 2.3 LLM / agent framework 是受控分析与表达层

OpenAI Agents SDK、Claude SDK、LangChain、LangGraph、PageIndex 或其他 agent framework 可以帮助理解任务、规划步骤、调用工具、生成候选分析和组织报告。

它们不应直接读取 projection 文件、PG 表或索引，并自行决定 active release、permission、hash、grounding 或 final truth。

推荐边界：

```text
Agent/framework can own investigation flow.
RTS service must own truth access rules.
```

## 3. 为什么不能只做外部 agent 调 API

外部 agent 调 RTS API 很重要，但它不能替代 RTS 内置 LLM harness。

原因：

- 普通服务、pipeline、PR bot 或业务系统不一定愿意集成一个完整 agent runtime。
- 很多调用方只需要一个稳定 endpoint，例如 `analyze_pr_diff` 或 `investigate_exception`。
- 如果每个外部 agent 都自己实现 scope、planning、grounding 和 report shape，行为会分散且难以审计。
- RTS 自己需要提供 human-friendly `/ask`，否则底层结构化 API 的价值很难被非工程调用者感知。

因此 RTS service 内部应有 managed LLM harness，负责从场景输入到 grounded output 的端到端处理。

## 4. 为什么也不能只做内置 LLM harness

RTS 也必须暴露稳定 API/MCP tools 给外部 agent。

原因：

- Codex、Claude、OpenAI agent、CI bot 或其他系统可能已经拥有代码 diff、workspace、ticket、日志、部署上下文。
- 高级外部 agent 更适合管理跨系统 workflow，但仍需要受控读取 RTS truth。
- MCP/tool mode 允许 RTS 被多种 agent 框架复用，而不绑定某一个 SDK。

因此 RTS 应暴露原子工具能力：

- scope / pack navigation
- find objects
- read object card
- read L2
- dependency traversal
- impact candidate
- test planning candidate
- grounding check
- trace report

这些工具返回结构化对象，而不是无约束自然语言。

## 5. SDK 和框架选择原则

RTS 不应该把 core service 绑定到某个 agent SDK。

推荐结构：

```text
RTS Java core
  -> ProjectionStore / QueryService / Permission / Trace / Grounding
  -> provider-neutral LlmClient
  -> internal controlled LLM harness
  -> REST API
  -> MCP tools
  -> optional scenario adapters
```

SDK 使用边界：

- OpenAI / Claude SDK 可以作为 model adapter 或 client adapter。
- LangGraph 可以作为未来 long-running workflow sidecar，而不是 Day1/Day2 truth core。
- Claude Agent SDK / Codex 类 agent 可以作为外部 coding/review agent，通过 RTS MCP/API 调工具。
- PageIndex 可以借鉴或用于 source/evidence ingestion 实验，不应替代 runtime projection 和 RTS L2 truth surface。

禁止边界：

- 不让 SDK 直接读取 projection store 并自行回答 truth。
- 不把 permission、release、hash、grounding 只写进 prompt。
- 不让外部 agent memory 参与 RTS truth validation。

## 6. 当前实现阶段定位

截至 2026-05-10，按当前实现和本地验证结果，RTS 当前更适合这样理解：

```text
Dual-core RTS baseline established
Truth-source atomic capability service mostly working
Managed LLM harness present, but not yet the stable default surface across every AI-centric scenario
Some scenario endpoints and support surfaces exist, but many still behave like degraded information-service or deterministic/candidate surfaces rather than stable AI-normal-mode products
```

已经相对明确的能力：

- runtime projection 读取
- active release
- scope and permission gate
- object manifest / card
- Lucene find
- L2 read and hash validation
- dependency traversal
- structured REST API
- MCP / agent tool surface skeleton
- query trace and refusal contract
- `/ask` 能调用 LLM Responses adapter
- impact/test/message/governance/pipeline support endpoints exist as deterministic or candidate-producing tools

主要缺口：

- `/ask` 的 managed analysis answer 还没有在所有相关文档里成为稳定的 primary service stance
- model output claim validation 仍不足
- scope mediator / planner / orchestrator 仍偏简单
- impact / test / governance 目前仍过多被表述为 degraded information-service or deterministic/candidate support surface，而不是 AI-normal-mode managed analysis
- PR diff、exception investigation、failed-message analysis 等场景虽然已有 endpoint/support surface，但尚未在文档和产品叙事里稳定成 managed AI 正常态

因此当前大量 API 输出是结构化 JSON 是合理状态。下一步不是弱化 AI，而是让同一组稳定原子能力同时支撑 deterministic truth/information service 和 managed analysis service，并明确后者在场景入口中的正常产品身份。

## 7. 场景化目标

RTS 最终不是一个没有 LLM 的索引服务。

它应该成为有 LLM 调查能力的规则真相服务。

典型场景：

### 7.1 PR diff impact analysis

```text
PR diff
  -> LLM extracts changed source fields / mappings / business terms
  -> RTS tools find related rules, lookups, helpers and dependencies
  -> RTS/LLM returns impact candidates, test suggestions, citations and trace
```

输出必须是 candidate，不是 final release approval。

### 7.2 Exception investigation

```text
exception / stack / failed message / log
  -> LLM extracts fields, message type, failure location and likely business anchors
  -> RTS tools retrieve related rule / lookup / helper / dependency
  -> RTS/LLM returns likely investigation paths, unknowns, required inputs and trace
```

输出必须区分 grounded facts、inferences、unknowns 和 next evidence needed。

### 7.3 Human question answering

```text
business question
  -> RTS /ask managed harness
  -> grounded L2 answer
  -> human-readable controlled analysis and expression plus citations and trace
```

这要求 `/ask` 不只是返回 raw L2 JSON，而是返回稳定的 managed analysis answer，并保留 facts、citations、warnings 和 trace。

## 8. 推荐实现顺序

### Step 1: 完成 managed `/ask`

- 返回 grounded managed analysis answer。
- 保留 L2 facts、citations、dependencies、warnings 和 trace。
- 对 model answer 做 claim grounding validation。
- 不让模型增加未被 L2 支撑的业务事实。

### Step 2: 稳定原子工具层

- 把 find/read/dependency/impact/test/grounding/trace 作为统一工具 surface。
- REST 和 MCP 共享同一套权限、release、trace 和 grounding 规则。
- 工具输出保持结构化。

### Step 3: 做第一个场景 endpoint

优先选择一个端到端场景来展示价值：

- `analyze_pr_diff`
- `investigate_exception`
- `analyze_failed_message`

现有 `/api/v1/analyze/*`、`/api/v1/message/*`、`/api/v1/governance/*` 和 pipeline/report endpoint 是可复用的 truth-source atomic capability substrate。当前 scenario endpoints 已能包装这些能力返回 grounded candidate report；只有当 managed harness 对外部场景输入进行规划、证据选择、候选解释、unknown/next-evidence synthesis，并通过 grounding/evaluation 后，才算达到 AI-normal-mode scenario endpoint。

### Step 4: 对外开放 tool mode

- 完善 MCP tools。
- 支持外部 agent 自己 planning。
- RTS tool 继续强制 gate 和 trace。

### Step 5: 再评估复杂框架

只有当出现 long-running workflow、human-in-the-loop state machine、跨系统恢复执行等需求时，再评估 LangGraph 或其他 agent framework sidecar。

## 9. 判断标准

选择设计时使用以下判断：

- 如果问题是“如何读取和证明 RTS truth”，放在 RTS service。
- 如果问题是“如何理解一个外部场景输入”，可以交给 LLM harness 或外部 agent。
- 如果问题是“如何把结果写成报告、PR comment、排查建议”，可以交给 LLM。
- 如果问题是“这个 claim 是否可以作为事实”，必须回到 RTS L2 / dependency / governance / trace。
- 如果问题是“该输出能否直接驱动自动化动作”，只有 deterministic contract 或 recorded human decision 可以充当 automation decision input；candidate/inference output 不能直接充当批准、发布门禁、根因关闭或 QA signoff。

一句话：

```text
LLM investigates and explains.
RTS service gates and proves.
Runtime projection supplies governed truth material.
```
