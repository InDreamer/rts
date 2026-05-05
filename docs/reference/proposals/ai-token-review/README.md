# RTS AI Service Strategy Review Materials — 2026-04-27 to 2026-04-28

This folder keeps the remaining AI service strategy and review materials after broad first-round AI backend service drafts were distilled and removed.

Current RTS-owned extractions live in:

- `../../rts-service-interface-and-output-principles.md`
- `../../rts-retrieval-principles.md`
- `../../../confirmed/project-alignment-summary-zh.md`
- `../../../confirmed/system-constitution-v1.md`

## What remains here

Recommended reading order:

1. `2026-04-27-rts-ai-agent-service-strategy.md` — long-term product/architecture strategy: RTS as a governed truth service with internal KB, retrieval, LLM, and service surfaces.
2. `2026-04-27-rts-four-layer-architecture-audit.md` — four-layer architecture audit: L1 truth, L2 mapping/projection, L3 retrieval, L4 applications.
3. `2026-04-28-rts-ai-backend-service-deep-recommendations.md` — useful long-term principles around candidate-only outputs, deterministic baseline, comparison, failure taxonomy, data boundaries, and caller policy.
4. `2026-04-28-rts-final-ai-token-review-decision.md` — historical final review conclusion for the token-specific workstream.
5. `2026-04-28-rts-ai-backend-service-critical-token-review.md` — historical strict challenge report; useful mainly for risk and failure categories.

## Removed drafts

The early broad AI backend service drafts were removed after their useful ideas were extracted. They framed RTS too strongly as a first-round broad AI backend service and are no longer the recommended narrative.

## Current interpretation

Do not read this folder as limiting RTS long-term scope.

The long-term RTS direction remains: RTS is a service that contains Knowledge Base, retrieval/indexing, LLM, governance workflow, and API/MCP/Q&A/pipeline-facing surfaces.

The useful ideas preserved from this folder are:

- LLM is an internal RTS capability, but LLM answers are not automatically truth.
- RTS should distinguish facts, inferences, unknowns, candidates, and human decisions.
- Agent/API/MCP callers may read and cite approved truth, and submit candidates or open questions, but must not directly rewrite approved truth.
- Deterministic retrieval, URI, dependency graph, and templates are first-class capabilities, not merely LLM support utilities.
- RTS should track failure types such as wrong scope, unsupported assertion, hallucinated dependency, over-broad noise, authority drift, and unsafe data handling.
