<!-- docmeta
role: agent
layer: 2
parent: AGENTS.md
children: []
summary: local navigation and safety rules for portable RTS skills bundled with this repository
read_when:
  - coding agent needs to use or modify repo-bundled RTS skills
  - preparing to copy RTS to a company computer and run source-to-KB workflows
  - validating portable skills before installation
skip_when:
  - task does not involve skills or source-to-KB workflow preparation
source_of_truth:
  - skills/rts-workflow-source-profiler/SKILL.md
  - skills/rts-source-to-kb-pack/SKILL.md
  - skills/rts-kb-pack-review/SKILL.md
  - docs/confirmed/source-to-kb-skill-pipeline-plan-zh.md
-->

# Skills AGENTS

## Purpose

This directory bundles portable RTS skills with the repository. They are meant to travel with the repo to a company computer before corporate source assets are available.

## Local Map

- `rts-workflow-source-profiler/` — profile read-only workflow source into `sources/{source_bundle_id}/`.
- `rts-source-to-kb-pack/` — generate `kb/{pack_id}/` from an existing source profile.
- `rts-kb-pack-review/` — review generated KB and produce `ask-user-questions.json`.

## Safety

- These skills do not assume a company code structure.
- They must not modify company source.
- They must not copy long proprietary source excerpts into RTS.
- Snapshot/runtime publisher skills are intentionally not implemented yet; production publisher requires deterministic validator/hash/signoff/projection scripts.

## Search Order

When using a bundled skill directly, read:

1. The skill's `SKILL.md`.
2. The specific `references/*.md` named by the skill.
3. The exact template files needed for the output.
4. `docs/confirmed/source-to-kb-skill-pipeline-plan-zh.md` only when broader pipeline context is needed.
