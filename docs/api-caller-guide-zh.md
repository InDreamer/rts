<!-- docmeta
role: leaf
layer: 2
parent: docs/INDEX.md
children: []
summary: API caller guide for RTS query, ask, object, dependency, and trace responses
read_when:
  - 需要调用 RTS REST API 或理解响应字段
  - 需要确认 caller_id、API key、scope、refusal reason 或 warning 的语义
  - 需要给业务集成方或查询调用者说明 RTS service 用法
skip_when:
  - 只需要本地启动或维护 RTS Java service
  - 只需要修改 KB-to-index projection contract
source_of_truth:
  - src/main/java/com/rts/api
  - src/main/java/com/rts/model
  - docs/java-service-runbook-zh.md
-->

# RTS API 调用方指南

本文面向业务集成方和查询调用者，说明如何向 RTS 服务发起查询、理解返回结果、处理拒答，以及可用的 sample scope。

## 1. 身份与认证

每个请求都需要携带 `caller_id` 和 API key。`caller_id` 放在请求 body，API key 放在 header。

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

```text
caller_id: tester
api_key: tester-key
```

## 2. Scope 说明

Scope 由四个维度组成，必须全部提供，且必须属于当前 active release：

| 字段 | 含义 | sample 可用值 |
|---|---|---|
| `channel` | 渠道 | `tradition` |
| `product` | 产品线 | `stella` |
| `pack` | 规则包 | 见下表 |
| `domain` | 领域 | `cutoff-fixing` / `core` |

当前默认 active release 是：

```text
rel-2026-05-06-photo-fxd-ndf-cutoff
```

默认可用 scope：

| pack | domain | 说明 |
|---|---|---|
| `fxd-ndf-cutoff-fixing` | `cutoff-fixing` | NDF fixing cutoff 规则，demo-signoff/photo reconstructed golden |

回滚 sample release 是 `rel-2026-05-06`。只有切换 active release 后，以下 scope 才可查询：

| pack | domain | 说明 |
|---|---|---|
| `payments` | `core` | 支付金额规则 |
| `fees` | `core` | 手续费规则 |
| `accounts` | `core` | 账户状态规则 |
| `fx` | `core` | 汇率类型规则 |
| `compliance` | `core` | 筛查标志规则 |

scope 不属于当前 active release 时，服务会返回 `scope_unclear`。

## 3. 主要入口

下面所有示例默认使用当前 photo active release 的 scope：

```json
{
  "channel": "tradition",
  "product": "stella",
  "pack": "fxd-ndf-cutoff-fixing",
  "domain": "cutoff-fixing"
}
```

### 3.1 确定性查询 `/api/v1/query`

用自然语言提问，返回基于 L2 truth 的结构化答案。不调用 LLM，结果来自 projection store。

最小请求 body：

```json
{
  "query": "fixing time 怎么生成",
  "caller_id": "tester",
  "scope_hint": {
    "channel": "tradition",
    "product": "stella",
    "pack": "fxd-ndf-cutoff-fixing",
    "domain": "cutoff-fixing"
  },
  "output_mode": "default",
  "use_llm": false
}
```

关键字段：

| 字段 | 必填 | 说明 |
|---|---|---|
| `query` | 是 | 自然语言问题 |
| `caller_id` | 是 | 调用方 ID |
| `scope_hint` | 建议 | channel/product/pack/domain；缺省则可能触发 `scope_unclear` |
| `output_mode` | 否 | `default` |
| `use_llm` | 否 | 当前 `/query` 仍按确定性查询路径处理；需要 controlled LLM harness 时调用 `/ask` |

### 3.2 LLM 增强查询 `/api/v1/ask`

使用受控 LLM harness 回答，工具链仍受 scope、permission、grounding 和 trace 约束。LLM 未启用时退化为 grounded tool output。

请求 body 与 `/query` 类似，额外支持 `max_tool_calls`。LLM 仍只能通过 allowlisted RTS tools 读取 projection。

