<!-- docmeta
role: agent
layer: 1
parent: null
children: []
summary: coding-agent document index and active development navigation for RTS
read_when:
  - coding agent starts work in this repository
  - need confirmed document reading order
  - need repository-specific edit and archaeology boundaries
skip_when:
  - user only needs the public README route
source_of_truth:
  - docs/confirmed/README.md
  - docs/confirmed/project-alignment-summary-zh.md
-->

# AGENTS

## Purpose

This file is the primary document index for future development work in this repository.

Default rule:

- Read only the confirmed documents listed below.
- Use reference documents only when the task explicitly needs historical rationale or retrieval design details.
- Ignore archived material unless the user explicitly asks for archaeology.

## Confirmed Docs

Read these in order:

1. [docs/confirmed/project-alignment-summary-zh.md](/home/ubuntu/repos/rts/docs/confirmed/project-alignment-summary-zh.md)
2. [docs/confirmed/system-constitution-v1.md](/home/ubuntu/repos/rts/docs/confirmed/system-constitution-v1.md)
3. [docs/confirmed/kb-to-index-projection-contract-zh.md](/home/ubuntu/repos/rts/docs/confirmed/kb-to-index-projection-contract-zh.md)
4. [docs/confirmed/day1-query-service-and-llm-harness-plan-zh.md](/home/ubuntu/repos/rts/docs/confirmed/day1-query-service-and-llm-harness-plan-zh.md)
5. [docs/confirmed/day2-agentic-retrieval-evolution-plan-zh.md](/home/ubuntu/repos/rts/docs/confirmed/day2-agentic-retrieval-evolution-plan-zh.md)
6. [docs/confirmed/final-llm-agent-service-plan-zh.md](/home/ubuntu/repos/rts/docs/confirmed/final-llm-agent-service-plan-zh.md)

These documents are the active development baseline. For first implementation work, read through Day1. Read Day2 only when planning post-Day1 retrieval, LLM, MCP, rerank, or agentic evolution. Read the final LLM agent service plan when designing the complete RTS service surface, controlled harness, memory/context, MCP/API/Q&A/pipeline consumption, or agent support boundary.

## Reference Docs

Open these only when needed:

- [docs/reference/ov-boundary-and-adoption.md](/home/ubuntu/repos/rts/docs/reference/ov-boundary-and-adoption.md)
  Use for OV boundary questions and what was intentionally borrowed or excluded.
- [docs/reference/ov-kb-retrieval-design.md](/home/ubuntu/repos/rts/docs/reference/ov-kb-retrieval-design.md)
  Use for L0/L1/L2 semantics and retrieval behavior reference.
- [docs/reference/java-index-layer-full-plan-zh.md](/home/ubuntu/repos/rts/docs/reference/java-index-layer-full-plan-zh.md)
  Use for the Java index/query layer reference implementation plan.
- [docs/reference/minimal-ov-integration.md](/home/ubuntu/repos/rts/docs/reference/minimal-ov-integration.md)
  Use for historical scope guardrails.
- [docs/reference/handoff/HANDOFF-PROMPT.md](/home/ubuntu/repos/rts/docs/reference/handoff/HANDOFF-PROMPT.md)
  Use only when legacy pack shape examples or historical pack workflow details are required.

## Archived Material

Do not read by default:

- [docs/archive](/home/ubuntu/repos/rts/docs/archive)
- [archive/python-mvp-2026-04-20](/home/ubuntu/repos/rts/archive/python-mvp-2026-04-20)

These locations contain:

- historical discussion
- legacy navigation files
- archived prototype material
- the Python MVP and its sample data

They are not part of the active implementation path.

## Active Direction

The active implementation path is:

- JDK 17 Java query/index service
- Day1 controlled LLM harness
- TRS-controlled truth layer
- no OpenViking runtime dependency
- no active Python MVP development
