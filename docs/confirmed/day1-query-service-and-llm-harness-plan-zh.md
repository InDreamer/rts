<!-- docmeta
role: leaf
layer: 3
parent: docs/confirmed/README.md
children: []
summary: compact Day1 RTS query/tool service and controlled LLM harness baseline
read_when:
  - 需要落地或核对当前 Day1 RTS 查询/工具/LLM harness 边界
  - 需要判断 Day1 做什么、不做什么、完成标准是什么
  - 需要确认 filesystem projection store、Lucene、L2 read、trace、MCP 和 /ask 的第一版职责
skip_when:
  - 只需要最终 LLM agent service 路线图
  - 只需要 Day2 检索增强或 agentic retrieval
  - 需要完整历史长版 Day1 计划
source_of_truth:
  - docs/confirmed/kb-to-index-projection-contract-zh.md
  - docs/confirmed/system-constitution-v1.md
  - src/main/java/com/rts/store/FileSystemProjectionStore.java
  - sample-projection/runtime-store
-->

# RTS Day1 Query Service And Controlled LLM Harness

> 状态：confirmed compact baseline
> 压缩日期：2026-05-08
> 原完整长版：`docs/archive/reference-proposals/day1-query-service-and-llm-harness-plan-zh.md`

## 1. Day1 Goal

Day1 交付的是 RTS 双核心栈中的第一版可运行 baseline：

```text
RTS Truth-Source Atomic Capability Service
  + lightweight governed index
  + filesystem runtime projection store
  + Lucene scoped BM25
  + L2 read/hash validation
  + trace/refusal contract
  + REST API and thin MCP surface

RTS Managed LLM Analysis Service
  + controlled LLM harness skeleton
  + tool-only truth access
  + grounded managed /ask entry
```

Day1 的目标不是完整 agentic RAG，也不是重型企业搜索平台，而是在同一个 truth boundary 内同时立住两件事：一是稳定的受控真相源原子能力面，二是第一版可审计的 managed LLM 分析入口。Day1 不要求所有场景都成熟，但它不能把 AI 长期写成可有可无的整理层。

## 2. Hard Boundaries

- KB publishes approved runtime projection.
- Query service reads projection; it does not own canonical truth.
- API/MCP/LLM harness all use the same controlled service tools.
- Final facts must come from L2 object, dependency edge, manifest, or authorized governance view.
- Search hit, card, summary, LLM output, and memory do not own truth.
- LLM may identify intent, choose tools, ask clarification, perform controlled analysis, and express grounded results.
- LLM may not bypass scope, permission, release, L2 read, grounding, refusal, or trace.
- deterministic capability is first-class information service, not a lower-grade product path.

## 3. Day1 Does

Day1 must support:

- active release pointer
- projection manifest validation
- scope registry
- object manifest
- object cards
- dependency edges
- content refs
- structured rule/lookup/helper L2 runtime objects
- filesystem-backed store behind interfaces
- local Lucene index over card/search text within scope
- deterministic lookup by URI/id/target/source anchor where available
- scoped BM25 candidate search
- L2 content read with hash validation
- dependency traversal with depth and purpose limits
- query trace and LLM run trace
- refusal when scope/release/permission/L2/dependency conditions fail
- REST service surface
- thin MCP adapter over the same service layer
- `/query` as deterministic truth/information service
- `/ask` as controlled managed analysis entry with tool-only access

## 4. Day1 Does Not

Day1 should not include:

- OpenSearch as required infrastructure
- vector database or primary vector recall
- PostgreSQL/Flyway as required first storage
- full graph database
- free multi-agent workflow
- autonomous truth mutation
- pipeline release gate
- source ingestion / authoring / review workflow
- raw production data access
- memory writing into rule truth

## 5. Minimal Store Shape

The filesystem store is an implementation of the projection store interface, not the conceptual truth model.

Minimum runtime store:

```text
active-release.json
releases/{release_id}/
  release-manifest.json
  scopes.jsonl
  object-manifest.jsonl
  caller-profiles.jsonl
  l2/{rules,lookups,helpers}/...
  navigation/
    object-cards.jsonl
    l0-l1-views.jsonl
    aliases.jsonl
    confusables.jsonl
  governance/
    governance-access-refs.jsonl
    evidence-summaries/
    review-summaries/
    report-summaries/
  dependencies/
    dependency-edges.jsonl
    field-bindings.jsonl
  index-artifacts/
    opensearch-docs.jsonl
traces/
  query-trace.jsonl
  llm-run-trace.jsonl
```

