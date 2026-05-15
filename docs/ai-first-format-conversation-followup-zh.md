<!-- docmeta
role: analysis
layer: supplemental
parent: docs/ai-readable-format-strategy-zh.html
children: []
summary: Captures follow-up clarifications from the AI-first KB/projection format discussion that were not fully covered in the original HTML report.
read_when:
  - 需要回顾本次关于 KB、runtime projection、LLM input context、格式边界的后续澄清
  - 需要判断 AI Context 是否是新 truth 层、何时生成、由谁生成
  - 需要评估当前 runtime projection 是否需要为了 LLM context 做调整
skip_when:
  - 只需要完整格式矩阵和视觉报告
  - 只需要 confirmed baseline authority
source_of_truth:
  - docs/confirmed/project-alignment-summary-zh.md
  - docs/confirmed/kb-to-index-projection-contract-zh.md
  - docs/confirmed/internal-llm-agent-service-implementation-plan-zh.md
-->

# AI-first Format Follow-up Notes

> 状态：conversation follow-up note
> 创建日期：2026-05-15
> 范围：记录本次对话中在 `docs/ai-readable-format-strategy-zh.html` 之后进一步澄清出的重点。

本文不是新的 confirmed baseline，也不替代 runtime projection contract。

它的作用是把本次讨论中“后来才说清楚”的点集中记录下来，避免后续继续混淆：

- AI-first 语境下，人工直接看材料不是主要权重。
- `AI Context Format` 这个名字容易误解，更适合叫 `LLM Input Context Envelope`。
- 这个 envelope 不是新 truth 层，不是新 projection，也不应在发布时预生成。
- 当前 runtime projection 基本够作为 LLM context 的原材料，不需要推倒。
- 真正需要增强的是角色标注、schema contract、运行时组装规则和上下文安全门禁。

## 1. 术语修正

### 1.1 “运行时能力”应改成“服务运行时适配度”

原报告中的“运行时能力”容易被误解为：

```text
AI 读起来是否顺畅
```

更准确的意思是：

```text
某种格式是否适合作为 RTS service 在运行时安全读取、校验、过滤、追踪、返回事实的载体。
```

它关注的是服务行为，而不是模型阅读舒适度。

包括：

- 能否被 Java service/API/tool 稳定解析。
- 能否做 schema validation、必填字段检查、类型检查。
- 能否绑定 `release_id`、`scope`、`permission`、`content_hash`。
- 能否按 URI、object id、target path、dependency 快速查找。
- 能否支持 grounding、trace、refusal、审计复盘。
- 能否避免 AI 把展示文字、注释、样式或历史讨论误当 truth。

推荐列名：

```text
服务运行时适配度
```

英文可以叫：

```text
Service runtime suitability
```

## 2. 分层口径补充

原报告里的分层可以继续保留，但需要更明确地区分“truth 层”和“运行时组装层”。

### 2.1 Source

Source 是原始证据和输入材料。

例如：

- 业务文档
- Excel
- XML/FpML
- XSLT
- Java
- 截图 OCR
- 样例报文

Source 的作用是证明规则从哪里来。

Source 不应该被普通 runtime query 直接当 truth 读取。

### 2.2 KB Authoring

KB 是治理和整理层。

它负责：

- 从 source 中抽取 rule / lookup / helper。
- 记录 evidence。
- 暴露 ambiguity 和 conflict。
- 支持 AI-first review。
- 支持少量人工裁决和 signoff。

AI 可以大量阅读和维护 KB，但 KB 里的 candidate、review notes、open questions 不能自动成为 runtime truth。

### 2.3 Runtime Projection

Runtime projection 是发布后的服务运行视图。

它负责：

- 把 approved truth 变成 service 可读取对象。
- 提供 manifest、scope、object manifest、L2、dependency、governance summary。
- 支撑 release、scope、permission、hash、grounding、trace。

Runtime projection 是 LLM context 的主要原材料来源。

### 2.4 Index / Query

Index/query 层负责找对象和规划读取。

它使用：

- object card
- L0/L1
- alias
- confusable
- search text
- dependency edges
- field bindings

Index hit 不拥有 truth。

命中后必须回到 L2 runtime object 或授权 governance summary。

### 2.5 LLM Input Context Envelope

这是本次对话中重点澄清的层。

它不是新的 truth 层。

它只是每次调用 LLM 前，由 runtime service 从 projection 中挑选、裁剪、标注出来的一包材料。

更准确的名字不建议叫 `AI Context Format`，而建议叫：

```text
LLM Input Context Envelope
```

中文：

```text
模型输入上下文信封
```

### 2.6 Presentation

