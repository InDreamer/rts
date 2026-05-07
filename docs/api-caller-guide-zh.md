# RTS API 调用方指南

本文面向**业务集成方和查询调用者**，说明如何向 RTS 服务发起查询、理解返回结果、处理拒答，以及可用的 scope 范围。

---

## 1. 身份与认证

每个请求都需要携带 `caller_id` 和 API Key。`caller_id` 放在请求 body，API Key 放在 header。

```bash
-H 'X-RTS-API-Key: <your-api-key>'
```

body 中：

```json
{
  "caller_id": "your-caller-id"
}
```

sample 环境内置 caller：

```
caller_id: tester
api_key:   tester-key
```

---

## 2. Scope 说明

Scope 由四个维度组成，必须全部提供，且必须属于当前 active release：

| 字段 | 含义 | sample 可用值 |
|---|---|---|
| `channel` | 渠道 | `tradition` |
| `product` | 产品线 | `stella` |
| `pack` | 规则包 | 见下表 |
| `domain` | 领域 | `core` / `cutoff-fixing` |

**当前默认 active release (`rel-2026-05-06`) 可用 scope：**

| pack | domain | 说明 |
|---|---|---|
| `payments` | `core` | 支付金额规则 |
| `fees` | `core` | 手续费规则 |
| `accounts` | `core` | 账户状态规则 |
| `fx` | `core` | 汇率类型规则 |
| `compliance` | `core` | 筛查标志规则 |

**photo reconstructed draft release (`rel-2026-05-06-photo-fxd-ndf-cutoff`) 可用 scope：**

| pack | domain | 说明 |
|---|---|---|
| `fxd-ndf-cutoff-fixing` | `cutoff-fixing` | NDF fixing cutoff 规则（draft，未 signoff） |

> 注意：scope 必须属于当前 active release，否则返回 `scope_unclear` 拒答。查询前可先用 `/api/v1/find` 搜索确认对象存在。

---

## 3. 主要入口

### 3.1 确定性查询 `/api/v1/query`

用自然语言提问，返回基于 L2 truth 的结构化答案。**不调用 LLM**，结果完全来自 projection store。

请求：

```bash
curl -sS -X POST http://localhost:8080/api/v1/query \
  -H 'Content-Type: application/json' \
  -H 'X-RTS-API-Key: tester-key' \
  -d '{
    "query": "payment.amount 怎么生成",
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

关键字段：

| 字段 | 必填 | 说明 |
|---|---|---|
| `query` | 是 | 自然语言问题 |
| `caller_id` | 是 | 调用方 ID |
| `scope_hint` | 建议 | channel/product/pack/domain，缺省则可能触发 `scope_unclear` |
| `output_mode` | 否 | `default`（默认） |
| `use_llm` | 否 | `false` 为确定性模式（推荐），`true` 需服务端启用 LLM |

### 3.2 LLM 增强查询 `/api/v1/ask`

使用受控 LLM harness 回答，工具链仍受 scope/permission/grounding 约束。LLM 未启用时退化为工具链输出。

```bash
curl -sS -X POST http://localhost:8080/api/v1/ask \
  -H 'Content-Type: application/json' \
  -H 'X-RTS-API-Key: tester-key' \
  -d '{
    "query": "fee.amount 怎么生成",
    "caller_id": "tester",
    "scope_hint": {
      "channel": "tradition",
      "product": "stella",
      "pack": "fees",
      "domain": "core"
    },
    "output_mode": "default",
    "max_tool_calls": 8
  }' | jq
```

### 3.3 候选搜索 `/api/v1/find`

按关键词搜索对象，返回候选列表。适合在发起精确查询前探索哪些对象存在。

```bash
curl -sS -X POST http://localhost:8080/api/v1/find \
  -H 'Content-Type: application/json' \
  -H 'X-RTS-API-Key: tester-key' \
  -d '{
    "query": "rate type",
    "caller_id": "tester",
    "scope": {
      "channel": "tradition",
      "product": "stella",
      "pack": "fx",
      "domain": "core"
    },
    "object_types": ["rule", "helper"],
    "anchors": [],
    "limit": 5,
    "output_mode": "default"
  }' | jq
