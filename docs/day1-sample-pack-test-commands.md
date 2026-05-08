<!-- docmeta
role: leaf
layer: 2
parent: docs/INDEX.md
children: []
summary: manual curl commands for current sample runtime projection smoke testing
read_when:
  - 需要快速手工验证当前 sample release 查询、依赖、L2 和 LLM harness
  - 需要复制 sample curl 命令进行本地 smoke test
skip_when:
  - 需要完整运行手册或配置说明
  - 需要面向业务集成方的 API 字段解释
source_of_truth:
  - sample-projection/runtime-store
  - docs/java-service-runbook-zh.md
  - docs/api-caller-guide-zh.md
-->

# Sample Runtime Projection Test Commands

These commands assume the current default active release:

```text
rel-2026-05-06-photo-fxd-ndf-cutoff
```

The default query scope is:

```text
tradition / stella / fxd-ndf-cutoff-fixing / cutoff-fixing
```

Payments/fees/accounts/fx/compliance commands only work after switching the active release to `rel-2026-05-06`.

## Start Service

Run this from the repository root:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) \
RTS_STORE_ROOT="$PWD/sample-projection/runtime-store" \
mvn spring-boot:run
```

Keep this terminal running, then open another terminal for the commands below.

## Query Default Photo Pack

```bash
curl -sS -X POST http://localhost:8080/api/v1/query \
  -H 'Content-Type: application/json' \
  -H 'X-RTS-API-Key: tester-key' \
  -d '{"query":"fixing time 怎么生成","caller_id":"tester","scope_hint":{"channel":"tradition","product":"stella","pack":"fxd-ndf-cutoff-fixing","domain":"cutoff-fixing"},"output_mode":"default","use_llm":false}'
```

Expected: `answer_type` is `answer`, with `facts`, `cited_objects`, `dependencies`, `trace_id`, and photo/demo warnings.

## Dependency Test

```bash
curl -sS -X POST http://localhost:8080/api/v1/objects/dependencies \
  -H 'Content-Type: application/json' \
  -H 'X-RTS-API-Key: tester-key' \
  -d '{"uri":"rts://tradition/stella/fxd-ndf-cutoff-fixing/photo-reconstructed/rules/rule_fxd_ndf_fixing_time","direction":"forward","depth":1,"purpose":"explain","caller_id":"tester"}'
```

## Direct L2 Read Test

```bash
curl -sS -X POST http://localhost:8080/api/v1/objects/content \
  -H 'Content-Type: application/json' \
  -H 'X-RTS-API-Key: tester-key' \
  -d '{"uri":"rts://tradition/stella/fxd-ndf-cutoff-fixing/photo-reconstructed/rules/rule_fxd_ndf_fixing_time","purpose":"answer","caller_id":"tester"}'
```

## Find Test

```bash
curl -sS -X POST http://localhost:8080/api/v1/find \
  -H 'Content-Type: application/json' \
  -H 'X-RTS-API-Key: tester-key' \
  -d '{"query":"cutoff lookup fixing time","caller_id":"tester","scope":{"channel":"tradition","product":"stella","pack":"fxd-ndf-cutoff-fixing","domain":"cutoff-fixing"},"object_types":["rule","lookup","helper"],"anchors":[],"limit":5,"output_mode":"default"}'
```

## Refusal Test: Inactive Scope

This should return `answer_type: refusal` with `scope_unclear` while the default photo release is active.

```bash
curl -sS -X POST http://localhost:8080/api/v1/query \
  -H 'Content-Type: application/json' \
  -H 'X-RTS-API-Key: tester-key' \
  -d '{"query":"payment.amount 怎么生成","caller_id":"tester","scope_hint":{"channel":"tradition","product":"stella","pack":"payments","domain":"core"},"output_mode":"default","use_llm":false}'
```

## Refusal Test: Unauthorized Product

This should return `answer_type: refusal` with `unauthorized_scope`.

```bash
curl -sS -X POST http://localhost:8080/api/v1/query \
  -H 'Content-Type: application/json' \
  -H 'X-RTS-API-Key: tester-key' \
  -d '{"query":"fixing time 怎么生成","caller_id":"tester","scope_hint":{"channel":"tradition","product":"aurora","pack":"fxd-ndf-cutoff-fixing","domain":"cutoff-fixing"},"output_mode":"default","use_llm":false}'
```

## Refusal Test: Missing Scope

This should return `answer_type: refusal` with `scope_unclear`.

```bash
curl -sS -X POST http://localhost:8080/api/v1/query \
  -H 'Content-Type: application/json' \
  -H 'X-RTS-API-Key: tester-key' \
  -d '{"query":"fixing time 怎么生成","caller_id":"tester","scope_hint":null,"output_mode":"default","use_llm":false}'
```

## Ask Harness Test

This uses the controlled LLM harness. With default local config, LLM is disabled and the harness still returns grounded tool output.

```bash
curl -sS -X POST http://localhost:8080/api/v1/ask \
  -H 'Content-Type: application/json' \
  -H 'X-RTS-API-Key: tester-key' \
  -d '{"query":"fixing time 怎么生成","caller_id":"tester","scope_hint":{"channel":"tradition","product":"stella","pack":"fxd-ndf-cutoff-fixing","domain":"cutoff-fixing"},"output_mode":"default","max_tool_calls":8}'
```

## Trace Read Test

Replace `<trace_id>` with a trace id returned by a previous query.

```bash
curl -sS -X GET http://localhost:8080/api/v1/traces/<trace_id> \
  -H 'X-RTS-Caller-Id: tester' \
  -H 'X-RTS-API-Key: tester-key'
```

## Optional: Switch To Legacy Day1 Sample

Stop the service, then write the rollback active release pointer from the repository root. This is sample-only local validation; do not use direct active pointer edits as a shared or production release procedure:

```bash
cat > "$PWD/sample-projection/runtime-store/active-release.json" <<'JSON'
{
  "active_release_id": "rel-2026-05-06",
  "rollback_target_release_id": null,
  "updated_at": "2026-05-06T00:00:00Z",
  "updated_by": "manual-validation"
}
JSON
```

Restart the service. Then payments/fees/accounts/fx/compliance scopes are active, for example:

```bash
curl -sS -X POST http://localhost:8080/api/v1/query \
  -H 'Content-Type: application/json' \
  -H 'X-RTS-API-Key: tester-key' \
  -d '{"query":"payment.amount 怎么生成","caller_id":"tester","scope_hint":{"channel":"tradition","product":"stella","pack":"payments","domain":"core"},"output_mode":"default","use_llm":false}'
```

To restore the default photo pack, stop the service and write:

```bash
cat > "$PWD/sample-projection/runtime-store/active-release.json" <<'JSON'
{
  "active_release_id": "rel-2026-05-06-photo-fxd-ndf-cutoff",
  "rollback_target_release_id": "rel-2026-05-06",
  "updated_at": "2026-05-07T00:00:00Z",
  "updated_by": "sample"
}
JSON
```
