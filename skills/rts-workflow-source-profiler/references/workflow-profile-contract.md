# Source Inventory / Coverage Map Contract

The profiler creates source inventory, coverage mapping, and evidence-backed claims only. It must not create KB truth.

The term `source profile` is intentionally deprecated for MVP execution. Treat this output as a navigation and coverage artifact, not as authoritative business truth.

Output shape:

```text
sources/{source_bundle_id}/
  source-manifest.yaml
  source-index.yaml
  workflow-map.yaml
  claims.jsonl
  extraction-notes.md
  unresolved-questions.yaml
  raw/                         # optional
  normalized/                  # optional
```

Minimum workflow coverage:

- gRPC inbound service, proto, handler, or adapter
- message classification or routing decision
- upstream XML/FpML parse and semantic field extraction
- Java transformation logic
- enum/config static mapping
- XSLT template and target XML construction
- Camel route or processor flow
- PostgreSQL schema, SQL, repository, or mapping table involvement
- Excel/CSV mapping involvement
- runtime profile/config/feature flag involvement when accessible
- downstream XML/SCBML assembly
- Solace producer/topic/queue publication
- tests and fixtures proving behavior
- fallback/default/error paths

Use explicit status for every area:

```text
found
not_found
not_applicable
not_accessible
needs_user_confirmation
```

Every workflow step must have either `source_refs` or an unresolved question. Do not leave silent gaps.

## Source Inventory Rules

- `source-index.yaml` stores stable locators, hashes/revisions, permission state, scan status, and short summaries.
- `workflow-map.yaml` stores coverage and navigation links only.
- Source inventory must not store final business rules, unsupported absence claims, or inferred end-to-end behavior as truth.
- Long proprietary source excerpts must not be copied into RTS.

## claims.jsonl Rules

`claims.jsonl` is required for the MVP. Each line is one claim with this minimum shape:

```json
{"schema_version":"source-claim-v1","claim_id":"claim-example-001","claim_type":"field_mapping","status":"supported","subject":"target.exampleField","assertion":"Example field is populated from upstream XPath X when condition Y holds.","source_anchors":[{"source_id":"src-example","path":"src/main/java/...","line_range":[84,112],"anchor_type":"code_path"}],"evidence_type":"code_path","extraction_method":"manual_source_read","confidence":"medium","limits":[]}
```

Allowed statuses:

```text
supported
user_confirmed
runtime_observed
blocked
unsupported
inferred
contradicted
not_accessible
```

Only `supported`, `user_confirmed`, and `runtime_observed` can later enter KB truth. All other statuses must be review/warning/blocker material.

Do not write negative claims such as "no fallback exists" unless the claim records the searched scope and supporting source anchors. Otherwise use `blocked` or `not_accessible`.
