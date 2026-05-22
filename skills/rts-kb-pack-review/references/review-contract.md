# Source-Backed KB Pack Review Contract

Review output is a gate, not a rewrite. Default to review-only.

Required checks:

- required directory and files
- `metadata.yaml` source bundle and scope
- source inventory exists and is traceable
- `claims.jsonl` exists and is parseable
- stable object ids and object types
- rule/lookup/helper required fields
- `claim_refs` resolve
- KB truth uses only allowed claim statuses: `supported`, `user_confirmed`, `runtime_observed`
- disallowed claim statuses stay in review/warning material: `blocked`, `unsupported`, `inferred`, `contradicted`, `not_accessible`
- evidence refs resolve
- review refs resolve
- source anchors resolve when company source is available
- workflow closure from gRPC inbound to Solace outbound
- field bindings from upstream XML/FpML to downstream XML/SCBML
- dependency closure
- fallback/default/error path coverage
- Java/Camel/XSLT/DB/Excel/enum evidence coverage
- runtime config captured as evidence or explicit gap
- tests captured as evidence or explicit gap
- unresolved ambiguity and conflict handling
- unsupported claim
- anchor laundering
- coverage gap
- contradiction
- runtime config gap
- negative claim hallucination
- completion report

Allowed readiness values:

```text
not_ready_blocking_questions
not_ready_missing_source_inventory
not_ready_missing_claims
not_ready_contract_errors
ready_for_kb_draft_mvp_completion
```

Do not claim production signoff, production snapshot readiness, or production runtime projection readiness.

## Finding Types

Use structured findings for:

```text
unsupported_claim
anchor_laundering
coverage_gap
contradiction
runtime_config_gap
negative_claim_hallucination
missing_source_inventory
missing_claims
claim_status_gate_violation
missing_source_anchor
test_gap
requires_user_confirmation
out_of_scope
```

Every blocking finding should either reference affected objects/claims or explain why the missing evidence prevents object references.
