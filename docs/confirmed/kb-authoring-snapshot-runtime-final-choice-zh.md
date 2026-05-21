<!-- docmeta
role: leaf
layer: 3
parent: docs/confirmed/README.md
children: []
summary: final executable contract for RTS source intake, structured KB authoring, canonical signoff snapshot, runtime projection, and derived views
read_when:
  - 需要固定 source 到结构化 KB 再到 runtime projection 的落地契约
  - 需要判断新生成器下次必须产出哪些目录、文件和字段
  - 需要最终决定 RTS KB 应该用 JSON 还是 YAML
  - 需要冻结 KB authoring、canonical signoff snapshot、runtime projection、index/view/context 的分层选择
  - 需要设计 signoff、content hash、human confirmation、publisher 或 schema freeze
skip_when:
  - 只需要调用已经运行的 API
  - 只需要 PM 视角解释 runtime projection 目录
  - 只需要历史方案或外部检索参考
source_of_truth:
  - docs/confirmed/project-alignment-summary-zh.md
  - docs/confirmed/system-constitution-v1.md
  - docs/confirmed/kb-to-index-projection-contract-zh.md
  - docs/confirmed/kb-runtime-index-layer-standard-zh.md
  - kb/tradition-to-stella-fxd-ndf-cutoff-fixing-split
  - runtime-store/releases
  - src/main/java/com/rts/store/FileSystemProjectionStore.java
  - src/main/java/com/rts/query/QueryService.java
-->

# KB Authoring / Snapshot / Runtime Projection Final Contract

> 状态：confirmed final contract
> 日期：2026-05-21
> 范围：固定 RTS 从 source intake 到结构化 KB、canonical signoff snapshot、runtime projection、derived index/view/context 的落地方案
> 关系：本文固定“下次生成器必须长什么样”；更细的 KB/runtime/index 字段参考 `docs/confirmed/kb-runtime-index-layer-standard-zh.md`
> 实现状态：当前 repo 已有 KB authoring 样例和 runtime projection 样例；canonical signoff snapshot publisher、production provenance gate、write-once release gate 仍需实现。

## 0. 最终固定结论

RTS 不采用“纯 JSON KB”或“纯 YAML KB”二选一。最终固定为五层链路：

```text
Layer 0  Source intake bundle
  -> Layer 1  Structured KB authoring package
  -> Layer 1.5  Canonical signoff snapshot
  -> Layer 2  Runtime projection release package
  -> Layer 3  Derived index / view / LLM context envelope
```

格式固定如下：

| 层 | 默认格式 | 是否拥有 truth | 允许谁直接读 |
|---|---|---|---|
| Source intake | YAML manifest + raw/normalized source refs | 否；它是证据入口，不是规则 truth | extractor、KB generator、review agent、publisher |
| Structured KB authoring | YAML + Markdown，必要时 CSV/TSV attachments | 是，作为 governed working truth | KB authoring agent、review agent、publisher、少量人工 reviewer |
| Canonical signoff snapshot | JSON + JSONL | 是，作为 immutable signoff truth | publisher、release audit、governance tooling |
| Runtime projection | JSON + JSONL + L2 JSON | 是，作为 service truth | RTS service、query/index、managed LLM harness、tool surface |
| Derived outputs | Lucene/OpenSearch/JSONL/HTML/Markdown/request JSON | 否 | search、HTML/doc generator、LLM context assembler |

一句话：

```text
Source 负责可追溯。
KB 负责可维护。
Snapshot 负责可签核。
Runtime projection 负责可服务。
Index、HTML、LLM context 只负责派生使用，不能拥有 truth。
```

## 1. 生成顺序

任何新的规则包，必须按这个顺序生成和验收：

```text
0. 选择 identity 和 scope
1. 建 source intake bundle
2. 生成 structured KB authoring package
3. 校验 KB package
4. 生成 canonical signoff snapshot
5. 完成 signoff
6. 生成 immutable runtime projection release
7. 生成 index / HTML / LLM context views
8. 运行 service smoke tests
```

每一步的输入、输出和拒绝条件如下：

| 步骤 | 输入 | 必须输出 | 拒绝条件 |
|---|---|---|---|
| Identity / scope | source request、业务范围、目标系统 | `pack_id`、`source_bundle_id`、`scope`、`canonical_revision` | scope 不唯一；pack id 与已有 pack 冲突 |
| Source intake | 原始文档、代码、表格、截图、对话确认或外部 locator | `sources/{source_bundle_id}/source-manifest.yaml`、`source-index.yaml`、`extraction-notes.md` | 没有 stable locator、source revision、hash 或权限状态 |
| Structured KB | source bundle、抽取结果、review 意见 | `kb/{pack_id}/metadata.yaml`、`rules/*.yaml`、`lookups/*.yaml`、`helpers/*.yaml`、`evidence/`、`review/`、`reports/` | object id 不稳定；evidence/ref 无法解析；scope 不兼容 |
| KB validation | KB package | validation report、blocking issue count | schema 错、引用断、open blocker 未关闭 |
| Snapshot | validated KB | `kb/{pack_id}/snapshots/{snapshot_id}/...` | canonical hash 不可复现；snapshot 覆盖旧 id |
| Signoff | snapshot、review decision、human confirmation | `signoff.json` | signoff 未覆盖所有 projected objects；production actor 不可识别 |
| Runtime projection | signed snapshot | `runtime-store/releases/{release_id}/...` | 缺 snapshot id/hash/signoff id；L2 hash 不匹配；release id 已存在 |
| Derived views | runtime projection | `index-artifacts/`、HTML/Markdown review views、LLM context envelope templates | 派生层新增业务断言；未保留 source uri/hash/role/trace |
| Smoke tests | active release | query/read/hash/dependency/trace test result | service 绕过 projection 读 KB；trace 缺 release/object/hash |

