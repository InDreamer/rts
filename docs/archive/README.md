<!-- docmeta
role: domain
layer: 2
parent: docs/INDEX.md
children:
  - docs/archive/generated-artifacts/README.md
summary: route archived RTS materials that must not override confirmed or reference docs
read_when:
  - 需要追溯历史架构讨论、旧 proposal、生成产物或视觉资产
  - 需要确认某个旧材料为什么不属于 active baseline
skip_when:
  - 需要当前 RTS confirmed baseline
  - 需要运行服务或调用 API
source_of_truth:
  - docs/confirmed/README.md
  - docs/reference/README.md
  - docs/archive/generated-artifacts/README.md
-->

# Archived Docs

This directory contains historical RTS material that is not part of the active development baseline.

Older archive materials were reviewed and either deleted, distilled into current documents, or moved here to avoid confusing the active reference set.

Current distilled documents:

- Current RTS baseline: `docs/confirmed/project-alignment-summary-zh.md`
- RTS Constitution v2: `docs/confirmed/system-constitution-v1.md`
- RTS project background and pack model: `docs/reference/rts-project-background-and-pack-model.md`
- RTS retrieval principles distilled from OV/KB history: `docs/reference/rts-retrieval-principles.md`
- RTS publication/refusal principles: `docs/reference/rts-publication-and-refusal-principles.md`
- RTS service interface/output principles: `docs/reference/rts-service-interface-and-output-principles.md`
- Java index/query layer distilled reference: `docs/reference/java-index-layer-full-plan-zh.md`
- LLM-mediated query/harness distilled reference: `docs/reference/llm-enhanced-index-and-harness-design-zh.md`
- Final LLM agent service roadmap summary: `docs/confirmed/final-llm-agent-service-plan-zh.md`
- Day1 query/service compact baseline: `docs/confirmed/day1-query-service-and-llm-harness-plan-zh.md`
- Day2 controlled retrieval compact roadmap: `docs/confirmed/day2-agentic-retrieval-evolution-plan-zh.md`
- External review constraints summary: `docs/reference/external-review-final-2026-04-20.md`
- OV retrieval lessons summary: `docs/reference/ov-kb-retrieval-design.md`
- Compact glossary: `docs/reference/project-keywords-glossary-zh.md`

Archived folders:

- `assets/` — old OV/TRS architecture diagrams and visual assets.
- `generated-artifacts/` — generated proposal PDFs, markdown exports, diagram exports, and preview images.
- `reference-proposals/` — old domain architecture proposal materials.
- `reference-proposals/ai-token-review/` — archived long AI token review drafts; default reference route keeps only the consolidated rationale summary.

Duplicate proposal copies under `reference-proposals/` were removed when the same markdown/PDF already existed under `generated-artifacts/`.
The full 2026-04-20 Java index/query layer plan was archived here after being distilled into the shorter reference summary.
The full 2026-05-06 final LLM agent service plan was archived here after being distilled into the confirmed roadmap summary.
The full 2026-05-06 LLM-enhanced index and harness discussion was archived here after being distilled into the shorter reference summary.
The full Day1, Day2, external review, OV retrieval, and glossary documents were archived here after being distilled into compact confirmed/reference summaries.
The long AI token review drafts were archived under `reference-proposals/ai-token-review/` after being distilled into the consolidated historical rationale summary.

Only archive index pages are governed by docmeta/catalog. Individual archived originals and generated proposal exports are inventory-only historical files; they should not be treated as current source-of-truth documents.

Do not treat archived material as overriding `docs/confirmed/`.
