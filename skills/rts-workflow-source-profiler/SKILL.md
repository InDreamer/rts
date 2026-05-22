---
name: rts-workflow-source-profiler
description: Build a source inventory / coverage map and evidence-backed claims for a read-only complete workflow source codebase. Use when Codex needs to analyze company or external source assets for a full transformation workflow, such as gRPC inbound FpML/XML through Java, enum, XSLT, Camel, PostgreSQL, tests, Excel mappings, and Solace outbound SCBML/XML, and produce sources/{source_bundle_id}/source-manifest.yaml, source-index.yaml, workflow-map.yaml, claims.jsonl, extraction-notes.md, and unresolved-questions.yaml without generating KB truth, modifying source code, or treating the inventory as authoritative business truth.
---

# RTS Workflow Source Inventory

## Purpose

Build a source inventory / coverage map for a complete transformation workflow. Do not generate KB objects in this skill. The output is navigation, coverage, source anchors, and evidence-backed candidate claims only.

The inventory is not truth. It cannot by itself support KB claims.

## Required References

Open these before writing output:

- `references/workflow-profile-contract.md`
- `references/source-safety-boundary.md`
- `references/templates/source-manifest.yaml`
- `references/templates/source-index.yaml`
- `references/templates/workflow-map.yaml`
- `references/templates/claims.jsonl`
- `references/templates/extraction-notes.md`
- `references/templates/unresolved-questions.yaml`

If this skill is used inside the RTS repo, also read:

- `docs/confirmed/kb-authoring-snapshot-runtime-final-choice-zh.md`
- `docs/confirmed/source-to-kb-skill-pipeline-plan-zh.md`

## Workflow

1. Confirm the target workflow boundary:
   - inbound protocol and entrypoint, usually gRPC
   - upstream payload, usually FpML or another XML
   - downstream payload, usually SCBML or another XML
   - outbound protocol, usually Solace
   - allowed read paths and forbidden paths
   - whether tests, DB metadata/sample rows, Excel files, and runtime config may be read
2. Inspect source read-only. Use fast search first:
   - Java: service, handler, processor, route, mapper, enum, transformer
   - XML/XSLT: templates, target paths, namespace declarations
   - Camel: routes, processors, endpoints
   - PostgreSQL: schemas, SQL, seed/mapping tables, repository classes
   - Excel/CSV: mapping workbooks or exported tables
   - Tests: unit/integration fixtures proving expected behavior
   - Runtime config: profiles, feature flags, broker/topic/queue config, deployment files when accessible
3. Create stable source ids and locators. Prefer path + symbol + line range + commit/revision/hash when available.
4. Build `workflow-map.yaml` as a coverage map from entrypoint to outbound publication. It may contain candidate links, but not final business truth.
5. Build `claims.jsonl` from evidence-backed claims. Every claim must have source anchors, evidence type, extraction method, status, confidence, and limits.
6. Put unknowns into `unresolved-questions.yaml`; do not silently infer truth.
7. Write only under `sources/{source_bundle_id}/`.

## Output Directory

Produce exactly this baseline shape:

```text
sources/{source_bundle_id}/
  source-manifest.yaml
  source-index.yaml
  workflow-map.yaml
  claims.jsonl
  extraction-notes.md
  unresolved-questions.yaml
  raw/                         # optional; only if explicitly allowed
  normalized/                  # optional; only source-derived summaries/tables
```

## Claim Status Rules

Allowed claim statuses:

- `supported`: directly supported by source anchors, tests, configs, samples, or structured evidence.
- `user_confirmed`: confirmed by a named user or approved workflow and recorded as such.
- `runtime_observed`: observed by running a test, sample transformation, or runtime query with recorded command/result.
- `blocked`: cannot be resolved without missing source, environment, DB, broker, SME, or permission.
- `unsupported`: proposed but not supported by source evidence.
- `inferred`: plausible interpretation, not directly proven.
- `contradicted`: conflicting source or evidence exists.
- `not_accessible`: source/evidence is required but inaccessible.

Only `supported`, `user_confirmed`, and `runtime_observed` claims may later enter KB truth. Other statuses must remain review/warning/blocker material.

## Rules

- Do not modify the source repository.
- Do not copy long proprietary source excerpts into RTS.
- Do not generate `kb/{pack_id}/` from this skill.
- Do not generate snapshot or runtime projection artifacts.
- Do not treat source inventory, workflow map, or claim hints as confirmed KB truth.
- Do not assert negative claims such as “no fallback exists” unless the reviewed source scope and search evidence support the absence; otherwise mark `blocked` or `not_accessible`.
- If a source cannot be read, record the limitation and whether it blocks KB generation.

## Completion Check

Before finishing, verify:

- `claims.jsonl` exists and every nontrivial candidate assertion has source anchors or a non-supported status.
- gRPC inbound path is identified or marked missing.
- input XML parse/classification path is identified or marked missing.
- Java, enum, XSLT, Camel, PostgreSQL, Excel, runtime config, and tests are either mapped or explicitly marked absent/not available.
- SCBML output assembly and Solace outbound path are identified or marked missing.
- every workflow step has source refs or an unresolved question.
- no output outside `sources/{source_bundle_id}/` was created.
