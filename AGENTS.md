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

- Read only the confirmed documents listed below unless the task is explicitly about operational usage or historical rationale.
- Use reference documents only when the task explicitly needs supporting rationale, terminology, retrieval background, or historical tradeoffs.
- Ignore archived material unless the user explicitly asks for archaeology.

## Confirmed Docs

Read these in order:

1. [docs/confirmed/project-alignment-summary-zh.md](docs/confirmed/project-alignment-summary-zh.md)
2. [docs/confirmed/system-constitution-v1.md](docs/confirmed/system-constitution-v1.md)
3. [docs/confirmed/kb-to-index-projection-contract-zh.md](docs/confirmed/kb-to-index-projection-contract-zh.md)
4. [docs/confirmed/runtime-projection-product-guide-zh.md](docs/confirmed/runtime-projection-product-guide-zh.md)
5. [docs/confirmed/llm-harness-and-agent-integration-alignment-zh.md](docs/confirmed/llm-harness-and-agent-integration-alignment-zh.md)
6. [docs/confirmed/internal-llm-agent-service-implementation-plan-zh.md](docs/confirmed/internal-llm-agent-service-implementation-plan-zh.md)
7. [docs/confirmed/day1-query-service-and-llm-harness-plan-zh.md](docs/confirmed/day1-query-service-and-llm-harness-plan-zh.md)
8. [docs/confirmed/day2-agentic-retrieval-evolution-plan-zh.md](docs/confirmed/day2-agentic-retrieval-evolution-plan-zh.md)
9. [docs/confirmed/final-llm-agent-service-plan-zh.md](docs/confirmed/final-llm-agent-service-plan-zh.md)
10. [docs/confirmed/document-decision-register-zh.md](docs/confirmed/document-decision-register-zh.md)

These documents are the active development baseline.

Read them with this mental model:

- RTS is a **dual-core stack**: a controlled truth-source atomic capability service and a managed LLM agent analysis service.
- AI is a **controlled analysis-and-expression layer**, not an answer organizer.
- deterministic capability is first-class, but AI-centric scenario services treat managed analysis as the normal product mode.
- when LLM is unavailable, scenario behavior degrades to structured information provision; that degraded mode does not redefine the product identity.
- candidate-only and human decision boundaries are authority boundaries, not ceilings on analysis depth.

For first implementation work, understand the dual-core positioning and truth boundaries before reading Day1/Day2 execution details. Read the runtime projection product guide when explaining the runtime package to PMs, product owners, integrators, or agents without needing exact field contracts. Read the LLM harness and agent integration alignment note when deciding whether capability belongs in managed mode, tool mode, or both. Read the internal LLM agent implementation plan when changing service contracts, tool orchestration, scenario endpoints, memory/context, evaluation, or rollout controls. Read Day1 and Day2 when deciding phased implementation order. Read the final roadmap when checking long-range direction rather than current authority. Read the decision register when continuing documentation cleanup or deciding what historical material to retain, compress, archive, or delete.

For conflicts inside confirmed docs, use the topic-specific authority table in [docs/confirmed/README.md](docs/confirmed/README.md); dates and wording emphasis do not set precedence.

## Reference Docs

Open these only when needed:

- [docs/reference/README.md](docs/reference/README.md)
  Use when you need support materials that explain rationale, glossary terms, retrieval principles, or historical design tradeoffs without overriding confirmed baseline.
- [docs/reference/ov-boundary-and-adoption.md](docs/reference/ov-boundary-and-adoption.md)
  Use for OV boundary questions and what was intentionally borrowed or excluded.
- [docs/reference/ov-kb-retrieval-design.md](docs/reference/ov-kb-retrieval-design.md)
  Use for L0/L1/L2 semantics and retrieval behavior reference.
- [docs/reference/java-index-layer-full-plan-zh.md](docs/reference/java-index-layer-full-plan-zh.md)
  Use for the distilled Java index/query layer reference summary; the full historical plan is archived.
- [docs/reference/minimal-ov-integration.md](docs/reference/minimal-ov-integration.md)
  Use for historical scope guardrails.

## Operational Docs

Open these when the task is about running or calling the local service rather than changing architecture:

- [docs/java-service-runbook-zh.md](docs/java-service-runbook-zh.md)
  Use for local startup, config, testing, troubleshooting, and admin/runtime operations.
- [docs/api-caller-guide-zh.md](docs/api-caller-guide-zh.md)
  Use for REST API requests, response fields, scope, warnings, refusal handling, deterministic truth/information queries, and managed analysis requests.

## Archived Material

Do not read by default:

- [docs/archive](docs/archive)
- [archive/python-mvp-2026-04-20](archive/python-mvp-2026-04-20)

These locations contain:

- historical discussion
- legacy navigation files
- archived prototype material
- the Python MVP and its sample data

They are not part of the active implementation path.

## Active Direction

The active implementation path is:

- JDK 17 Java query/index service
- RTS-controlled truth layer and stable atomic capability surface
- managed LLM harness inside RTS plus external tool mode for outside agents
- deterministic capability as first-class substrate
- scenario services whose normal product mode is managed analysis
- degraded information-service behavior when LLM is unavailable
- no OpenViking runtime dependency
- no active Python MVP development
