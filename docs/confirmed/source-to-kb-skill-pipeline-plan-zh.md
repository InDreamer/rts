<!-- docmeta
role: leaf
layer: 3
parent: docs/confirmed/README.md
children: []
summary: company-executable source-backed KB draft MVP baseline for turning workflow source assets into source inventory, evidence-backed claims, KB draft, review questions, and completion reports
read_when:
  - 需要设计从公司代码库或完整 workflow source 生成 RTS KB draft 的 MVP agent skill 体系
  - 需要固定 source inventory / coverage map、claims.jsonl、KB draft、source-backed review 和 blocker questions 的边界
  - 需要把 gRPC 到 Solace、FpML 到 SCBML 的完整转换 workflow 纳入 source-backed KB 生成
  - 需要规划在公司电脑上处理受限 source 资产前的执行边界
skip_when:
  - 只需要查看已经生成好的 KB pack
  - 只需要调用现有 RTS API
  - 只需要 PM 视角解释 runtime projection
source_of_truth:
  - docs/confirmed/kb-authoring-snapshot-runtime-final-choice-zh.md
  - docs/confirmed/kb-runtime-index-layer-standard-zh.md
  - docs/confirmed/kb-to-index-projection-contract-zh.md
-->

# Source-Backed KB Draft MVP Pipeline Plan

> 状态：confirmed MVP baseline
> 日期：2026-05-21
> 范围：固定公司环境可执行的 source-backed KB draft MVP；本地只定义 docs、skills、contracts、templates、runbook 和验证门槛。
> 当前边界：真实公司 source 不在本地，因此本文不声称已验证真实 source、准确完整 KB、production snapshot、formal signoff 或 production runtime readiness。
> Portable skill 状态：前三个 skill 已随 repo 放在 `skills/` 下；本次 MVP 将它们约束为 source inventory / evidence-backed claims / source-backed review 链路。

## 1. 两个方案的固定边界

本文同时固定两个层次，但二者不能混用：

| 层次 | 当前最佳实践 / MVP | 未来最终实现 |
|---|---|---|
| 目标 | 生成可审核的 source-backed KB draft 和 blocker questions | 生成可发布、可审计、可服务的 truth release |
| 主链路 | source codebase -> source inventory / coverage map -> evidence-backed claims -> KB draft -> source-backed review -> blocker questions -> completion report | source -> deterministic discovery / parsers / test execution -> source inventory -> evidence-first claim ledger -> governed KB -> validator/adversarial review -> frozen approved artifact -> machine-checkable signoff -> runtime activation/projection -> traced service responses |
| source map 角色 | 薄导航和覆盖清单，不是 truth | 由 deterministic discovery 生成，带 hash/revision/tool version，仍不是 truth |
| claims 角色 | 轻量 `claims.jsonl`，只让 eligible claims 进入 KB draft | 结构化 claim ledger，支持 validator、diff、coverage、contradiction search |
| LLM/skill 角色 | 编排、回源阅读、整理产物、暴露不确定性 | 编排、解释候选、生成 draft；不得绕过 validator/publisher |
| deterministic 能力 | `rg`、结构化 parser、测试发现、手工 source anchors；只做 bounded evidence extraction | AST/LSP/call graph/XML/XSLT/SQL/Excel parser/test execution/schema validator/hash validator/projection validator |
| 发布链 | 不做 production snapshot/signoff/runtime projection | frozen artifact、signoff、runtime activation/projection 是发布控制，不重新解释业务 |
| 成功标准 | unsupported claim rate、incorrect claim rate、coverage gap、review time、blocker quality 比 direct source -> KB 更好 | hash 可复现、activation 可追踪、release 可回滚、service answer 可 trace 到 release/object/evidence |

MVP 不允许宣称未来最终实现已经完成。未来发布链也不允许把 snapshot/signoff/runtime projection 当作新的业务理解阶段。

## 2. MVP 目标

目标 workflow 仍然是完整报文转换链路：

```text
gRPC inbound
  -> message classification
  -> FpML / upstream XML parsing
  -> Java / enum / Camel / XSLT / PostgreSQL / Excel mapping rules
  -> SCBML / downstream XML assembly
  -> Solace outbound
```

MVP 公司执行链路固定为：