Presentation 是展示层。

例如：

- HTML
- dashboard
- PDF
- report

它可以展示 AI 分析和少量人工抽查视图。

它不应该回流成 truth source。

## 3. LLM Input Context Envelope 的定位

### 3.1 它不是什么

它不是：

- 新 KB。
- 新 runtime projection。
- 新 index。
- 新 truth source。
- 对 L2 的智能总结。
- 发布时固定生成的 artifact。
- 让 AI 自己决定哪些内容能当事实的机制。

### 3.2 它是什么

它是：

```text
某一次 ask/scenario/tool run 中，RTS service 准备喂给 LLM 的受控材料包。
```

它基于 runtime projection 生成，但不创造新的 truth。

它只做三件事：

1. 选择：这次问题需要哪些 L2、dependency、governance summary。
2. 裁剪：不一定把整个 L2 都给 AI，只给本次问题相关字段。
3. 标注：告诉 AI 每段材料能不能当事实、来自哪里、属于哪个 release、hash 是什么。

### 3.3 为什么 runtime projection 本身还不等于 LLM context

Runtime projection 是完整服务材料库。

它包含：

- release manifest
- scope registry
- object manifest
- object cards
- L2 objects
- dependency edges
- field bindings
- governance summaries
- navigation views
- caller profiles
- index artifacts

但每次 LLM 调用通常只需要其中一小部分。

例如：

```text
用户问 fixingDate
```

需要的可能只是：

- fixingDate rule 的 L2
- 相关 warning
- URI
- release id
- L2 hash

不需要整个 pack、所有 lookup、所有 helper、所有 governance summaries。

所以 runtime projection 是材料库，LLM input context envelope 是本次调用的装箱结果。

## 4. LLM Input Context Envelope 何时生成

它应该在运行时生成。

也就是：

```text
/ask
scenario endpoint
external tool mode wrapper
MCP managed wrapper
```

每次执行时根据当前请求生成。

不建议在 projection 发布时提前生成完整 context。

原因：

- context 取决于用户问题。
- context 取决于 intent。
- context 取决于 resolved scope。
- context 取决于 caller permission。
- context 取决于 token budget。
- context 取决于是否需要 governance view。
- context 取决于 dependency traversal depth。

同一份 projection 可以被不同问题组装成不同 context。

## 5. LLM Input Context Envelope 由谁生成

应主要由程序生成。

不是由 AI 自己生成。

推荐流程：

```text
用户问题
  -> RTS resolve scope
  -> RTS find object
  -> RTS read L2 / dependency / governance summary
  -> 程序按模板生成 context envelope
  -> LLM 基于 envelope 生成分析草稿
  -> RTS 做 claim/grounding validation
```

AI 可以提出“可能还需要什么对象”，但最终：

- 哪些对象可读
- 哪些字段可进入上下文
- 哪些内容可标记为 truth eligible
- 哪些 governance summary 可展开
- 哪些内容需要 redaction

都必须由 RTS service 决定。

## 6. LLM Input Context Envelope 的最小形态

短期不需要复杂设计。

可以先做一个很薄的 envelope。

每个 context item 至少包含：

```yaml
kind: l2_fact
truth_eligible: true
uri: rts://...
release_id: rel-2026-05-06
hash: sha256:...
allowed_use: answer_fact
content: ...
```

关键字段解释：

- `kind`：这段是什么，例如 `l2_fact`、`dependency`、`object_card`、`governance_warning`、`memory`。
- `truth_eligible`：这段能不能支撑事实 claim。
- `uri`：对应哪个 RTS object。
- `release_id`：来自哪个 release。
- `hash`：对应 L2 或 summary 的 hash。
- `allowed_use`：这段允许怎么用，例如 `answer_fact`、`navigation_only`、`warning_only`、`candidate_only`。
- `content`：给模型看的实际内容。

### 6.1 示例：L2 fact context item

```yaml
kind: l2_fact
truth_eligible: true
uri: rts://tradition/stella/.../rules/rule_fxd_ndf_fixing_date
release_id: rel-2026-05-06-photo-fxd-ndf-cutoff
hash: sha256:338bd616...
allowed_use: answer_fact
content:
  summary: Fixing date copies source fixingDate directly to Stella fixingDate.
  source_path: /FpML/trade/fxSingleLeg/nonDeliverableForward/fixing/fixingDate/text()
  target_path: /scb:SCBML/.../conf:fixingDate
  dependencies: []
```

### 6.2 示例：navigation-only context item