```

### 3.4 读取对象 L2 内容 `/api/v1/objects/content`

直接读取某个 object 的完整 L2 内容（规则/帮助/查找表原文）。

```bash
curl -sS -X POST http://localhost:8080/api/v1/objects/content \
  -H 'Content-Type: application/json' \
  -H 'X-RTS-API-Key: tester-key' \
  -d '{
    "uri": "rts://tradition/stella/accounts/day1/rules/rule_account_status",
    "purpose": "answer",
    "caller_id": "tester"
  }' | jq
```

### 3.5 依赖遍历 `/api/v1/objects/dependencies`

查询某个 object 的依赖关系（向前/向后/双向）。

```bash
curl -sS -X POST http://localhost:8080/api/v1/objects/dependencies \
  -H 'Content-Type: application/json' \
  -H 'X-RTS-API-Key: tester-key' \
  -d '{
    "uri": "rts://tradition/stella/fees/day1/rules/rule_fee_amount",
    "direction": "forward",
    "depth": 1,
    "purpose": "explain",
    "caller_id": "tester"
  }' | jq
```

`direction` 可选值：`forward`、`reverse`、`both`。

### 3.6 读取 Trace `/api/v1/traces/{traceId}`

查看某次查询的执行详情，包括用了哪些 L2 对象、grounding 情况、耗时。

```bash
curl -sS http://localhost:8080/api/v1/traces/<trace_id> \
  -H 'X-RTS-Caller-Id: tester' \
  -H 'X-RTS-API-Key: tester-key' | jq
```

`trace_id` 从 `/query` 或 `/ask` 的响应中获取。

---

## 4. 响应结构

### 正常答案

```json
{
  "answer_type": "answer",
  "scope": { "channel": "...", "product": "...", "pack": "...", "domain": "..." },
  "release_id": "rel-2026-05-06",
  "facts": [
    {
      "text": "...",
      "uri": "rts://...",
      "release_id": "rel-2026-05-06",
      "source": "l2:sha256:..."
    }
  ],
  "cited_objects": ["rts://..."],
  "dependencies": [...],
  "trace_id": "...",
  "warnings": []
}
```

| 字段 | 说明 |
|---|---|
| `answer_type` | `answer` / `refusal` / `partial` / `clarification` |
| `facts` | 来自 L2 truth 的结构化事实，每条含来源 hash |
| `cited_objects` | 本次答案引用的 object URI 列表 |
| `dependencies` | 关联依赖边 |
| `trace_id` | 可用于追踪本次查询执行详情 |
| `warnings` | 非阻塞警告，如 draft/photo-reconstructed 标注 |

### 拒答

```json
{
  "answer_type": "refusal",
  "refusal": {
    "reason": "scope_unclear",
    "what_is_missing": "...",
    "what_user_can_provide": [...],
    "partial_candidates_exist": false
  },
  "trace_id": "..."
}
```

**常见拒答原因：**

| `reason` | 含义 | 处理建议 |
|---|---|---|
| `scope_unclear` | scope 未提供或不在 active release | 检查 scope_hint 四个字段是否正确 |
| `unauthorized_scope` | caller 无权访问该 scope | 确认 caller_id 和 api_key 正确，联系管理员确认权限 |
| `object_not_found` | 在该 scope 下未找到匹配对象 | 用 `/find` 先搜索确认对象存在 |
| `only_similarity_no_structured_match` | 只有相似候选，无精确匹配 | 换用更精确的查询词或用 `/find` 查候选 |
| `active_release_missing` | 服务未加载任何 release | 联系运维，检查 store 是否配置 |

---

## 5. 注意事项

- 所有请求 body 字段使用 `snake_case`，不要用 `camelCase`（否则返回 400）。
- `scope_hint` 中的 scope 必须属于当前 active release，否则返回 `scope_unclear`。
- `warnings` 中出现 `draft` / `photo-reconstructed` 标注时，返回内容**不能作为 signoff truth 使用**。
- `facts[].source` 以 `l2:sha256:...` 格式标识来源，可通过 trace 核查引用完整性。
