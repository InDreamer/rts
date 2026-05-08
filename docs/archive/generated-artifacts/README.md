<!-- docmeta
role: leaf
layer: 3
parent: docs/archive/README.md
children: []
summary: inventory of archived generated RTS proposal PDFs, markdown exports, diagrams, and previews
read_when:
  - 需要追溯旧 proposal 生成产物或图表导出
  - 需要确认 archive/generated-artifacts 中各文件来源
skip_when:
  - 需要当前 confirmed baseline
  - 需要引用 active implementation docs
source_of_truth:
  - docs/archive/generated-artifacts
  - tools
-->

# RTS Generated Proposal Artifacts — 2026-04-27 to 2026-04-28

This directory stores generated proposal documents, PDFs, diagram exports, and visual QA previews produced during the RTS Domain Architecture / AI token review workstream.

For the curated review narrative and final decision, see:

- [`../../reference/proposals/ai-token-review/README.md`](../../reference/proposals/ai-token-review/README.md)

## Contents

- `rts-ai-business-proposal.md/pdf` — early business-oriented AI proposal.
- `rts-ai-foundation-mobile.md/pdf` — mobile-friendly RTS AI foundation proposal.
- `rts-domain-architecture-proposal.md/pdf` — English Domain Architecture proposal.
- `rts-domain-architecture-proposal-cn.md/pdf` — Chinese Domain Architecture proposal.
- `rts-domain-architecture-proposal-cn-v2.md/pdf` — refined Chinese proposal version.
- `rts-ai-analysis-service-key-request-review-brief.pdf` — generated PDF for the RTS AI Analysis Service key request review brief.
- `rts-domain-architecture-diagram.*` — architecture diagram exports and manifest.
- `preview/` — selected PNG previews used for visual QA.

## Reproducibility

Generation scripts are kept in `/tools`, including:

- `tools/generate_ai_proposal_pdf.py`
- `tools/generate_rts_mobile_pdf.py`
- `tools/generate_rts_domain_architecture_pdf.py`
- `tools/generate_rts_domain_architecture_pdf_cn.py`
- `tools/generate_rts_domain_architecture_pdf_cn_v2.py`
- `tools/generate_rts_ai_analysis_service_key_request_pdf.py`