## 2. 固定目录总览

推荐 repo 形状：

```text
sources/
  {source_bundle_id}/
    source-manifest.yaml
    source-index.yaml
    extraction-notes.md
    raw/                         # optional; raw files or local copies when allowed
    normalized/                  # optional; extracted CSV/JSON/text when allowed
    attachments/                 # optional; screenshots, exported tables, temporary parse outputs

kb/
  {pack_id}/
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
    attachments/                 # optional; CSV/TSV or normalized tables used by KB objects
    snapshots/{snapshot_id}/
      snapshot-manifest.json
      objects.jsonl
      dependency-edges.jsonl
      evidence-refs.jsonl
      review-decisions.jsonl
      human-confirmation-log.jsonl
      change-log.jsonl
      signoff.json
      projection-map.jsonl

runtime-store/
  active-release.json
  releases/{release_id}/
    release-manifest.json
    scopes.jsonl
    object-manifest.jsonl
    caller-profiles.jsonl
    l2/rules/{rule_id}.json
    l2/lookups/{lookup_id}.json
    l2/helpers/{helper_id}.json
    navigation/object-cards.jsonl
    navigation/l0-l1-views.jsonl
    navigation/aliases.jsonl
    navigation/confusables.jsonl
    dependencies/dependency-edges.jsonl
    dependencies/field-bindings.jsonl
    governance/governance-access-refs.jsonl
    governance/evidence-summaries/{object_or_pack_id}.json
    governance/review-summaries/{object_or_pack_id}.json
    governance/report-summaries/{pack_or_release_id}.json
    index-artifacts/opensearch-docs.jsonl
    index-artifacts/lucene/       # generated binary index; rebuildable
  traces/query-trace.jsonl
  traces/llm-run-trace.jsonl
```

当前 repo 已有 `kb/tradition-to-stella-fxd-ndf-cutoff-fixing-split/` 和 `runtime-store/releases/rel-2026-05-06/` 样例。它们用于兼容和验证方向；后续新生产包必须补齐 `sources/` 和 `snapshots/`。

## 3. Layer 0：Source Intake Bundle

Source intake 是生成链路的证据入口。它不直接成为规则 truth，也不允许 runtime service 读取。

目录固定为：

```text
sources/{source_bundle_id}/
  source-manifest.yaml
  source-index.yaml
  extraction-notes.md
  raw/                         # optional
  normalized/                  # optional
  attachments/                 # optional
```

### 3.1 `source-manifest.yaml`

必须字段：

```yaml
schema_version: source-bundle-v1
source_bundle:
  id: src-tradition-stella-fxd-ndf-2026-05-21
  name: Tradition Stella FXD.NDF source bundle
  status: captured
  captured_at: 2026-05-21T00:00:00Z
  captured_by: extractor-or-approved-workflow
scope:
  source_system: Tradition
  target_system: Stella
  product_scope: FXD.NDF
  domain: cutoff-fixing
source_revision:
  repo_ref: git:abc123
  external_revision: vendor-export-2026-05-21
  capture_time: 2026-05-21T00:00:00Z
hash_profile:
  algorithm: sha256
  normalization: raw-bytes-or-declared-normalization
permissions:
  classification: internal
  allowed_purposes: [authoring, review, publisher]
  redaction_required: false
```

Rules:

- 如果 raw 文件允许入库，放入 `raw/` 并记录 hash。
- 如果 raw 文件不能入库，`source-index.yaml` 必须记录外部 stable locator、revision、hash 或不可 hash 的理由。
- Source bundle 不能被 runtime service 当 truth 读取。

### 3.2 `source-index.yaml`

每个 source item 必须有稳定 locator：

```yaml
schema_version: source-index-v1
items:
  - source_id: src-code-static-mapping-lookup-core
    source_type: codebase
    locator:
      repo: tradition-transformer
      commit: abc123
      path: src/main/java/.../StaticMappingLookupCore.java
      symbol: getCutoffWithFallback
      line_range: [120, 168]
    content_hash: sha256:...
    permission_state: allowed
    redaction_state: not_required
    used_by:
      - rule_fxd_ndf_primary_rate_source
      - lk_fxd_ndf_cutoff_by_pair_and_locode
```