```yaml
kind: object_card
truth_eligible: false
uri: rts://tradition/stella/.../rules/rule_fxd_ndf_fixing_date
release_id: rel-2026-05-06-photo-fxd-ndf-cutoff
hash: sha256:2c0054...
allowed_use: navigation_only
content:
  summary: Fixing date copies source fixingDate directly to Stella fixingDate.
```

这里的重点是：

```text
object_card 可以帮助找到对象，但不能单独支撑最终事实。
```

## 7. 当前项目是否已经有这个东西

当前项目已经有雏形，但还没有正式命名为稳定格式。

已有基础：

- `ContextKind`：已经区分 `object_card`、`l2_fact`、`dependency`、`governance_warning`、`memory` 等类型。
- `ContextItem`：已经有 `kind`、`source`、`truthEligible`、`objectUri`、`hash`、`text`。
- `ContextSnapshot`：已经记录 context items、hash、token estimate、truthEligible count。
- `OpenAiCompatibleLlmClient`：已经会先读取 grounded service result，再传给模型。

不足：

- 还没有稳定的 `llm-input-context-envelope-v1` schema。
- 现在更像运行时对象和 prompt 拼装逻辑，而不是一份明确的格式契约。
- 当前 context 更偏 URI/hash preservation，尚未形成清晰的 per-item `allowed_use`、`redaction_state`、`field_path`、`source_role` 约定。

## 8. 和 KB 的关系

LLM Input Context Envelope 主要不是 KB 设计问题。

它更偏：

```text
query layer / LLM harness / tool orchestration
```

KB 不需要知道某次用户问了什么。

KB 的职责是：

- 管理完整 truth graph。
- 产出 canonical objects。
- 记录 evidence/review/adjudication/signoff。
- 发布 runtime projection。

LLM context 的职责是：

- 在某次运行时，从 projection 中挑选材料。
- 给模型一个受控、低噪声、可引用的材料包。

因此：

```text
KB 负责 truth formation。
Projection 负责 truth publication。
LLM context envelope 负责 per-request truth packaging。
```

## 9. 当前 runtime projection 是否需要调整

结论：

```text
不需要为了 LLM context 大改当前 runtime projection。
```

当前 sample projection 已经有比较完整的原材料：

- `release-manifest.json`
- `object-manifest.jsonl`
- `navigation/object-cards.jsonl`
- `navigation/l0-l1-views.jsonl`
- `l2/**/*.json`
- `dependencies/dependency-edges.jsonl`
- `dependencies/field-bindings.jsonl`
- `governance/governance-access-refs.jsonl`
- `governance/*-summaries/*.json`

这些已经可以支持运行时组装 LLM context。

建议做小增强，而不是大改。

### 9.1 建议增强：统一 `truth_role` / `view_role`

当前 L0/L1 里已经出现：

```json
"view_role": "navigation_not_truth"
```

这是好的。

建议推广到更多 projection artifact。

例如：

```json
"truth_role": "l2_fact"
```

```json
"truth_role": "navigation_only"
```

```json
"truth_role": "authorized_governance_summary"
```

这样 context builder 不需要猜某个对象能不能支撑 fact。

### 9.2 建议增强：object card 明确 navigation-only

Object card 很适合定位、消歧和初步排序。

但它不应该被当作最终事实。

建议 object card 也明确：

```json
"truth_role": "navigation_only"
```

或者：

```json
"view_role": "navigation_not_truth"
```

### 9.3 建议增强：L2 字段用途约定

L2 中有些字段适合进入事实 context：

- `logic`
- `inputs`
- `target`
- `dependencies`
- `examples`

有些字段更像 caution：

- `operational_warnings`
- `production_gate`
- `review_state.open_questions`

有些字段只是指针：

- `governance_summary_refs`
- `source_lineage`

建议在 schema 文档中约定这些字段的默认用途。

不一定要改所有 JSON 文件。

更重要的是让 context builder 有明确规则。

### 9.4 建议增强：governance summary 标明用途

Governance summary 应该明确：

- 是否可进入默认 answer context。
- 是否只用于 warning。
- 是否需要 governance permission。
- 是否可支撑 fact。
- 是否只是 evidence locator。

建议加类似：

```json
"truth_role": "authorized_governance_summary",
"allowed_use": ["warning", "evidence_explanation"],
"requires_permission": "governance_tools"
```

### 9.5 建议增强：lookup 大表 metadata

当前 lookup L2 中已经有：

- inputs
- logic pipeline
- output fields
- examples
- reverse fallback

这对 demo 和小 lookup 已经够用。

如果未来有真正大表，并使用 CSV/TSV 存储，需要补 companion metadata：

- key columns
- output columns
- blank semantics
- reverse fallback
- coalesce behavior
- source hash
- row-level hash 或 table hash

