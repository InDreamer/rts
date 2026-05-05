<!-- docmeta
role: leaf
layer: 3
parent: docs/reference/README.md
children: []
summary: RTS service interface and output quality principles distilled from AI service strategy materials
read_when:
  - 需要设计 API/MCP/agent tool surface
  - 需要定义 RTS/LLM 输出结构和失败类型
  - 需要区分事实、推断、候选建议和人工决定
skip_when:
  - 只需要最高层项目总纲
  - 只需要 token approval history
source_of_truth:
  - docs/confirmed/project-alignment-summary-zh.md
  - docs/confirmed/system-constitution-v1.md
  - docs/reference/rts-retrieval-principles.md
-->

# RTS Service Interface and Output Principles

This document keeps the useful long-term service ideas from the AI service strategy materials, without preserving the old broad token-approval framing.

## 1. RTS Exposes A Service, Not A File Dump

External callers should not need to know where every rule file lives.

They should call RTS through controlled service surfaces such as API, MCP, Q&A, workbench, SDK, or pipeline integration.

Inside RTS, the service can use Knowledge Base, index, retrieval, dependency metadata, and LLM to produce an answer. But the answer must remain traceable to governed truth.

## 2. Suggested API / Tool Surface

These names are reference ideas, not final API commitments.

| Capability | Purpose | Boundary |
|---|---|---|
| `query_truth` | Ask a scoped question about approved truth. | Must resolve scope or ask for clarification. |
| `find_objects` | Find candidate packs, rules, lookups, or helpers. | Returns candidates, not final answers. |
| `read_object` | Read a specific rule / lookup / helper by stable URI. | Object read should include snapshot/version. |
| `get_overview` | Read L1 overview for navigation. | Overview helps decide what to read next; it is not final rule truth. |
| `get_tree` | Browse channel/product/pack structure. | Must respect caller permissions. |
| `get_truth_changes` | Compare truth snapshots or ask what changed. | Used for cache invalidation, impact analysis, and downstream awareness. |
| `submit_candidate` | Submit a possible issue, rule, conflict, or open question. | Must enter candidate/review flow; cannot update approved truth directly. |

A small, hard API surface is safer than a universal “ask anything” endpoint.

## 3. Output Should Separate Five Things

RTS/LLM output should clearly separate:

1. **Facts**: backed by rule IDs, source, evidence, review, or publication state
2. **Inferences**: analysis derived from facts
3. **Unknowns**: missing evidence, unresolved scope, or open conflicts
4. **Candidates**: suggested impact areas, tests, rules, questions, or explanations awaiting review
5. **Human decisions**: reviewer adjudication or signoff results

Do not make candidates look like approved truth.

## 4. Impact Analysis Output Shape

Impact analysis should prefer candidate language:

- affected object candidate
- dependency path candidate
- possible regression area
- unknown / needs review
- confidence or rationale
- cited rule / lookup / helper URI

It should not present itself as the final impact conclusion unless the result is fully supported by governed truth and review state.

## 5. Test Planning Output Shape

Test planning should distinguish:

- deterministic template output
- rule-condition-derived tests
- LLM-suggested additional cases
- unsupported or speculative cases
- reviewer accepted / rejected cases

This keeps test planning useful without turning generated text into final QA authority.

## 6. Agent Rule

For external agents and MCP tools, the default rule is:

```text
read approved truth, cite it, submit candidates; never rewrite truth directly
```

Agents may help find gaps and propose candidates. They must not mutate canonical packs, approve rules, or convert conversation memory into truth.

## 7. Failure Types To Track

Useful failure categories:

- **wrong scope**: answer uses the wrong channel, product, pack, or rule scope
- **unsupported assertion**: material claim has no rule/evidence/review support
- **hallucinated dependency**: invented dependency path or relationship
- **critical miss**: misses an important affected rule, dependency, or test area
- **over-broad noise**: produces broad generic risk lists that reduce signal
- **authority drift**: candidate output reads like final truth or final decision
- **unsafe data handling**: sensitive or unauthorized data enters prompt, output, or logs
- **instruction injection failure**: untrusted source text changes model behavior

These categories help evaluate RTS service quality beyond whether an answer sounds plausible.

## 8. Sensitive Data Boundary

RTS may know about many source systems, but LLM context assembly should be minimal and permission-aware.

Be especially careful with:

- raw production messages
- customer/account/trade/message identifiers
- defect attachments
- monitor alert text
- incident details
- release approval records

The safest default is to provide structured, sanitized, scoped context unless a caller has explicit permission and the use case requires more.

## 9. Deterministic Capabilities Are First-Class

RTS should not use LLM for work that stable structure can do better.

First-class deterministic capabilities include:

- scope resolution
- stable URI lookup
- dependency graph traversal
- L0/L1/L2 navigation
- fixed output templates
- trace construction

LLM should add value by organizing, explaining, finding non-obvious candidates, summarizing uncertainty, and turning complex material into reviewer-friendly questions.
