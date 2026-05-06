# Day1 Sample Pack Test Commands

## Start Service

Run this from the repository root:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) \
RTS_STORE_ROOT=/Users/tuziliji/projects/rts/sample-projection/runtime-store \
./gradlew bootRun
```

Keep this terminal running, then open another terminal for the commands below.

## Query Pack: Payments

```bash
curl -sS -X POST http://localhost:8080/api/v1/query \
  -H 'Content-Type: application/json' \
  -H 'X-RTS-API-Key: tester-key' \
  -d '{"query":"payment.amount 怎么生成","caller_id":"tester","scope_hint":{"channel":"tradition","product":"stella","pack":"payments","domain":"core"},"output_mode":"default","use_llm":false}'
```

## Query Pack: Fees

```bash
curl -sS -X POST http://localhost:8080/api/v1/query \
  -H 'Content-Type: application/json' \
  -H 'X-RTS-API-Key: tester-key' \
  -d '{"query":"fee.amount 怎么生成","caller_id":"tester","scope_hint":{"channel":"tradition","product":"stella","pack":"fees","domain":"core"},"output_mode":"default","use_llm":false}'
```

## Query Pack: Accounts

```bash
curl -sS -X POST http://localhost:8080/api/v1/query \
  -H 'Content-Type: application/json' \
  -H 'X-RTS-API-Key: tester-key' \
  -d '{"query":"account.status 怎么生成","caller_id":"tester","scope_hint":{"channel":"tradition","product":"stella","pack":"accounts","domain":"core"},"output_mode":"default","use_llm":false}'
```

## Query Pack: FX

```bash
curl -sS -X POST http://localhost:8080/api/v1/query \
  -H 'Content-Type: application/json' \
  -H 'X-RTS-API-Key: tester-key' \
  -d '{"query":"fx.rate_type 怎么生成","caller_id":"tester","scope_hint":{"channel":"tradition","product":"stella","pack":"fx","domain":"core"},"output_mode":"default","use_llm":false}'
```

## Query Pack: Compliance

```bash
curl -sS -X POST http://localhost:8080/api/v1/query \
  -H 'Content-Type: application/json' \
  -H 'X-RTS-API-Key: tester-key' \
  -d '{"query":"screening flag","caller_id":"tester","scope_hint":{"channel":"tradition","product":"stella","pack":"compliance","domain":"core"},"output_mode":"default","use_llm":false}'
```

## Dependency Test

```bash
curl -sS -X POST http://localhost:8080/api/v1/objects/dependencies \
  -H 'Content-Type: application/json' \
  -H 'X-RTS-API-Key: tester-key' \
  -d '{"uri":"rts://tradition/stella/fees/day1/rules/rule_fee_amount","direction":"forward","depth":1,"purpose":"explain","caller_id":"tester"}'
```

## Direct L2 Read Test

```bash
curl -sS -X POST http://localhost:8080/api/v1/objects/content \
  -H 'Content-Type: application/json' \
  -H 'X-RTS-API-Key: tester-key' \
  -d '{"uri":"rts://tradition/stella/accounts/day1/rules/rule_account_status","purpose":"answer","caller_id":"tester"}'
```

## Find Test

```bash
curl -sS -X POST http://localhost:8080/api/v1/find \
  -H 'Content-Type: application/json' \
  -H 'X-RTS-API-Key: tester-key' \
  -d '{"query":"rate type","caller_id":"tester","scope":{"channel":"tradition","product":"stella","pack":"fx","domain":"core"},"object_types":["rule","helper"],"anchors":[],"limit":5,"output_mode":"default"}'
```

## Refusal Test: Unauthorized Product

This should return `answer_type: refusal` with `unauthorized_scope`.

```bash
curl -sS -X POST http://localhost:8080/api/v1/query \
  -H 'Content-Type: application/json' \
  -H 'X-RTS-API-Key: tester-key' \
  -d '{"query":"payment.amount 怎么生成","caller_id":"tester","scope_hint":{"channel":"tradition","product":"aurora","pack":"payments","domain":"core"},"output_mode":"default","use_llm":false}'
```

## Refusal Test: Missing Scope

This should return `answer_type: refusal` with `scope_unclear`.

```bash
curl -sS -X POST http://localhost:8080/api/v1/query \
  -H 'Content-Type: application/json' \
  -H 'X-RTS-API-Key: tester-key' \
  -d '{"query":"payment.amount 怎么生成","caller_id":"tester","scope_hint":null,"output_mode":"default","use_llm":false}'
```

## Ask Harness Test

This uses the controlled LLM harness. With default local config, LLM is disabled and the harness still returns grounded tool output.

```bash
curl -sS -X POST http://localhost:8080/api/v1/ask \
  -H 'Content-Type: application/json' \
  -H 'X-RTS-API-Key: tester-key' \
  -d '{"query":"fee.amount 怎么生成","caller_id":"tester","scope_hint":{"channel":"tradition","product":"stella","pack":"fees","domain":"core"},"output_mode":"default","max_tool_calls":8}'
```

## Trace Read Test

Replace `<trace_id>` with a trace id returned by a previous query.

```bash
curl -sS -X GET http://localhost:8080/api/v1/traces/<trace_id> \
  -H 'X-RTS-Caller-Id: tester' \
  -H 'X-RTS-API-Key: tester-key'
```
