---
name: rts-source-to-kb-pack
description: Generate an RTS structured KB draft package from source inventory, evidence-backed claims, and real source anchors. Use when Codex has sources/{source_bundle_id}/source-manifest.yaml, source-index.yaml, workflow-map.yaml, claims.jsonl, extraction-notes.md, and unresolved-questions.yaml, and needs to produce kb/{pack_id}/metadata.yaml, rules/*.yaml, lookups/*.yaml, helpers/*.yaml, evidence/evidence-index.yaml, review/review-index.yaml, and reports without treating the source inventory as truth, modifying source code, or claiming production signoff.
---

# RTS Source-Backed KB Pack

## Purpose

Turn source inventory plus evidence-backed claims into a structured KB draft package. This skill creates governed working truth candidates, not snapshot truth and not runtime service truth.

KB truth must be source-backed. Do not generate KB objects from workflow-map prose alone.

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
- `references/templates/completion-report.md`

If this skill is used inside the RTS repo, also read:

- `docs/confirmed/kb-authoring-snapshot-runtime-final-choice-zh.md`
- `docs/confirmed/kb-runtime-index-layer-standard-zh.md`
- `docs/confirmed/source-to-kb-skill-pipeline-plan-zh.md`

## Workflow

1. Read the source inventory and claims first:
   - `sources/{source_bundle_id}/source-manifest.yaml`
   - `sources/{source_bundle_id}/source-index.yaml`
   - `sources/{source_bundle_id}/workflow-map.yaml`
   - `sources/{source_bundle_id}/claims.jsonl`
   - `sources/{source_bundle_id}/unresolved-questions.yaml`
2. Re-read the real source anchors for every nontrivial KB claim when the company source is available. The inventory is navigation only.
3. Choose `pack_id`, `canonical_revision`, scope, product, source system, target system, and workflow domain.
4. Create the KB directory shape.
5. Convert eligible claims into rule, lookup, and helper objects:
   - Rule: business output, target XML structure, routing decision, or transformation behavior
   - Lookup: DB, Excel, enum, config, or static mapping table
   - Helper: reusable parsing, normalization, composition, fallback, or selection logic
6. Preserve claim and source evidence:
   - every KB object must reference `claim_refs`
   - every claim ref must resolve to `supported`, `user_confirmed`, or `runtime_observed`
   - weak, inferred, blocked, unsupported, contradicted, or inaccessible claims must go to review/warnings
7. Write reports:
   - `extraction-report.md`
   - `review-checklist.md`
   - `closure-check.md`
   - `completion-report.md`
8. Do not create snapshot/runtime artifacts.

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
  reports/completion-report.md
  attachments/                 # optional
```

## Claim Gate

Only these claim statuses may enter KB truth:

- `supported`
- `user_confirmed`
- `runtime_observed`

These statuses must not enter KB truth:

- `blocked`
- `unsupported`
- `inferred`
- `contradicted`
- `not_accessible`

Non-eligible claims belong in `review/review-index.yaml`, `warnings`, `reports/closure-check.md`, or `review/ask-user-questions.json`.

## Modeling Requirements

The KB must represent the whole workflow, not just a local field:

- inbound gRPC handling
- message classification
- input XML semantic fields and source paths
- output XML target paths
- Java/Camel/XSLT/DB/Excel/enum evidence
- runtime config where available, or an explicit runtime config gap
- fallback/default/error path
- field bindings
- dependency graph
- tests as evidence or explicit test gap
- unresolved ambiguity

## Anti-Hallucination Rules

Use review records for:

- `missing_source`
- `conflicting_source`
- `weak_evidence`
- `unsupported_claim`
- `inferred_behavior`
- `anchor_laundering_risk`
- `negative_claim_hallucination_risk`
- `requires_runtime_config`
- `requires_runtime_db_sample`
- `requires_user_confirmation`
- `out_of_scope`

Do not hide uncertainty in prose. If it affects truth, make it structured in `review/review-index.yaml`.

## Completion Check

Before finishing, verify:

- `metadata.yaml` references the source bundle.
- `claims.jsonl` was read and every nontrivial KB object has `claim_refs`.
- every `claim_ref` used by KB truth resolves to `supported`, `user_confirmed`, or `runtime_observed`.
- every rule/lookup/helper has stable id, object_type, status, scope, revision or signoff status, source anchors or evidence refs, and dependencies.
- workflow coverage includes gRPC inbound through Solace outbound.
- reports do not contain the only copy of rule truth.
- `completion-report.md` states that this is MVP KB draft output, not production readiness.
- no files were written outside `kb/{pack_id}/` except explicitly requested logs.
