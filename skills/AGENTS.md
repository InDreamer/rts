<!-- docmeta
role: agent
layer: 2
parent: AGENTS.md
children: []
summary: local navigation and safety rules for portable RTS source-backed KB draft MVP skills bundled with this repository
read_when:
  - coding agent needs to use or modify repo-bundled RTS skills
  - preparing to copy RTS to a company computer and run source-backed KB draft MVP workflows
  - validating portable skills before installation
skip_when:
  - task does not involve skills or source-backed KB draft workflow preparation
source_of_truth:
  - skills/rts-workflow-source-profiler/SKILL.md
  - skills/rts-source-to-kb-pack/SKILL.md
  - skills/rts-kb-pack-review/SKILL.md
  - docs/confirmed/source-to-kb-skill-pipeline-plan-zh.md
-->

# Skills AGENTS

## Purpose

This directory bundles portable RTS skills with the repository. They are meant to travel with the repo to a company computer before corporate source assets are available.

MVP boundary:

- The skills produce a source-backed KB draft and review artifacts.
- They do not prove accurate/complete KB truth.
- They do not produce production snapshot, formal signoff, immutable runtime projection, or production readiness.

## Local Map

- `rts-workflow-source-profiler/` — build source inventory / coverage map and `claims.jsonl` under `sources/{source_bundle_id}/`.
- `rts-source-to-kb-pack/` — generate `kb/{pack_id}/` from eligible claims plus real source anchors; the inventory is navigation only.
- `rts-kb-pack-review/` — run source-backed KB draft review and produce `ask-user-questions.json` plus `completion-report.md`.

## Safety

- These skills do not assume a company code structure.
- They must not modify company source.
- They must not copy long proprietary source excerpts into RTS.
- Source inventory / coverage map is not business truth.
- Nontrivial KB truth must have `claim_id` and source anchor.
- Only `supported`, `user_confirmed`, and `runtime_observed` claims may enter KB truth.
- `blocked`, `unsupported`, `inferred`, `contradicted`, and `not_accessible` claims belong in review/warning/blocker material only.
- Snapshot/runtime publisher skills are intentionally not implemented yet; production publisher requires deterministic validator/hash/signoff/projection scripts.

## Search Order

When using a bundled skill directly, read:

1. The skill's `SKILL.md`.
2. The specific `references/*.md` named by the skill.
3. The exact template files needed for the output.
4. `docs/confirmed/source-to-kb-skill-pipeline-plan-zh.md` only when broader pipeline context is needed.
