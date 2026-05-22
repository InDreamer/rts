# KB Draft Completion Report

## Status

- Source bundle id: `{source_bundle_id}`
- Pack id: `{pack_id}`
- Workflow scope: `{workflow_scope}`
- MVP completion decision: `not_ready_blocking_questions | not_ready_missing_source_inventory | not_ready_missing_claims | not_ready_contract_errors | ready_for_kb_draft_mvp_completion`
- Non-production statement: This report covers MVP KB draft readiness only. It does not claim production signoff, production snapshot readiness, or production runtime projection readiness.

## Source And Claims

- Company source revision / hash / unavailable reason: `{source_revision_or_reason}`
- Claim source: `sources/{source_bundle_id}/claims.jsonl`
- Claim counts by status:
  - supported: 0
  - user_confirmed: 0
  - runtime_observed: 0
  - blocked: 0
  - unsupported: 0
  - inferred: 0
  - contradicted: 0
  - not_accessible: 0

## Coverage

- gRPC inbound: `{status}`
- message classification: `{status}`
- upstream XML/FpML parse: `{status}`
- Java/Camel/XSLT/DB/Excel/enum evidence: `{status}`
- runtime config: `{status_or_gap}`
- downstream XML/SCBML assembly: `{status}`
- Solace outbound: `{status}`
- tests and fixtures: `{status_or_gap}`
- fallback/default/error paths: `{status_or_gap}`

## Review Summary

- KB object counts: rules=0, lookups=0, helpers=0
- Blocking count: 0
- Blocker questions path: `review/ask-user-questions.json`
- Unsupported / inferred / blocked / contradicted / not_accessible counts: `0 / 0 / 0 / 0 / 0`

## Not Verified

- `{item}`