## 10. 是否应该把格式化好的内容提前放进 projection

可以提前放一部分，但不应提前放最终 LLM context。

可以提前放：

- object card summary
- L0 text
- L1 JSON
- search text
- example summary
- warning summary
- field bindings
- dependency hints

这些是 object-level reusable views。

不应提前放：

- 某次问题的 final context packet
- 某个 caller 权限下的 context
- 某个 token budget 下的裁剪结果
- 某个 scenario intent 下的组合结果

原因：

```text
最终 context 是 query-specific / caller-specific / budget-specific / intent-specific。
```

所以合理分工是：

```text
projection 预生成可复用的对象级视图；
运行时按请求组装最终 LLM input envelope。
```

## 11. 生成方式不应依赖“智能改写”

LLM context 生成不应该是：

```text
让 AI 看 L2，然后 AI 自己总结一份 context。
```

更安全的方式是：

```text
程序模板 + 固定字段选择 + 权限检查 + hash preservation。
```

程序可以做：

- 选对象。
- 选字段。
- 取摘要字段。
- 保留原始 L2 JSON 片段。
- 加 URI/release/hash。
- 标记 truth eligibility。
- 标记 allowed use。
- 控制 token budget。

AI 不应该决定：

- 哪些材料是 truth。
- 哪些 hash 有效。
- 哪些权限允许。
- 哪些 governance 内容可见。

## 12. 安全门禁

LLM Input Context Envelope 需要几条硬规则：

- 只有 L2、dependency、authorized governance summary 可以 `truth_eligible=true`。
- Object card、search hit、L0/L1 默认只能 `navigation_only`。
- Memory、chat history、workspace notes、raw diff/log 默认 `truth_eligible=false`。
- 没有 URI/hash 的内容不能支撑 fact。
- 没有权限的 governance material 不能进入 context。
- Context 为空或无 truth eligible item 时，应 clarification、partial 或 refusal。
- 最终 answer 仍要做 grounding validation。

## 13. 本次讨论中形成的 enhancement 清单

### 13.1 文档表达修复

- 把“运行时能力”改成“服务运行时适配度”。
- 把 “AI Context Format” 改成 “LLM Input Context Envelope / 模型输入上下文信封”。
- 明确 AI-first 权重：AI/agent 直接阅读、审核、维护是主场；人工直接查看只是少量抽查。
- 明确 Presentation 是派生展示层，不是 truth 层。

### 13.2 KB 侧增强

- 定义受限 YAML subset。
- 禁止复杂 anchors、隐式 boolean/date、重复 key、自由字段漂移。
- 发布前对 KB YAML/Markdown 做 schema/governance validation。
- 治理叙述可以是 Markdown，但关键裁决、open questions、evidence refs 需要结构化同步。

### 13.3 Runtime projection 侧增强

- 不需要大改。
- 统一 `truth_role` / `view_role`。
- object card 明确 navigation-only。
- L2 字段按 fact / caution / governance ref 做用途约定。
- projection schema 更细分版本化。

### 13.4 Query / LLM harness 侧增强

- 定义薄的 `llm-input-context-envelope-v1`。
- 运行时生成 context envelope。
- 程序模板生成，不由 AI 自己生成。
- 每个 context item 带 `kind`、`truth_eligible`、`uri`、`release_id`、`hash`、`allowed_use`。
- 最终回答继续走 grounding validation。

### 13.5 CSV / TSV 侧增强

- 大 lookup 表可以用 CSV/TSV。
- 必须配 companion metadata。
- metadata 至少包含 key columns、output columns、blank semantics、fallback/reverse/coalesce、source hash。

### 13.6 HTML 侧定位

- HTML 继续用于报告、dashboard、AI-mediated review、少量人工抽查。
- runtime/query/tool 不应解析 HTML 取 truth。
- HTML 应由 projection 或 analysis output 派生，不回流成 truth source。

## 14. 最终建议

当前项目格式路线是对的：

```text
KB YAML/Markdown
  -> runtime projection JSON/JSONL
  -> query/index/service gate
  -> per-request LLM input context envelope
  -> managed answer / scenario report / HTML presentation
```

后续不建议大规模改格式。

更应该做的是：

1. 把 projection artifact 的 truth role 标清楚。
2. 把 LLM input envelope 当作运行时装箱契约，而不是新 truth 层。
3. 把 KB YAML 和 projection JSON/JSONL 的 schema contract 补强。
4. 让程序负责 context 组装，让 AI 负责受控分析和表达。

一句话：

```text
Projection 提供可信材料；LLM Input Context Envelope 只是本次调用的安全装箱。
```
