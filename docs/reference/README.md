<!-- docmeta
role: domain
layer: 2
parent: docs/INDEX.md
children:
  - docs/reference/java-index-layer-full-plan-zh.md
  - docs/reference/llm-enhanced-index-and-harness-design-zh.md
  - docs/reference/minimal-ov-integration.md
  - docs/reference/ov-boundary-and-adoption.md
  - docs/reference/ov-kb-retrieval-design.md
  - docs/reference/project-keywords-glossary-zh.md
  - docs/reference/external-review-final-2026-04-20.md
  - docs/reference/proposals/ai-token-review/README.md
  - docs/reference/rts-project-background-and-pack-model.md
  - docs/reference/rts-publication-and-refusal-principles.md
  - docs/reference/rts-retrieval-principles.md
  - docs/reference/rts-service-interface-and-output-principles.md
summary: route RTS reference materials that support but do not override confirmed baseline
read_when:
  - 需要历史 rationale、OV 背景、检索原则、术语表或早期 Java 参考方案
  - confirmed docs 指向参考材料以解释背景
skip_when:
  - 需要当前 active baseline 或 Day1/Day2 实现方向
  - 需要 archived archaeology
source_of_truth:
  - docs/confirmed/README.md
  - docs/reference
-->

# Reference Docs

This directory contains useful supporting materials for RTS, but it is not the default baseline for day-to-day alignment.

Use these files when a task needs:

- RTS project background and pack/object model (`rts-project-background-and-pack-model.md`)
- RTS publication, refusal, trace, and release safety principles (`rts-publication-and-refusal-principles.md`)
- RTS retrieval principles distilled from OV/KB history (`rts-retrieval-principles.md`)
- RTS service interface and output principles (`rts-service-interface-and-output-principles.md`)
- distilled LLM-mediated query and harness reference for scope mediation, context, memory, and controlled tool use (`llm-enhanced-index-and-harness-design-zh.md`)
- OV boundary rationale and adoption discussion
- distilled Java index/query layer reference summary (`java-index-layer-full-plan-zh.md`)
- retrieval semantics and scope-aware loading details
- external critique or proposal background
- AI token review rationale summary (`proposals/ai-token-review/README.md`)
- a Chinese glossary of current project terms and examples (`project-keywords-glossary-zh.md`)

Current convention:

- Treat `docs/confirmed/project-alignment-summary-zh.md` as the first alignment entry.
- Treat `rts-project-background-and-pack-model.md` as the current reference for historical sample context and pack/object shape.
- Treat `rts-publication-and-refusal-principles.md` as the current RTS-owned publication/refusal/trace safety reference.
- Treat `rts-retrieval-principles.md` as the current RTS-owned retrieval principle document.
- Treat `rts-service-interface-and-output-principles.md` as the current RTS-owned API/MCP/output quality reference.
- Treat `llm-enhanced-index-and-harness-design-zh.md` as the distilled LLM mediation/harness reference. The full historical discussion lives in archive and does not override confirmed Day1/Day2 or final roadmap baseline.
- Treat OV-related documents here as historical/reference thinking, not as a final engineering baseline.
- Treat `java-index-layer-full-plan-zh.md` as the distilled Java index/query layer reference. The full historical plan lives in archive and is not a current infrastructure commitment.
- Treat `external-review-final-2026-04-20.md` as a historical critique that motivated confirmed contracts.
- Treat `proposals/ai-token-review/README.md` as a consolidated historical rationale summary; the old long review drafts are archived outside the default reference route after their useful guardrails were extracted.
- Do not treat older proposals or handoff notes as overriding the confirmed docs.