### 3.3 候选搜索 `/api/v1/find`

按关键词搜索对象，返回候选列表。适合在发起精确查询前探索哪些对象存在。

常用请求字段：`query`、`caller_id`、`scope`、`object_types`、`anchors`、`limit`、`output_mode`。

### 3.4 读取对象 L2 内容 `/api/v1/objects/content`

直接读取某个 object 的 L2 内容。

常用请求字段：`uri`、`purpose`、`caller_id`。示例 URI：`rts://tradition/stella/fxd-ndf-cutoff-fixing/photo-reconstructed/rules/rule_fxd_ndf_fixing_time`。

### 3.5 依赖遍历 `/api/v1/objects/dependencies`

查询某个 object 的依赖关系。

常用请求字段：`uri`、`direction`、`depth`、`purpose`、`caller_id`。`direction` 可选值：`forward`、`reverse`、`both`。

### 3.6 读取 Trace `/api/v1/traces/{traceId}`

查看某次查询的执行详情，包括用了哪些 L2 对象、grounding 情况、耗时。

`trace_id` 从 `/query` 或 `/ask` 的响应中获取。

完整可复制 curl 命令见 sample runtime projection smoke test 文档。

## 4. 响应结构

### 正常答案

```json
{
  "answer_type": "answer",
  "scope": {
    "channel": "tradition",
    "product": "stella",
    "pack": "fxd-ndf-cutoff-fixing",
    "domain": "cutoff-fixing"
  },
  "release_id": "rel-2026-05-06-photo-fxd-ndf-cutoff",
  "facts": [
    {
      "text": "...",
      "uri": "rts://...",
      "release_id": "rel-2026-05-06-photo-fxd-ndf-cutoff",
      "source": "l2:sha256:..."
    }
  ],
  "cited_objects": ["rts://..."],
  "dependencies": [],
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
| `warnings` | 非阻塞警告，如 demo-signoff/photo-reconstructed 标注 |

### 拒答

```json
{
  "answer_type": "refusal",
  "refusal": {
    "reason": "scope_unclear",
    "what_is_missing": "...",
    "what_user_can_provide": [],
    "partial_candidates_exist": false
  },
  "trace_id": "..."
}
```

常见拒答原因：

| `reason` | 含义 | 处理建议 |
|---|---|---|
| `scope_unclear` | scope 未提供或不在 active release | 检查 scope_hint 四个字段是否正确，或切换 active release |
| `unauthorized_scope` | caller 无权访问该 scope | 确认 caller_id 和 api_key 正确，联系管理员确认权限 |
| `object_not_found` | 在该 scope 下未找到匹配对象 | 用 `/find` 先搜索确认对象存在 |
| `only_similarity_no_structured_match` | 只有相似候选，无精确匹配 | 换用更精确的查询词或用 `/find` 查候选 |
| `active_release_missing` | 服务未加载任何 release | 联系运维，检查 store 是否配置 |

## 5. 旧 sample release

如果需要查询 payments、fees、accounts、fx 或 compliance，先按运行手册切换 active release 到：

```text
rel-2026-05-06
```

切换后才可以使用这类 scope：

```json
{
  "channel": "tradition",
  "product": "stella",
  "pack": "payments",
  "domain": "core"
}
```

## 6. 注意事项

- 所有请求 body 字段使用 `snake_case`，不要用 `camelCase`。
- `scope_hint` 中的 scope 必须属于当前 active release，否则返回 `scope_unclear`。
- `warnings` 或 governance summary 中出现 `photo-reconstructed`、`demo-signoff`、`open_questions` 或 production gate 标注时，返回内容不能作为 production signoff truth 使用。
- `facts[].source` 以 `l2:sha256:...` 格式标识来源，可通过 trace 核查引用完整性。
- 启用 LLM 时必须显式配置 base URL、API key 和模型；LLM 请求会把受控上下文发送到配置的模型端点，生产或敏感环境需要先确认数据外发边界。
