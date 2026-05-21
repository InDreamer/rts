# KB Pack Review Contract

Review output is a gate, not a rewrite. Default to review-only.

Required checks:

- required directory and files
- `metadata.yaml` source bundle and scope
- stable object ids and object types
- rule/lookup/helper required fields
- evidence refs resolve
- review refs resolve
- workflow closure from gRPC inbound to Solace outbound
- field bindings from upstream XML/FpML to downstream XML/SCBML
- dependency closure
- fallback/default/error path coverage
- Java/Camel/XSLT/DB/Excel/enum evidence coverage
- tests captured as evidence or explicit gap
- unresolved ambiguity and conflict handling
- snapshot skeleton readiness

Allowed readiness values:

```text
not_ready_blocking_questions
ready_for_snapshot_skeleton
ready_for_demo_runtime_skeleton
not_ready_missing_source_profile
not_ready_contract_errors
```

Do not claim production signoff or production runtime projection readiness.
