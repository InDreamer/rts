# RTS AI Token Review Materials — 2026-04-27 to 2026-04-28

This folder preserves the RTS discussion and review artifacts from the 2026-04-27 to 2026-04-28 AI/LLM token review workstream.

## Final decision

Start from:

- [`2026-04-28-rts-final-ai-token-review-decision.md`](./2026-04-28-rts-final-ai-token-review-decision.md)

Final position:

```yaml
verdict: Conditional approve for evidence evaluation only
not_approved_as: Broad RTS AI Backend Service
approved_direction: RTS Impact Analysis and Test Planning Evidence Evaluation PoC
recommended_name_zh: RTS 变更影响分析与测试规划证据评估 PoC
recommended_name_en: RTS Impact Analysis and Test Planning Evidence Evaluation PoC
first_round_form: Evaluation harness / decision-support workflow
not_first_round_form: Multi-caller backend AI service
```

In short: do **not** submit the first request as a broad AI backend service. Reframe it as an evidence harness that proves whether LLM adds measurable value over deterministic indexes/templates, Copilot/Yoda, and existing SME workflows.

## Reading order

1. [`2026-04-28-rts-final-ai-token-review-decision.md`](./2026-04-28-rts-final-ai-token-review-decision.md) — final consolidated review decision.
2. [`2026-04-28-rts-ai-backend-service-deep-recommendations.md`](./2026-04-28-rts-ai-backend-service-deep-recommendations.md) — actionable recommendations after the strict review.
3. [`2026-04-28-rts-ai-backend-service-critical-token-review.md`](./2026-04-28-rts-ai-backend-service-critical-token-review.md) — strict AI Factory / token approval challenge report.
4. [`2026-04-27-rts-ai-analysis-service-key-request-review-brief.md`](./2026-04-27-rts-ai-analysis-service-key-request-review-brief.md) — original review-team brief that positioned RTS as a reusable LLM-backed backend service.
5. Earlier strategy/background reports:
   - [`2026-04-27-rts-ai-agent-service-strategy.md`](./2026-04-27-rts-ai-agent-service-strategy.md)
   - [`2026-04-27-rts-llm-api-key-application-brief.md`](./2026-04-27-rts-llm-api-key-application-brief.md)
   - [`2026-04-27-rts-four-layer-architecture-audit.md`](./2026-04-27-rts-four-layer-architecture-audit.md)
   - [`2026-04-27-rts-ai-analysis-service-key-request-brief-simple.md`](./2026-04-27-rts-ai-analysis-service-key-request-brief-simple.md)
   - [`2026-04-27-rts-ai-analysis-service-key-request-brief-v2.md`](./2026-04-27-rts-ai-analysis-service-key-request-brief-v2.md)

## Generated deliverables

Generated PDFs, proposal markdown, diagram artifacts, and preview images are stored under:

- [`../generated-artifacts/`](../generated-artifacts/)

Key generated artifacts include:

- `rts-domain-architecture-proposal-cn-v2.md/pdf`
- `rts-domain-architecture-proposal-cn.md/pdf`
- `rts-ai-foundation-mobile.md/pdf`
- `rts-ai-analysis-service-key-request-review-brief.pdf`
- `rts-domain-architecture-diagram.*`

## Final recommended next artifact

Prepare a charter before submitting any token/LLM request:

```text
RTS Impact Analysis and Test Planning Evidence Evaluation Charter
```

The charter should include:

- first-round scope
- allowed / forbidden data
- caller policy
- goldenset protocol
- deterministic baseline
- Copilot/Yoda comparator
- LLM output schema
- SME scoring rubric
- failure taxonomy
- data-safety evidence
- RACI
- service expansion evidence gates

## Boundary summary

First-round scope should preserve only:

- transformation impact analysis
- test planning / regression checklist

Defer or remove from first request:

- historical transaction sample discovery
- defect triage / monitor alerts
- cross-role summaries as token justification
- release / rollback notes
- pipeline / monitor / Chatbot / MCP / agent integrations
- raw production data access