Current filesystem implementation does not require a standalone `content-refs.jsonl` file. L2 content references are derived from `object-manifest.jsonl` fields such as `content_ref`, `l2_storage_ref`, and `content_hash`, then verified against files under `l2/`.

`caller-profiles.jsonl` is release-scoped runtime access metadata. It is part of the runtime store boundary for permission checks, not part of the rule content truth surface.

Future PostgreSQL/object-store migration must preserve query semantics by keeping callers behind store interfaces.

`confusables.jsonl` may exist in Day1 as released navigation data. Day1 may expose or display those relations for disambiguation, but it should not treat negative/confusable retrieval as an autonomous planner or ranking authority. Active multi-step use of confusable/negative evidence belongs to Day2.

## 6. Core API Semantics

Names may evolve, but Day1 needs these service meanings:

| Capability | Purpose | Boundary |
|---|---|---|
| plan/query plan | resolve intent/scope/anchors | may use deterministic parser or controlled LLM extractor |
| find | find candidate objects within resolved scope | candidates only |
| object card read | navigation and disambiguation | not final truth |
| L2 content read | final fact surface | requires active release and hash validation |
| dependencies | explain/navigation/impact preview | cannot expand scope automatically |
| query | deterministic truth/information service | must cite facts and trace |
| ask | controlled managed analysis entry | LLM uses tools, not direct store access |
| trace read | audit and debugging | shows scope, candidates, tools, L2 reads, refusal |

## 7. Controlled LLM Harness

Day1 `/ask` is allowed, but constrained.

Allowed responsibilities:

- parse user intent
- extract anchors and possible scope
- ask clarification when scope is missing
- call allowlisted tools
- choose object cards and L2 reads
- perform controlled analysis over governed truth
- return human-readable grounded answer with warnings, ambiguity, draft/release state, and trace context preserved

Prohibited:

- direct file/DB access
- modifying projection
- inventing facts
- using unscoped search hit as final answer
- hiding refusal reason
- presenting candidate impact/test suggestions as final decisions

Minimum tools:

- `resolve_scope`
- `find_objects`
- `get_object_card`
- `read_object_l2`
- `get_dependencies`
- `explain_trace`

## 8. Refusal Contract

Day1 must refuse, degrade, or ask clarification when:

- scope is unresolved
- caller lacks permission
- active release is missing or not unique
- projection manifest/schema validation fails
- object hit exists but L2 is unreadable
- dependency required for answer is not released
- object state is not answerable for caller profile
- query asks for governance details without authorization
- LLM tool budget is exhausted before grounding
- only card/search summary exists but no L2 truth exists

When managed analysis is unavailable, `/ask` may degrade to bounded information-service output under the same release/scope/permission/trace contract. That degraded continuity mode exists to preserve access to truth material; it does not redefine RTS product identity or make AI-centric scenario services equivalent to pure deterministic retrieval.

Refusal is product behavior, not an error fallback.

## 9. Output Contract

Service output should separate:

- facts
- inferences
- unknowns
- candidates
- human decisions

Every answer should include:

- answer type: answer / clarification / refusal / partial
- resolved scope or missing scope reason
- cited object URIs
- L2/hash grounding where applicable
- warnings or governance summary when allowed
- trace id

candidate / human decision separation is an authority boundary, not a limit on how deeply managed analysis may reason.

## 10. Day1 Definition Of Done

Day1 is done when:

- approved runtime projection can be ingested
- manifest/schema/hash/L2 refs are validated
- release/scope/object/card/dependency/content refs are queryable
- scoped deterministic lookup works
- scoped Lucene BM25 find works
- L2 read verifies hash
- dependency traversal respects scope/release/permission/depth
- refusal cases are covered by tests
- query trace and LLM run trace are written
- `/ask` uses controlled tools before answering
- `/ask` returns a stable managed answer surface with grounding validation, not just raw model text beside tool output
- LLM final answer has citation/trace validation strong enough to reject unsupported claims
- minimal MCP adapter uses the same service layer
- a small golden set covers normal, ambiguous, missing-L2, permission, and refusal cases

## 11. Current Priority

Implementation should prioritize:

1. projection/store contract correctness
2. L2 read and hash validation
3. scope and permission gates
4. trace/refusal completeness
5. grounded managed `/ask` with controlled analysis and claim validation
6. stable REST/MCP atomic capability surface

Day2 enhancements should wait until Day1 stops failing on scope, L2, refusal, and trace.
