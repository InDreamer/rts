---
name: rts-source-to-kb-pack
description: Generate an RTS structured KB authoring package from an existing RTS source profile. Use when Codex has sources/{source_bundle_id}/source-manifest.yaml, source-index.yaml, workflow-map.yaml, extraction-notes.md, and unresolved-questions.yaml, and needs to produce kb/{pack_id}/metadata.yaml, rules/*.yaml, lookups/*.yaml, helpers/*.yaml, evidence/evidence-index.yaml, review/review-index.yaml, and reports without bypassing the source profile, modifying source code, or claiming production signoff.
---

# RTS Source To KB Pack

## Purpose

Turn a completed source profile into a structured KB authoring package. This skill creates governed working truth, not snapshot truth and not runtime service truth.

## Required References

Open these before writing output:

- `references/kb-pack-contract.md`
- `references/rule-lookup-helper-modeling.md`
- `references/templates/metadata.yaml`
- `references/templates/rule.yaml`
- `references/templates/lookup.yaml`
- `references/templates/helper.yaml`
- `references/templates/evidence-index.yaml`
- `references/templates/review-index.yaml`

If this skill is used inside the RTS repo, also read:

- `docs/confirmed/kb-authoring-snapshot-runtime-final-choice-zh.md`
- `docs/confirmed/kb-runtime-index-layer-standard-zh.md`

## Workflow

1. Read the source profile first:
   - `sources/{source_bundle_id}/source-manifest.yaml`
   - `sources/{source_bundle_id}/source-index.yaml`
   - `sources/{source_bundle_id}/workflow-map.yaml`
   - `sources/{source_bundle_id}/unresolved-questions.yaml`
2. Choose `pack_id`, `canonical_revision`, scope, product, source system, target system, and workflow domain.
3. Create the KB directory shape.
4. Convert workflow steps into rule, lookup, and helper objects:
   - Rule: business output, target XML structure, routing decision, or transformation behavior
   - Lookup: DB, Excel, enum, config, or static mapping table
   - Helper: reusable parsing, normalization, composition, fallback, or selection logic
5. Preserve source evidence:
   - every object should reference source ids or evidence ids
   - weak evidence and inferred behavior must go to review
6. Write reports:
   - `extraction-report.md`
   - `review-checklist.md`
   - `closure-check.md`
7. Do not create snapshot/runtime artifacts.

## Output Directory

Produce exactly this baseline shape:

```text
kb/{pack_id}/
  metadata.yaml
  README.md
  rules/{rule_id}.yaml
  lookups/{lookup_id}.yaml
  helpers/{helper_id}.yaml
  evidence/evidence-index.yaml
  review/review-index.yaml
  reports/extraction-report.md
  reports/review-checklist.md
  reports/closure-check.md
  attachments/                 # optional
```

## Modeling Requirements

The KB must represent the whole workflow, not just a local field:

- inbound gRPC handling
- message classification
- input XML semantic fields and source paths
- output XML target paths
- Java/Camel/XSLT/DB/Excel/enum evidence
- fallback/default/error path
- field bindings
- dependency graph
- tests as evidence or explicit test gap
- unresolved ambiguity

## Uncertainty Rules

Use review records for:

- `missing_source`
- `conflicting_source`
- `weak_evidence`
- `inferred_behavior`
- `requires_runtime_db_sample`
- `requires_user_confirmation`
- `out_of_scope`

Do not hide uncertainty in prose. If it affects truth, make it structured in `review/review-index.yaml`.

## Completion Check

Before finishing, verify:

- `metadata.yaml` references the source bundle.
- every rule/lookup/helper has stable id, object_type, status, scope, revision or signoff status, source anchors or evidence refs, and dependencies.
- workflow coverage includes gRPC inbound through Solace outbound.
- reports do not contain the only copy of rule truth.
- no files were written outside `kb/{pack_id}/` except explicitly requested logs.
