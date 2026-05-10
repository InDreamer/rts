<!-- docmeta
role: leaf
layer: 3
parent: docs/confirmed/README.md
children: []
summary: compact Day2 controlled agentic retrieval, MCP, rerank, impact, and test-planning evolution roadmap
read_when:
  - 需要规划 Day1 后的检索、MCP、impact/test planning 或受控 agentic retrieval
  - 需要判断哪些能力可以 Day2 做、哪些仍不能做
  - 需要评估 rerank、negative retrieval、optional vector 和 scenario candidates
skip_when:
  - 需要当前 Day1 query/tool service baseline
  - 需要完整历史长版 Day2 计划
source_of_truth:
  - docs/confirmed/day1-query-service-and-llm-harness-plan-zh.md
  - docs/confirmed/final-llm-agent-service-plan-zh.md
  - src/main/resources/application.yml
-->

# RTS Day2 Controlled Agentic Retrieval Roadmap

> 状态：confirmed compact roadmap
> 压缩日期：2026-05-08
> 原完整长版：`docs/archive/reference-proposals/day2-agentic-retrieval-evolution-plan-zh.md`

## 1. Day2 Goal

Day2 把 Day1 的受控工具调用扩展成 **controlled agentic retrieval**，并把双核心栈中的托管分析服务从第一版入口推进到更稳定的场景产品能力。

核心变化：

- LLM/harness 可以规划多步 tool sequence。
- 检索可以加入 negative/confusable candidates、rerank、alias/entity boost。
- MCP tool surface 可以更稳定地服务外部 agents。
- impact/test planning 可以输出更高价值的 candidate artifacts。
- PR diff / exception / failed message / governance review 等场景可以逐步进入 managed AI 正常模式。

核心不变：

```text
agentic is retrieval behavior, not truth authority.
```

## 2. Start Conditions

Day2 不应在 Day1 基础不稳时启动。

前提：

- active release / scope / permission gates stable
- object manifest and cards stable enough for navigation
- L2 read and hash validation reliable
- dependency graph usable
- refusal contract tested
- trace complete enough to debug
- `/ask` already uses controlled tools

如果 Day1 仍经常拿错 scope、缺 L2、trace 不完整，Day2 只会放大风险。

## 3. Day2 Capabilities

Day2 可以逐步加入：

| Capability | Purpose | Boundary |
|---|---|---|
| Query Planner V2 | generate multi-step retrieval plan | schema constrained; not free agent |
| Tool Orchestrator | execute limited plan with budgets | every tool call policy checked |
| Negative/confusable retrieval | surface easy-to-confuse scopes/objects | never expands truth scope automatically |
| Rerank / score fusion | improve candidate ordering | after hard gates only |
| Optional vector | semantic candidate discovery | only after scope gate and evaluation proof |
| Expanded MCP tools | stable external agent tool mode | same service gates as REST |
| Impact candidates | affected object/dependency candidates | not final impact approval |
| Test planning candidates | regression/boundary candidates | not final QA authority |
| Governance-authorized view | evidence/review/report summary or pointer | permissioned and traced |

## 4. Technical Direction

Day2 can stay on JDK 17.

Suggested progression:

1. Keep filesystem/Lucene baseline stable.
2. Add alias/entity boost and better object cards.
3. Add negative/confusable relation data.
4. Add reranker before vector.
5. Evaluate vector only after BM25/alias/card/rerank are insufficient.
6. Consider OpenSearch only when scale, ops support, or unified BM25/vector/aggregation need justifies it.

No scoring layer may override scope, permission, release, or L2 grounding.

## 5. MCP And Tool Surface

Day2 MCP should become a formal agent-facing tool surface.

Candidate tools:

- `rts_resolve_scope`
- `rts_list_scopes`
- `rts_find_objects`
- `rts_get_object_card`
- `rts_read_object_l2`
- `rts_get_dependencies`
- `rts_impact_preview`
- `rts_test_plan_candidates`
- `rts_get_trace`

MCP remains a wrapper over the same truth service. It must not own separate query logic.

## 6. Scenario Outputs

Impact analysis output should be candidate language:

- affected object candidate
- dependency path candidate
- possible target/source impact
- unknowns and required evidence
- cited URI and trace

Test planning output should distinguish:

- deterministic template tests
- rule-condition-derived tests
- LLM-suggested additional candidates
- unsupported/speculative cases
- reviewer accepted/rejected state where available

这些输出继续遵守 authority boundary：它们不是 release approval、final impact conclusion、root cause 或 QA signoff。

但 candidate 边界不应被误读成能力天花板。Day2 的目标是提高这些场景输出的分析价值、采纳率和审查效率，而不是只生成更漂亮的保守措辞。

## 7. Evaluation Harness

Every Day2 feature must be evaluated before default enablement.

Track:

- correct scope rate
- wrong-scope/confusable errors
- grounded answer rate
- unsupported assertion rate
- critical miss rate
- impact candidate usefulness
- impact candidate adoption / reviewer acceptance
- test candidate usefulness
- test candidate adoption / regression hit quality
- review question usefulness
- evidence review time saved or conflict simplification quality
- tool budget/cost
- trace completeness

Feature flags:

- `planner_v2_enabled`
- `tool_orchestrator_enabled`
- `reranker_enabled`
- `confusable_check_enabled`
- `vector_recall_enabled`
- `impact_candidates_enabled`
- `test_plan_candidates_enabled`
- `mcp_expanded_tools_enabled`

Negative/confusable retrieval maps to `confusable_check_enabled` in the current service configuration. If a separate negative-retrieval feature is added later, document it as a planned flag with its runtime property name.

Current code may expose some candidate-producing or support surfaces behind these flags before the full Day2 planner/orchestrator is mature. Treat those as deterministic support tools unless a feature is explicitly evaluated and documented as default Day2 agentic behavior.

All Day2 features must be disable-able and fallback to Day1 behavior.

## 8. Guardrails

- Hard gates never become ranking features.
- Scope/permission/release/object state/L2 availability must pass before ranking.
- Tool calls are policy-checked.
- Agentic retrieval has max steps, max L2 reads, max dependency depth, and cost limits.
- Final answer remains grounded in L2/dependency/authorized governance view.
- Runtime learning does not mutate truth.
- Memory and trace feedback can improve retrieval quality, not rule facts.

## 9. Day2 Still Does Not Do

- autonomous rule creation/update
- automatic approval/signoff
- pipeline release gate
- unrestricted raw production data analysis
- generic agent orchestration platform
- memory-as-truth
- vector as primary entry
- cross-scope recall before scope resolution

## 10. Exit Criteria

Day2 is mature when:

- multi-step retrieval beats Day1 on golden set without increasing wrong-scope errors
- impact/test candidates have reviewer-visible incremental value
- MCP external agents cannot bypass gates
- trace can replay agentic steps
- refusal behavior remains correct under planner/rerank/vector modes
- all new capabilities can be disabled to recover Day1 behavior

## 11. Current Priority

Do Day2 in this order:

1. scope/navigation tools
2. planner V2 and controlled orchestrator
3. negative/confusable retrieval
4. expanded MCP tool surface
5. impact/test candidate endpoints
6. rerank
7. optional vector
8. governance-authorized evidence/review summaries