```text
source codebase
  -> source inventory / coverage map
  -> evidence-backed claims
  -> KB draft
  -> source-backed review
  -> blocker questions
  -> completion report
```

MVP 产物用于生成可审核的 KB draft，不是 production truth。它必须让损失、缺口和不确定性显式可见，而不是把 agent 的中间摘要包装成事实。

## 3. Truth 边界

### 3.1 Source inventory 降权

`sources/{source_bundle_id}/` 是 source inventory / coverage map，不是 source profile truth。它只能回答：

```text
读了哪些 source
source 的类型、locator、revision/hash、权限状态是什么
发现了哪些 workflow 区域和候选入口
哪些区域 found / not_found / not_applicable / not_accessible / needs_user_confirmation
哪些路径被排除以及原因
哪些 claim_id 引用了这些 source anchors
```

它不能单独支持 KB truth，不能保存最终业务规则，不能保存未验证的端到端行为结论，也不能用来证明“没有 fallback/default/error path”。

### 3.2 claims.jsonl 是硬输入

`sources/{source_bundle_id}/claims.jsonl` 是 KB 生成的硬输入。每条非平凡 KB truth 都必须来自 claim，并且每条进入 KB truth 的 claim 必须有：

```text
claim_id
claim_type
status
subject
assertion
source_anchors
evidence_type
extraction_method
confidence
limits
```

允许进入 KB truth 的 claim status 只有：

```text
supported
user_confirmed
runtime_observed
```

以下状态不能进入 KB truth，只能进入 review、warning 或 blocker questions：

```text
blocked
unsupported
inferred
contradicted
not_accessible
```

### 3.3 Source-backed KB

KB generator 必须同时读取真实 source anchors 和 `claims.jsonl`。`source-index.yaml`、`workflow-map.yaml` 或 coverage map 的摘要只能用于导航和覆盖检查，不能作为业务断言证据。

每个 rule / lookup / helper 的非平凡事实必须引用 `claim_refs`，并能通过 claim 回到 source anchor、测试、样例、配置、DB/Excel evidence 或用户确认。

### 3.4 Source-backed review

Review 不是格式检查。Review 必须回源抽查或全查高风险 claims，并检查：

```text
unsupported claim
anchor laundering
coverage gap
contradiction
runtime config gap
negative claim hallucination
```

`anchor laundering` 指 claim 引用了一个文件或附近代码，但该 anchor 不能证明该断言。`negative claim hallucination` 指仅因为没有搜到就声称不存在 fallback/default/error path。

## 4. MVP 能力拆分

| 编号 | 名称 | 类型 | 当前状态 | MVP 目的 |
|---|---|---|---|---|
| 1 | `rts-workflow-source-profiler` | portable repo skill | implemented | 生成 source inventory / coverage map 和 `claims.jsonl`，不生成 KB truth |
| 2 | `rts-source-to-kb-pack` | portable repo skill | implemented | 基于真实 source anchors + supported claims 生成 KB draft |
| 3 | `rts-kb-pack-review` | portable repo skill | implemented | 做 source-backed review，输出 blocking count、blocker questions 和 completion report |
| 4 | snapshot publisher | deterministic scripts/validator | out of MVP | 从 validated KB 生成 canonical snapshot |
| 5 | runtime projection publisher | deterministic scripts/validator | out of MVP | 从 signed artifact 生成 runtime projection |

MVP 不包含：

```text
production snapshot
formal signoff workflow
immutable runtime projection
full AST/LSP/call graph platform
本地真实 source 验证
准确完整 KB 或 production readiness 声明
```

## 5. Skill 1：Source Inventory / Coverage Map

### 5.1 职责

`rts-workflow-source-profiler` 保留现有目录名以避免破坏 repo skill 路径，但语义降权为 source inventory / coverage map generator。它不创建 KB truth。

它必须生成：

```text
sources/{source_bundle_id}/
  source-manifest.yaml
  source-index.yaml
  workflow-map.yaml
  claims.jsonl
  extraction-notes.md
  unresolved-questions.yaml
  raw/                         # optional; only if explicitly allowed
  normalized/                  # optional; only source-derived summaries/tables
```

### 5.2 Source inventory 覆盖项

每个 workflow 区域必须有明确状态：

