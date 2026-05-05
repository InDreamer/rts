<!-- docmeta
role: leaf
layer: 3
parent: docs/reference/README.md
children: []
summary: RTS retrieval principles distilled from archived OV/KB discussions, expressed without making OV the engineering baseline
read_when:
  - 需要理解 RTS 如何降低检索误召回和 AI 幻觉
  - 需要设计 API/MCP/Q&A/agent 的读取顺序
  - 需要区分普通 runtime view 和权限化 evidence/review access
skip_when:
  - 只需要项目最高层总纲
  - 只需要 OV 历史实现细节
source_of_truth:
  - docs/confirmed/project-alignment-summary-zh.md
  - docs/confirmed/system-constitution-v1.md
  - docs/reference/ov-kb-retrieval-design.md
-->

# RTS Retrieval Principles

This document keeps the useful retrieval ideas from the archived OV/KB discussions without making OpenViking the RTS engineering baseline.

## 1. Retrieval Is Not Truth

Retrieval systems are librarians.

They may help callers find and load governed truth, but they must not decide what is true, certify ambiguity, or write runtime learning back into canonical truth.

Every retrieval result should point back to governed RTS objects: rule, pack, evidence, review/adjudication state, and publication state.

## 2. Scope First, Content Later

RTS should narrow scope before reading details.

Preferred order:

1. identify source-target channel
2. identify product or domain area
3. identify pack
4. identify rule / lookup / helper
5. load original object and authorized supporting context

This is a safety principle, not only a performance optimization. Similar rules from the wrong channel or product can be more dangerous than no result.

## 3. L0 / L1 / L2 Are Precision Views

L0, L1, and L2 are not directory levels. They are different precision views of the same resource.

- **L0**: very short abstract used for cheap recall and scope filtering
- **L1**: structured overview used for rerank, disambiguation, and navigation
- **L2**: original governed object or authorized source needed for final answer

RTS should avoid sending L2 content to AI before scope and candidate relevance are clear.

## 4. L1 Is A Safety Layer, Not Documentation Decoration

L1 quality determines whether the system can filter plausible but wrong candidates.

A good L1 should help answer:

- what this channel/product/pack covers
- which rules/lookups/helpers are included
- what dependencies matter
- whether the user question is outside this scope
- what known ambiguity or state constraints affect use

Weak L1 turns retrieval into noisy similarity search. Strong L1 helps prevent cross-pack and cross-product contamination.

## 5. URI-Like Stable Addressing Is Preferred

RTS should prefer stable, URI-like object addressing for service use.

The exact scheme can evolve, but the address should be:

- stable across service calls
- reversible to channel/product/pack/object
- usable in API/MCP/Q&A/pipeline responses
- traceable in audit logs
- independent from model-generated prose

Examples may use URI-like forms, but the constitutional requirement is stable, traceable object identity.

## 6. Runtime View Should Be Low-Noise But Not Blind

Ordinary runtime retrieval should be low-noise. It should not dump all evidence, review notes, reports, and historical discussion into every AI context.

However, RTS as a truth service must still support permissioned access to evidence, review state, conflict records, and adjudication records when the caller is allowed and the task requires it.

Therefore the adjusted principle is:

- default runtime view: focused rules / lookups / helpers / essential metadata
- authorized explanation or governance view: evidence, review, conflict, adjudication, and publication state can be expanded

This replaces the older absolute rule that evidence/review/reports never enter runtime.

## 7. Generated Summaries Need Governance

L0 and L1 can be AI-assisted, but governed content must not rely on unreviewed automatic summaries when precision matters.

For high-trust rule use, L0/L1 should be versioned, reproducible, and reviewable enough that retrieval behavior can be explained later.

## 8. OV Is A Reference Pattern Only

OpenViking influenced these retrieval ideas:

- layered reading
- stable resource addressing
- scoped retrieval
- low-noise context loading

RTS may borrow these ideas, but OV is not the truth owner and is not the required runtime baseline.
