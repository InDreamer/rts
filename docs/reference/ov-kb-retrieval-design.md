<!-- docmeta
role: leaf
layer: 3
parent: docs/reference/README.md
children: []
summary: distilled OpenViking-inspired retrieval lessons for RTS without making OV the runtime baseline
read_when:
  - 需要理解 OV 对 RTS 检索设计的可借鉴点
  - 需要快速查看 L0/L1/L2、URI、layered loading 和 anti-hallucination 的历史来源
skip_when:
  - 需要当前 confirmed baseline
  - 需要完整 OV retrieval 历史设计原文
source_of_truth:
  - docs/reference/rts-retrieval-principles.md
  - docs/reference/ov-boundary-and-adoption.md
-->

# OV Retrieval Lessons Summary

> 状态：distilled reference
> 原完整原文：`docs/archive/reference-proposals/ov-kb-retrieval-design.md`

## What RTS Keeps

OV 讨论中仍适用于 RTS 的思想：

- Stable URI-like addressing for packs, rules, lookups, helpers.
- Layered loading: cheap navigation first, precise L2 read later.
- Scope-first retrieval before content recall.
- L0/L1/card as navigation and disambiguation views.
- L2 as final service-readable fact surface.
- Traceable retrieval path for audit and debugging.
- Tool-based access instead of model-direct file access.

## What RTS Rejects Or Downgrades

- OV is not the RTS runtime dependency baseline.
- OV memory extraction must not write rule truth.
- Vector retrieval cannot run before scope/permission/release gates.
- Resource tree shape is not mandatory; runtime projection may live in filesystem, database, object store, or other controlled store.
- Old “evidence/review/reports never enter runtime” has been replaced by operational view plus permissioned governance view.
- Bidirectional sync/write-back to canonical truth is forbidden.

## L0/L1/L2 Interpretation

- **L0**: cheap abstract for recall and scope filtering.
- **L1/card**: structured overview for navigation, rerank, and disambiguation.
- **L2**: structured runtime object or authorized governed source required for final answer.

L0/L1 are not replacements for L2. Final facts must return to L2 or authorized governance view.

## Current Use

Read this summary only for historical retrieval rationale. For active implementation, use:

- Day1 query/tool service baseline
- Day2 controlled agentic retrieval roadmap
- runtime projection contract
- RTS retrieval principles