```text
found
not_found
not_applicable
not_accessible
needs_user_confirmation
```

覆盖项包括：

- gRPC inbound service、proto、handler 或 adapter
- message classification 或 routing decision
- upstream XML/FpML parse 和 semantic field extraction
- Java transformation logic
- enum/config static mapping
- XSLT template 和 target XML construction
- Camel route 或 processor flow
- PostgreSQL schema、SQL、repository 或 mapping table
- Excel/CSV mapping
- downstream XML/SCBML assembly
- Solace producer/topic/queue publication
- tests and fixtures proving behavior
- fallback/default/error paths

每个 workflow step 必须有 `source_refs` 或 unresolved question；不能留下 silent gap。

### 5.3 claims.jsonl

`claims.jsonl` 每行一个 claim。最小结构：

```json
{"schema_version":"source-claim-v1","claim_id":"claim-example-001","claim_type":"field_mapping","status":"supported","subject":"target.exampleField","assertion":"Example field is populated from upstream XPath X when condition Y holds.","source_anchors":[{"source_id":"src-example","path":"src/main/java/...","line_range":[84,112],"anchor_type":"code_path"}],"evidence_type":"code_path","extraction_method":"manual_source_read","confidence":"medium","limits":[]}
```

Claim types 至少包括：

```text
structural_fact
field_mapping
lookup_mapping
helper_logic
workflow_edge
fallback_default
error_path
test_observation
runtime_observation
user_confirmation
unresolved_question
```

## 6. Skill 2：Source-Backed KB Draft

### 6.1 职责

`rts-source-to-kb-pack` 生成 KB draft。它必须读取：

```text
sources/{source_bundle_id}/source-manifest.yaml
sources/{source_bundle_id}/source-index.yaml
sources/{source_bundle_id}/workflow-map.yaml
sources/{source_bundle_id}/claims.jsonl
sources/{source_bundle_id}/unresolved-questions.yaml
真实 source anchors 指向的公司 source
```

它不得只依据 workflow map 或 source inventory 摘要生成 KB truth。

### 6.2 输出

固定输出：

```text
kb/{pack_id}/
  metadata.yaml
  README.md
  rules/{rule_id}.yaml
  lookups/{lookup_id}.yaml
  helpers/{helper_id}.yaml
  evidence/evidence-index.yaml
  review/review-index.yaml
  reports/extraction-report.md
  reports/review-checklist.md
  reports/closure-check.md
  reports/completion-report.md
  attachments/                 # optional
```

每个 rule / lookup / helper 必须包含：

```text
claim_refs
source_anchors 或 evidence_refs
claim_status_used
warnings
```

只有 `supported`、`user_confirmed`、`runtime_observed` claims 可以进入 rule/lookup/helper truth。`blocked`、`unsupported`、`inferred`、`contradicted`、`not_accessible` 必须进入 review 或 warning。

### 6.3 建模原则

Rule / Lookup / Helper 拆分标准：

| 类型 | 放什么 |
|---|---|
| Rule | 一个业务输出、目标 XML 结构、路由决策或转换规则 |
| Lookup | DB/Excel/enum/config 支撑的可复用映射表或枚举 |
| Helper | 可复用解析、拼接、标准化、fallback 或选择逻辑 |

KB 必须覆盖完整 workflow，不只覆盖局部字段：

- inbound entrypoint
- message classification
- input XML path / input semantic field
- target XML path / output semantic field
- Java/Camel/XSLT/DB/Excel/enum evidence
- fallback/default/error path
- field binding
- dependency graph
- tests as evidence or explicit test gap
- unresolved ambiguity

## 7. Skill 3：Source-Backed KB Review

### 7.1 职责

`rts-kb-pack-review` 独立 review KB draft。默认 review-only，不静默改写 truth。

Review 必须检查：

```text
required files and directories
metadata scope
source inventory traceability
claims.jsonl presence and parseability
claim_refs resolve
claim status gate
source anchors resolve where company source is available
unsupported claim
anchor laundering
coverage gap
contradiction
runtime config gap
negative claim hallucination
workflow closure
dependency closure
test evidence or test gap
blocking count
blocker questions
completion report
```

### 7.2 输出

固定输出或更新：

