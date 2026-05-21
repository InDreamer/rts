# KB Pack Contract

Create only the KB authoring package. Do not create snapshots or runtime projection files.

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
  attachments/                 # optional
```

Required source inputs:

```text
sources/{source_bundle_id}/source-manifest.yaml
sources/{source_bundle_id}/source-index.yaml
sources/{source_bundle_id}/workflow-map.yaml
sources/{source_bundle_id}/unresolved-questions.yaml
```

Every KB object should include:

- `schema_version`
- stable `id`
- `object_type`
- `status`
- `signoff_status`
- `revision`
- `scope`
- `source_anchors` or `evidence_refs`
- `dependencies`
- type-specific structured body
- `warnings` when applicable

Reports explain and review; they must not be the only location of rule truth.