### 3.3 `extraction-notes.md`

只写抽取过程和限制，不写最终规则 truth。必须包含：

```text
source scope
what was extracted
what was excluded
known gaps
normalization decisions
review questions raised
```

## 4. Layer 1：Structured KB Authoring Package

KB 是可维护、可 review 的 governed working truth。默认格式是 YAML + Markdown。

目录固定为：

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
  attachments/                 # optional
  snapshots/                   # generated after validation and signoff
```

### 4.1 `metadata.yaml`

必须字段：

```yaml
schema_version: kb-pack-v1
pack:
  id: tradition-to-stella-fxd-ndf-cutoff-fixing-split
  name: Tradition -> Stella FXD.NDF cutoff-fixing split
  status: demo_signoff
  version: 0.2.0-photo-reconstructed
  canonical_revision: photo-reconstructed-2026-05-06
source_bundle:
  id: src-tradition-stella-fxd-ndf-2026-05-21
  manifest: ../../sources/src-tradition-stella-fxd-ndf-2026-05-21/source-manifest.yaml
scope:
  source_system: Tradition
  target_system: Stella
  product_scope: FXD.NDF
  domain: cutoff-fixing
  target_area:
    - /scb:SCBML/.../conf:fixing
objects:
  rules: 6
  lookups: 1
  helpers: 1
indexes:
  evidence_index: evidence/evidence-index.yaml
  review_index: review/review-index.yaml
  extraction_report: reports/extraction-report.md
  review_checklist: reports/review-checklist.md
  closure_check: reports/closure-check.md
release_eligibility:
  intended_tier: demo
  blocking_issues_count: 0
  open_questions_count: 0
```

Rules:

- `pack.id` 必须等于目录名 `{pack_id}`。
- `canonical_revision` 必须能被 snapshot 和 runtime manifest 引用。
- `scope` 是 object 默认 scope；object 局部 scope 只能收窄，不能冲突。
- `status` 必须是枚举，不能用自然语言替代。

推荐状态枚举：

```text
candidate
ai_reviewed
human_review_required
demo_signoff
production_signoff
deprecated
superseded
```

### 4.2 `rules/{rule_id}.yaml`

Rule 表示一个目标规则对象。必须字段：

```yaml
schema_version: kb-object-v1
id: rule_fxd_ndf_primary_rate_source
object_type: rule
status: demo_signoff
signoff_status: demo_signoff
revision: 2
scope:
  inherits: pack
source_anchors:
  - source_id: src-code-static-mapping-lookup-core
    locator_ref: source-index.yaml#src-code-static-mapping-lookup-core
evidence_refs:
  - ev-primary-rate-source-lookup
review_refs:
  - rv-primary-rate-source-closure
inputs:
  - name: fixing_currency1
    source_path: /FpML/trade/fxSingleLeg/nonDeliverableForward/fixing/quotedCurrencyPair/currency1/text()
logic:
  summary: Resolve the hedge primary rate source from the cutoff lookup.
  pipeline:
    - op: read_xpath
      source: /FpML/.../currency1/text()
      out: fixing_currency1
    - op: lookup_value
      lookup: lk_fxd_ndf_cutoff_by_pair_and_locode
      return_field: hedge_primary_source
      using_inputs:
        fixing_currency1: fixing_currency1
      out: hedge_primary_source
target:
  kind: block
  path: /scb:SCBML/.../conf:primaryRateSource
  emits:
    - path: /scb:SCBML/.../conf:rateSource
      from: hedge_primary_source
dependencies:
  lookups:
    - lk_fxd_ndf_cutoff_by_pair_and_locode
  helpers: []
  rules: []
examples:
  - name: sample_usdtwd_primary_rate_source
    inputs:
      fixing_currency1: USD
    expected:
      hedge_primary_source: TAIFX1
warnings: []
```

### 4.3 `lookups/{lookup_id}.yaml`

Lookup 表示可复用表或枚举映射。必须字段：

```yaml
schema_version: kb-object-v1
id: lk_fxd_ndf_cutoff_by_pair_and_locode
object_type: lookup
status: demo_signoff
signoff_status: demo_signoff
revision: 1
scope:
  inherits: pack
source_anchors:
  - source_id: src-sheet-tradition-stella-cutoff
evidence_refs:
  - ev-cutoff-table
key:
  columns:
    - pair
    - locode
  normalization:
    pair: uppercase-no-separator
    locode: uppercase
return_fields:
  - hedge_primary_source
  - hedge_secondary_source
  - fixing_time
fallback:
  reverse_pair_allowed: true
  missing_value_policy: unresolved
data:
  inline_rows: []
  attachment_ref: attachments/cutoff-table.tsv