```text
kb/{pack_id}/reports/review-checklist.md
kb/{pack_id}/reports/closure-check.md
kb/{pack_id}/reports/completion-report.md
kb/{pack_id}/review/review-index.yaml
kb/{pack_id}/review/ask-user-questions.json
```

### 7.3 Readiness

允许 readiness 值：

```text
not_ready_blocking_questions
not_ready_missing_source_inventory
not_ready_missing_claims
not_ready_contract_errors
ready_for_kb_draft_mvp_completion
```

MVP review 不输出 `ready_for_production_signoff` 或 `ready_for_production_runtime_projection`。

## 8. 公司环境执行顺序

完整公司环境执行顺序固定为：

```text
1. 确认公司 source repo 只读可访问。
2. 收集 workflow scope、allowed read paths、forbidden paths、test/DB/Excel 访问权限。
3. 运行 rts-workflow-source-profiler，生成 source inventory / coverage map 和 claims.jsonl。
4. 快速检查 coverage map：每个 workflow 区域都有状态，没有 silent gap。
5. 运行 rts-source-to-kb-pack，基于真实 source anchors + supported claims 生成 KB draft。
6. 运行 rts-kb-pack-review，执行 source-backed review。
7. 读取 ask-user-questions.json，只处理 blocking blocker questions。
8. 根据用户确认更新 claims/review/KB。
9. 重跑 review，直到 blocking count 为 0 或明确记录 deferred blocker。
10. 写 completion-report.md。
```

公司电脑第一轮目标是可审核 KB draft 和清楚的 blocker questions，不是 production release。

## 9. Completion report

`reports/completion-report.md` 必须记录：

```text
source bundle id
pack id
company source revision / hash / unavailable reason
workflow scope
coverage summary
claim counts by status
KB object counts
unsupported / inferred / blocked / contradicted / not_accessible counts
blocking count
blocker questions path
test execution status
runtime config access status
what was not verified
MVP completion decision
explicit non-production statement
```

如果真实 source 不可访问，completion report 必须写明不可完成 company execution，不能声称验证通过。

## 10. 未来最终实现的升级门槛

只有当 MVP 证明 `source inventory + claims + source-backed review` 明显优于 direct source -> KB，才进入未来最终实现。证明标准至少包括：

- unsupported claim rate 下降。
- incorrect claim rate 下降。
- 重要 workflow 区域漏扫率下降。
- 工程师 review 时间下降或 review 质量提升。
- blocker questions 被用户认为必要，而不是流程噪音。
- 重新生成后 KB diff 能解释“为什么变了”。

未来最终实现必须新增这些机器边界：

```text
deterministic source discovery/index
claim ledger schema validator
source anchor resolver
contradiction and omission checker
KB package validator
frozen approved artifact hash
machine-checkable signoff record
runtime activation record
service trace to release/object/claim/evidence
```

发布链最小形态可以先压缩为：

```text
source evidence
  -> reviewed KB
  -> approved release manifest + frozen runtime JSON
  -> active-release pointer
  -> traced service responses
```

只有在多消费者、多视图、严格审计或 KB 到服务格式存在非平凡转换时，才拆成完整 canonical snapshot + signoff + runtime projection 三层。无论是否拆层，发布阶段都不能新增业务断言。

## 11. 本地可完成与公司环境才可完成

本地可完成：

- 更新 confirmed plan、skills、contracts、templates、runbook。
- 运行文档 contract 校验。
- 确认 repo 没有 archive 修改。

公司环境才可完成：

- 真实 source scanning。
- source inventory / coverage map 生成。
- `claims.jsonl` 真实 evidence capture。
- source-backed KB draft 生成。
- source-backed review。
- blocker questions 确认。
- completion report 对真实 source 的结论。

## 12. 最短版

```text
不要把 source profile 当 truth。
不要让 KB 只吃 source map 摘要。
先生成 source inventory / coverage map。
再生成 evidence-backed claims。
KB truth 只接受 supported / user_confirmed / runtime_observed claims。
Review 必须回源查 unsupported claim、anchor laundering、coverage gap、contradiction、runtime config gap、negative claim hallucination。
MVP 到 completion report 为止，不包含 production snapshot、formal signoff 或 immutable runtime projection。
```
