# Source-Backed KB Pack Contract

Create only the KB draft authoring package. Do not create snapshots or runtime projection files.

Required output:

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

Required source inputs:

```text
sources/{source_bundle_id}/source-manifest.yaml
sources/{source_bundle_id}/source-index.yaml
sources/{source_bundle_id}/workflow-map.yaml
sources/{source_bundle_id}/claims.jsonl
sources/{source_bundle_id}/unresolved-questions.yaml
real source anchors referenced by claims, when company source is available
```

`source-index.yaml` and `workflow-map.yaml` are navigation and coverage inputs. They are not business truth. KB truth must come from eligible claims and their source anchors.

Every KB object should include:

- `schema_version`
- stable `id`
- `object_type`
- `status`
- `signoff_status`
- `revision`
- `scope`
- `claim_refs`
- `claim_status_used`
- `source_anchors` or `evidence_refs`
- `dependencies`
- type-specific structured body
- `warnings` when applicable

Allowed claim statuses for KB truth:

```text
supported
user_confirmed
runtime_observed
```

Disallowed claim statuses for KB truth:

```text
blocked
unsupported
inferred
contradicted
not_accessible
```

Disallowed statuses must be represented as review findings, warnings, blocker questions, or explicit gaps.

Reports explain and review; they must not be the only location of rule truth.
