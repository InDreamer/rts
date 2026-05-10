<!-- docmeta
role: leaf
layer: 3
parent: docs/reference/README.md
children: []
summary: consolidated historical AI token review rationale and retained RTS LLM risk principles
read_when:
  - 需要追溯 RTS LLM token/PoC 申请口径、风险分类或审核叙事
  - 需要了解为什么早期 broad AI backend service 叙事被降级为 evidence harness
skip_when:
  - 需要当前 confirmed LLM harness 或 service integration baseline
  - 只需要 Day1/Day2 实现任务
source_of_truth:
  - docs/confirmed/README.md
  - docs/reference/rts-service-interface-and-output-principles.md
  - docs/reference/rts-retrieval-principles.md
-->

# RTS AI Token Review Rationale

> 状态：historical consolidated summary
> 时间范围：2026-04-27 to 2026-04-28
> 目的：保留 AI token / PoC 审核中仍有价值的风险原则，同时把会误导当前 RTS 方向的旧长稿移出默认 reference 路径。
> 原始长稿：`docs/archive/reference-proposals/ai-token-review/`

## Current Use

这份材料只用于历史 rationale：

- 为什么 LLM output 不能被当成 truth。
- 为什么服务化 LLM 必须有 scope、权限、trace、grounding 和人工决策边界。
- Why impact analysis / test planning must preserve candidate-only as an authority boundary before recorded review, rather than pretending AI output is final truth.
- 为什么 deterministic retrieval、dependency graph、template baseline 和 Copilot/Yoda 对照不能被 LLM 叙事跳过。

它不限制 RTS 的长期范围。当前长期方向仍以 confirmed baseline 为准：RTS 是包含 Knowledge Base、retrieval/indexing、LLM、governance workflow、API/MCP/Q&A/pipeline-facing surfaces 的 truth service。

## Retained Decisions

The useful ideas preserved from the archived long-form review drafts are:

- RTS may contain internal LLM capability, but LLM answers are not truth.
- RTS output must distinguish facts, inferences, unknowns, candidates, and human decisions.
- Agent/API/MCP callers may read and cite approved truth, and may submit candidates or open questions, but must not directly rewrite approved truth.
- Deterministic retrieval, URI identity, dependency graph, template baselines, and refusal behavior are first-class service capabilities.
- LLM value should be evaluated against deterministic baseline, Copilot/Yoda-like workflows, and SME goldensets before being promoted to broad automation.
- Early impact analysis and test planning outputs should be candidate-only unless backed by governed truth and review state; that boundary limits authority, not the depth of AI analysis.
- Sensitive operational data, production messages, monitor alerts, defect attachments, customer identifiers, and release decision records require separate approval before entering model context or logs.
- Failure types to track include wrong scope, unsupported assertion, hallucinated dependency, critical miss, over-broad noise, authority drift, unsafe data handling, and instruction injection failure.

## Retired Framing

The following old positions are no longer current baseline:

- “First round must have no pipeline, monitor, MCP, agent, or service integration” was a token-approval PoC constraint, not a long-term RTS product boundary.
- “Runtime projection never includes evidence/review/reports” has been replaced by the current operational-view versus governance-authorized-view model.
- “Knowledge-Bases/” as a required runtime tree name is historical terminology. Current docs should describe runtime projection as a service view, regardless of filesystem or database storage.
- Broad “AI backend service” language should not be used without service contracts, caller boundaries, data boundaries, cost controls, evaluation metrics, and fallback behavior.
- LLM API access should not be justified by platform vision alone. The service must show what deterministic structure can do and where LLM adds measurable value.

## Consolidated Historical Outcome

The historical review would not approve an unrestricted RTS AI backend service. It would approve only a narrow evidence-evaluation PoC for impact analysis and test planning, using approved runtime projection, dependency metadata, desensitized inputs, candidate-only outputs, SME scoring, and strict cost/data controls.

Current RTS planning may go beyond that historical PoC, but only by keeping the same core guardrails: scoped truth reads, permissioned context, grounding validation, candidate/fact separation, and human-owned final decisions.
