<!-- docmeta
role: domain
layer: 2
parent: docs/INDEX.md
children:
  - docs/transformation-rule-system/system-constitution-v1.md
  - docs/transformation-rule-system/ov-boundary-and-adoption.md
  - docs/transformation-rule-system/minimal-ov-integration.md
  - docs/transformation-rule-system/ov-kb-discussion-synthesis-2026-04-16.md
  - docs/transformation-rule-system/ov-kb-retrieval-design.md
  - docs/transformation-rule-system/session-handoff-2026-04-16.md
summary: route design questions for truth-source governance, OV boundary, retrieval design, and controlled OV adoption
read_when:
  - the request is about system principles for the transformation rule system
  - the request is about whether and how OpenViking should be used
  - the request needs a decision between constitution, boundary, or integration scope
skip_when:
  - the request is only about the original standalone diagram asset
  - the exact design leaf is already known
source_of_truth:
  - transformation-rule-system-handoff-2026-04-16/HANDOFF-PROMPT.md
  - docs/transformation-rule-system
-->

# Transformation Rule System Index

## Scope

This domain covers the design posture for a bank XML transformation rule system where pack completion establishes truth, ambiguity must remain explicit, LLM freedom must be actively constrained, and approved truth must be projected into retrieval in a narrower runtime form.

## Open One Of These Leaves

- `docs/transformation-rule-system/system-constitution-v1.md` — the formal system constitution: what the system is optimizing for, what the LLM is allowed to do, and what must never drift.
- `docs/transformation-rule-system/ov-boundary-and-adoption.md` — the clean boundary between the rule truth source and OpenViking, including the parts worth borrowing and the parts that must stay outside.
- `docs/transformation-rule-system/minimal-ov-integration.md` — a smallest-possible OV integration path with scope, stop rules, rollout phases, and a rough engineering envelope.
- `docs/transformation-rule-system/ov-kb-discussion-synthesis-2026-04-16.md` — the cleaned full-session synthesis: user goals, key corrections, standout insights, final decisions, and open questions.
- `docs/transformation-rule-system/ov-kb-retrieval-design.md` — the retrieval architecture: projection model, L0/L1/L2 layered loading, OV capability selection, and validated query scenarios.
- `docs/transformation-rule-system/session-handoff-2026-04-16.md` — condensed handoff for the next session: what has been concluded, what was corrected, and where to continue learning OV in plain language.

## Do Not Read This For

- pack-level authoring instructions that already live in the handoff prompt
- implementation details of OpenViking internals
- runtime agent behavior unrelated to truth-source governance
