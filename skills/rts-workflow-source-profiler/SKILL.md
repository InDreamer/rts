---
name: rts-workflow-source-profiler
description: Profile a read-only complete workflow source codebase into an RTS source intake bundle. Use when Codex needs to analyze company or external source assets for a full transformation workflow, such as gRPC inbound FpML/XML through Java, enum, XSLT, Camel, PostgreSQL, tests, Excel mappings, and Solace outbound SCBML/XML, and produce sources/{source_bundle_id}/source-manifest.yaml, source-index.yaml, workflow-map.yaml, extraction-notes.md, and unresolved-questions.yaml without generating KB truth or modifying source code.
---

# RTS Workflow Source Profiler

## Purpose

Build a source profile for a complete transformation workflow. Do not generate KB objects in this skill. The output is evidence and workflow mapping only.

## Required References

Open these before writing output:

- `references/workflow-profile-contract.md`
- `references/source-safety-boundary.md`
- `references/templates/source-manifest.yaml`
- `references/templates/source-index.yaml`
- `references/templates/workflow-map.yaml`
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
2. Inspect source read-only. Use fast search first:
   - Java: service, handler, processor, route, mapper, enum, transformer
   - XML/XSLT: templates, target paths, namespace declarations
   - Camel: routes, processors, endpoints
   - PostgreSQL: schemas, SQL, seed/mapping tables, repository classes
   - Excel/CSV: mapping workbooks or exported tables
   - Tests: unit/integration fixtures proving expected behavior
3. Create stable source ids and locators. Prefer path + symbol + line range + commit/revision when available.
4. Build `workflow-map.yaml` from entrypoint to outbound publication.
5. Record candidate rule, lookup, helper, field binding, fallback, default, error path, and test coverage.
6. Put unknowns into `unresolved-questions.yaml`; do not silently infer truth.
7. Write only under `sources/{source_bundle_id}/`.

## Output Directory

Produce exactly this baseline shape:

```text
sources/{source_bundle_id}/
  source-manifest.yaml
  source-index.yaml
  workflow-map.yaml
  extraction-notes.md
  unresolved-questions.yaml
  raw/                         # optional; only if explicitly allowed
  normalized/                  # optional; only source-derived summaries/tables
```

## Rules

- Do not modify the source repository.
- Do not copy long proprietary source excerpts into RTS.
- Do not generate `kb/{pack_id}/` from this skill.
- Do not generate snapshot or runtime projection artifacts.
- Do not treat source profile entries as confirmed KB truth.
- If a source cannot be read, record the limitation and whether it blocks KB generation.

## Completion Check

Before finishing, verify:

- gRPC inbound path is identified or marked missing.
- input XML parse/classification path is identified or marked missing.
- Java, enum, XSLT, Camel, PostgreSQL, Excel, and tests are either mapped or explicitly marked absent/not available.
- SCBML output assembly and Solace outbound path are identified or marked missing.
- every workflow step has source refs or an unresolved question.
- no output outside `sources/{source_bundle_id}/` was created.
