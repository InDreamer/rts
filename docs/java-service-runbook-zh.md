# RTS Java Service Runbook

本文说明如何在本地运行 RTS Java 服务、执行校验、调用 API/MCP，并处理常见启动问题。

## 1. 环境需求

- JDK 17。
- macOS/Linux shell。
- Maven 3.8+。
- 可选工具：`curl`、`jq`、`lsof`。
- 网络：首次构建需要访问 Maven Central 下载依赖。

检查 Java：

```bash
java -version
```

macOS 推荐显式指定 JDK 17：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
```

固定 JDK 路径示例：

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home
```

## 2. 基本校验

从仓库根目录执行：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home mvn verify
```

更严格的全量重跑：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home mvn clean verify
```

预期结果：

```text
BUILD SUCCESS
```

`verify` 会执行普通测试和 golden 测试。

## 3. Runtime Store

RTS 运行时读取 runtime projection store，不直接读取 KB/candidate pack 作为 truth。

默认配置：

```bash
RTS_STORE_ROOT=runtime-store
```

仓库提供 sample store：

```bash
sample-projection/runtime-store
```

runtime projection 已切到多视图布局：

```text
runtime-store/
  active-release.json
  releases/<release-id>/
    release-manifest.json
    scopes.jsonl
    object-manifest.jsonl
    caller-profiles.jsonl
    l2/{rules,lookups,helpers}/
    navigation/{object-cards,l0-l1-views,aliases,confusables}.jsonl
    governance/governance-access-refs.jsonl
    governance/{evidence-summaries,review-summaries,report-summaries}/
    dependencies/{dependency-edges,field-bindings}.jsonl
    index-artifacts/{lucene,opensearch-docs.jsonl}
  traces/
```

当前 sample 默认 active release 是：

```text
rel-2026-05-06-photo-fxd-ndf-cutoff
```

photo reconstructed golden scope：

```text
tradition / stella / fxd-ndf-cutoff-fixing / cutoff-fixing
```

回滚 sample release 是：

```text
rel-2026-05-06
```

其中可验证的 sample scope 包括：

```text
tradition / stella / payments / core
tradition / stella / fees / core
tradition / stella / accounts / core
tradition / stella / fx / core
tradition / stella / compliance / core
```

photo pack 是 demo-signoff/photo reconstructed golden release，用于验证多视图 runtime projection；其中仍保留 production gate 和 open questions，不能当 production signoff truth。

注意：只有 active release 中存在的 scope 才能查询。scope 不在 active release 时，服务会返回 `scope_unclear`。

## 4. 启动服务

默认使用 Spring Boot 端口 `8080`：

```bash
RTS_STORE_ROOT=/Users/tuziliji/projects/rts/sample-projection/runtime-store \
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home \
mvn spring-boot:run
```

Keep this terminal running, then open another terminal for curl commands.

如果 `8080` 被占用，再指定其他端口：

```bash
RTS_STORE_ROOT=/Users/tuziliji/projects/rts/sample-projection/runtime-store \
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home \
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=19091
```

如果不确定哪些端口可用，也可以使用随机空闲端口：

```bash
RTS_STORE_ROOT=/Users/tuziliji/projects/rts/sample-projection/runtime-store \
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home \
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=0
```

此时从启动日志读取实际端口，例如：

```text
Tomcat started on port <RANDOM_PORT>
```

如果端口被占用，可以先查占用：

```bash
lsof -nP -iTCP:8080 -sTCP:LISTEN
```

## 5. 快速验证

### MCP 工具清单

```bash
curl -s http://localhost:8080/mcp/tools | jq
```

预期：返回 JSON，包含 `rts_ask`、`rts_find_objects`、`rts_read_object_l2`、`rts_trace_report` 等工具。

### 查询 photo pack

```bash
curl -sS -X POST http://localhost:8080/api/v1/query \
  -H 'Content-Type: application/json' \
  -H 'X-RTS-API-Key: tester-key' \
  -d '{
    "query": "fixing time 怎么生成？",
    "caller_id": "tester",
    "scope_hint": {
      "channel": "tradition",
      "product": "stella",
      "pack": "fxd-ndf-cutoff-fixing",
      "domain": "cutoff-fixing"
    },
    "output_mode": "default",
    "use_llm": false
  }' | jq
```

预期：

- `answer_type` 为 `answer`。
- 返回 `facts`、`cited_objects`、`dependencies`、`trace_id`。
- fact source 应包含 `l2:sha256:...`。
- `warnings` 应保留 photo reconstructed / demo signoff 风险提示。

### 查询回滚 sample

如果需要查询 payments 等普通 sample scope，先按第 9 节切换 active release 到 `rel-2026-05-06`，再查询对应 scope。

### scope 缺失拒答

```bash
curl -sS -X POST http://localhost:8080/api/v1/query \
  -H 'Content-Type: application/json' \
  -H 'X-RTS-API-Key: tester-key' \
  -d '{
    "query": "payment amount 怎么生成？",
    "caller_id": "tester",
    "output_mode": "default",
    "use_llm": false
  }' | jq
```

预期：

- `answer_type` 为 `refusal`。
- `refusal.reason` 为 `scope_unclear`。

### 读取 trace

把 `<TRACE_ID>` 替换成查询返回的 `trace_id`：

```bash
curl -sS http://localhost:8080/api/v1/traces/<TRACE_ID> \
  -H 'X-RTS-Caller-Id: tester' \
  -H 'X-RTS-API-Key: tester-key' | jq
```

