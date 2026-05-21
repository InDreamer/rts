---
name: rts-kb-pack-review
description: Independently review an RTS KB pack for contract compliance, workflow completeness, source evidence coverage, dependency closure, ambiguity, and readiness for snapshot skeleton. Use when Codex needs to inspect kb/{pack_id}/ generated from source profiles, update review/review-index.yaml, reports/review-checklist.md, reports/closure-check.md, and produce review/ask-user-questions.json for blocking questions without silently rewriting truth or claiming production runtime projection.
---

# RTS KB Pack Review

## Purpose

Review a generated RTS KB pack independently from the generator. Default to review-only: report findings, update review artifacts, and produce user questions. Modify KB truth only when the user explicitly asks for fixes.

## Required References

Open these before review:

- `references/review-contract.md`
- `references/ask-user-questions-contract.md`
- `references/templates/ask-user-questions.json`
- `references/templates/review-index.yaml`
- `references/templates/closure-check.md`

If this skill is used inside the RTS repo, also read:

- `docs/confirmed/kb-authoring-snapshot-runtime-final-choice-zh.md`
- `docs/confirmed/source-to-kb-skill-pipeline-plan-zh.md`

## Workflow

1. Read `kb/{pack_id}/metadata.yaml`.
2. Read all rules, lookups, helpers, evidence index, review index, and reports.
3. If a matching `sources/{source_bundle_id}/` exists, read `source-index.yaml` and `workflow-map.yaml`.
4. Check contract compliance:
   - required files
   - required fields
   - stable ids and object types
   - scope consistency
   - evidence refs and source anchors
5. Check workflow completeness:
   - gRPC inbound
   - message classification
   - FpML/XML parse
   - Java/Camel/XSLT/DB/Excel/enum transformation
   - SCBML/XML assembly
   - Solace outbound
6. Check dependency closure and field binding coverage.
7. Check ambiguity and conflict handling.
8. Write or update review artifacts.

## Output

Produce or update:

```text
kb/{pack_id}/reports/review-checklist.md
kb/{pack_id}/reports/closure-check.md
kb/{pack_id}/review/review-index.yaml
kb/{pack_id}/review/ask-user-questions.json
```

## Severity

Use:

- `blocking`: must be answered before snapshot/runtime skeleton.
- `important`: should be answered before production signoff.
- `clarifying`: improves KB but can remain documented.

## askUserQuestionTool Boundary

`ask-user-questions.json` is the portable handoff format. If the runtime exposes askUserQuestionTool, ask only 1 to 3 `blocking` questions at a time.

Every question must include:

- question id
- severity
- object refs
- the exact question
- why it is needed
- 2 to 3 suggested options when possible
- what each option changes
- whether freeform is allowed
- which gate it blocks

Do not ask the user questions that source analysis can answer. Reinspect source or KB first.

## Snapshot / Runtime Boundary

This skill may say whether the KB appears ready for snapshot skeleton. It must not claim production signoff or production runtime projection.

Allowed conclusion examples:

- `not_ready_blocking_questions`
- `ready_for_snapshot_skeleton`
- `ready_for_demo_runtime_skeleton`
- `not_ready_missing_source_profile`

## Completion Check

Before finishing, verify:

- review checklist exists.
- closure check states blocking count and readiness.
- review-index contains structured findings.
- ask-user-questions.json contains only questions requiring user judgment.
- no production signoff claim was made.
