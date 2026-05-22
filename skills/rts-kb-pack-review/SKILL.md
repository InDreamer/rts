---
name: rts-kb-pack-review
description: Independently review an RTS KB draft for contract compliance, workflow completeness, source-backed claim evidence, anchor laundering, coverage gaps, contradictions, runtime config gaps, negative-claim hallucination, dependency closure, ambiguity, blocking count, and blocker questions without silently rewriting truth or claiming production runtime projection.
---

# RTS Source-Backed KB Pack Review

## Purpose

Review a generated RTS KB draft independently from the generator. Default to review-only: report findings, update review artifacts, produce blocker questions, and write a completion report. Modify KB truth only when the user explicitly asks for fixes.

This review must be source-backed when company source is available. A KB object that merely cites a source inventory summary is not sufficiently supported.

## Required References

Open these before review:

- `references/review-contract.md`
- `references/ask-user-questions-contract.md`
- `references/templates/ask-user-questions.json`
- `references/templates/review-index.yaml`
- `references/templates/closure-check.md`
- `references/templates/completion-report.md`

If this skill is used inside the RTS repo, also read:

- `docs/confirmed/kb-authoring-snapshot-runtime-final-choice-zh.md`
- `docs/confirmed/source-to-kb-skill-pipeline-plan-zh.md`

## Workflow

1. Read `kb/{pack_id}/metadata.yaml`.
2. Read all rules, lookups, helpers, evidence index, review index, and reports.
3. Read matching source inventory:
   - `sources/{source_bundle_id}/source-index.yaml`
   - `sources/{source_bundle_id}/workflow-map.yaml`
   - `sources/{source_bundle_id}/claims.jsonl`
4. When company source is available, inspect the source anchors for high-risk and sampled KB claims.
5. Check contract compliance:
   - required files
   - required fields
   - stable ids and object types
   - scope consistency
   - `claim_refs`, evidence refs, and source anchors
6. Check claim gate:
   - KB truth uses only `supported`, `user_confirmed`, or `runtime_observed` claims
   - `blocked`, `unsupported`, `inferred`, `contradicted`, and `not_accessible` claims appear only in review/warning material
7. Check workflow completeness:
   - gRPC inbound
   - message classification
   - FpML/XML parse
   - Java/Camel/XSLT/DB/Excel/enum transformation
   - runtime config or explicit gap
   - SCBML/XML assembly
   - Solace outbound
8. Check hallucination and omission risks:
   - unsupported claim
   - anchor laundering
   - coverage gap
   - contradiction
   - runtime config gap
   - negative claim hallucination
9. Check dependency closure and field binding coverage.
10. Write or update review artifacts.

## Output

Produce or update:

```text
kb/{pack_id}/reports/review-checklist.md
kb/{pack_id}/reports/closure-check.md
kb/{pack_id}/reports/completion-report.md
kb/{pack_id}/review/review-index.yaml
kb/{pack_id}/review/ask-user-questions.json
```

## Severity

Use:

- `blocking`: must be answered before MVP completion.
- `important`: should be answered before any demo release or later snapshot work.
- `clarifying`: improves KB but can remain documented.

## askUserQuestionTool Boundary

`ask-user-questions.json` is the portable handoff format. If the runtime exposes askUserQuestionTool, ask only 1 to 3 `blocking` questions at a time.

Every question must include:

- question id
- severity
- object refs
- claim refs when available
- the exact question
- why it is needed
- 2 to 3 suggested options when possible
- what each option changes
- whether freeform is allowed
- which gate it blocks

Do not ask the user questions that source analysis can answer. Reinspect source or KB first.

## MVP Boundary

This skill may say whether the KB draft appears ready for MVP completion. It must not claim production signoff, production snapshot, or production runtime projection readiness.

Allowed conclusion examples:

- `not_ready_blocking_questions`
- `not_ready_missing_source_inventory`
- `not_ready_missing_claims`
- `not_ready_contract_errors`
- `ready_for_kb_draft_mvp_completion`

## Completion Check

Before finishing, verify:

- review checklist exists.
- closure check states blocking count and readiness.
- completion report exists and includes explicit non-production statement.
- review-index contains structured findings.
- ask-user-questions.json contains only questions requiring user judgment.
- no production signoff, production snapshot, or production runtime projection claim was made.
