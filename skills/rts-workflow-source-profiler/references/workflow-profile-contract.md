# Workflow Profile Contract

The profiler creates evidence and workflow mapping only. It must not create KB truth.

Output shape:

```text
sources/{source_bundle_id}/
  source-manifest.yaml
  source-index.yaml
  workflow-map.yaml
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
