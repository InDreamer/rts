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

`/ask` 是 RTS 的 managed analysis service 入口。它使用受控 LLM harness 回答，工具链仍受 scope、permission、grounding 和 trace 约束。LLM 未启用或 provider 不可用时，`/ask` 会降级为结构化信息提供服务，而不是继续假装自己处于同等分析能力状态。

请求 body 与 `/query` 类似，额外支持 `max_tool_calls`。LLM 仍只能通过 allowlisted RTS tools 读取 projection。

`/ask` 返回统一 answer envelope。成功答案除了 `answer` 文本外，仍必须包含：

- `schema_version`：当前为 `service-answer.v2`。
- `facts`：每条事实都必须来自本次读取过的 L2 object。
- `cited_objects`：被引用的 object URI。
- `grounding_map`：claim 到 L2 URI/content hash 的映射。
- `budget_usage`：tool/L2/model/latency budget 使用情况。
- `trace_id`：可用于审计工具序列、L2 reads 和 grounding。

如果模型输出无法通过 claim-level grounding validation，服务会返回 `refusal.reason=unsupported_claim` 或 `hash_mismatch`，不会把自由文本当作事实答案。

`RTS_TOOL_ORCHESTRATOR_ENABLED=false` 时，`/ask` 会降级为 deterministic `/query` 风格的信息服务输出。显式开启 orchestrator 后，内部 `/ask` 链路会先生成受控 `AgentPlan`，记录 intent、scenario type、scope snapshot、release snapshot、tool plan 和 expected evidence。Planner 只允许决定工具计划或 clarification，不允许直接产出业务事实。

### 3.3 Scenario endpoints

复杂外部输入可以走 scenario endpoint。当前实现已经提供统一 `scenario-report.v1`、grounded candidate / information-service output、citations、grounding map、warnings 和 trace。外部输入只作为线索，不是真相。

对 PR diff、exception impact、failed message 和 test planning 这类 AI-centric 场景，managed AI 仍是目标正常产品模式；但当前 endpoint 多数仍主要编排 deterministic/candidate support services。对应 feature flag 关闭或 LLM 不可用时，服务会降级为结构化信息提供或 candidate support surface。本地验证或受控环境需要显式开启对应开关。

### 3.3.1 Automation boundary

无论是 `/ask` 还是 scenario endpoint，只要输出类型仍属于 `candidate`、`inference`、`unknown` 或 investigation path，它就不能被当作：

- release approval
- final root cause closure
- QA signoff
- pipeline allow/block gate
- unrecorded human decision

只有两类内容可以直接驱动自动化动作：

1. deterministic contract 已明确允许的 machine-readable result
2. 已记录的人类裁决 / approved truth / signed release state

这条边界对 managed mode 和 external agent tool mode 同样成立。

当前入口：

| Endpoint | 场景 | 输出边界 |
|---|---|---|
| `POST /api/v1/scenario/analyze-pr-diff` | PR diff impact analysis | impact/test candidate，不是 release approval |
| `POST /api/v1/scenario/investigate-exception` | exception/log investigation | investigation path，不是 final root cause |
| `POST /api/v1/scenario/analyze-failed-message` | failed/raw message analysis | target candidate，不是生产转换结果 |
| `POST /api/v1/scenario/plan-tests` | test planning | test suggestions，不是 QA signoff |
| `POST /api/v1/scenario/governance-review` | governance review assistant | reviewer questions / conflict candidates，不是 human decision |

请求字段：

```json
{
  "input": "diff/log/raw message text",
  "caller_id": "tester",
  "scope": {
    "channel": "tradition",
    "product": "stella",
    "pack": "fxd-ndf-cutoff-fixing",
    "domain": "cutoff-fixing"
  },
  "output_mode": "default",
  "max_objects": 5
}
```

统一 report 字段包括 `schema_version`、`status`、`scenario_type`、`input_summary`、`facts`、`inferences`、`candidates`、`unknowns`、`next_evidence_needed`、`citations`、`grounding_map`、`trace_id`、`budget_usage`、`warnings`。

### 3.4 候选搜索 `/api/v1/find`

按关键词搜索对象，返回候选列表。适合在发起精确查询前探索哪些对象存在。

常用请求字段：`query`、`caller_id`、`scope`、`object_types`、`anchors`、`limit`、`output_mode`。

### 3.5 读取对象 L2 内容 `/api/v1/objects/content`

直接读取某个 object 的 L2 内容。

常用请求字段：`uri`、`purpose`、`caller_id`。示例 URI：`rts://tradition/stella/fxd-ndf-cutoff-fixing/photo-reconstructed/rules/rule_fxd_ndf_fixing_time`。

### 3.6 依赖遍历 `/api/v1/objects/dependencies`

查询某个 object 的依赖关系。

常用请求字段：`uri`、`direction`、`depth`、`purpose`、`caller_id`。`direction` 可选值：`forward`、`reverse`、`both`。

### 3.7 读取 Trace `/api/v1/traces/{traceId}`

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
  "schema_version": "service-answer.v2",
  "grounding_map": {
    "claims": []
  },
  "budget_usage": {},
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
| `grounding_map` | claim 到 L2 object/hash 的映射；search hit 或 memory 不能成为 fact source |
| `budget_usage` | 本次请求的工具、L2、模型、延迟预算使用 |
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
| `tool_budget_exhausted` | tool、L2、依赖深度、上下文或延迟预算耗尽 | 缩小问题范围或降低读取深度；不要使用半截结果作为事实 |
| `model_provider_failure` | LLM provider timeout、不可用或返回格式非法 | 稍后重试，或改用 `/query` / tool mode 获取 deterministic truth |
| `unsupported_claim` | 模型或调用方请求的事实 claim 无法被本次 L2/依赖/governance evidence 支撑 | 改用更明确的 object URI、target path 或读取 grounding report |
| `hash_mismatch` | claim 引用的 L2 hash 与本次读取结果不一致 | 重新读取 active release；检查调用方是否混用了旧 trace 或旧 release |
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