预期：返回 `l2_read_uris`、`grounding_map`、`budget_usage`、`status`。

## 6. 常用入口

### REST

- `POST /api/v1/query`：确定性查询。
- `POST /api/v1/ask`：受控 LLM harness 入口；默认本地 LLM disabled，仍会走受控工具输出 grounded answer。
- `POST /api/v1/find`：候选搜索。
- `POST /api/v1/objects/get`：读取 object card 和 dependency summary。
- `POST /api/v1/objects/content`：读取 L2 content。
- `POST /api/v1/objects/dependencies`：依赖遍历。
- `GET /api/v1/traces/{traceId}`：读取 trace。
- `POST /api/v1/message/*`：raw message 到 target candidate 支持工具。
- `POST /api/v1/analyze/*`：impact、test plan、grounding、readiness、compare、conflict。
- `POST /api/v1/feedback/*`：feedback 和 non-truth memory。
- `POST /api/v1/governance/*`：治理辅助和人工 decision 记录。
- `POST /api/v1/pipeline/release-readiness`：pipeline 可读 readiness report。
- `POST /api/v1/reports/trace`：trace report。

### MCP

- `GET /mcp/tools`：工具清单。
- `POST /mcp/tools/rts_find_objects`
- `POST /mcp/tools/rts_read_object_l2`
- `POST /mcp/tools/rts_ask`
- `POST /mcp/tools/rts_trace_report`
- 其他工具以 `/mcp/tools/<tool_name>` 暴露。

## 7. 权限和示例 caller

sample store 内置 caller：

```text
caller_id: tester
api key: tester-key
```

请求 header：

```bash
-H 'X-RTS-API-Key: tester-key'
```

请求 body 使用 snake_case 字段，例如：

```json
{
  "caller_id": "tester",
  "scope_hint": {},
  "output_mode": "default"
}
```

不要使用 camelCase，例如 `callerId`、`scopeHint`，否则会返回 400。

## 8. LLM 配置

默认：

```text
RTS_LLM_ENABLED=false
```

此时 `/ask` 使用 disabled LLM client，通过受控工具链返回 grounded answer，适合本地验证。

如需启用 OpenAI-compatible endpoint：

```bash
RTS_LLM_ENABLED=true \
RTS_LLM_BASE_URL=https://example.com \
RTS_LLM_API_KEY=... \
RTS_LLM_MODEL=... \
RTS_LLM_WIRE_API=responses \
RTS_STORE_ROOT=/Users/tuziliji/projects/rts/sample-projection/runtime-store \
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home \
mvn spring-boot:run
```

LLM 仍只能通过 allowlisted tools 读取 RTS truth，不能绕过 scope、permission、release、L2、trace 和 grounding validation。

## 9. 切换 sample active release

默认 active release 是 photo pack：

```text
rel-2026-05-06-photo-fxd-ndf-cutoff
```

如果要切回普通 sample，先停止服务，然后写入 active pointer：

```bash
cat > /Users/tuziliji/projects/rts/sample-projection/runtime-store/active-release.json <<'JSON'
{
  "active_release_id": "rel-2026-05-06",
  "rollback_target_release_id": null,
  "updated_at": "2026-05-06T00:00:00Z",
  "updated_by": "manual-validation"
}
JSON
```

再重启服务并查询：

```bash
curl -sS -X POST http://localhost:8080/api/v1/query \
  -H 'Content-Type: application/json' \
  -H 'X-RTS-API-Key: tester-key' \
  -d '{
    "query": "payment amount 怎么生成？",
    "caller_id": "tester",
    "scope_hint": {
      "channel": "tradition",
      "product": "stella",
      "pack": "payments",
      "domain": "core"
    },
    "output_mode": "default",
    "use_llm": false
  }' | jq
```

切回默认 photo pack：

```bash
cat > /Users/tuziliji/projects/rts/sample-projection/runtime-store/active-release.json <<'JSON'
{
  "active_release_id": "rel-2026-05-06-photo-fxd-ndf-cutoff",
  "rollback_target_release_id": "rel-2026-05-06",
  "updated_at": "2026-05-07T00:00:00Z",
  "updated_by": "sample"
}
JSON
```

## 10. 常见问题

### curl 返回 Sub2API HTML

说明你打到的端口不是 RTS，而是当前机器上其他服务。先查端口：

```bash
lsof -nP -iTCP:8080 -sTCP:LISTEN
```

如果确认 `8080` 被其他服务占用，再用 `--server.port=<PORT>` 或 `--server.port=0` 启动 RTS。

### Port already in use

换端口或用随机端口：

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=0
```

### 400 Unrecognized field

请求 JSON 字段必须用 snake_case：

```text
caller_id
scope_hint
output_mode
use_llm
max_tool_calls
```

### scope_unclear: Scope is not active in release

查询 scope 不属于当前 active release。检查：

```bash
cat sample-projection/runtime-store/active-release.json
cat sample-projection/runtime-store/releases/<release_id>/scopes.jsonl
```

### list scopes 返回空

可能是当前 caller profile 没有 `scope_tools` 权限。普通 query 仍可能可用。用 `/api/v1/query` 直接验证业务 scope。

### 依赖下载失败

确认网络能访问 Maven Central，或配置企业 Maven mirror。

## 11. 提交前检查

提交代码前至少执行：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home mvn verify
git diff --check
git status --short
```