dependencies: {}
warnings: []
```

Rules:

- 小表可以 `inline_rows`。
- 大表必须放 `attachments/*.csv|*.tsv`，并记录 hash、schema 和 normalization。
- fallback/reverse/missing policy 必须显式写出，不能靠 report prose 补充。

### 4.4 `helpers/{helper_id}.yaml`

Helper 表示可复用计算、解析或选择逻辑。必须字段：

```yaml
schema_version: kb-object-v1
id: hlp_fxd_ndf_fixing_quoted_currency_pair
object_type: helper
status: demo_signoff
signoff_status: demo_signoff
revision: 1
scope:
  inherits: pack
inputs:
  - name: currency1
  - name: currency2
outputs:
  - name: normalized_pair
logic:
  summary: Build the normalized fixing quoted currency pair.
  pipeline:
    - op: concat
      args: [currency1, currency2]
      out: normalized_pair
consumers:
  rules:
    - rule_fxd_ndf_fixing_quoted_currency_pair
dependencies: {}
evidence_refs:
  - ev-quoted-currency-pair-helper
warnings: []
```

### 4.5 `evidence/evidence-index.yaml`

Evidence index 保存证据引用，不复制整个 source。必须字段：

```yaml
schema_version: evidence-index-v1
evidence:
  - evidence_id: ev-primary-rate-source-lookup
    evidence_type: code_and_table
    object_refs:
      - rule_fxd_ndf_primary_rate_source
      - lk_fxd_ndf_cutoff_by_pair_and_locode
    source_refs:
      - source_id: src-code-static-mapping-lookup-core
      - source_id: src-sheet-tradition-stella-cutoff
    locator_summary: Static mapping lookup plus cutoff sheet rows.
    quote_or_summary: Primary rate source is resolved through the cutoff lookup.
    confidence: high
    permission_state: allowed
    redaction_state: not_required
```

### 4.6 `review/review-index.yaml`

Review index 保存结构化审核结论。必须字段：

```yaml
schema_version: review-index-v1
decisions:
  - decision_id: rv-primary-rate-source-closure
    object_refs:
      - rule_fxd_ndf_primary_rate_source
    decision_type: ambiguity_closure
    status: accepted
    question_or_conflict: Which source should drive primaryRateSource?
    decision: Use cutoff lookup hedge_primary_source.
    decided_by: ai-review-plus-human-confirmation
    decided_at: 2026-05-21T00:00:00Z
    evidence_refs:
      - ev-primary-rate-source-lookup
    remaining_risk: []
    production_gate: passed
```

### 4.7 `reports/*.md`

Reports 是 review/presentation material，不是 canonical truth。固定三个报告：

```text
reports/extraction-report.md     # 抽取了什么、排除了什么、source 限制
reports/review-checklist.md      # 每个 object 的 schema/evidence/scope/dependency 检查
reports/closure-check.md         # open question、blocker、remaining risk 关闭情况
```

Reports 不能成为唯一保存规则逻辑、lookup fallback、权限结论或 signoff 的位置。

## 5. Layer 1.5：Canonical Signoff Snapshot

Snapshot 是 KB 和 runtime projection 之间的签核冻结物。它必须 write-once。

目录固定为：

```text
kb/{pack_id}/snapshots/{snapshot_id}/
  snapshot-manifest.json
  objects.jsonl
  dependency-edges.jsonl
  evidence-refs.jsonl
  review-decisions.jsonl
  human-confirmation-log.jsonl
  change-log.jsonl
  signoff.json
  projection-map.jsonl
```

Rules:

- `{snapshot_id}` 一旦签核，不允许原地覆盖。
- Snapshot 冻结对象语义，不冻结 YAML 空白、注释或编辑器格式。
- Runtime projection 必须能从 snapshot 证明每个 L2 object 的来源。
- Demo snapshot 可以使用 role/process identity；production snapshot 必须支持真实 actor 或批准 workflow identity。

### 5.1 `snapshot-manifest.json`

必须字段：

```json
{
  "snapshot_id": "snap-2026-05-21-fxd-ndf-v1",
  "schema_version": "canonical-snapshot-v1",
  "release_tier": "production",
  "pack_id": "tradition-to-stella-fxd-ndf-cutoff-fixing-split",
  "source_bundle_id": "src-tradition-stella-fxd-ndf-2026-05-21",
  "canonical_revision": "photo-reconstructed-2026-05-06",
  "generated_at": "2026-05-21T00:00:00Z",
  "generated_by": "publisher",
  "normalization_profile": "rts-canonical-json-v1",
  "normalization_version": "1.0.0",
  "hash_algorithm": "sha256",
  "source_repo_ref": "git:abc123",
  "source_authoring_refs": [
    {
      "path": "rules/rule_fxd_ndf_primary_rate_source.yaml",
      "content_hash": "sha256:..."
    }
  ],
  "object_count": 8,
  "blocking_issues_count": 0,
  "snapshot_hash": "sha256:...",
  "content_hash_summary": "sha256:..."
}
```

### 5.2 `objects.jsonl`

每行一个规范化后的 rule、lookup 或 helper object。必须字段：

```text
snapshot_id
schema_version
object_id
object_type
scope
lifecycle_state
revision
signoff_status
content_hash
source_bundle_id
source_authoring_ref
source_anchors
evidence_refs
review_decision_refs
logic / lookup / helper structured body
dependencies
release_eligibility
```

`content_hash` 基于 canonical normalized JSON 计算，不基于 YAML 源文本。

### 5.3 `dependency-edges.jsonl`

每行一条对象依赖。必须字段：

```text
edge_id
snapshot_id
from_object_id
from_object_type
to_object_id
to_object_type
dependency_type
field_path
required_for_runtime
content_hash
```

### 5.4 `evidence-refs.jsonl`

每行一个规范化证据引用。必须字段：

```text
evidence_id
snapshot_id
object_ids
source_bundle_id
source_ids
locator
quote_or_summary
permission_state
redaction_state
confidence
content_hash
```

### 5.5 `review-decisions.jsonl`

每行一个 review/adjudication decision。必须字段：

```text
decision_id
snapshot_id
object_ids
decision_type
status
question_or_conflict
decision
decided_by
decided_at
evidence_refs
review_gate_id
policy_version
validation_run_id
remaining_risk
production_gate
supersession_or_revocation
```

### 5.6 `human-confirmation-log.jsonl`

这不是聊天历史，而是影响 truth 的结构化确认日志。必须字段：

```text
confirmation_id
snapshot_id
object_ids
prompt_or_question
prompt_hash
confirmed_value
confirmation_type
confirmed_by
confirmed_at
evidence_refs
applies_to_revision
applies_to_snapshot_id
allowed_confirmation_type
```

### 5.7 `change-log.jsonl`

每行一个从上一 revision 或上一 snapshot 到当前 snapshot 的变化。必须字段：

```text
change_id
snapshot_id
object_id
previous_revision
new_revision
previous_content_hash
new_content_hash
change_type
change_summary
changed_by
changed_at
review_decision_refs
```

### 5.8 `signoff.json`

必须字段：

```json
{
  "signoff_id": "signoff-2026-05-21-fxd-ndf-v1",
  "snapshot_id": "snap-2026-05-21-fxd-ndf-v1",
  "snapshot_hash": "sha256:...",
  "status": "signed_off",
  "release_tier": "production",
  "policy_version": "signoff-policy-v1",
  "validation_run_id": "validation-2026-05-21-001",
  "signed_off_by": {
    "actor_type": "human",
    "actor_id": "reviewer-or-approved-workflow",
    "role": "domain_reviewer"
  },
  "signed_off_at": "2026-05-21T00:00:00Z",
  "signature_ref": "sigstore-or-local-signature-ref",
  "included_object_hashes": [
    {
      "object_id": "rule_fxd_ndf_primary_rate_source",
      "revision": 2,
      "content_hash": "sha256:..."
    }
  ],
  "release_eligibility": {
    "eligible_for_runtime_projection": true,
    "blocking_issues_count": 0,
    "remaining_risk": [],
    "reopened_ambiguities": [],
    "production_gate": "passed"
  }
}
```

### 5.9 `projection-map.jsonl`

这是 snapshot 到 runtime projection 的证明链。每行一个 projected object。必须字段：

```text
release_id
snapshot_id
snapshot_object_id
snapshot_object_hash
runtime_uri
runtime_l2_storage_ref
runtime_l2_hash
projection_transform_profile
field_mapping_version
```

Auditor 必须能回答：

- 哪个 signed snapshot object 生成了这个 L2 object。
- signoff 中记录的 object hash 是否仍匹配。
- 用了哪个 transform profile。
- runtime L2 hash 是否与 object manifest 和 content ref 匹配。

## 6. Canonicalization 和 hash profile

Snapshot hash 使用 `rts-canonical-json-v1`：

- JSON canonicalization 优先遵循 RFC 8785 JSON Canonicalization Scheme。
- 所有 JSON/JSONL 必须是 UTF-8。
- Object keys 按 Unicode code point 词典序排序。
- canonical hash input 不包含无意义空白。
- 字符串不被 publisher 悄悄改写业务含义。
- 业务代码、货币对、XPath、account-like 值必须保持 string。
- omitted、null、空数组、空对象不等价，除非 schema 明确。
- 数组保留语义顺序；set-like 字段必须定义排序 key。
- JSONL 文件 hash 基于逐行 canonical object，按文件稳定 key 排序，LF 连接并带 trailing LF。
- CSV/TSV attachment 按声明的 normalization profile hash；没有 profile 时按 raw UTF-8 bytes hash。
- Hash domain 必须显式：`snapshot_object`、`snapshot_manifest`、`signoff`、`runtime_l2`、`runtime_manifest`、`index_export`。

当前 Java runtime 已验证 L2 UTF-8 文件 hash；production publisher 还需要补 canonical JSON hashing 和 golden tests。

## 7. Layer 2：Runtime Projection Release Package

Runtime projection 是 RTS service truth。运行时查询、managed LLM、MCP/tools、index builder 默认只读这一层。

目录固定为：

```text
runtime-store/
  active-release.json
  releases/{release_id}/
    release-manifest.json
    scopes.jsonl
    object-manifest.jsonl
    caller-profiles.jsonl
    l2/rules/{rule_id}.json
    l2/lookups/{lookup_id}.json
    l2/helpers/{helper_id}.json
    navigation/object-cards.jsonl
    navigation/l0-l1-views.jsonl
    navigation/aliases.jsonl
    navigation/confusables.jsonl
    dependencies/dependency-edges.jsonl
    dependencies/field-bindings.jsonl
    governance/governance-access-refs.jsonl
    governance/evidence-summaries/{object_or_pack_id}.json
    governance/review-summaries/{object_or_pack_id}.json
    governance/report-summaries/{pack_or_release_id}.json
    index-artifacts/opensearch-docs.jsonl
    index-artifacts/lucene/
```

### 7.1 `release-manifest.json`

新 production release 必须包含 snapshot provenance：

```json
{
  "release_id": "rel-2026-05-21-fxd-ndf-v1",
  "projection_schema_version": "runtime-multiview-v1",
  "activation_state": "active",
  "release_tier": "production",
  "canonical_revision": "photo-reconstructed-2026-05-06",
  "canonical_snapshot_id": "snap-2026-05-21-fxd-ndf-v1",
  "canonical_snapshot_hash": "sha256:...",
  "signoff_id": "signoff-2026-05-21-fxd-ndf-v1",
  "generated_at": "2026-05-21T00:00:00Z",
  "released_at": "2026-05-21T00:01:00Z",
  "content_hash_summary": "sha256:...",
  "blocking_issues_count": 0,
  "rollback_target_release_id": null
}
```

Compatibility:

- Existing demo releases may keep `canonical_revision` only.
- New demo releases should include snapshot metadata when available.
- New production releases must include `canonical_snapshot_id`、`canonical_snapshot_hash`、`signoff_id`。

### 7.2 `scopes.jsonl`

每行一个 scope。必须字段：

```text
release_id
scope_id
source_system
target_system
product_scope
domain
target_area
activation_state
permission_profile
```

### 7.3 `object-manifest.jsonl`

每行一个 runtime object。必须字段：

```text
release_id
uri
object_id
object_type
scope_id
l2_storage_ref
l2_content_hash
snapshot_id
snapshot_object_id
snapshot_object_hash
activation_state
truth_role
view_role
governance_ref
dependency_refs
```

### 7.4 `l2/**/*.json`

L2 是服务读取的事实对象。必须字段：

```text
schema_version
release_id
uri
object_id
object_type
scope
truth_role
snapshot_id
snapshot_object_id
snapshot_object_hash
content_hash
logic / lookup / helper body
dependencies
evidence_summary_refs
review_summary_refs
warnings
```

L2 可以为了服务读取重排结构，但不能新增 snapshot 没有的业务断言，除非 `projection_transform_profile` 和 `projection-map.jsonl` 明确说明。

### 7.5 Navigation、dependency、governance

这些文件是 projection 内的受控视图：

```text
navigation/object-cards.jsonl         # object 摘要卡，给搜索和 UI 定位
navigation/l0-l1-views.jsonl          # L0/L1 聚合视图，不能单独当 truth
navigation/aliases.jsonl              # alias 和同义定位
navigation/confusables.jsonl          # 易混淆对象和拒答提示
dependencies/dependency-edges.jsonl   # runtime dependency graph
dependencies/field-bindings.jsonl     # source/target 字段绑定
governance/governance-access-refs.jsonl
governance/*-summaries/*.json         # release-bound, permission-filtered summaries
```

Governance summary 必须 release-bound、permission-filtered、traceable。它不能从 `kb/evidence`、`kb/review`、raw reports 或 chat logs 旁路读取。

### 7.6 Runtime immutability

Production publisher 必须拒绝覆盖已有 `runtime-store/releases/{release_id}/`。修正任何规则事实都必须生成新 release id，再切换 `active-release.json`。

Lucene 这类二进制索引可以重建；它不是 truth。

## 8. Layer 3：Derived Index / View / LLM Context

Layer 3 只能从 runtime projection 或 release-bound governance summaries 派生。

### 8.1 Index layer

固定输出：

```text
runtime-store/releases/{release_id}/index-artifacts/
  opensearch-docs.jsonl
  lucene/
```

Index 可以包含：

```text
uri
object_id
object_type
scope
title
summary
aliases
target_paths
source_anchors
dependency_refs
release_id
content_hash
truth_role
view_role
```

Index 禁止包含 projection 中不存在的业务断言。

### 8.2 HTML / Markdown views

HTML 和 Markdown 是 presentation/review view。允许展示：

- file tree
- object cards
- warnings
- evidence summary
- review summary
- rejection gate result
- query walkthrough

禁止：

- 被 runtime service 当 truth 读取
- 反向覆盖 KB object
- 保存唯一一份规则逻辑

### 8.3 LLM Input Context Envelope

LLM context envelope 是请求时由 service 装箱的输入，不是 stored truth layer。

每个 context item 必须包含：

```text
context_item_id
source_uri
content_hash
truth_role or view_role
truth_eligible
redaction_state
permission_purpose
tool_call_id or trace_id
release_id
scope
```

Allowed default truth inputs:

- active runtime projection manifest
- scopes
- object manifest
- L2 objects
- dependency and field binding views
- permissioned governance summaries

Disallowed as truth:

- search hit alone
- L0/L1 alone
- HTML
- report prose alone
- LLM memory
- raw chat transcript
- Git working tree
- unsigned snapshot
- unsigned KB authoring file

## 9. Reader Matrix

| Reader | 可直接读取 | 不可直接读取为 truth |
|---|---|---|
| Source extractor | source bundle、raw/normalized material | runtime projection as authoring truth |
| KB authoring agent | source bundle、KB YAML/Markdown、evidence/review/report | active service trace as KB truth |
| Review agent | KB、evidence、review、reports、snapshot staging | raw chat transcript as final truth |
| Publisher | source bundle、validated KB、snapshot staging、signed snapshot | unstructured conversation-only signoff |
| Release audit | signed snapshot、release manifest、projection-map、authorized governance summaries | unsigned snapshot、raw evidence without permission |
| RTS runtime service | active runtime projection | KB authoring、snapshot、source bundle |
| Managed LLM harness | service tools returned facts、L2 fields、dependency facts、authorized governance summaries | raw KB、raw snapshot、raw evidence |
| External agent/tool mode | REST/MCP/tool responses | local projection files、KB files、snapshot files |
| Index builder | projection manifest、object manifest、cards、L0/L1、aliases、dependency views | raw KB、snapshot、source bundle |
| HTML/doc generator | projection summaries、authorized governance summaries、presentation reports | HTML itself as truth |

## 10. Publisher Contract

Publisher 是唯一允许从 KB 进入 snapshot、再进入 runtime projection 的流程。

Publisher input:

```text
sources/{source_bundle_id}/source-manifest.yaml
sources/{source_bundle_id}/source-index.yaml
kb/{pack_id}/metadata.yaml
kb/{pack_id}/rules/*.yaml
kb/{pack_id}/lookups/*.yaml
kb/{pack_id}/helpers/*.yaml
kb/{pack_id}/evidence/evidence-index.yaml
kb/{pack_id}/review/review-index.yaml
kb/{pack_id}/reports/*.md
```

Publisher output:

```text
kb/{pack_id}/snapshots/{snapshot_id}/snapshot-manifest.json
kb/{pack_id}/snapshots/{snapshot_id}/objects.jsonl
kb/{pack_id}/snapshots/{snapshot_id}/dependency-edges.jsonl
kb/{pack_id}/snapshots/{snapshot_id}/evidence-refs.jsonl
kb/{pack_id}/snapshots/{snapshot_id}/review-decisions.jsonl
kb/{pack_id}/snapshots/{snapshot_id}/human-confirmation-log.jsonl
kb/{pack_id}/snapshots/{snapshot_id}/change-log.jsonl
kb/{pack_id}/snapshots/{snapshot_id}/signoff.json
kb/{pack_id}/snapshots/{snapshot_id}/projection-map.jsonl
runtime-store/releases/{release_id}/...
runtime-store/releases/{release_id}/index-artifacts/...
```

Publisher 必须执行：

1. schema validation
2. source locator resolution
3. evidence/ref resolution
4. dependency graph validation
5. canonical normalization
6. canonical hash generation
7. signoff coverage check
8. production gate check
9. runtime projection generation
10. L2 hash validation
11. projection-map validation
12. service smoke tests

## 11. Acceptance / Rejection Gates

Production runtime release 只有满足以下条件才可接受：

1. Source bundle 有 stable locator、revision、hash 或明确的 hash exception。
2. KB authoring validation passes。
3. Evidence refs、review refs、source anchors 全部可解析。
4. Canonical signoff snapshot validation passes。
5. Snapshot canonical hash 可复现。
6. `signoff.json` 存在、已签核、覆盖所有 projected objects。
7. `release_tier` 是 production，production gate passed。
8. `blocking_issues_count` 是 0。
9. 每个 released object 有 deterministic content hash。
10. Runtime projection 引用 source snapshot id、snapshot hash、signoff id。
11. `projection-map.jsonl` 存在且覆盖所有 projected L2 objects。
12. Projection validation passes。
13. Query smoke test 证明 L2 read、hash validation、dependency traversal、answer assembly、trace。

Production 必须拒绝：

- process-only signoff
- missing `source_bundle_id`
- missing `canonical_snapshot_id`
- missing `canonical_snapshot_hash`
- missing `signoff_id`
- unsigned or mutable snapshot
- object projected without snapshot object hash
- runtime L2 content diverges without transform profile
- human confirmation without actor identity and object revision
- authorized governance material accessed through a non-release-bound side path
- service reads KB/snapshot/source bundle as default query truth

Demo packs may relax actor identity completeness, but不能放松 content hash、scope、L2、projection validation 和 trace。Demo artifacts 必须携带 `release_tier: "demo"`，不能通过 production release gate。

## 12. 文件格式选择

| 文件/目录 | 固定格式 | 原因 |
|---|---|---|
| `sources/**/source-manifest.yaml` | YAML | source capture 和权限字段需要易读、易 review |
| `sources/**/source-index.yaml` | YAML | locator 需要人工和 agent 都好改 |
| `sources/**/raw` | 原格式 | source 证据不强行转码 |
| `sources/**/normalized` | CSV/TSV/JSON/text | 为 extraction 和 hash 服务 |
| `kb/**/metadata.yaml` | YAML | pack 入口、scope 和索引适合 authoring |
| `kb/**/rules/*.yaml` | YAML | 规则逻辑需要 readable diff 和 review 注释 |
| `kb/**/lookups/*.yaml` | YAML + CSV/TSV attachments | 小表可内嵌，大表独立 |
| `kb/**/helpers/*.yaml` | YAML | helper 逻辑需要结构化但可读 |
| `kb/**/evidence/*.yaml` | YAML | 证据 locator 和 summary 需要 review |
| `kb/**/review/*.yaml` | YAML | 审核结论需要可读且可结构化 |
| `kb/**/reports/*.md` | Markdown | 报告和 checklist 是说明层 |
| `kb/**/snapshots/*.json` | JSON | 签核 manifest/signoff 需要 deterministic schema/hash |
| `kb/**/snapshots/*.jsonl` | JSONL | 对象、边、证据、review、change log 是 line records |
| `runtime-store/**/release-manifest.json` | JSON | service manifest |
| `runtime-store/**/object-manifest.jsonl` | JSONL | object registry |
| `runtime-store/**/l2/**/*.json` | JSON | service truth object |
| `runtime-store/**/navigation/*.jsonl` | JSONL | rebuildable navigation views |
| `runtime-store/**/dependencies/*.jsonl` | JSONL | graph/index friendly |
| `runtime-store/**/governance/*.json(l)` | JSON/JSONL | release-bound permissioned summaries |
| `runtime-store/**/index-artifacts/opensearch-docs.jsonl` | JSONL | portable search export |
| `runtime-store/**/index-artifacts/lucene/` | Lucene binary | rebuildable service index |
| `html/*.html` | HTML | presentation only |

## 13. 与当前代码的差距

当前实现已经支持：

- `FileSystemProjectionStore` 从 `runtime-store/active-release.json` 读取 active release。
- 读取和验证 `release-manifest.json`、`scopes.jsonl`、`object-manifest.jsonl`、`l2/**/*.json`、navigation、dependencies、governance refs。
- 校验 L2 文件 hash 和 projection 基本状态。
- QueryService 通过 runtime projection 提供受控读取。

当前仍需补齐：

- `sources/{source_bundle_id}/` 生成器模板和 validator。
- `kb/**/snapshots/**` canonical publisher。
- Canonical JSON hash golden tests。
- `ReleaseManifest` 增加 `releaseTier`、`canonicalSnapshotId`、`canonicalSnapshotHash`、`signoffId`。
- `FileSystemProjectionStore` 增加 production provenance validation。
- Production write-once publish gate。
- Snapshot-to-projection `projection-map.jsonl` validator。
- LLM context item role/hash/permission contract enforcement。

Migration rule:

1. Existing demo releases may remain compatible with `canonical_revision` only.
2. New demo releases should include source bundle and snapshot metadata when available.
3. New production releases must include source bundle, canonical signoff snapshot, signoff proof and snapshot-to-projection proof chain.
4. Code changes must add schema validation、canonical hash tests、manifest provenance fields、immutable publish gate before production enforcement.

## 14. Rejected Alternatives

| 方案 | 结论 |
|---|---|
| Pure JSON KB authoring | Rejected。JSON 用于 snapshot/runtime/index export，不作为默认 authoring surface。 |
| YAML-only canonical truth | Rejected。YAML 用于 authoring，signoff freeze 必须 canonical JSON/JSONL。 |
| DB as canonical truth | Rejected for v1。DB 可作为 derived query backend，必须可从 signed snapshot/projection 重建。 |
| HTML/report as truth | Rejected。HTML/report 只能展示、review、解释。 |
| Human confirmation only in conversation | Rejected。影响 truth 的确认必须进入 structured decision/confirmation records。 |
| Runtime reads KB directly | Rejected。service truth 是 active runtime projection。 |

## 15. 最短版

未来生成器只要记住这一版：

```text
1. 把 source 固定成 sources/{source_bundle_id}/，只做证据入口和 locator。
2. 把规则写成 kb/{pack_id}/ 的 YAML/Markdown authoring package。
3. 发布前生成 kb/{pack_id}/snapshots/{snapshot_id}/ 的 canonical JSON/JSONL signoff snapshot。
4. 从 signed snapshot 生成 runtime-store/releases/{release_id}/ 的 immutable JSON/JSONL runtime projection。
5. Index、HTML、LLM context 只能从 runtime projection 或 release-bound governance summaries 派生，不能拥有 truth。
```
